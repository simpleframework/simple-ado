package net.simpleframework.ado.db.common;

import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class SQLValue extends AbstractParamsValue<SQLValue> {
	private final StringBuilder _sql = new StringBuilder();

	public SQLValue() {
	}

	public SQLValue(final String sql, final Object... values) {
		_sql.append(sql);
		addValues(values);
	}

	public String getSql() {
		return _sql.toString();
	}

	public void setSql(final String sql) {
		_sql.setLength(0);
		_sql.append(sql);
	}

	public SQLValue addSql(final Object sql) {
		_sql.append(sql);
		return this;
	}

	@Override
	public String getKey() {
		final StringBuffer sb = new StringBuffer();
		sb.append(ObjectUtils.hashStr(getSql())).append("-").append(valuesToString());
		return sb.toString();
	}

	private static final long serialVersionUID = -293936736340487065L;
}
