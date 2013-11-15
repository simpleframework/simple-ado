package net.simpleframework.ado.db.jdbc;

import net.simpleframework.common.StringUtils;
import net.simpleframework.common.coll.ParameterMap;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DatabaseMeta {
	public final static String MySQL = "mysql";

	public final static String Oracle = "oracle";

	public final static String HSQL = "hsql";

	public final static String MSSQL_SERVER = "sqlserver";

	public final static String POSTGRESQL = "postgresql";

	String _url;

	String _databaseProductName;

	private final ParameterMap _alias = new ParameterMap().add("Oracle", Oracle)
			.add("Microsoft SQL Server", MSSQL_SERVER).add("MySQL", MySQL)
			.add("HSQL Database Engine", HSQL).add("PostgreSQL", POSTGRESQL);

	public String getDatabaseProductName() {
		return StringUtils.blank(_alias.get(_databaseProductName));
	}

	public String getUrl() {
		return _url;
	}

	public boolean isOracle() {
		return Oracle.equals(getDatabaseProductName());
	}

	public boolean isMySql() {
		return MySQL.equals(getDatabaseProductName());
	}

	public boolean isHSql() {
		return HSQL.equals(getDatabaseProductName());
	}

	public boolean isMSSql() {
		return MSSQL_SERVER.equals(getDatabaseProductName());
	}

	public boolean isPostgreSql() {
		return POSTGRESQL.equals(getDatabaseProductName());
	}
}
