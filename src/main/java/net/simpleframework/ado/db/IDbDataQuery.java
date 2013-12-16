package net.simpleframework.ado.db;

import javax.sql.DataSource;

import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.query.IDataQuery;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbDataQuery<T> extends IDataQuery<T> {

	/**
	 * 获取数据源
	 * 
	 * @return
	 */
	DataSource getDataSource();

	/**
	 * 当前query的sql对象
	 * 
	 * @return
	 */
	SQLValue getSqlValue();

	/**
	 * 添加条件并重置
	 * 
	 * @param ev
	 */
	void addCondition(ExpressionValue ev);

	/**
	 * 添加排序列
	 * 
	 * @param columns
	 */
	void addOrderBy(DbTableColumn... columns);

	IDbDataQuery<T> setResultSetType(int resultSetType);

	IDbDataQuery<T> setResultSetConcurrency(int resultSetConcurrency);
}
