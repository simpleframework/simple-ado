package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.jdbc.dialect.DefaultJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.HSQLJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.IJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.MySqlJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.OracleJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.PostgresqlJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.SqlServerJdbcDialect;
import net.simpleframework.ado.trans.ITransactionCallback;
import net.simpleframework.common.Convert;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractJdbcProvider extends ObjectEx implements IJdbcProvider {

	/* 缓存事务连接 */
	protected final ThreadLocal<Connection> CONNECTIONs = new ThreadLocal<>();
	/* 标识当前连接在嵌套 */
	protected final ThreadLocal<Integer> NESTED = new ThreadLocal<>();

	private final DataSource dataSource;

	public AbstractJdbcProvider(final DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@Override
	public DataSource getDataSource() {
		return dataSource;
	}

	@Override
	public void doQuery(final SQLValue sqlVal, final IQueryCallback callback) {
		doQuery(sqlVal, callback, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
	}

	@Override
	public DatabaseMeta getDatabaseMeta() {
		return JdbcUtils.getDatabaseMetaData(getDataSource());
	}

	protected IJdbcDialect createJdbcNative() {
		final DatabaseMeta meta = getDatabaseMeta();
		if (meta.isMySql()) {
			return new MySqlJdbcDialect();
		} else if (meta.isOracle()) {
			return new OracleJdbcDialect();
		} else if (meta.isMSSql()) {
			return new SqlServerJdbcDialect();
		} else if (meta.isHSql()) {
			return new HSQLJdbcDialect();
		} else if (meta.isPostgreSql()) {
			return new PostgresqlJdbcDialect();
		} else {
			return new DefaultJdbcDialect();
		}
	}

	private IJdbcDialect jdbcNative;

	@Override
	public IJdbcDialect getJdbcDialect() {
		if (jdbcNative == null) {
			jdbcNative = createJdbcNative();
		}
		return jdbcNative;
	}

	@Override
	public Connection getConnection() throws SQLException {
		return getConnection(AUTO_COMMIT.get() != null);
	}

	@Override
	public Connection getConnection(final boolean autoCommit) throws SQLException {
		if (autoCommit) {
			return _getAutoCommit();
		}
		Connection connection = CONNECTIONs.get();
		if (connection == null) {
			connection = _getAutoCommit();
		}
		return connection;
	}

	private Connection _getAutoCommit() throws SQLException {
		final Connection connection = getDataSource().getConnection();
		if (!connection.getAutoCommit()) {
			connection.setAutoCommit(true);
		}
		return connection;
	}

	protected Connection beginTran() throws SQLException {
		final Connection connection = getConnection();
		if (isBeginTransaction(connection)) {
			/* 如果存在正在运行的事务连接,直接返回 */
			int i = Convert.toInt(NESTED.get());
			NESTED.set(++i);
			return connection;
		}
		if (connection.getAutoCommit()) {
			connection.setAutoCommit(false);
		}
		// connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
		CONNECTIONs.set(connection);
		return connection;
	}

	protected void commitTran(final Connection connection) throws SQLException {
		/* 不在嵌套中则提交 */
		if (!isTranNested()) {
			connection.commit();
		}
	}

	protected void endTran(final Connection connection) {
		if (isTranNested()) {
			int i = Convert.toInt(NESTED.get());
			if (--i <= 0) {
				NESTED.remove();
			} else {
				NESTED.set(i);
			}
		} else {
			CONNECTIONs.remove();
			closeAll(connection, null, null);
		}
	}

	private boolean isTranNested() {
		return NESTED.get() != null;
	}

	private boolean isBeginTransaction(final Connection connection) {
		return connection == CONNECTIONs.get();
	}

	@Override
	public void closeAll(final Connection connection, final Statement stat, final ResultSet rs) {
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
			getLog().warn(e);
		}
	}

	/* 获取自动提交连接 */
	protected final ThreadLocal<Boolean> AUTO_COMMIT = new ThreadLocal<>();

	@Override
	public <T> T doAutoCommit(final ITransactionCallback<T> callback) {
		synchronized (AUTO_COMMIT) {
			try {
				AUTO_COMMIT.set(Boolean.TRUE);
				return callback.onTransactionCallback();
			} catch (final Throwable e) {
				throw ADOException.of(e);
			} finally {
				AUTO_COMMIT.remove();
			}
		}
	}
}
