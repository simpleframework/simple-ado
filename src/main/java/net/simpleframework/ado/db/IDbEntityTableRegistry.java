package net.simpleframework.ado.db;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityTableRegistry {

	/**
	 * 创建DbEntityTable实例，并由系统自动注册
	 * 
	 * @return
	 */
	DbEntityTable createEntityTable();
}
