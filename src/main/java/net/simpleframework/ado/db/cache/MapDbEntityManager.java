package net.simpleframework.ado.db.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.coll.LRUMap;
import net.simpleframework.common.object.ObjectUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class MapDbEntityManager<T> extends AbstractCacheDbEntityManager<T> {

	public MapDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
	}

	public MapDbEntityManager() {
		super(null);
	}

	private int maxCacheSize;
	{
		setMaxCacheSize(5000);
	}

	private Map<Object, Object> idCache, vCache;

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	@Override
	public void setMaxCacheSize(final int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
		if (maxCacheSize > 0) {
			idCache = Collections.synchronizedMap(new LRUMap<Object, Object>(maxCacheSize));
			vCache = Collections.synchronizedMap(new LRUMap<Object, Object>(maxCacheSize));
		} else {
			idCache = new ConcurrentHashMap<Object, Object>();
			vCache = new ConcurrentHashMap<Object, Object>();
		}
	}

	@Override
	public synchronized void reset() {
		idCache.clear();
	}

	@Override
	public Object getCache(final String key) {
		final Object id = idCache.get(key);
		return id == null ? null : vCache.get(id);
	}

	@Override
	public void putCache(final String key, final Object val) {
		Object id = null;
		if (val instanceof IIdBeanAware) {
			id = (((IIdBeanAware) val).getId()).getValue();
		} else if (val instanceof Map) {
			id = ((Map<?, ?>) val).get("ID");
		}
		if (id == null) {
			id = ObjectUtils.hashStr(val);
		}
		idCache.put(key, id);
		vCache.put(id, val);
	}

	@Override
	public void removeCache(final String key) {
		idCache.remove(key);
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		sb.append(super.toString());
		sb.append("\r\n");
		sb.append("    maxCacheSize=").append(maxCacheSize).append(", cacheSize=")
				.append(vCache.size());
		return sb.toString();
	}
}
