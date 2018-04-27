package net.simpleframework.ado.db.cache;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.ado.db.BeanWrapper;
import net.simpleframework.ado.db.DbDataQuery;
import net.simpleframework.ado.db.DbEntityManager;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.IDbDataQuery;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.event.IDbEntityListener;
import net.simpleframework.ado.db.event.IDbListener;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.IJdbcTransactionEvent.JdbcTransactionEvent;
import net.simpleframework.ado.db.jdbc.JdbcUtils;
import net.simpleframework.ado.query.DataQueryUtils;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.Convert;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractCacheDbEntityManager<T> extends DbEntityManager<T>
		implements IDbEntityCache {
	protected int maxCacheSize;
	{
		setMaxCacheSize(2000);
	}

	/* 缓存key->id */
	protected Map<String, Object> idCache;

	// 反向map，保存keys
	protected Map<String, Set<String>> keysCache;

	public AbstractCacheDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
		if (entityTable != null) {
			final int maxCacheSize = entityTable.getMaxCacheSize();
			if (maxCacheSize > 0) {
				setMaxCacheSize(maxCacheSize);
			}
		}
	}

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	@Override
	public void setMaxCacheSize(final int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	@Override
	public synchronized void reset() {
		idCache.clear();
	}

	@Override
	public void removeCache(final String key) {
		idCache.remove(key);
	}

	protected String getId(final Object val) {
		if (val == null) {
			return null;
		}
		Object id = null;
		if (val instanceof IIdBeanAware) {
			final ID _id = ((IIdBeanAware) val).getId();
			id = _id != null ? _id.getValue() : null;
		} else if (val instanceof Map) {
			id = ((Map<?, ?>) val).get("ID");
		} else {
			id = BeanUtils.getProperty(val, "id");
		}
		return id == null ? null : Convert.toString(id);
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
				for (final String uniqueColumn : eTable.getCacheColumns()) {
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
	protected int delete(final IDbEntityListener<T> l, final IParamsValue paramsValue) {
		final List<T> keys = DataQueryUtils.toList(queryBeans(paramsValue));
		try {
			return super.delete(l, paramsValue);
		} finally {
			for (final T t : keys) {
				if (getJdbcProvider().inTrans()) {
					final CacheTransactionEvent jEvent = JdbcUtils
							.addTransactionEvent(new CacheTransactionEvent());
					jEvent.addRobject(this, t);
				}
				removeVal(t);
			}
		}
	}

	@Override
	protected int update(final IDbEntityListener<T> l, final String[] columns, final T... objects) {
		try {
			return super.update(l, columns, objects);
		} finally {
			// 同一个bean，由于条件不同，可能有多个key，当更新时，直接从缓存删掉(更好办法？)
			for (final T t : objects) {
				if (getJdbcProvider().inTrans()) {
					final CacheTransactionEvent jEvent = JdbcUtils
							.addTransactionEvent(new CacheTransactionEvent());
					jEvent.addRobject(this, t);
				}
				removeVal(t);
			}
		}
	}

	protected class CacheTransactionEvent extends JdbcTransactionEvent {

		private final Map<AbstractCacheDbEntityManager<T>, List<T>> removes = new HashMap<>();

		public void addRobject(final AbstractCacheDbEntityManager<T> mgr, final T t) {
			List<T> list = removes.get(mgr);
			if (list == null) {
				removes.put(mgr, list = new ArrayList<>());
			}
			list.add(t);
		}

		@Override
		public void onFinally(final Connection connection) {
			for (final Map.Entry<AbstractCacheDbEntityManager<T>, List<T>> e : removes.entrySet()) {
				final AbstractCacheDbEntityManager<T> mgr = e.getKey();
				for (final T t : e.getValue()) {
					mgr.removeVal(t);
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
		}
		return t;
	}

	@Override
	public IDbDataQuery<T> queryBeans(final String[] columns, final IParamsValue paramsValue) {
		if (getEntityTable().isNoCache()) {
			return super.queryBeans(columns, paramsValue);
		} else {
			final BeanWrapper<T> wrapper = new BeanWrapper<>(columns, getBeanClass());
			final IJdbcProvider jdbcProvider = getJdbcProvider();
			return new DbDataQuery<T>(dbFactory, this,
					createSQLValue(null /* columns */, paramsValue)) {
				@SuppressWarnings("unchecked")
				@Override
				public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
					final String key = toUniqueString(rs);
					if (key == null) {
						return wrapper.toBean(jdbcProvider, rs);
					}
					T t = (T) getCache(key);

					if (t == null || t instanceof Map
							|| wrapper.getPropertiesCount(t) < rs.getMetaData().getColumnCount()) {
						if ((t = wrapper.toBean(jdbcProvider, rs)) != null) {
							putCache(key, t);
						}
					} else {
						// 扩展属性每次更新
						wrapper.setPropertiesExt(t, jdbcProvider, rs);
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
		}
		return (Map<String, Object>) data;
	}

	@Override
	public IDbDataQuery<Map<String, Object>> queryMapSet(final String[] columns,
			final IParamsValue paramsValue) {
		if (getEntityTable().isNoCache()) {
			return super.queryMapSet(columns, paramsValue);
		} else {
			return new DbDataQuery<Map<String, Object>>(dbFactory, this,
					createSQLValue(columns, paramsValue)) {
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

	protected void removeKeys(final String id) {
		// 删除id缓存
		final Set<String> keys = keysCache.remove(id);
		if (keys != null) {
			for (final String key : keys) {
				idCache.remove(key);
			}
		}
	}

	protected void putKeys(final String id, final String key) {
		Set<String> keys = keysCache.get(id);
		if (keys == null) {
			keysCache.put(id, keys = new HashSet<>());
		}
		keys.add(key);
	}
}
