package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import net.simpleframework.ado.db.common.SQLValue;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IStatementCreator {

	/**
	 * 创建PreparedStatement对象
	 * 
	 * @param connection
	 * @param sqlVal
	 * @param resultSetType
	 * @param resultSetConcurrency
	 * @return
	 * @throws SQLException
	 */
	PreparedStatement prepareStatement(Connection connection, SQLValue sqlVal, int resultSetType,
			int resultSetConcurrency) throws SQLException;

	PreparedStatement prepareStatement(Connection connection, SQLValue sqlVal) throws SQLException;

	/**
	 * 
	 * @param connection
	 * @param sql
	 * @return
	 * @throws SQLException
	 */
	PreparedStatement prepareStatement(Connection connection, String sql) throws SQLException;

	/**
	 * 
	 * @return
	 * @throws SQLException
	 */
	Statement createStatement(Connection connection) throws SQLException;
}
