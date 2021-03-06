package net.simpleframework.ado.db.jdbc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.jdbc.dialect.IJdbcDialect;
import net.simpleframework.ado.trans.ITransactionCallback;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IJdbcProvider {

	/**
	 * 获取数据源
	 * 
	 * @return
	 */
	DataSource getDataSource();

	Connection getConnection(boolean autoCommit) throws SQLException;

	Connection getConnection() throws SQLException;

	DatabaseMeta getDatabaseMeta();

	/**
	 * 获取Statement的创建者
	 * 
	 * @return
	 */
	IStatementCreator getStatementCreator();

	/**
	 * 执行并提供连接回调
	 * 
	 * @param dataSource
	 * @param callback
	 * @return
	 */
	<T> T doExecute(IConnectionCallback<T> callback);

	/**
	 * 批量更新
	 * 
	 * @param sqlArr
	 * @return
	 */
	int[] doBatch(CharSequence[] sqlArr);

	int[] doBatch(CharSequence sql, int batchCount, IBatchValueSetter setter);

	/**
	 * 更新
	 * 
	 * @param sqlValue
	 * @return
	 */
	int doUpdate(SQLValue sqlVal);

	/**
	 * 查询对象
	 * 
	 * @param sqlValue
	 * @param extractor
	 * @return
	 */
	<T> T queryObject(SQLValue sqlVal, IQueryExtractor<T> extractor);

	void doQuery(SQLValue sqlVal, IQueryCallback callback);

	/**
	 * 获取本地sql对象
	 * 
	 * @return
	 */
	IJdbcDialect getJdbcDialect();

	/**
	 * 执行查询操作
	 * 
	 * @param sqlVal
	 * @param callback
	 * @param resultSetType
	 * @param resultSetConcurrency
	 */
	void doQuery(SQLValue sqlVal, IQueryCallback callback, int resultSetType,
			int resultSetConcurrency);

	/**
	 * 执行事务及回调
	 * 
	 * @param callback
	 * @return
	 */
	<T> T doExecuteTransaction(ITransactionCallback<T> callback);

	boolean inTrans();

	void closeAll(Connection connection, Statement stat, ResultSet rs);

	<T> T doAutoCommit(ITransactionCallback<T> callback);
}
