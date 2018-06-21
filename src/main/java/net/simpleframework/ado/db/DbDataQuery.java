package net.simpleframework.ado.db;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import javax.sql.DataSource;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.ado.db.jdbc.IQueryCallback;
import net.simpleframework.ado.db.jdbc.IQueryExtractor;
import net.simpleframework.ado.query.AbstractDataQuery;
import net.simpleframework.ado.query.IDataQueryListener;
import net.simpleframework.common.coll.LRUMap;
import net.simpleframework.common.th.NotImplementedException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DbDataQuery<T> extends AbstractDataQuery<T> implements IDbDataQuery<T> {

	private AbstractDbManager dbManager;

	private SQLValue sqlVal;

	/* 分页取值的大小, 当等于0时, 要手动close */
	private int fetchSize = -1;

	/* 分页取得的数据缓存 */
	private Map<Integer, T> dataCache;

	public DbDataQuery(final DbManagerFactory managerFactory, final AbstractDbManager dbManager,
			final SQLValue sqlVal) {
		this.dbManager = dbManager;
		this.sqlVal = sqlVal;
	}

	public DbDataQuery(final DbManagerFactory managerFactory, final AbstractDbManager dbManager,
			final String sql, final Object[] values) {
		this(managerFactory, dbManager, new SQLValue(sql, values));
	}

	public AbstractDbManager getDbManager() {
		return dbManager;
	}

	public IJdbcProvider getJdbcProvider() {
		return getDbManager().getJdbcProvider();
	}

	@Override
	public DataSource getDataSource() {
		return getJdbcProvider().getDataSource();
	}

	@Override
	public DbDataQuery<T> setFetchSize(final int fetchSize) {
		if (this.fetchSize != fetchSize) {
			this.fetchSize = fetchSize;
			dataCache = null;
		}
		return this;
	}

	@Override
	public int getFetchSize() {
		if (fetchSize < 0 || fetchSize >= Integer.MAX_VALUE) {
			setFetchSize(15);
		}
		return fetchSize;
	}

	private Map<Integer, T> getDataCache() {
		if (dataCache == null) {
			dataCache = Collections.synchronizedMap(new LRUMap<Integer, T>(getFetchSize() * 5));
		}
		return dataCache;
	}

	@Override
	public void reset() {
		super.reset();
		dataCache = null;
	}

	@Override
	public int getCount() {
		if (count < 0) {
			final SQLValue countSql = new SQLValue(
					getJdbcProvider().getJdbcDialect().toCountSQL(sqlVal.getSql()), sqlVal.getValues());
			count = getJdbcProvider().queryObject(countSql, new IQueryExtractor<Integer>() {
				@Override
				public Integer extractData(final ResultSet rs) throws SQLException, ADOException {
					return rs.next() ? rs.getInt(1) : 0;
				}
			});
		}
		return count;
	}

	public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
		throw NotImplementedException.of(getClass(), "mapRow");
	}

	/*------------------------- fetchSize==0 --------------------------*/
	private Connection _conn;
	private PreparedStatement _ps;
	private ResultSet _rs;

	@Override
	public void close() {
		getJdbcProvider().closeAll(_conn, _ps, _rs);
	}

	@Override
	public T next() {
		T bean = null;
		i++;
		final int fetchSize = getFetchSize();
		if (fetchSize <= 0) {
			try {
				if (i == 0 && _rs == null) {
					final IJdbcProvider jdbcProvider = getJdbcProvider();
					_conn = jdbcProvider.getConnection();
					_ps = jdbcProvider.getStatementCreator().prepareStatement(_conn, sqlVal,
							getResultSetType(), getResultSetConcurrency());
					_rs = _ps.executeQuery();
				}
				if (_rs != null) {
					if (_rs.isClosed()) {
						return null;
					}
					if (_rs.next()) {
						bean = mapRow(_rs, i);
					} else {
						close();
					}
				}
			} catch (final Exception e) {
				close();
				throw ADOException.of(e);
			}
		} else if (i < getCount() && i >= 0) {
			final Map<Integer, T> dataCache = getDataCache();
			bean = dataCache.get(i);
			if (bean == null) {
				final String sql = sqlVal.getSql();
				final String lsql = getJdbcProvider().getJdbcDialect().toLimitSQL(sql, i, fetchSize);
				final boolean absolute = lsql.equals(sql);

				final ArrayList<T> rVal = new ArrayList<>();
				getJdbcProvider().doQuery(new SQLValue(lsql, sqlVal.getValues()).setOsql(sql),
						new IQueryCallback() {
							@Override
							public void processRow(final ResultSet rs) throws SQLException, ADOException {
								if (absolute && i > 0) {
									rs.getStatement().setFetchSize(fetchSize);
									rs.absolute(i);
								}
								int j = -1;
								while (rs.next()) {
									final int k = i + ++j;
									if (dataCache.containsKey(k)) {
										break;
									}
									final T row = mapRow(rs, j);
									if (j == 0) {
										rVal.add(row);
									}
									dataCache.put(k, row);
									// 在oracle测试中
									if (j == fetchSize - 1) {
										break;
									}
								}
							}
						}, getResultSetType(), getResultSetConcurrency());
				final Iterator<T> it = rVal.iterator();
				bean = it.hasNext() ? it.next() : null;
			}
		}

		if (bean != null) {
			pIndex++;
		} else {
			pIndex = -1;
		}
		final boolean pageEnd = (pIndex + 1) == (fetchSize == 0 ? getCount() : fetchSize);
		for (final IDataQueryListener<T> listener : getListeners()) {
			listener.next(this, bean, pIndex, pageEnd);
		}
		if (pageEnd) {
			pIndex = -1;
		}
		return bean;
	}

	private int pIndex = -1;

	@Override
	public SQLValue getSqlValue() {
		return sqlVal;
	}

	@Override
	public void addCondition(final ExpressionValue ev) {
		final SQLValue sqlVal = getSqlValue();
		final String sql = getJdbcProvider().getJdbcDialect().toConditionSQL(sqlVal.getSql(),
				ev.getExpression());
		sqlVal.setSql(sql);
		sqlVal.addValues(ev.getValues());
		reset();
	}

	@Override
	public void addOrderBy(final DbTableColumn... columns) {
		if (columns == null) {
			return;
		}
		final SQLValue sqlVal = getSqlValue();
		final String sql = getJdbcProvider().getJdbcDialect().toOrderBySQL(sqlVal.getSql(), columns);
		sqlVal.setSql(sql);
	}

	private int resultSetType, resultSetConcurrency;

	int getResultSetType() {
		if (resultSetType == 0) {
			resultSetType = getJdbcProvider().getJdbcDialect().getResultSetType();
		}
		return resultSetType;
	}

	int getResultSetConcurrency() {
		if (resultSetConcurrency == 0) {
			resultSetConcurrency = ResultSet.CONCUR_READ_ONLY;
		}
		return resultSetConcurrency;
	}

	@Override
	public DbDataQuery<T> setResultSetType(final int resultSetType) {
		this.resultSetType = resultSetType;
		return this;
	}

	@Override
	public IDbDataQuery<T> setResultSetConcurrency(final int resultSetConcurrency) {
		this.resultSetConcurrency = resultSetConcurrency;
		return this;
	}

	public Object doResultSetMetaData(final ResultSetMetaDataCallback callback) {
		return getJdbcProvider().queryObject(new SQLValue(
				getJdbcProvider().getJdbcDialect().toConditionSQL(sqlVal.getSql(), "1 = 2"),
				sqlVal.getValues()), new IQueryExtractor<Object>() {
					@Override
					public Object extractData(final ResultSet rs) throws SQLException, ADOException {
						return callback.doResultSetMetaData(rs.getMetaData());
					}
				});
	}

	public static interface ResultSetMetaDataCallback {

		Object doResultSetMetaData(ResultSetMetaData metaData) throws SQLException;
	}
}
