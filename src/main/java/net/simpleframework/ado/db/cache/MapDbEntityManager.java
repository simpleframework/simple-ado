package net.simpleframework.ado.db.cache;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.coll.LRUMap;

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
		final int maxCacheSize = entityTable.getMaxCacheSize();
		if (maxCacheSize > 0) {
			setMaxCacheSize(maxCacheSize);
		}
	}

	public MapDbEntityManager() {
		super(null);
	}

	private Map<String, Object> vCache;

	@Override
	public void setMaxCacheSize(final int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
		if (maxCacheSize > 0) {
			vCache = Collections.synchronizedMap(new LRUMap<String, Object>(maxCacheSize));
		} else {
			vCache = new ConcurrentHashMap<>();
		}
	}

	@Override
	public Object getCache(final String key) {
		final String id = idCache.get(key);
		return id != null ? vCache.get(id) : null;
	}

	@Override
	public void putCache(final String key, final Object val) {
		final String id = getId(val);
		if (id != null) {
			// 插入id缓存
			idCache.put(key, id);
			// 插入key值缓存
			putKeys(id, key);
			// 插入值缓存
			vCache.put(id, val);
		}
	}

	@Override
	public void removeVal(final Object val) {
		final String id = getId(val);
		if (id != null) {
			// 删除值缓存
			vCache.remove(id);
			// 删除id缓存
			removeKeys(id);
		}
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
