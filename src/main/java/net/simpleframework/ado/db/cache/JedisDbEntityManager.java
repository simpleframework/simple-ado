package net.simpleframework.ado.db.cache;

import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.IoUtils;
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

	private JedisPool pool;

	public void setJedisPool(final JedisPool pool) {
		this.pool = pool;
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
			doJedisException(jedis, e);
		} finally {
			// 返还到连接池
			returnResource(jedis);
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
				jedis.set(id.getBytes(), IoUtils.serialize(val));
			}
		} catch (final Exception e) {
			doJedisException(jedis, e);
		} finally {
			returnResource(jedis);
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
				returnResource(jedis);
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
				returnResource(jedis);
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

	private void returnResource(final Jedis jedis) {
		if (jedis == null) {
			return;
		}
		try {
			pool.returnResource(jedis);
		} catch (final Exception e) {
			doJedisException(jedis, e);
		}
	}

	private void doJedisException(final Jedis jedis, final Exception e) {
		if (jedis != null) {
			pool.returnBrokenResource(jedis);
			log.warn(e);
		}
	}
}
