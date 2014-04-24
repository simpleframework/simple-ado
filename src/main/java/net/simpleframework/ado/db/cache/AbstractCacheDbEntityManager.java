package net.simpleframework.ado.db.cache;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.ado.db.BeanWrapper;
import net.simpleframework.ado.db.DbDataQuery;
import net.simpleframework.ado.db.DbEntityManager;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.IDbDataQuery;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.event.IDbEntityListener;
import net.simpleframework.ado.db.event.IDbListener;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.common.BeanUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
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
		try {
			return super.update(l, columns, objects);
		} finally {
			// 同一个bean，由于条件不同，可能有多个key，当更新时，直接从缓存删掉(更好办法？)
			for (final T t : objects) {
				removeCache(toUniqueString(t));
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
}
