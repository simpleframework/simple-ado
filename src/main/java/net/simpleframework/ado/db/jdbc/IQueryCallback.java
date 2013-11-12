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
public interface IQueryCallback {

	/**
	 * 处理结果集的行
	 * 
	 * @param rs
	 * @throws SQLException
	 * @throws ADOException
	 */
	void processRow(ResultSet rs) throws SQLException, ADOException;
}
