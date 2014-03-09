package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class JdbcTransactionUtils {

	private static ThreadLocal<Connection> CONNECTIONS = new ThreadLocal<Connection>();

	/* 标识当前连接在嵌套 */
	private static ThreadLocal<Boolean> NESTED = new ThreadLocal<Boolean>();

	static Connection begin(final DataSource dataSource) throws SQLException {
		final Connection connection = getConnection(dataSource);
		if (isBeginTransaction(connection)) {
			/* 如果存在正在运行的事务连接,直接返回 */
			NESTED.set(Boolean.TRUE);
			return connection;
		}
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
		CONNECTIONS.set(connection);
		return connection;
	}

	static void commit(final Connection connection) throws SQLException {
		/* 不在嵌套中则提交 */
		if (!isNested()) {
			connection.commit();
		}
	}

	static void end(final Connection connection) {
		if (isNested()) {
			NESTED.remove();
		} else {
			CONNECTIONS.remove();
			closeAll(connection, null, null);
		}
	}

	private static boolean isNested() {
		return NESTED.get() != null;
	}

	private static boolean isBeginTransaction(final Connection connection) {
		return connection == CONNECTIONS.get();
	}

	static Connection getConnection(final DataSource dataSource) throws SQLException {
		Connection connection = CONNECTIONS.get();
		if (connection == null) {
			connection = dataSource.getConnection();
			if (!connection.getAutoCommit()) {
				connection.setAutoCommit(true);
			}
		}
		return connection;
	}

	static void closeAll(final Connection connection, final Statement stat, final ResultSet rs) {
		try {
			if (connection != null && !connection.isClosed()) {
				if (!isBeginTransaction(connection)) {
					connection.close();
				}
			}
			if (stat != null && !stat.isClosed()) {
				stat.close();
			}
			if (rs != null && !rs.isClosed()) {
				rs.close();
			}
		} catch (final SQLException e) {
			log.warn(e);
		}
	}

	private static final Log log = LogFactory.getLogger(JdbcTransactionUtils.class);
}
