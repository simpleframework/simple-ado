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
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
			final String id = idCache.get(key);
			if (id != null) {
				return IoUtils.deserialize(jedis.get(id.getBytes()));
			}
		} catch (final Exception e) {
			// 释放redis对象
			getLog().warn(e);
		} finally {
			// 返还到连接池
			if (jedis != null) {
				jedis.close();
			}
		}
		return null;
	}

	@Override
	public void putCache(final String key, final Object val) {
		Jedis jedis = null;
		try {
			jedis = pool.getResource();
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
			getLog().warn(e);
		} finally {
			if (jedis != null) {
				jedis.close();
			}
		}
	}

	@Override
	public void removeCache(final String key) {
		final String id = idCache.remove(key);
		if (id != null) {
			Jedis jedis = null;
			try {
				jedis = pool.getResource();
				jedis.del(id.getBytes());
			} catch (final Exception e) {
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
				jedis.del(id.getBytes());
			} catch (final Exception e) {
				getLog().warn(e);
			} finally {
				if (jedis != null) {
					jedis.close();
				}
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
