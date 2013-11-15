package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

import net.simpleframework.ado.db.common.TableColumn;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.JdbcUtils;
import net.simpleframework.common.BeanUtils;
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
			final TableColumn col = TableColumn.getTableColumns(beanClass).get(field);
			if (col == null) {
				continue;
			}
			if (columns != null && columns.length > 0) {
				boolean find = false;
				for (final String column : columns) {
					if (col.getName().equals(column) || col.getSqlName().equals(column)) {
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

	public T toBean(final IJdbcProvider jdbcProvider, final ResultSet rs) throws SQLException {
		T bean = null;
		try {
			bean = beanClass.newInstance();
		} catch (final Exception e) {
			log.error(e);
			return null;
		}

		for (final PropertyCache cache : collection) {
			if (cache.sqlColumnIndex <= 0) {
				final int sqlColumnIndex = JdbcUtils.lookupColumnIndex(rs.getMetaData(),
						cache.dbColumn.getName());
				if (sqlColumnIndex <= 0) {
					continue;
				} else {
					cache.sqlColumnIndex = sqlColumnIndex;
				}
			}

			final Class<?> propertyType = cache.dbColumn.getPropertyClass();
			final Object object = jdbcProvider.getJdbcDialect().getResultSetValue(rs,
					cache.sqlColumnIndex, propertyType);
			if (object == null) {
				continue;
			}
			BeanUtils.setProperty(bean, cache.propertyName, object);
		}
		return bean;
	}

	private class PropertyCache {
		String propertyName;

		TableColumn dbColumn;

		int sqlColumnIndex;
	}
}
