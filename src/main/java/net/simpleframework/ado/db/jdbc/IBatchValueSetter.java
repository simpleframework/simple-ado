package net.simpleframework.ado.db.jdbc;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IBatchValueSetter {

	/**
	 * 
	 * @param ps
	 * @param row
	 * @throws SQLException
	 */
	void setValues(PreparedStatement ps, int row) throws SQLException;
}
