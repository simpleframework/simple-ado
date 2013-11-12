package net.simpleframework.ado;

import net.simpleframework.common.I18n;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public abstract class ADOUtils {

	public static void doInit() {
		I18n.addBasename(IADOManagerFactory.class);
	}
}
