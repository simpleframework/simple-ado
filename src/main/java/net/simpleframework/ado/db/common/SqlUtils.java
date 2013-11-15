package net.simpleframework.ado.db.common;

import net.simpleframework.common.logger.Log;
import net.simpleframework.common.logger.LogFactory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public abstract class SqlUtils {
	static Log log = LogFactory.getLogger(SqlUtils.class);

	public static String sqlEscape(final String aString) {
		if (aString == null) {
			return "";
		}
		if (aString.indexOf("'") == -1) {
			return aString;
		}
		final StringBuilder aBuffer = new StringBuilder(aString);
		int insertOffset = 0;
		for (int i = 0; i < aString.length(); i++) {
			if (aString.charAt(i) == '\'') {
				aBuffer.insert(i + insertOffset++, "'");
			}
		}
		return aBuffer.toString();
	}

	public static String trimSQL(final String sql) {
		return sql == null ? "" : sql.trim().replaceAll("  +", " ").replaceAll(" *, *", ",");
	}

	public static String getIdsSQLParam(final String idColumnName, final int size) {
		final StringBuilder sb = new StringBuilder();
		sb.append(idColumnName);
		if (size == 1) {
			sb.append(" = ?");
		} else {
			sb.append(" in (");
			for (int i = 0; i < size; i++) {
				if (i > 0) {
					sb.append(",");
				}
				sb.append("?");
			}
			sb.append(")");
		}
		return sb.toString();
	}
}
