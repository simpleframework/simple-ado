package net.simpleframework.ado.db.event;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityListener<T> extends IDbListener {

	/* delete event */

	void onBeforeDelete(IDbEntityManager<T> manager, IParamsValue paramsValue) throws Exception;

	void onAfterDelete(IDbEntityManager<T> manager, IParamsValue paramsValue) throws Exception;

	/* insert event */

	void onBeforeInsert(IDbEntityManager<T> manager, T[] beans) throws Exception;

	void onAfterInsert(IDbEntityManager<T> manager, T[] beans) throws Exception;

	/* update event */

	void onBeforeUpdate(IDbEntityManager<T> manager, String[] columns, T[] beans) throws Exception;

	void onAfterUpdate(IDbEntityManager<T> manager, String[] columns, T[] beans) throws Exception;
}
