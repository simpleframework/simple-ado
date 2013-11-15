package net.simpleframework.ado.db.jdbc;

import java.io.IOException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.common.ClassUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.object.ObjectEx;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DefaultStatementCreator extends ObjectEx implements IStatementCreator {
	private final IJdbcProvider jdbcProvider;

	public DefaultStatementCreator(final IJdbcProvider jdbcProvider) {
		this.jdbcProvider = jdbcProvider;
	}

	@Override
	public PreparedStatement prepareStatement(final Connection connection, final SQLValue sqlVal)
			throws SQLException {
		return prepareStatement(connection, sqlVal, ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_READ_ONLY);
	}

	@Override
	public PreparedStatement prepareStatement(final Connection connection, final SQLValue sqlVal,
			final int resultSetType, final int resultSetConcurrency) throws SQLException {
		final PreparedStatement ps = getNativeConnection(connection).prepareStatement(
				sqlVal.getSql(), resultSetType, resultSetConcurrency);
		final Object[] values = getParameterValues(sqlVal.getValues());
		if (values != null) {
			for (int i = 1; i <= values.length; i++) {
				final Object value = values[i - 1];
				try {
					jdbcProvider.getJdbcDialect().setParameterValue(ps, i, value);
				} catch (final IOException e) {
					throw ADOException.of(e);
				}
			}
		}
		return ps;
	}

	@Override
	public PreparedStatement prepareStatement(final Connection connection, final String sql)
			throws SQLException {
		return getNativeConnection(connection).prepareStatement(sql);
	}

	@Override
	public Statement createStatement(final Connection connection) throws SQLException {
		return getNativeConnection(connection).createStatement();
	}

	private Object[] getParameterValues(final Object[] values) {
		if (values == null) {
			return null;
		}
		final Object[] newValues = new Object[values.length];
		for (int i = 0; i < values.length; i++) {
			if (values[i] instanceof Enum<?>) {
				newValues[i] = ((Enum<?>) values[i]).ordinal();
			} else if (values[i] instanceof ID) {
				newValues[i] = ((ID) values[i]).getValue();
			} else if (values[i] instanceof IIdBeanAware) {
				newValues[i] = ((IIdBeanAware) values[i]).getId().getValue();
			} else {
				newValues[i] = values[i];
			}
		}
		return newValues;
	}

	private Connection getNativeConnection(final Connection connection) throws SQLException {
		final Class<?> cClazz = connection.getClass();
		final String clazzName = cClazz.getName();
		try {
			if (clazzName.startsWith("com.mchange.v2.c3p0")) {
				if (getRawConnectionMethod == null) {
					getRawConnectionMethod = JdbcUtils.class.getMethod("getNativeConnection",
							Connection.class);
				}
				if (rawConnectionOperationMethod == null) {
					rawConnectionOperationMethod = cClazz.getMethod("rawConnectionOperation",
							Method.class, Object.class, Object[].class);
				}
				if (RAW_CONNECTION == null) {
					RAW_CONNECTION = cClazz.getField("RAW_CONNECTION").get(null);
				}
				return (Connection) ClassUtils.invoke(rawConnectionOperationMethod, connection,
						getRawConnectionMethod, null, new Object[] { RAW_CONNECTION });
			} else if (clazzName.startsWith("weblogic.jdbc")) {
				if (getVendorConnectionMethod == null) {
					getVendorConnectionMethod = ClassUtils.forName(
							"weblogic.jdbc.extensions.WLConnection").getMethod("getVendorConnection");
				}
				return (Connection) ClassUtils.invoke(getVendorConnectionMethod, connection);
			}
		} catch (final Exception e) {
			log.warn(e);
		}
		return connection;
	}

	// c3p0
	private Method getRawConnectionMethod, rawConnectionOperationMethod;
	private Object RAW_CONNECTION;

	// weblogic
	private Method getVendorConnectionMethod;
}
