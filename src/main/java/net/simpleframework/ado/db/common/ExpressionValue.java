package net.simpleframework.ado.db.common;

import java.util.ArrayList;
import java.util.Date;

import net.simpleframework.ado.EFilterRelation;
import net.simpleframework.ado.FilterItem;
import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.common.ETimePeriod;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.TimePeriod;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ExpressionValue extends AbstractParamsValue {
	private static final long serialVersionUID = 8251357074671323990L;

	private String expression;

	public ExpressionValue(final String expression, final Object... values) {
		addValues(values);
		this.expression = StringUtils.text(expression, "1=1");
	}

	public String getExpression() {
		return expression;
	}

	public void setExpression(final String expression) {
		this.expression = expression;
	}

	@Override
	public String getKey() {
		final StringBuilder sb = new StringBuilder();
		final String expression = getExpression();
		if (StringUtils.hasText(expression)) {
			sb.append(expression).append("-");
		}
		sb.append(valuesToString());
		return sb.toString();
	}

	public static ExpressionValue toExpressionValue(final FilterItem item) {
		final Object val = item.getValue();
		final EFilterRelation relation = item.getRelation();
		if ((relation != EFilterRelation.isNull && relation != EFilterRelation.isNotNull)
				&& val == null) {
			return null;
		}
		ArrayList<Object> al = null;
		StringBuilder sql = null;
		if (val instanceof TimePeriod) {
			final TimePeriod timePeriod = (TimePeriod) val;
			ETimePeriod tp;
			if (timePeriod == null || (tp = timePeriod.getTimePeriod()) == ETimePeriod.none) {
				return null;
			}
			sql = new StringBuilder();
			al = new ArrayList<Object>();
			final String column = item.getColumn();
			if (tp == ETimePeriod.custom) {
				final Date from = timePeriod.getFrom();
				final Date to = timePeriod.getTo();
				if (from != null || to != null) {
					sql.append("(").append(column);
					if (from != null) {
						sql.append(" > ?");
						al.add(from);
					}
					if (to != null) {
						if (from != null) {
							sql.append(" and ").append(column);
						}
						sql.append(" < ?");
						al.add(to);
					}
					sql.append(")");
				}
			} else {
				sql.append(column).append(" > ?");
				al.add(timePeriod.getTime());
			}
		} else {
			sql = new StringBuilder();
			al = new ArrayList<Object>();
			final FilterItem orItem = item.getOrItem();
			final boolean q = orItem != null && orItem.getValue() != null;
			if (q) {
				sql.append("(");
				doFilterItem(orItem, sql, al);
				sql.append(" or ");
			}
			doFilterItem(item, sql, al);
			if (q) {
				sql.append(")");
			}
		}
		return (sql == null || sql.length() == 0) ? null : new ExpressionValue(sql.toString(),
				al.toArray());
	}

	private static void doFilterItem(final FilterItem item, final StringBuilder sql,
			final ArrayList<Object> al) {
		final EFilterRelation relation = item.getRelation();
		final Object val = item.getValue();
		sql.append(item.getColumn()).append(" ").append(relation);
		if (relation == EFilterRelation.like) {
			sql.append(" '%").append(val).append("%'");
		} else if (relation != EFilterRelation.isNull && relation != EFilterRelation.isNotNull) {
			sql.append(" ?");
			al.add(val);
		}
	}
}
