package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IJdbcTransactionEvent {

	/**
	 * 异常执行操作中触发
	 * 
	 * @param connection
	 */
	void onThrowable(Connection connection);

	void onSuccess(Connection connection);

	/**
	 * 完成操作中触发
	 * 
	 * @param connection
	 */
	void onFinally(Connection connection);

	public static class JdbcTransactionEvent implements IJdbcTransactionEvent {
		protected final List<Object> objects = new ArrayList<Object>();

		public void addObject(final Object object) {
			objects.add(object);
		}

		@Override
		public void onThrowable(final Connection connection) {
		}

		@Override
		public void onFinally(final Connection connection) {
			for (final Object object : objects) {
				onFinally(connection, object);
			}
		}

		protected void onFinally(final Connection connection, final Object object) {
		}

		@Override
		public void onSuccess(final Connection connection) {
			for (final Object object : objects) {
				onSuccess(connection, object);
			}
		}

		protected void onSuccess(final Connection connection, final Object object) {
		}
	}
}
