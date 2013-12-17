package net.simpleframework.ado.db.event;

import net.simpleframework.ado.db.IDbManager;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractDbListener extends ObjectEx implements IDbListener {

	@Override
	public void onBeforeExecute(final IDbManager manager, final SQLValue[] sqlValues) {
	}

	@Override
	public void onAfterExecute(final IDbManager manager, final SQLValue[] sqlValues) {
		doAfterEvent(manager, sqlValues);
	}

	protected void doAfterEvent(final IDbManager manager, final Object params) {
	}
}
