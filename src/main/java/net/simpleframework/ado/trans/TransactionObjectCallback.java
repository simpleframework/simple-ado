package net.simpleframework.ado.trans;

import net.simpleframework.ado.ADOException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class TransactionObjectCallback<T> implements ITransactionCallback<T> {

	@Override
	public abstract T onTransactionCallback() throws ADOException;
}
