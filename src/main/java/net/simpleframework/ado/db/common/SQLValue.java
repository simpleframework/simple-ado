package net.simpleframework.ado.db.common;

import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class SQLValue extends AbstractParamsValue<SQLValue> {
	private final StringBuilder _sql = new StringBuilder();

	/* 最初的sql */
	private String osql;

	public SQLValue() {
	}

	public SQLValue(final CharSequence sql, final Object... values) {
		_sql.append(sql);
		addValues(values);
	}

	public String getSql() {
		return _sql.toString();
	}

	public SQLValue setSql(final String sql) {
		_sql.setLength(0);
		_sql.append(sql);
		return this;
	}

	public SQLValue addSql(final Object sql) {
		_sql.append(sql);
		return this;
	}

	public String getOsql() {
		return osql != null ? osql : getSql();
	}

	public SQLValue setOsql(final String osql) {
		this.osql = osql;
		return this;
	}

	@Override
	public String getKey() {
		final StringBuilder sb = new StringBuilder();
		sb.append(ObjectUtils.hashStr(getSql())).append("-").append(valuesToString());
		return sb.toString();
	}

	private static final long serialVersionUID = -293936736340487065L;
}
