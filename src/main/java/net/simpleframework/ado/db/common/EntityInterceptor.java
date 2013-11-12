package net.simpleframework.ado.db.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface EntityInterceptor {

	/**
	 * 定义实体bean的拦截监听器.
	 * 
	 * @return
	 */
	String[] listenerTypes();

	/**
	 * 返回要处理的列
	 * 
	 * @return
	 */
	String[] columns() default {};
}
