package net.simpleframework.ado.db.event;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityListener extends IDbListener {

	/* delete event */

	void onBeforeDelete(IDbEntityManager<?> manager, IParamsValue paramsValue) throws Exception;

	void onAfterDelete(IDbEntityManager<?> manager, IParamsValue paramsValue) throws Exception;

	/* insert event */

	void onBeforeInsert(IDbEntityManager<?> manager, Object[] beans) throws Exception;

	void onAfterInsert(IDbEntityManager<?> manager, Object[] beans) throws Exception;

	/* update event */

	void onBeforeUpdate(IDbEntityManager<?> manager, String[] columns, Object[] beans)
			throws Exception;

	void onAfterUpdate(IDbEntityManager<?> manager, String[] columns, Object[] beans)
			throws Exception;
}
