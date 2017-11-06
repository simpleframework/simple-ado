package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.JdbcUtils;
import net.simpleframework.ado.db.jdbc.dialect.IJdbcDialect;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class BeanWrapper<T> extends ObjectEx {

	private final List<PropertyCache> properties = new ArrayList<>();

	private final Class<T> beanClass;

	public BeanWrapper(final String[] columns, final Class<T> beanClass) {
		this.beanClass = beanClass;

		for (final String field : BeanUtils.getProperties(beanClass).keySet()) {
			final DbTableColumn col = DbTableColumn.getTableColumns(beanClass).get(field);
			if (col == null) {
				continue;
			}
			if (columns != null && columns.length > 0) {
				boolean find = false;
				for (final String column : columns) {
					if (col.getName().equals(column) || col.getAlias().equals(column)) {
						find = true;
						break;
					}
				}
				if (!find) {
					continue;
				}
			}
			final PropertyCache cache = new PropertyCache();
			cache.propertyName = field;
			cache.dbColumn = col;
			properties.add(cache);
		}
	}

	public Class<T> getBeanClass() {
		return beanClass;
	}

	public int getPropertiesCount(final T bean) {
		int size = properties.size();
		if (bean instanceof ObjectEx) {
			size += getPropertiesExt((ObjectEx) bean).size();
		}
		return size;
	}

	public void setPropertiesExt(final T bean, final IJdbcProvider jdbcProvider, final ResultSet rs)
			throws SQLException {
		if (!(bean instanceof ObjectEx)) {
			return;
		}
		// 扩展属性每次更新
		final ObjectEx _bean = (ObjectEx) bean;
		final Set<String> set = getPropertiesExt(_bean);
		if (set.size() == 0) {
			return;
		}
		final IJdbcDialect dialect = jdbcProvider.getJdbcDialect();
		final ResultSetMetaData rsmd = rs.getMetaData();
		for (final String k : set) {
			final int iCol = JdbcUtils.lookupColumnIndex(rsmd, k);
			if (iCol > 0) {
				final Object val2 = _bean.getAttr(k);
				final Object val = dialect.getResultSetValue(rs, iCol,
						val2 != null ? val2.getClass() : null);
				if (!ObjectUtils.objectEquals(val2, val)) {
					_bean.setAttr(k, val);
				}
			}
		}
	}

	public T toBean(final IJdbcProvider jdbcProvider, final ResultSet rs) throws SQLException {
		T bean = null;
		try {
			bean = beanClass.newInstance();
		} catch (final Exception e) {
			getLog().error(e);
			return null;
		}

		final IJdbcDialect dialect = jdbcProvider.getJdbcDialect();
		final ResultSetMetaData rsmd = rs.getMetaData();

		int[] _indexs = null;
		final int c = rsmd.getColumnCount();
		if (c > properties.size() && bean instanceof ObjectEx) {
			_indexs = new int[c];
		}

		for (final PropertyCache cache : properties) {
			if (cache.sqlColumnIndex <= 0) {
				final int sqlColumnIndex = JdbcUtils.lookupColumnIndex(rsmd, cache.dbColumn.getName());
				if (sqlColumnIndex <= 0) {
					continue;
				} else {
					cache.sqlColumnIndex = sqlColumnIndex;
					if (_indexs != null) {
						_indexs[sqlColumnIndex - 1] = 1; // 已使用
					}
				}
			} else if (_indexs != null) {
				_indexs[cache.sqlColumnIndex - 1] = 1; // 已使用
			}

			final Object val = dialect.getResultSetValue(rs, cache.sqlColumnIndex,
					cache.dbColumn.getPropertyClass());
			if (val != null) {
				BeanUtils.setProperty(bean, cache.propertyName, val);
			}
		}

		if (_indexs != null) {
			for (int i = 1; i <= _indexs.length; i++) {
				if (_indexs[i - 1] == 0) { // 未使用
					final ObjectEx _bean = (ObjectEx) bean;
					final String k = JdbcUtils.lookupColumnName(rsmd, i).toLowerCase();
					_bean.setAttr(k, dialect.getResultSetValue(rs, i));
					getPropertiesExt(_bean).add(k);
				}
			}
		}
		return bean;
	}

	private Set<String> getPropertiesExt(final ObjectEx bean) {
		@SuppressWarnings("unchecked")
		Set<String> set = (Set<String>) bean.getAttr("@getPropertiesExt");
		if (set == null) {
			bean.setAttr("@getPropertiesExt", set = new HashSet<>());
		}
		return set;
	}

	private class PropertyCache {
		String propertyName;

		DbTableColumn dbColumn;

		int sqlColumnIndex;
	}
}
