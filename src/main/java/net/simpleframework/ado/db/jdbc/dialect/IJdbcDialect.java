package net.simpleframework.ado.db.jdbc.dialect;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.simpleframework.ado.db.common.TableColumn;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IJdbcDialect {

	/**
	 * 获取查询的count语句
	 * 
	 * @return
	 */
	String toCountSQL(String sql);

	/**
	 * 获取加入排序列的sql
	 * 
	 * @param sql
	 * @param columns
	 * @return
	 */
	String toOrderBySQL(String sql, TableColumn... columns);

	/**
	 * sql加上条件
	 * 
	 * @param sql
	 * @param condition
	 * @return
	 */
	String toConditionSQL(String sql, String condition);

	/**
	 * 获取分页语句
	 * 
	 * @param sql
	 * @param i
	 * @param fetchSize
	 * @return
	 */
	String toLimitSQL(String sql, int i, int fetchSize);

	/**
	 * 获取游标类型
	 * 
	 * @return
	 */
	int getResultSetType();

	/**
	 * 设置ResultSet的值
	 * 
	 * @param rs
	 * @param columnIndex
	 * @param requiredType
	 * @return
	 * @throws SQLException
	 */
	Object getResultSetValue(ResultSet rs, int columnIndex, Class<?> requiredType)
			throws SQLException;

	Object getResultSetValue(ResultSet rs, int columnIndex) throws SQLException;

	void setParameterValue(PreparedStatement ps, int paramIndex, Object inValue)
			throws SQLException, IOException;
}
