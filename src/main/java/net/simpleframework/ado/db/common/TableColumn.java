package net.simpleframework.ado.db.common;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.ColumnData;
import net.simpleframework.ado.ColumnMeta;
import net.simpleframework.ado.db.DbEntityTable;
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
public class TableColumn extends ColumnData {

	private DbEntityTable dbTable;

	public TableColumn(final String name) {
		super(name, (String) null);
	}

	public TableColumn(final String name, final String text) {
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
		sb.append(getSqlName());
		return sb.toString();
	}

	private final static Map<Class<?>, Map<String, TableColumn>> columnsCache;
	static {
		columnsCache = new ConcurrentHashMap<Class<?>, Map<String, TableColumn>>();
	}

	public static Map<String, TableColumn> getTableColumns(final Class<?> beanClass) {
		Map<String, TableColumn> data = columnsCache.get(beanClass);
		if (data != null) {
			return data;
		}
		data = new HashMap<String, TableColumn>();
		for (final PropertyWrapper pw : BeanUtils.getProperties(beanClass).values()) {
			ColumnMeta meta = null;
			if (pw.field != null) {
				meta = pw.field.getAnnotation(ColumnMeta.class);
			}
			if (meta == null) {
				meta = pw.getter.getAnnotation(ColumnMeta.class);
			}
			if (meta != null && meta.ignore()) {
				continue;
			}
			final TableColumn col = new TableColumn(pw.name);
			col.setPropertyClass(pw.type);
			if (meta != null) {
				String columnText, columnSqlName;
				if (StringUtils.hasText(columnText = meta.columnText())) {
					col.setText(I18n.replaceI18n(columnText));
				}
				if (StringUtils.hasText(columnSqlName = meta.columnMappingName())) {
					col.setSqlName(columnSqlName);
				}
			}
			data.put(pw.name, col);
		}
		columnsCache.put(beanClass, data);
		return data;
	}

	private static final long serialVersionUID = -241399268622218668L;
}
