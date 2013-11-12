package net.simpleframework.ado.db;

import java.util.Map;

import net.simpleframework.ado.IADOManager;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.event.IDbListener;
import net.simpleframework.ado.db.jdbc.DatabaseMeta;
import net.simpleframework.ado.db.jdbc.IBatchValueSetter;
import net.simpleframework.ado.db.jdbc.IConnectionCallback;
import net.simpleframework.ado.db.jdbc.IQueryExtractor;
import net.simpleframework.ado.trans.ITransactionManager;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public interface IDbManager extends IADOManager, ITransactionManager {

	/**
	 * 获取元数据
	 * 
	 * @return
	 */
	DatabaseMeta getDatabaseMeta();

	/* query */
	<T> T executeQuery(SQLValue value, IQueryExtractor<T> extractor);

	/**
	 * 查询并返回Map对象
	 * 
	 * @param value
	 * @return
	 */
	Map<String, Object> executeQuery(SQLValue value);

	IDbDataQuery<Map<String, Object>> executeQuerySet(SQLValue sqlVal);

	/* execute */
	<T> T execute(IConnectionCallback<T> connection);

	/* update */
	int execute(IDbListener l, SQLValue... sqlValues);

	int execute(SQLValue... sqlValues);

	int executeTransaction(IDbListener l, SQLValue... sqlValues);

	int executeTransaction(SQLValue... sqlValues);

	/* batch */
	int[] batchUpdate(String... sql);

	int[] batchUpdate(String sql, int batchCount, IBatchValueSetter setter);
}
