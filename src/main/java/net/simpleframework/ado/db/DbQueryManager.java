package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.jdbc.IQueryExtractor;
import net.simpleframework.ado.trans.ITransactionCallback;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DbQueryManager extends AbstractDbManager implements IDbQueryManager {

	@Override
	public Map<String, Object> queryForMap(final CharSequence sql, final Object... params) {
		return queryForMap(new SQLValue(sql, params));
	}

	@Override
	public Map<String, Object> queryForMap(final SQLValue sqlVal) {
		return executeQuery(sqlVal);
	}

	@Override
	public IDbDataQuery<Map<String, Object>> query(final SQLValue sqlVal) {
		return executeQuerySet(sqlVal);
	}

	@Override
	public IDbDataQuery<Map<String, Object>> query(final CharSequence sql, final Object... params) {
		return query(new SQLValue(sql, params));
	}

	@Override
	public long queryForLong(final SQLValue sqlVal) {
		return executeQuery(sqlVal, new IQueryExtractor<Long>() {
			@Override
			public Long extractData(final ResultSet rs) throws SQLException, ADOException {
				return rs.next() ? rs.getLong(1) : 0l;
			}
		});
	}

	@Override
	public int queryForInt(final SQLValue sqlVal) {
		return executeQuery(sqlVal, new IQueryExtractor<Integer>() {
			@Override
			public Integer extractData(final ResultSet rs) throws SQLException, ADOException {
				return rs.next() ? rs.getInt(1) : 0;
			}
		});
	}

	@Override
	public boolean queryForBool(final SQLValue sqlVal) {
		return executeQuery(sqlVal, new IQueryExtractor<Boolean>() {
			@Override
			public Boolean extractData(final ResultSet rs) throws SQLException, ADOException {
				return rs.next() ? rs.getBoolean(1) : false;
			}
		});
	}

	@Override
	public <T> IDbDataQuery<T> query(final SQLValue value, final Class<T> beanClass) {
		return createQueryEntitySet(null, value, beanClass);
	}

	@Override
	public <T> T doExecuteTransaction(final ITransactionCallback<T> callback) {
		return getJdbcProvider().doExecuteTransaction(callback);
	}
}
