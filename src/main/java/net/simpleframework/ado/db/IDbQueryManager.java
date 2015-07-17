package net.simpleframework.ado.db;

import java.util.Map;

import net.simpleframework.ado.db.common.SQLValue;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbQueryManager extends IDbManager {

	Map<String, Object> queryForMap(CharSequence sql, Object... params);

	Map<String, Object> queryForMap2(SQLValue sqlVal);

	long queryForLong(SQLValue sqlVal);

	int queryForInt(SQLValue sqlVal);

	boolean queryForBool(SQLValue sqlVal);

	/**
	 * 
	 * @param sqlVal
	 * @return
	 */
	IDbDataQuery<Map<String, Object>> query(SQLValue sqlVal);

	/**
	 * 
	 * @param sql
	 * @return
	 */
	IDbDataQuery<Map<String, Object>> query(CharSequence sql, Object... params);

	/**
	 * 
	 * @param value
	 * @param beanClass
	 * @return
	 */
	<T> IDbDataQuery<T> query(SQLValue sqlVal, Class<T> beanClass);
}
