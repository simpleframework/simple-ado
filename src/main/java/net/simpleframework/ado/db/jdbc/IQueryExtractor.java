package net.simpleframework.ado.db.jdbc;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.simpleframework.ado.ADOException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IQueryExtractor<T> {

	/**
	 * 解开单个对象
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 * @throws ADOException
	 */
	T extractData(ResultSet rs) throws SQLException, ADOException;
}
