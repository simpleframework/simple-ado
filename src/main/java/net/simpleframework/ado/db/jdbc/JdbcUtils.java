package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import net.simpleframework.ado.ADOException;
import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class JdbcUtils {
	static Log log = LogFactory.getLogger(JdbcUtils.class);

	private static Map<DataSource, DatabaseMeta> databaseMetaDataMap = new ConcurrentHashMap<DataSource, DatabaseMeta>();

	public static DatabaseMeta getDatabaseMetaData(final DataSource dataSource) {
		DatabaseMeta _metaData = databaseMetaDataMap.get(dataSource);
		if (_metaData != null) {
			return _metaData;
		}
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			final DatabaseMetaData metaData = connection.getMetaData();
			_metaData = new DatabaseMeta();
			_metaData._databaseProductName = metaData.getDatabaseProductName();
			_metaData._url = metaData.getURL();
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException e) {
					log.warn(e);
				}
			}
		}
		databaseMetaDataMap.put(dataSource, _metaData);

		return _metaData;
	}

	public static int lookupColumnIndex(final ResultSetMetaData resultSetMetaData,
			final String columnName) throws SQLException {
		if (columnName != null) {
			for (int i = 1; i <= resultSetMetaData.getColumnCount(); i++) {
				if (columnName.equalsIgnoreCase(lookupColumnName(resultSetMetaData, i))) {
					return i;
				}
			}
		}
		return 0;
	}

	public static String lookupColumnName(final ResultSetMetaData resultSetMetaData,
			final int columnIndex) throws SQLException {
		String name = resultSetMetaData.getColumnLabel(columnIndex);
		if (name == null || name.length() < 1) {
			name = resultSetMetaData.getColumnName(columnIndex);
		}
		return name;
	}

	/*------------------------------------- Ope --------------------------------*/

	public static boolean isTableExists(final DataSource dataSource, final String tablename) {
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			return connection.getMetaData().getTables(null, null, tablename, new String[] { "TABLE" })
					.next();
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			if (connection != null) {
				try {
					connection.close();
				} catch (final SQLException e) {
					log.warn(e);
				}
			}
		}
	}

	// 判断 oracle sequence 是否已经存在
	public static boolean isSequenceExists(final DataSource dataSource, final String sequencename) {
		Connection connection = null;
		PreparedStatement ps = null;
		try {
			connection = dataSource.getConnection();
			ps = connection
					.prepareStatement("select sequence_name from user_sequences where sequence_name='"
							+ sequencename.toUpperCase() + "'");
			return ps.executeQuery().next();
		} catch (final SQLException ex) {
			throw ADOException.of(ex);
		} finally {
			try {
				if (connection != null) {
					connection.close();
				}
				if (ps != null) {
					ps.close();
				}
			} catch (final SQLException e) {
				log.warn(e);
			}
		}
	}

	static final ThreadLocal<Map<Class<?>, IJdbcTransactionEvent>> ON_AFTER_EXECUTE;
	static {
		ON_AFTER_EXECUTE = new ThreadLocal<Map<Class<?>, IJdbcTransactionEvent>>();
	}

	public static List<IJdbcTransactionEvent> getTransactionEvents() {
		final Map<Class<?>, IJdbcTransactionEvent> events = ON_AFTER_EXECUTE.get();
		if (events != null) {
			return new ArrayList<IJdbcTransactionEvent>(events.values());
		}
		return null;
	}

	public static void removeTransactionEvents() {
		ON_AFTER_EXECUTE.remove();
	}

	@SuppressWarnings("unchecked")
	public static <T extends IJdbcTransactionEvent> T getTransactionEvent(final T event) {
		Map<Class<?>, IJdbcTransactionEvent> events = ON_AFTER_EXECUTE.get();
		if (events == null) {
			ON_AFTER_EXECUTE.set(events = new LinkedHashMap<Class<?>, IJdbcTransactionEvent>());
		}
		final Class<?> eClass = event.getClass();
		IJdbcTransactionEvent t = events.get(eClass);
		if (t == null) {
			events.put(eClass, t = event);
		}
		return (T) t;
	}
}
