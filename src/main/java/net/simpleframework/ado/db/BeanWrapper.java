package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.JdbcUtils;
import net.simpleframework.ado.db.jdbc.dialect.IJdbcDialect;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class BeanWrapper<T> extends ObjectEx {

	private final Collection<PropertyCache> collection;

	private final Class<T> beanClass;

	public BeanWrapper(final String[] columns, final Class<T> beanClass) {
		this.beanClass = beanClass;
		final Collection<String> fields = BeanUtils.getProperties(beanClass).keySet();
		collection = new ArrayList<PropertyCache>(fields.size());

		for (final String field : fields) {
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
			collection.add(cache);
		}
	}

	public Class<T> getBeanClass() {
		return beanClass;
	}

	public Collection<PropertyCache> getCollection() {
		return collection;
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
		if (c > collection.size() && bean instanceof ObjectEx) {
			_indexs = new int[c];
		}

		for (final PropertyCache cache : collection) {
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
			}

			final Class<?> propertyType = cache.dbColumn.getPropertyClass();
			final Object val = dialect.getResultSetValue(rs, cache.sqlColumnIndex, propertyType);
			if (val == null) {
				continue;
			}
			BeanUtils.setProperty(bean, cache.propertyName, val);
		}

		if (_indexs != null) {
			for (int i = 1; i <= _indexs.length; i++) {
				if (_indexs[i - 1] == 0) { // 未使用
					final Object val = dialect.getResultSetValue(rs, i);
					String k;
					if (val != null && StringUtils.hasText(k = JdbcUtils.lookupColumnName(rsmd, i))) {
						((ObjectEx) bean).setAttr(k.toLowerCase(), val);
					}
				}
			}
		}
		return bean;
	}

	private class PropertyCache {
		String propertyName;

		DbTableColumn dbColumn;

		int sqlColumnIndex;
	}
}
