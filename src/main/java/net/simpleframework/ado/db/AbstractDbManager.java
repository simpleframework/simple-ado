package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Map;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.AbstractADOManager;
import net.simpleframework.ado.IADOListener;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.event.IDbListener;
import net.simpleframework.ado.db.jdbc.DatabaseMeta;
import net.simpleframework.ado.db.jdbc.IBatchValueSetter;
import net.simpleframework.ado.db.jdbc.IConnectionCallback;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.IQueryExtractor;
import net.simpleframework.ado.db.jdbc.JdbcUtils;
import net.simpleframework.ado.trans.TransactionObjectCallback;
import net.simpleframework.common.coll.KVMap;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractDbManager extends AbstractADOManager implements IDbManager {

	protected DbManagerFactory dbFactory;

	public AbstractDbManager() {
	}

	public AbstractDbManager setDbManagerFactory(final DbManagerFactory dbFactory) {
		this.dbFactory = dbFactory;
		return this;
	}

	public IJdbcProvider getJdbcProvider() {
		return dbFactory.jdbcProvider;
	}

	@Override
	public <T> T executeQuery(final SQLValue sqlVal, final IQueryExtractor<T> extractor) {
		return getJdbcProvider().queryObject(sqlVal, extractor);
	}

	@Override
	public Map<String, Object> executeQuery(final SQLValue sqlVal) {
		return createQueryMap(null, sqlVal);
	}

	@Override
	public IDbDataQuery<Map<String, Object>> executeQuerySet(final SQLValue sqlVal) {
		return createQueryEntitySet(null, sqlVal);
	}

	protected int executeUpdate(final String sql) {
		return executeUpdate(new SQLValue(sql));
	}

	protected int executeUpdate(final SQLValue sqlVal) {
		return getJdbcProvider().doUpdate(sqlVal);
	}

	@Override
	public <T> T execute(final IConnectionCallback<T> callback) {
		return getJdbcProvider().doExecute(callback);
	}

	@Override
	public int execute(final IDbListener l, final SQLValue... sqlValues) {
		if (sqlValues == null || sqlValues.length == 0) {
			return 0;
		}
		int ret = 0;
		try {
			if (l != null) {
				l.onBeforeExecute(this, sqlValues);
			}
			final Collection<IADOListener> listeners = getListeners();
			for (final IADOListener listener : listeners) {
				((IDbListener) listener).onBeforeExecute(this, sqlValues);
			}

			for (final SQLValue sqlVal : sqlValues) {
				ret += executeUpdate(sqlVal);
			}
			if (l != null) {
				l.onAfterExecute(this, sqlValues);
			}
			for (final IADOListener listener : listeners) {
				((IDbListener) listener).onAfterExecute(this, sqlValues);
			}
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
		return ret;
	}

	@Override
	public int execute(final SQLValue... sqlValues) {
		return execute(null, sqlValues);
	}

	@Override
	public int[] batchUpdate(final String... sqlArr) {
		return getJdbcProvider().doBatch(sqlArr);
	}

	@Override
	public int[] batchUpdate(final String sql, final int batchCount, final IBatchValueSetter setter) {
		return getJdbcProvider().doBatch(sql, batchCount, setter);
	}

	@Override
	public int executeTransaction(final SQLValue... sqlValues) {
		return executeTransaction(null, sqlValues);
	}

	@Override
	public int executeTransaction(final IDbListener l, final SQLValue... sqlValues) {
		return doExecuteTransaction(new TransactionObjectCallback<Integer>() {
			@Override
			public Integer onTransactionCallback() {
				return execute(l, sqlValues);
			}
		});
	}

	protected DbTableColumn[] getColumns(final String[] columns) {
		if (columns != null) {
			final DbTableColumn[] objects = new DbTableColumn[columns.length];
			for (int i = 0; i < columns.length; i++) {
				objects[i] = new DbTableColumn(columns[i]);
			}
			return objects;
		} else {
			return null;
		}
	}

	protected Map<String, Object> mapRowData(final String[] columns, final ResultSet rs)
			throws SQLException {
		final ResultSetMetaData rsmd = rs.getMetaData();
		final int columnCount = columns != null ? columns.length : rsmd.getColumnCount();
		final Map<String, Object> mapData = new KVMap(columnCount).setCaseInsensitive(true);
		for (int i = 1; i <= columnCount; i++) {
			final String column = columns != null ? columns[i - 1] : null;
			String key;
			Object obj;
			if (column != null) {
				key = column;
				obj = getJdbcProvider().getJdbcDialect().getResultSetValue(rs,
						JdbcUtils.lookupColumnIndex(rsmd, key));
			} else {
				key = JdbcUtils.lookupColumnName(rsmd, i);
				obj = getJdbcProvider().getJdbcDialect().getResultSetValue(rs, i);
			}
			mapData.put(key, obj);
		}
		return mapData;
	}

	protected <T> T createQueryObject(final String[] columns, final SQLValue sqlVal,
			final Class<T> beanClass) {
		final BeanWrapper<T> wrapper = new BeanWrapper<T>(columns, beanClass);
		return executeQuery(sqlVal, new IQueryExtractor<T>() {

			@Override
			public T extractData(final ResultSet rs) throws SQLException, ADOException {
				return rs.next() ? wrapper.toBean(getJdbcProvider(), rs) : null;
			}
		});
	}

	protected Map<String, Object> createQueryMap(final String[] columns, final SQLValue sqlVal) {
		return executeQuery(sqlVal, new IQueryExtractor<Map<String, Object>>() {

			@Override
			public Map<String, Object> extractData(final ResultSet rs) throws SQLException,
					ADOException {
				return rs.next() ? mapRowData(columns, rs) : null;
			}
		});
	}

	protected DbDataQuery<Map<String, Object>> createQueryEntitySet(final String[] columns,
			final SQLValue sqlVal) {
		return new DbDataQuery<Map<String, Object>>(dbFactory, this, sqlVal) {

			@Override
			public Map<String, Object> mapRow(final ResultSet rs, final int rowNum)
					throws SQLException {
				return mapRowData(columns, rs);
			}
		};
	}

	protected <T> DbDataQuery<T> createQueryEntitySet(final String[] columns, final SQLValue sqlVal,
			final Class<T> beanClass) {
		final BeanWrapper<T> wrapper = new BeanWrapper<T>(columns, beanClass);
		return new DbDataQuery<T>(dbFactory, this, sqlVal) {

			@Override
			public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
				return wrapper.toBean(getJdbcProvider(), rs);
			}
		};
	}

	@Override
	public DatabaseMeta getDatabaseMeta() {
		return getJdbcProvider().getDatabaseMeta();
	}
}
