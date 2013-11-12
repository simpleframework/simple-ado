package net.simpleframework.ado.db;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.ColumnData;
import net.simpleframework.ado.EOrder;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.common.SqlUtils;
import net.simpleframework.ado.db.common.TableColumn;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.StringUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class SQLBuilder {

	private static StringBuilder buildSelectSQL(final StringBuilder sb, final DbEntityTable dbTable,
			final String[] columns) {
		sb.append("select ");
		if (columns == null || columns.length == 0) {
			sb.append("*");
		} else {
			int i = 0;
			final Map<String, TableColumn> tblColumns = dbTable.getTableColumns();
			for (final String column : columns) {
				if (!StringUtils.hasText(column)) {
					continue;
				}
				if (i++ > 0) {
					sb.append(",");
				}
				final TableColumn tCol = tblColumns.get(column);
				if (tCol != null) {
					final String sqlName = tCol.getSqlName();
					sb.append(sqlName);
					final String name = tCol.getName();
					if (!sqlName.equals(name)) {
						sb.append(" as ").append(name);
					}
				} else {
					sb.append(column);
				}
			}
		}
		sb.append(" from ").append(dbTable.getName());
		return sb;
	}

	private static StringBuilder buildDeleteSQL(final StringBuilder sb, final DbEntityTable dbTable) {
		return sb.append("delete from ").append(dbTable.getName());
	}

	private static String trimExpression(String expression) {
		if (expression != null) {
			expression = SqlUtils.trimSQL(expression);
			if (expression.toLowerCase().startsWith("where")) {
				expression = expression.substring(5).trim();
			}
		}
		return expression;
	}

	static StringBuilder buildUniqueColumns(final StringBuilder sb, final DbEntityTable dbTable) {
		int i = 0;
		final Map<String, TableColumn> tblColumns = dbTable.getTableColumns();
		for (final String uniqueColumn : dbTable.getUniqueColumns()) {
			if (i++ > 0) {
				sb.append(" and ");
			}
			final TableColumn tCol = tblColumns.get(uniqueColumn);
			if (tCol != null) {
				sb.append(tCol.getSqlName());
			} else {
				sb.append(uniqueColumn);
			}
			sb.append("=?");
		}
		return sb;
	}

	public static String getSelectUniqueSQL(final DbEntityTable dbTable, final String[] columns) {
		final StringBuilder sb = new StringBuilder();
		buildSelectSQL(sb, dbTable, columns);
		sb.append(" where ");
		buildUniqueColumns(sb, dbTable);
		return sb.toString();
	}

	public static String getSelectExpressionSQL(final DbEntityTable dbTable, final String[] columns,
			String expression) {
		final StringBuilder sb = new StringBuilder();
		buildSelectSQL(sb, dbTable, columns);
		expression = trimExpression(expression);
		if (StringUtils.hasText(expression)) {
			sb.append(" where ").append(expression);
			final ColumnData col = dbTable.getDefaultOrder();
			if (col != null) {
				if (expression.toLowerCase().indexOf("order by") == -1) {
					sb.append(" order by ").append(col.getSqlName());
					final EOrder o = col.getOrder();
					if (o != EOrder.normal) {
						sb.append(" ").append(o);
					}
				}
			}
		}
		return sb.toString();
	}

	public static String getDeleteUniqueSQL(final DbEntityTable dbTable) {
		final StringBuilder sb = new StringBuilder();
		buildDeleteSQL(sb, dbTable);
		sb.append(" where ");
		buildUniqueColumns(sb, dbTable);
		return sb.toString();
	}

	public static String getDeleteExpressionSQL(final DbEntityTable dbTable, String expression) {
		final StringBuilder sb = new StringBuilder();
		buildDeleteSQL(sb, dbTable);
		expression = trimExpression(expression);
		if (StringUtils.hasText(expression)) {
			sb.append(" where ").append(expression);
		}
		return sb.toString();
	}

	private static Object getVal(final Object bean, final String propertyName) {
		if (bean instanceof Map) {
			return ((Map<?, ?>) bean).get(propertyName);
		} else {
			return BeanUtils.getProperty(bean, propertyName);
		}
	}

	public static SQLValue getInsertSQLValue(final DbEntityTable dbTable, final Object object) {
		final StringBuilder sb = new StringBuilder();
		sb.append("insert into ").append(dbTable.getName()).append("(");
		final List<Object> vl = new ArrayList<Object>();
		int size = 0;
		for (final TableColumn tCol : dbTable.getTableColumns().values()) {
			if (size > 0) {
				sb.append(",");
			}
			sb.append(tCol.getSqlName());
			vl.add(getVal(object, tCol.getName()));
			size++;
		}
		sb.append(") values(");
		for (int i = 0; i < size; i++) {
			if (i > 0) {
				sb.append(",");
			}
			sb.append("?");
		}
		sb.append(")");
		return new SQLValue(sb.toString(), vl.toArray());
	}

	public static SQLValue getUpdateSQLValue(final DbEntityTable dbTable, final String[] columns,
			final Object object) {
		final String[] uniqueColumns = dbTable.getUniqueColumns();
		if (uniqueColumns == null || uniqueColumns.length == 0) {
			return null;
		}

		final List<Object> vl = new ArrayList<Object>();
		final StringBuilder sb = new StringBuilder();
		sb.append("update ").append(dbTable.getName()).append(" set ");

		final Collection<?> coll = (columns != null && columns.length > 0) ? Arrays.asList(columns)
				: dbTable.getTableColumns().values();

		int i = 0;
		for (final Object oCol : coll) {
			if (i++ > 0) {
				sb.append(",");
			}
			String key;
			Object val;
			if (oCol instanceof TableColumn) {
				key = ((TableColumn) oCol).getSqlName();
				val = getVal(object, ((TableColumn) oCol).getName());
			} else {
				key = (String) oCol;
				val = getVal(object, dbTable.getBeanPropertyName(key));
			}
			sb.append(key).append("=?");
			vl.add(val);
		}
		sb.append(" where ");
		buildUniqueColumns(sb, dbTable);
		for (final String column : uniqueColumns) {
			vl.add(getVal(object, dbTable.getBeanPropertyName(column)));
		}
		return new SQLValue(sb.toString(), vl.toArray());
	}
}
