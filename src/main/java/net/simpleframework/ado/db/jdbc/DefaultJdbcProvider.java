package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

import javax.sql.DataSource;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.trans.ITransactionCallback;
import net.simpleframework.common.StringUtils;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
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
	public int[] doBatch(final CharSequence[] sqlArr) {
		Connection connection = null;
		Statement stmt = null;
		try {
			stmt = getStatementCreator().createStatement(connection = getConnection());
			for (final CharSequence sql : sqlArr) {
				stmt.addBatch(sql.toString());
			}
			return stmt.executeBatch();
		} catch (final SQLException ex) {
			oprintln("Error SQL: " + StringUtils.join(sqlArr, "\r\n"));
			oprintln(ex.getMessage());
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, stmt, null);
		}
	}

	@Override
	public int[] doBatch(final CharSequence sql, final int batchCount,
			final IBatchValueSetter setter) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sql.toString());
			for (int i = 0; i < batchCount; i++) {
				setter.setValues(ps, i);
				ps.addBatch();
			}
			return ps.executeBatch();
		} catch (final SQLException ex) {
			oprintln("Error SQL: " + sql);
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
			if (connection.getAutoCommit()) {
				oprintln("Auto commit SQL: " + sqlVal.getSql());
			}
			return ps.executeUpdate();
		} catch (final Exception ex) {
			oprintln("Error SQL: " + sqlVal.getOsql());
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
			getLog().error("Error SQL: " + sqlVal.getOsql());
			throw ADOException.of(ex);
		} finally {
			closeAll(connection, ps, rs);
		}
	}

	protected long getSlowTimeMillis() {
		return -1;
	}

	@Override
	public void doQuery(final SQLValue sqlVal, final IQueryCallback callback,
			final int resultSetType, final int resultSetConcurrency) {
		final long timeMillis = getSlowTimeMillis();
		final long ls = System.currentTimeMillis();
		Connection connection = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		try {
			ps = getStatementCreator().prepareStatement(connection = getConnection(), sqlVal,
					resultSetType, resultSetConcurrency);
			callback.processRow(rs = ps.executeQuery());
			final long ld = System.currentTimeMillis() - ls;
			if (timeMillis >= 0 && ld >= timeMillis) {
				oprintln("[" + ld + "ms, " + sqlVal.getOsql() + "]");
			}
		} catch (final Exception ex) {
			throw ADOException.of("sql: " + sqlVal.getOsql(), ex);
		} finally {
			closeAll(connection, ps, rs);
		}
	}

	protected final ThreadLocal<Boolean> IN_TRANSACTION = new ThreadLocal<>();

	@Override
	public boolean inTrans() {
		return IN_TRANSACTION.get() != null;
	}

	@Override
	public <T> T doExecuteTransaction(final ITransactionCallback<T> callback) {
		List<IJdbcTransactionEvent> events = null;
		Connection connection = null;
		try {
			// synchronized (this) {
			IN_TRANSACTION.set(Boolean.TRUE);
			connection = beginTran();
			final T t = callback.onTransactionCallback();

			// 执行后缺省事件
			events = JdbcUtils.getTransactionEvents();

			// 当返回值含有"_throwable"属性，则回滚，可能被调用者try-catch掉
			if (t instanceof ObjectEx && ((ObjectEx) t).getAttr("_throwable") instanceof Throwable) {
				rollback(connection);
			} else {
				if (events != null) {
					for (final IJdbcTransactionEvent event : events) {
						event.onSuccess(connection);
					}
				}
				commitTran(connection);
			}
			return t;
			// }
		} catch (final Throwable th) {
			if (events != null) {
				for (final IJdbcTransactionEvent event : events) {
					event.onThrowable(connection);
				}
			} else {
				events = JdbcUtils.getTransactionEvents();
			}
			rollback(connection);
			throw ADOException.of(th);
		} finally {
			if (events != null) {
				for (final IJdbcTransactionEvent event : events) {
					event.onFinally(connection);
				}
			}
			endTran(connection);
			IN_TRANSACTION.remove();
			JdbcUtils.removeTransactionEvents();
		}
	}

	private void rollback(final Connection connection) {
		try {
			if (connection != null) {
				connection.rollback();
			}
		} catch (final SQLException ex) {
			getLog().warn(ex);
		}
	}
}
