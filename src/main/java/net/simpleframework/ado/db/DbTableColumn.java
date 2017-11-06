package net.simpleframework.ado.db;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.ColumnData;
import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.BeanUtils.PropertyWrapper;
import net.simpleframework.common.I18n;
import net.simpleframework.common.StringUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DbTableColumn extends ColumnData {

	private DbEntityTable dbTable;

	public DbTableColumn(final String name) {
		super(name, (String) null);
	}

	public DbTableColumn(final String name, final String text) {
		super(name, text, null);
	}

	public DbEntityTable getTable() {
		return dbTable;
	}

	public void setTable(final DbEntityTable dbTable) {
		this.dbTable = dbTable;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		final DbEntityTable dbTable = getTable();
		if (dbTable != null) {
			sb.append(dbTable.getName()).append(".");
		}
		sb.append(getAlias());
		return sb.toString();
	}

	private final static Map<Class<?>, Map<String, DbTableColumn>> columnsCache;
	static {
		columnsCache = new ConcurrentHashMap<>();
	}

	public static Map<String, DbTableColumn> getTableColumns(final Class<?> beanClass) {
		Map<String, DbTableColumn> data = columnsCache.get(beanClass);
		if (data != null) {
			return data;
		}
		data = new HashMap<>();
		for (final PropertyWrapper pw : BeanUtils.getProperties(beanClass).values()) {
			ColumnMeta meta = null;
			if (pw.field != null) {
				meta = pw.field.getAnnotation(ColumnMeta.class);
			}
			if (meta == null && pw.getter != null) {
				meta = pw.getter.getAnnotation(ColumnMeta.class);
			}
			if (meta != null && meta.ignore()) {
				continue;
			}

			final DbTableColumn col = new DbTableColumn(pw.name);
			col.setPropertyClass(pw.type);
			if (meta != null) {
				String columnText, columnAlias;
				if (StringUtils.hasText(columnText = meta.columnText())) {
					col.setText(I18n.replaceI18n(columnText));
				}
				if (StringUtils.hasText(columnAlias = meta.columnAlias())) {
					col.setAlias(columnAlias);
				}
			}
			data.put(pw.name, col);
		}
		columnsCache.put(beanClass, data);
		return data;
	}

	private static final long serialVersionUID = -241399268622218668L;
}
