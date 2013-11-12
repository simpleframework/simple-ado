package net.simpleframework.ado.db.cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.ado.db.BeanWrapper;
import net.simpleframework.ado.db.DbDataQuery;
import net.simpleframework.ado.db.DbEntityManager;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.IDbDataQuery;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.common.TableColumn;
import net.simpleframework.ado.db.event.IDbEntityListener;
import net.simpleframework.ado.db.event.IDbListener;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.IJdbcTransactionEvent;
import net.simpleframework.ado.trans.ITransactionCallback;
import net.simpleframework.common.BeanUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class AbstractCacheDbEntityManager<T> extends DbEntityManager<T> implements
		IDbEntityCache {
	public AbstractCacheDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
	}

	protected String toUniqueString(final Object object) {
		if (object == null) {
			return null;
		}
		final DbEntityTable eTable = getEntityTable();
		if (eTable.isNoCache()) {
			return null;
		}

		final StringBuilder sb = new StringBuilder();
		if (object instanceof SQLValue) {
			sb.append(((SQLValue) object).getKey());
		} else {
			sb.append(eTable.getName());
			if (object instanceof IParamsValue) {
				sb.append("-").append(((IParamsValue) object).getKey());
			} else {
				for (final String uniqueColumn : eTable.getUniqueColumns()) {
					sb.append("-");
					try {
						Object o;
						if (object instanceof ResultSet) {
							o = ((ResultSet) object).getObject(eTable.getSqlName(uniqueColumn));
						} else {
							o = BeanUtils.getProperty(object, eTable.getBeanPropertyName(uniqueColumn));
						}
						sb.append(AbstractParamsValue.valueToString(o));
					} catch (final Exception e) {
						return null;
					}
				}
			}
		}
		return sb.toString();
	}

	@Override
	protected int delete(final IDbEntityListener l, final IParamsValue paramsValue) {
		try {
			return super.delete(l, paramsValue);
		} finally {
			reset();
		}
	}

	@Override
	protected int update(final IDbEntityListener l, final String[] columns, final T... objects) {
		doUpdateObjects(objects);
		return super.update(l, columns, objects);
	}

	protected void doUpdateObjects(final Object... objects) {
		if (objects == null) {
			return;
		}
		final Map<String, IDbEntityCache> updates = entityCache.get();
		if (updates != null) {
			for (final Object object : objects) {
				final String key = toUniqueString(object);
				if (key != null) {
					updates.put(key, this);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public T queryForBean(final String[] columns, final IParamsValue paramsValue) {
		final String key = toUniqueString(paramsValue);
		if (key == null) {
			return super.queryForBean(columns, paramsValue);
		}
		T t = (T) getCache(key);
		if (t == null || t instanceof Map) {
			// 此处传递null，而非columns，目的是在缓存情况下，忽略columns参数
			// 以后再考虑更好的方法
			if ((t = super.queryForBean(null/* columns */, paramsValue)) != null) {
				putCache(key, t);
			}
		} else {
			doUpdateObjects(t);
		}
		return t;
	}

	@Override
	public IDbDataQuery<T> queryBeans(final String[] columns, final IParamsValue paramsValue) {
		if (getEntityTable().isNoCache()) {
			return super.queryBeans(columns, paramsValue);
		} else {
			final BeanWrapper<T> wrapper = new BeanWrapper<T>(columns, getBeanClass());
			final IJdbcProvider jdbcProvider = getJdbcProvider();
			return new DbDataQuery<T>(jdbcProvider, createSQLValue(null /* columns */, paramsValue)) {
				@SuppressWarnings("unchecked")
				@Override
				public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
					final String key = toUniqueString(rs);
					if (key == null) {
						return wrapper.toBean(jdbcProvider, rs);
					}
					T t = (T) getCache(key);
					if (t == null || t instanceof Map) {
						if ((t = wrapper.toBean(jdbcProvider, rs)) != null) {
							putCache(key, t);
						}
					}
					return t;
				}
			};
		}
	}

	/************************* Map Object ************************/

	@SuppressWarnings("unchecked")
	@Override
	public Map<String, Object> queryForMap(final String[] columns, final IParamsValue paramsValue) {
		final String key = toUniqueString(paramsValue);
		if (key == null) {
			return super.queryForMap(columns, paramsValue);
		}
		Object data = getCache(key);
		if (!(data instanceof Map)) {
			if ((data = super.queryForMap(columns, paramsValue)) != null) {
				putCache(key, data);
			}
		} else {
			if (columns != null && columns.length > 0
					&& !((Map<String, Object>) data).containsKey(columns[0])) {
				final Map<String, Object> nData = super.queryForMap(columns, paramsValue);
				if (nData != null) {
					((Map<String, Object>) data).putAll(nData);
				}
			}
			doUpdateObjects(data);
		}
		return (Map<String, Object>) data;
	}

	@Override
	public IDbDataQuery<Map<String, Object>> queryMapSet(final String[] columns,
			final IParamsValue paramsValue) {
		if (getEntityTable().isNoCache()) {
			return super.queryMapSet(columns, paramsValue);
		} else {
			return new DbDataQuery<Map<String, Object>>(getJdbcProvider(), createSQLValue(columns,
					paramsValue)) {

				@SuppressWarnings("unchecked")
				@Override
				public Map<String, Object> mapRow(final ResultSet rs, final int rowNum)
						throws SQLException {
					final String key = toUniqueString(rs);
					if (key == null) {
						return mapRowData(columns, rs);
					}
					Object data = getCache(key);
					if (!(data instanceof Map)) {
						if ((data = mapRowData(columns, rs)) != null) {
							putCache(key, data);
						}
					} else if (columns != null && columns.length > 0
							&& !((Map<String, Object>) data).containsKey(columns[0])) {
						final Map<String, Object> nData = mapRowData(columns, rs);
						if (nData != null) {
							((Map<String, Object>) data).putAll(nData);
						}
					}
					return (Map<String, Object>) data;
				}
			};
		}
	}

	/************************* ope ************************/

	@Override
	public int execute(final IDbListener l, final SQLValue... sqlValues) {
		final int ret = super.execute(l, sqlValues);
		boolean delete = false;
		for (final SQLValue sqlVal : sqlValues) {
			if (sqlVal.getSql().trim().toLowerCase().startsWith("delete")) {
				delete = true;
				break;
			}
		}
		if (delete) {
			// 此处暂时重置管理器
			reset();
		}
		return ret;
	}

	@Override
	public Object exchange(final Object o1, final Object o2, final TableColumn order,
			final boolean up) {
		final long[] ret = (long[]) super.exchange(o1, o2, order, up);
		if (ret == null) {
			return null;
		}
		final String orderName = order.getSqlName();
		final StringBuilder sb = new StringBuilder();
		sb.append(orderName).append(">=? and ").append(orderName).append("<=?");
		try {
			final IDbDataQuery<Map<String, Object>> qs = queryMapSet(new ExpressionValue(
					sb.toString(), ret[0], ret[1]));
			Map<String, Object> mObj;
			while ((mObj = qs.next()) != null) {
				final String key = toUniqueString(mObj);
				if (key != null) {
					removeCache(key);
				}
			}
			return ret;
		} catch (final Throwable th) {
			reset();
			throw ADOException.of(th);
		}
	}

	/************************* transaction ************************/
	private static ThreadLocal<Map<String, IDbEntityCache>> entityCache;
	static {
		entityCache = new ThreadLocal<Map<String, IDbEntityCache>>();
	}

	private final IJdbcTransactionEvent transactionEvent = new IJdbcTransactionEvent() {
		@Override
		public void onExecute(final Connection connection) {
			entityCache.set(new HashMap<String, IDbEntityCache>());
		}

		@Override
		public void onThrowable(final Connection connection) {
			// 错误时，清除cache
			final Map<String, IDbEntityCache> cache = entityCache.get();
			if (cache != null) {
				for (final Map.Entry<String, IDbEntityCache> entry : cache.entrySet()) {
					entry.getValue().removeCache(entry.getKey());
				}
			}
		}

		@Override
		public void onFinally(final Connection connection) {
			entityCache.remove();
		}
	};

	@Override
	public <M> M doExecuteTransaction(final ITransactionCallback<M> callback) {
		return getJdbcProvider().doExecuteTransaction(callback, transactionEvent);
	}
}
