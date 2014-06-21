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

	private Jedis getJedis() {
		return pool.getResource();
	}

	@Override
	public Object getCache(final String key) {
		try {
			final String id = idCache.get(key);
			if (id != null) {
				return IoUtils.deserialize(getJedis().get(id.getBytes()));
			}
		} catch (final Exception e) {
			log.warn(e);
		}
		return null;
	}

	@Override
	public void putCache(final String key, final Object val) {
		try {
			final String id = getId(val);
			if (id != null) {
				idCache.put(key, id);
				getJedis().set(id.getBytes(), IoUtils.serialize(val));
			}
		} catch (final Exception e) {
			log.warn(e);
		}
	}

	@Override
	public void removeCache(final String key) {
		final String id = idCache.remove(key);
		if (id != null) {
			getJedis().del(id.getBytes());
		}
	}

	@Override
	public void removeVal(final Object val) {
		final String id = getId(val);
		if (id != null) {
			getJedis().del(id.getBytes());
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
