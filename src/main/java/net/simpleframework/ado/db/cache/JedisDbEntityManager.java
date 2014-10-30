package net.simpleframework.ado.db.cache;

import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.IoUtils;
import net.simpleframework.common.JedisUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885) https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class JedisDbEntityManager<T> extends AbstractCacheDbEntityManager<T> {
	public JedisDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
	}

	public JedisDbEntityManager() {
		super(null);
	}

	public static int DEFAULT_EXPIRE_TIME = 60 * 60 * 24;

	private int expire = DEFAULT_EXPIRE_TIME;

	private JedisPool pool;

	public void setJedisPool(final JedisPool pool) {
		this.pool = pool;
	}

	public void setExpire(final int expire) {
		this.expire = expire;
	}

	@Override
	public Object getCache(final String key) {
		final Jedis jedis = pool.getResource();
		try {
			final String id = idCache.get(key);
			if (id != null) {
				return IoUtils.deserialize(jedis.get(id.getBytes()));
			}
		} catch (final Exception e) {
			// 释放redis对象
			JedisUtils.doJedisException(pool, jedis, e);
		} finally {
			// 返还到连接池
			JedisUtils.returnResource(pool, jedis);
		}
		return null;
	}

	@Override
	public void putCache(final String key, final Object val) {
		final Jedis jedis = pool.getResource();
		try {
			final String id = getId(val);
			if (id != null) {
				idCache.put(key, id);
				if (expire > 0) {
					jedis.setex(id.getBytes(), expire, IoUtils.serialize(val));
				} else {
					jedis.set(id.getBytes(), IoUtils.serialize(val));
				}
			}
		} catch (final Exception e) {
			JedisUtils.doJedisException(pool, jedis, e);
		} finally {
			JedisUtils.returnResource(pool, jedis);
		}
	}

	@Override
	public void removeCache(final String key) {
		final String id = idCache.remove(key);
		if (id != null) {
			final Jedis jedis = pool.getResource();
			try {
				jedis.del(id.getBytes());
			} finally {
				JedisUtils.returnResource(pool, jedis);
			}
		}
	}

	@Override
	public void removeVal(final Object val) {
		final String id = getId(val);
		if (id != null) {
			final Jedis jedis = pool.getResource();
			try {
				jedis.del(id.getBytes());
			} finally {
				JedisUtils.returnResource(pool, jedis);
			}
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

	@Override
	public void setMaxCacheSize(final int maxCacheSize) {
	}
}
