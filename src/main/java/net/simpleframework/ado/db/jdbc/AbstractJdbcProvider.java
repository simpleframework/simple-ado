package net.simpleframework.ado.db.jdbc;

import java.sql.ResultSet;

import javax.sql.DataSource;

import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.jdbc.dialect.DefaultJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.HSQLJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.IJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.MySqlJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.OracleJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.PostgresqlJdbcDialect;
import net.simpleframework.ado.db.jdbc.dialect.SqlServerJdbcDialect;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class AbstractJdbcProvider extends ObjectEx implements IJdbcProvider {

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
}
