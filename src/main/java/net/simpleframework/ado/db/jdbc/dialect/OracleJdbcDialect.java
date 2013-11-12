package net.simpleframework.ado.db.jdbc.dialect;

import java.sql.ResultSet;
import java.sql.SQLException;

import net.simpleframework.ado.db.jdbc.DatabaseMeta;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class OracleJdbcDialect extends DefaultJdbcDialect {

	@Override
	public String toLimitSQL(final String sql, final int i, final int fetchSize) {
		final StringBuilder sb = new StringBuilder();
		sb.append("select * from (select ROWNUM as rn, t_orcl.* from (").append(sql)
				.append(") t_orcl where ROWNUM <= ").append(i + fetchSize).append(") where rn > ")
				.append(i);
		return sb.toString();
	}

	@Override
	protected String getDbType() {
		return DatabaseMeta.Oracle;
	}

	@Override
	public Object getResultSetValue(final ResultSet rs, final int columnIndex,
			final Class<?> requiredType) throws SQLException {
		Object obj = super.getResultSetValue(rs, columnIndex, requiredType);
		if (obj != null) {
			final String className = obj.getClass().getName();
			if ("oracle.sql.TIMESTAMP".equals(className) || "oracle.sql.TIMESTAMPTZ".equals(className)) {
				obj = rs.getTimestamp(columnIndex);
			} else if (className.startsWith("oracle.sql.DATE")) {
				final String metaDataClassName = rs.getMetaData().getColumnClassName(columnIndex);
				if ("java.sql.Timestamp".equals(metaDataClassName)
						|| "oracle.sql.TIMESTAMP".equals(metaDataClassName)) {
					obj = rs.getTimestamp(columnIndex);
				} else {
					obj = rs.getDate(columnIndex);
				}
			}
		}
		return obj;
	}
}
