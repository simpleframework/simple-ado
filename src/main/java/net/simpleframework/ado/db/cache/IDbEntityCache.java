package net.simpleframework.ado.db.cache;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public interface IDbEntityCache {

	/**
	 * 
	 * @param key
	 * @return
	 */
	Object getCache(String key);

	/**
	 * 
	 * @param key
	 * @param data
	 */
	void putCache(String key, Object data);

	/**
	 * 
	 * @param key
	 */
	void removeCache(String key);

	void removeVal(Object val);

	/**
	 * 设置cache的最大空间值
	 * 
	 * @param maxCacheSize
	 */
	void setMaxCacheSize(int maxCacheSize);
}
