package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.SQLException;

import net.simpleframework.ado.ADOException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IConnectionCallback<T> {

	/**
	 * 连接回调
	 * 
	 * @param con
	 * @return
	 * @throws SQLException
	 * @throws ADOException
	 */
	T doInConnection(Connection con) throws SQLException, ADOException;
}
