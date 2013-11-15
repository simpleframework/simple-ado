package net.simpleframework.ado.db.jdbc.dialect;

import net.simpleframework.ado.db.common.JSqlParser;
import net.simpleframework.ado.db.jdbc.DatabaseMeta;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class SqlServerJdbcDialect extends DefaultJdbcDialect {

	@Override
	public String toLimitSQL(final String sql, final int i, final int fetchSize) {
		final StringBuilder sb = new StringBuilder();
		isolate(new VoidIsolation() {
			@Override
			public void run() throws Exception {
				sb.append(JSqlParser.toSqlServerLimit(sql, i, fetchSize));
			}
		});
		if (sb.length() == 0) {
			sb.append(super.toLimitSQL(sql, i, fetchSize));
		}
		return sb.toString();
	}

	@Override
	protected String getDbType() {
		return DatabaseMeta.MSSQL_SERVER;
	}
}
