package net.simpleframework.ado.db.jdbc.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import net.simpleframework.ado.db.jdbc.DatabaseMeta;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class PostgresqlJdbcDialect extends DefaultJdbcDialect {

	@Override
	public String toLimitSQL(final String sql, final int i, final int fetchSize) {
		final StringBuilder sb = new StringBuilder();
		sb.append(sql).append(" limit ");
		sb.append(fetchSize).append(" offset ").append(i);
		return sb.toString();
	}

	@Override
	protected String getDbType() {
		return DatabaseMeta.POSTGRESQL;
	}

	@Override
	public int getParameterType(final Class<?> paramType) {
		if (boolean.class.equals(paramType) || Boolean.class.equals(paramType)) {
			return Types.BOOLEAN;
		}
		return super.getParameterType(paramType);
	}

	@Override
	protected Object getBlobObject(final ResultSet rs, final int columnIndex) throws SQLException {
		return rs.getBinaryStream(columnIndex);
	}

	@Override
	protected Object getClobObject(final ResultSet rs, final int columnIndex) throws SQLException {
		return rs.getCharacterStream(columnIndex);
	}
}
