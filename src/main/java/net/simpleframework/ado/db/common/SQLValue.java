package net.simpleframework.ado.db.common;

import net.simpleframework.ado.IParamsValue.AbstractParamsValue;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class SQLValue extends AbstractParamsValue {
	private static final long serialVersionUID = -293936736340487065L;

	private String sql;

	public SQLValue(final String sql) {
		this.sql = sql;
	}

	public SQLValue(final String sql, final Object... values) {
		this.sql = sql;
		setValues(values);
	}

	public String getSql() {
		return sql;
	}

	public void setSql(final String sql) {
		this.sql = sql;
	}

	@Override
	public String getKey() {
		final StringBuffer sb = new StringBuffer();
		sb.append(ObjectUtils.hashStr(sql)).append("-").append(valuesToString());
		return sb.toString();
	}
}
