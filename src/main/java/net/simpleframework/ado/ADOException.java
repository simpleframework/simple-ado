package net.simpleframework.ado;

import net.simpleframework.common.th.RuntimeExceptionEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class ADOException extends RuntimeExceptionEx {

	public ADOException(final String msg, final Throwable cause) {
		super(msg, cause);
	}

	public static ADOException of(final String message) {
		return _of(ADOException.class, message);
	}

	public static ADOException of(final Throwable cause) {
		return _of(ADOException.class, null, cause);
	}

	private static final long serialVersionUID = -539640491680179667L;
}
