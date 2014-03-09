package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.trans.ITransactionCallback;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DefaultJdbcProvider extends AbstractJdbcProvider {
	private IStatementCreator statementCreator;

	public DefaultJdbcProvider(final DataSource dataSource) {
		super(dataSource);
	}

	@Override
	public IStatementCreator getStatementCreator() {
		if (statementCreator == null) {
			statementCreator = new DefaultStatementCreator(this);
		}
		return statementCreator;
	}

	@Override
	public <T> T doExecute(final IConnectionCallback<T> callback) {
		Connection connection = null;
		try {
			return callback.doInConnection(connection = getConnection());
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, null, null);
		}
	}

	@Override
	public int[] doBatch(final String[] sqlArr) {
		Connection connection = null;
		Statement stmt = null;
		try {
			stmt = getStatementCreator().createStatement(connection = getConnection());
			for (final String sql : sqlArr) {
				stmt.addBatch(sql);
			}
			return stmt.executeBatch();
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, stmt, null);
		}
	}

	@Override
	public int[] doBatch(final String sql, final int batchCount, final IBatchValueSetter setter) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sql);
			for (int i = 0; i < batchCount; i++) {
				setter.setValues(ps, i);
				ps.addBatch();
			}
			return ps.executeBatch();
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, ps, null);
		}
	}

	@Override
	public int doUpdate(final SQLValue sqlVal) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sqlVal);
			return ps.executeUpdate();
		} catch (final Exception ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, ps, null);
		}
	}

	@Override
	public <T> T queryObject(final SQLValue sqlVal, final IQueryExtractor<T> extractor) {
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sqlVal);
			return extractor.extractData(rs = ps.executeQuery());
		} catch (final Exception ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, ps, rs);
		}
	}

	@Override
	public void doQuery(final SQLValue sqlVal, final IQueryCallback callback,
			final int resultSetType, final int resultSetConcurrency) {
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sqlVal,
					resultSetType, resultSetConcurrency);
			callback.processRow(rs = ps.executeQuery());
		} catch (final Exception ex) {
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, ps, rs);
		}
	}

	@Override
	public <T> T doExecuteTransaction(final ITransactionCallback<T> callback,
			final IJdbcTransactionEvent event) {
		Connection connection = null;
		try {
			connection = JdbcTransactionUtils.begin(getDataSource());
			if (event != null) {
				event.onExecute(connection);
			}
			final T t = callback.onTransactionCallback();
			JdbcTransactionUtils.commit(connection);
			return t;
		} catch (final Throwable th) {
			try {
				if (connection != null) {
					connection.rollback();
				}
			} catch (final SQLException e2) {
				log.warn(e2);
			}

			if (event != null) {
				event.onThrowable(connection);
			}
			throw ADOException.of(th);
		} finally {
			JdbcTransactionUtils.end(connection);
			if (event != null) {
				event.onFinally(connection);
			}
		}
	}

	private Connection getConnection() throws SQLException {
		return JdbcTransactionUtils.getConnection(getDataSource());
	}

	private void closeAll(final Connection connection, final Statement stat, final ResultSet rs) {
		JdbcTransactionUtils.closeAll(connection, stat, rs);
	}
}
