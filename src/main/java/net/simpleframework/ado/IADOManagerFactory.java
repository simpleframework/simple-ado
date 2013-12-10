package net.simpleframework.ado;

import java.util.Collection;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IADOManagerFactory {

	/**
	 * 获取所有的实体管理器
	 * 
	 * @return
	 */
	Collection<? extends IADOManager> allEntityManager();
}
