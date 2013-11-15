package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IJdbcTransactionEvent {

	/**
	 * 执行操作中触发
	 * 
	 * @param connection
	 */
	void onExecute(Connection connection);

	/**
	 * 异常执行操作中触发
	 * 
	 * @param connection
	 */
	void onThrowable(Connection connection);

	/**
	 * 完成操作中触发
	 * 
	 * @param connection
	 */
	void onFinally(Connection connection);
}
