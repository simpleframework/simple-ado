package net.simpleframework.ado.db.cache;

import java.io.IOException;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.IoUtils;
import redis.clients.jedis.Jedis;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class JedisDbEntityManager<T> extends AbstractCacheDbEntityManager<T> {
	public JedisDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
	}

	public JedisDbEntityManager() {
		super(null);
	}

	private Jedis jedis;

	public void setJedis(final Jedis jedis) {
		this.jedis = jedis;
	}

	public Jedis getJedis() {
		return jedis;
	}

	@Override
	public Object getCache(final String key) {
		try {
			return IoUtils.deserialize(jedis.get(key.getBytes()));
		} catch (final IOException e) {
			throw ADOException.of(e);
		} catch (final ClassNotFoundException e) {
		}
		return null;
	}

	@Override
	public void putCache(final String key, final Object data) {
		try {
			jedis.set(key.getBytes(), IoUtils.serialize(data));
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
	}

	@Override
	public void removeCache(final String key) {
		jedis.del(key.getBytes());
	}

	@Override
	public void setMaxCacheSize(final int maxCacheSize) {
	}
}
