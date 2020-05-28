package net.simpleframework.ado.db.cache;

import java.io.IOException;
import java.util.Map;

import net.simpleframework.ado.db.DbEntityTable;
import net.simpleframework.common.SerializeUtils;
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
	public static int DEFAULT_EXPIRE_TIME = 60 * 60 * 24;

	private int expire = DEFAULT_EXPIRE_TIME;

	public JedisDbEntityManager() {
		this(null);
	}

	public JedisDbEntityManager(final DbEntityTable entityTable) {
		super(entityTable);
	}

	private JedisPool pool;

	public void setJedisPool(final JedisPool pool) {
		this.pool = pool;
	}

	public void setExpire(final int expire) {
		this.expire = expire;
	}

	@Override
	protected Map<String, Object> createAutoincMap() {
		return new JedisMap(pool, "AUTO_INC");
	}

	@Override
	public Object getCache(final String key) {
		Jedis jedis = null;
		try {
			final String id = getIdCache(key);
			if (id == null) {
				return null;
			}

			// 获取当前事务下的修改对象
			final Object val = getTransObj(id);
			if (val != null) {
				return val;
			}

			jedis = pool.getResource();
			return deserialize(jedis.get(id.getBytes()));
		} catch (final Throwable e) {
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
				putIdCache(key, id);

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
	public void removeVal(final Object val) {
		final String id = getId(val);
		if (id != null) {
			Jedis jedis = null;
			try {
				// 删除idCache
				removeIdCache(id);

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
		}
	}

	private byte[] serialize(final Object obj) throws IOException {
		return SerializeUtils.serialize(obj);
		// return IoUtils_kryo.serialize(obj, getBeanClass());
	}

	private Object deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
		return SerializeUtils.deserialize(bytes);
		// return IoUtils_kryo.deserialize(bytes, getBeanClass());
	}
}
