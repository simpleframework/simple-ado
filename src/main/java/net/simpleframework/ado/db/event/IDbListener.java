package net.simpleframework.ado.db.event;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.IADOListener;
import net.simpleframework.ado.db.IDbManager;
import net.simpleframework.ado.db.common.SQLValue;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbListener extends IADOListener {

	/**
	 * 
	 * @param service
	 * @param sqlValues
	 * @throws ADOException
	 */
	void onBeforeExecute(IDbManager manager, SQLValue[] sqlValues) throws Exception;

	/**
	 * 
	 * @param service
	 * @param sqlValues
	 * @throws ADOException
	 */
	void onAfterExecute(IDbManager manager, SQLValue[] sqlValues) throws Exception;
}
