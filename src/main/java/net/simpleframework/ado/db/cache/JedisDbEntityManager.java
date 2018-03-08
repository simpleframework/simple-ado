package net.simpleframework.ado.db.cache;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.IoUtils_hessian;
import net.simpleframework.common.coll.KVMap;
import net.simpleframework.common.coll.LRUMap;
import net.simpleframework.common.jedis.JedisMap;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class JedisDbEntityManager<T> extends AbstractCacheDbEntityManager<T> {

	public JedisDbEntityManager() {
		this(null);
	}

	public JedisDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);

		final int maxCacheSize = getMaxCacheSize();
		if (maxCacheSize > 0) {
			keysCache = Collections.synchronizedMap(new LRUMap<String, Set<String>>(maxCacheSize));
		} else {
			keysCache = new ConcurrentHashMap<>();
		}
	}

	public static int DEFAULT_EXPIRE_TIME = 60 * 60 * 24;

	private int expire = DEFAULT_EXPIRE_TIME;

	private JedisPool pool;

	public void setJedisPool(final JedisPool pool) {
		this.pool = pool;
		idCache = new JedisMap(pool, false, "JedisDbEntityManager:idCache", 3600);
	}

	public void setExpire(final int expire) {
		this.expire = expire;
	}

	@Override
	public Object getCache(final String key) {
		final KVMap kv = REQUEST_THREAD_CACHE.get();
		Jedis jedis = null;
		try {
			final String id = (String) idCache.get(key);
			if (id == null) {
				return null;
			}

			Object val = null;
			if (kv != null) {
				val = kv.get(id);
			}
			if (val == null) {
				jedis = pool.getResource();
				val = deserialize(jedis.get(id.getBytes()));
				if (kv != null) {
					kv.put(id, val);
				}
			}
			return val;
		} catch (final Throwable e) {
			removeCache(key);
			// 释放redis对象
			getLog().warn(e);
			return null;
		} finally {
			// 返还到连接池
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public void putCache(final String key, final Object val) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			final String id = getId(val);
			if (id != null) {
				idCache.put(key, id);
				// 插入key值缓存
				putKeys(id, key);

				if (expire > 0) {
					jedis.setex(id.getBytes(), expire, serialize(val));
				} else {
					jedis.set(id.getBytes(), serialize(val));
				}
			}
		} catch (final Throwable e) {
			getLog().warn(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public void removeCache(final String key) {
		final String id = (String) idCache.remove(key);
		if (id != null) {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				jedis.del(id.getBytes());
			} catch (final Throwable e) {
				getLog().warn(e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}
		}
	}

	@Override
	public void removeVal(final Object val) {
		final String id = getId(val);
		if (id != null) {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				if (jedis.del(id.getBytes()) == 0) {
					getLog().debug("jedis: del -> 0");
				}
			} catch (final Throwable e) {
				getLog().warn(e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
			}

			// 删除id缓存
			removeKeys(id);
		}
	}

	@Override
	protected String getId(final Object val) {
		final String id = super.getId(val);
		if (id == null) {
			return null;
		}
		return val.getClass().getSimpleName() + ":" + id;
	}

	private byte[] serialize(final Object obj) throws IOException {
		return IoUtils_hessian.serialize(obj);
		// return IoUtils_kryo.serialize(obj, getBeanClass());
	}

	private Object deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
		return IoUtils_hessian.deserialize(bytes);
		// return IoUtils_kryo.deserialize(bytes, getBeanClass());
	}
}
