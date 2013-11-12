package net.simpleframework.ado.db.jdbc.dialect;

import net.simpleframework.ado.db.jdbc.DatabaseMeta;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class MySqlJdbcDialect extends DefaultJdbcDialect {

	@Override
	public String toLimitSQL(final String sql, final int i, final int fetchSize) {
		final StringBuilder sb = new StringBuilder();
		sb.append(sql).append(" limit ");
		sb.append(i).append(",").append(fetchSize);
		return sb.toString();
	}

	@Override
	protected String getDbType() {
		return DatabaseMeta.MySQL;
	}
}
