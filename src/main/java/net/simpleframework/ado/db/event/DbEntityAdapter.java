package net.simpleframework.ado.db.event;

import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.IDbEntityManager;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.common.ID;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class DbEntityAdapter<T> extends AbstractDbListener implements IDbEntityListener<T> {

	@Override
	public void onBeforeInsert(final IDbEntityManager<T> manager, final T[] beans) throws Exception {
	}

	@Override
	public void onAfterInsert(final IDbEntityManager<T> manager, final T[] beans) throws Exception {
		doAfterEvent(manager, beans);
	}

	@Override
	public void onBeforeUpdate(final IDbEntityManager<T> manager, final String[] columns,
			final T[] beans) throws Exception {
	}

	@Override
	public void onAfterUpdate(final IDbEntityManager<T> manager, final String[] columns,
			final T[] beans) throws Exception {
		doAfterEvent(manager, beans);
	}

	@Override
	public void onBeforeDelete(final IDbEntityManager<T> manager, final IParamsValue paramsValue)
			throws Exception {
	}

	@Override
	public void onAfterDelete(final IDbEntityManager<T> manager, final IParamsValue paramsValue)
			throws Exception {
		doAfterEvent(manager, paramsValue);
	}

	/**
	 * 获取原始数据,一般在onBefore中调用
	 * 
	 * @param manager
	 * @param beanId
	 * @param columns
	 * @return
	 */
	protected Map<String, Object> getOriginal(final IDbEntityManager<?> manager, final ID beanId,
			final String... columns) {
		return manager.executeQuery(columns, new ExpressionValue("id=?", beanId));
	}

	protected Object getOriginalVal(final IDbEntityManager<?> manager, final ID beanId,
			final String column) {
		return getOriginal(manager, beanId, column).get(column);
	}
}
