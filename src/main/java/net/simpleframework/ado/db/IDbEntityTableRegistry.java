package net.simpleframework.ado.db;

import net.simpleframework.ado.IADOManagerFactoryAware;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityTableRegistry extends IADOManagerFactoryAware {

	/**
	 * 创建DbEntityTable实例，并由系统自动注册
	 * 
	 * @return
	 */
	DbEntityTable[] createEntityTables();
}
