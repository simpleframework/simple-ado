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
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class JdbcTransactionUtils {

	private static ThreadLocal<Connection> connections;
	static {
		connections = new ThreadLocal<Connection>();
	}

	static Connection begin(final DataSource dataSource) throws SQLException {
		final Connection connection = getConnection(dataSource);
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
		connections.set(connection);
		return connection;
	}

	static void end(final Connection connection) {
		connections.remove();
		closeAll(connection, null, null);
	}

	static boolean inTrans(final Connection connection) {
		return connection == connections.get();
	}

	static Connection getConnection(final DataSource dataSource) throws SQLException {
		Connection connection = connections.get();
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
				if (!JdbcTransactionUtils.inTrans(connection)) {
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
