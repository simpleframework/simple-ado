package net.simpleframework.ado.db.common;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ParamVal {
	private Object val;

	private int sqlType;

	public ParamVal(final Object val) {
		this.val = val;
	}

	public Object getVal() {
		return val;
	}

	public ParamVal setVal(final Object val) {
		this.val = val;
		return this;
	}

	public int getSqlType() {
		return sqlType;
	}

	public ParamVal setSqlType(final int sqlType) {
		this.sqlType = sqlType;
		return this;
	}
}
