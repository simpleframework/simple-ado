package net.simpleframework.ado.db.event;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IDbEntityListener extends IDbListener {

	/* delete event */

	void onBeforeDelete(IDbEntityManager<?> manager, IParamsValue paramsValue);

	void onAfterDelete(IDbEntityManager<?> manager, IParamsValue paramsValue);

	/* insert event */

	void onBeforeInsert(IDbEntityManager<?> manager, Object[] beans);

	void onAfterInsert(IDbEntityManager<?> manager, Object[] beans);

	/* update event */

	void onBeforeUpdate(IDbEntityManager<?> manager, String[] columns, Object[] beans);

	void onAfterUpdate(IDbEntityManager<?> manager, String[] columns, Object[] beans);
}
