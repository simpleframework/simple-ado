package net.simpleframework.ado.db;

import java.util.Map;

import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.db.common.TableColumn;
import net.simpleframework.ado.db.event.IDbEntityListener;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityManager<T> extends IDbManager {

	/**
	 * 获取表格元信息类
	 * 
	 * @return
	 */
	DbEntityTable getEntityTable();

	/**
	 * 扩展父类函数的参数
	 * 
	 * @param columns
	 * @param paramsValue
	 * @return
	 */
	Map<String, Object> executeQuery(String[] columns, IParamsValue paramsValue);

	/* select single */

	/**
	 * 根据指定条件返回Map实体对象。
	 * 
	 * @param paramsValue
	 * @return
	 */
	Map<String, Object> queryForMap(IParamsValue paramsValue);

	/**
	 * 根据指定条件返回Map实体对象，并指定返回的列
	 * 
	 * @param columns
	 * @param paramsValue
	 * @return
	 */
	Map<String, Object> queryForMap(String[] columns, IParamsValue paramsValue);

	/**
	 * 根据指定条件返回实体对象
	 * 
	 * @param paramsValue
	 * @return
	 */
	T queryForBean(IParamsValue paramsValue);

	/**
	 * 根据指定条件返回实体对象，并指定返回的列
	 * 
	 * @param columns
	 * @param paramsValue
	 * @return
	 */
	T queryForBean(String[] columns, IParamsValue paramsValue);

	/**
	 * 根据id返回实体对象
	 * 
	 * @param id
	 * @return
	 */
	T getBean(Object id);

	/**
	 * 根据id返回实体对象，并指定返回的列
	 * 
	 * @param columns
	 * @param id
	 * @return
	 */
	T getBean(String[] columns, Object id);

	/* select multi */

	IDbDataQuery<Map<String, Object>> queryMapSet(IParamsValue paramsValue);

	IDbDataQuery<Map<String, Object>> queryMapSet(String[] columns, IParamsValue paramsValue);

	IDbDataQuery<T> queryBeans(IParamsValue paramsValue);

	IDbDataQuery<T> queryBeans(String[] columns, IParamsValue paramsValue);

	/* update */

	int update(String[] columns, T... beans);

	int update(T... beans);

	int updateTransaction(String[] columns, T... beans);

	int updateTransaction(IDbEntityListener l, String[] columns, T... beans);

	int updateTransaction(T... beans);

	int updateTransaction(IDbEntityListener l, T... beans);

	/* insert */

	int insert(T... beans);

	int insertTransaction(IDbEntityListener l, T... beans);

	int insertTransaction(T... beans);

	/* delete */

	int delete(IParamsValue paramsValue);

	int deleteTransaction(IDbEntityListener l, IParamsValue paramsValue);

	int deleteTransaction(IParamsValue paramsValue);

	/* utils */

	int count(IParamsValue paramsValue);

	int sum(String column, IParamsValue paramsValue);

	int max(String column, IParamsValue paramsValue);

	Object exchange(Object bean1, Object bean2, TableColumn order, boolean up);
}
