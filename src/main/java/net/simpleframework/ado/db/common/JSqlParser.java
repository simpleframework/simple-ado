package net.simpleframework.ado.db.common;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import net.simpleframework.ado.EOrder;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.ado.db.jdbc.DatabaseMeta;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.LRUMap;

import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLOrderBy;
import com.alibaba.druid.sql.ast.SQLOrderingSpecification;
import com.alibaba.druid.sql.ast.expr.SQLAggregateExpr;
import com.alibaba.druid.sql.ast.expr.SQLAllColumnExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOpExpr;
import com.alibaba.druid.sql.ast.expr.SQLBinaryOperator;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLPropertyExpr;
import com.alibaba.druid.sql.ast.statement.SQLSelect;
import com.alibaba.druid.sql.ast.statement.SQLSelectItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectOrderByItem;
import com.alibaba.druid.sql.ast.statement.SQLSelectQueryBlock;
import com.alibaba.druid.sql.parser.SQLParserUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class JSqlParser {
	public static String format(final String sql, final String db) {
		return SQLUtils.format(sql, db);
	}

	/**
	 * 定义count语句的缓存
	 */
	private static Map<String, String> countSQLCache = Collections
			.synchronizedMap(new LRUMap<String, String>(1000));

	public static String wrapCount(final String sql, final String dbType) {
		String nsql = countSQLCache.get(sql);
		if (nsql != null) {
			return nsql;
		}
		final SQLSelect sqlSelect = SQLParserUtils.createSQLStatementParser(sql, dbType)
				.parseSelect().getSelect();
		sqlSelect.setOrderBy(null);
		final SQLSelectQueryBlock qBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
		if (qBlock.getGroupBy() == null) {
			final List<SQLSelectItem> items = qBlock.getSelectList();
			boolean aggregate = false;
			for (final SQLSelectItem item : items) {
				if (item.getExpr() instanceof SQLAggregateExpr) {
					aggregate = true;
					break;
				}
			}
			if (!aggregate) {
				final SQLAggregateExpr count = new SQLAggregateExpr("count");
				count.getArguments().add(new SQLAllColumnExpr());
				items.clear();
				items.add(new SQLSelectItem(count));
				try {
					BeanUtils.setProperty(qBlock, "orderBy", null);
				} catch (final Exception e) {
				}
				nsql = SQLUtils.toSQLString(sqlSelect, dbType);
			}
		}
		if (nsql != null) {
			countSQLCache.put(sql, nsql);
		}
		return nsql;
	}

	public static String addOrderBy(final String sql, final String dbType,
			final TableColumn... columns) {
		if (columns == null || columns.length == 0) {
			return sql;
		}
		final SQLSelect sqlSelect = SQLParserUtils.createSQLStatementParser(sql, dbType)
				.parseSelect().getSelect();
		final SQLSelectQueryBlock qBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
		SQLOrderBy orderBy = null;
		if (BeanUtils.hasProperty(qBlock, "orderBy")) {
			orderBy = (SQLOrderBy) BeanUtils.getProperty(qBlock, "orderBy");
			if (orderBy == null) {
				BeanUtils.setProperty(qBlock, "orderBy", orderBy = new SQLOrderBy());
			}
		} else {
			orderBy = sqlSelect.getOrderBy();
			if (orderBy == null) {
				sqlSelect.setOrderBy(orderBy = new SQLOrderBy());
			}
		}

		final List<SQLSelectOrderByItem> items = orderBy.getItems();
		for (int i = columns.length - 1; i >= 0; i--) {
			final TableColumn dbColumn = columns[i];
			final SQLExpr expr = new SQLIdentifierExpr(dbColumn.getSqlName());
			SQLExpr expr2 = expr;
			final DbEntityTable dbTable = dbColumn.getTable();
			if (dbTable != null) {
				expr2 = new SQLPropertyExpr(expr, dbTable.getName());
			}

			SQLSelectOrderByItem item = null;
			for (final SQLSelectOrderByItem o : items) {
				final SQLExpr e = o.getExpr();
				if (e.equals(expr) || e.equals(expr2)) {
					item = o;
					break;
				}
			}
			if (item == null) {
				item = new SQLSelectOrderByItem();
			} else {
				items.remove(item);
			}

			item.setExpr(expr2);
			item.setType(dbColumn.getOrder() == EOrder.asc ? SQLOrderingSpecification.ASC
					: SQLOrderingSpecification.DESC);
			items.add(0, item);
		}
		return SQLUtils.toSQLString(sqlSelect, dbType);
	}

	public static String addCondition(final String sql, final String dbType, final String condition) {
		if (!StringUtils.hasText(condition)) {
			return sql;
		}
		final SQLSelect sqlSelect = SQLParserUtils.createSQLStatementParser(sql, dbType)
				.parseSelect().getSelect();
		final SQLSelectQueryBlock qBlock = (SQLSelectQueryBlock) sqlSelect.getQuery();
		final SQLExpr expr = SQLParserUtils.createExprParser(condition, dbType).expr();
		if (qBlock.getWhere() == null) {
			qBlock.setWhere(expr);
		} else {
			qBlock.setWhere(new SQLBinaryOpExpr(qBlock.getWhere(), SQLBinaryOperator.BooleanAnd, expr));
		}
		return SQLUtils.toSQLString(sqlSelect, dbType);
	}

	public static String toSqlServerLimit(final String sql, final int i, final int fetchSize) {
		final SQLSelect sqlSelect = SQLParserUtils
				.createSQLStatementParser(sql, DatabaseMeta.MSSQL_SERVER).parseSelect().getSelect();
		final SQLOrderBy orderBy = sqlSelect.getOrderBy();
		final String oStr = orderBy == null ? "ORDER BY CURRENT_TIMESTAMP" : SQLUtils
				.toSQLString(orderBy);
		sqlSelect.setOrderBy(null);
		final String sql2 = SQLUtils.toSQLString(sqlSelect, DatabaseMeta.MSSQL_SERVER);

		final StringBuilder sb = new StringBuilder();
		sb.append("SELECT * FROM (");
		sb.append("SELECT ROW_NUMBER() OVER(").append(oStr).append(") AS rownum, ");
		sb.append(sql2.substring(7));
		sb.append(") t_ss WHERE rownum BETWEEN ").append(i + 1).append(" AND ")
				.append(i + fetchSize + 1);
		return sb.toString();
	}
}
