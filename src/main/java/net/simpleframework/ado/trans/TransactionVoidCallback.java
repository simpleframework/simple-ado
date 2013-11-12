package net.simpleframework.ado.trans;

import net.simpleframework.ado.ADOException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class TransactionVoidCallback extends TransactionObjectCallback<Object> {

	@Override
	public Object onTransactionCallback() throws ADOException {
		doTransactionVoidCallback();
		return null;
	}

	protected abstract void doTransactionVoidCallback() throws ADOException;
}
