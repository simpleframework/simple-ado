package net.simpleframework.ado.db;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.sql.DataSource;

import net.simpleframework.ado.IADOManagerFactory;
import net.simpleframework.ado.db.cache.MapDbEntityManager;
import net.simpleframework.ado.db.jdbc.DefaultJdbcProvider;
import net.simpleframework.ado.db.jdbc.IJdbcProvider;
import net.simpleframework.common.object.ObjectEx;
import net.simpleframework.common.object.ObjectFactory;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class DbManagerFactory extends ObjectEx implements IADOManagerFactory {

	private static Map<DataSource, DbManagerFactory> mFactoryCache = new HashMap<DataSource, DbManagerFactory>();

	public static DbManagerFactory get(final DataSource dataSource) {
		DbManagerFactory factory = mFactoryCache.get(dataSource);
		if (factory == null) {
			mFactoryCache.put(dataSource, factory = new DbManagerFactory(dataSource));
		}
		return factory;
	}

	protected IJdbcProvider jdbcProvider;

	public DbManagerFactory(final DataSource dataSource) {
		jdbcProvider = new DefaultJdbcProvider(dataSource);
	}

	protected Map<Class<?>, IDbEntityManager<?>> eManagerCache;
	{
		eManagerCache = new ConcurrentHashMap<Class<?>, IDbEntityManager<?>>();
	}

	public IDbEntityManager<?> createEntityManager(final Class<?> beanClass) {
		return ObjectFactory.create(MapDbEntityManager.class);
	}

	public DbManagerFactory regist(final DbEntityTable... eTables) {
		for (final DbEntityTable eTable : eTables) {
			final Class<?> beanClass = eTable.getBeanClass();
			final DbEntityManager<?> eManager = (DbEntityManager<?>) createEntityManager(beanClass);
			eManager.setEntityTable(eTable).setDbManagerFactory(this);
			eManagerCache.put(beanClass, eManager);
		}
		return this;
	}

	@Override
	public Collection<IDbEntityManager<?>> allEntityManager() {
		return eManagerCache.values();
	}

	public DbEntityTable getEntityTable(final String name) {
		for (final IDbEntityManager<?> mgr : allEntityManager()) {
			final DbEntityTable eTbl = mgr.getEntityTable();
			if (eTbl.getName().equalsIgnoreCase(name)) {
				return eTbl;
			}
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	public <T> IDbEntityManager<T> getEntityManager(final Class<T> beanClass) {
		final Class<?> nClass = ObjectFactory.original(beanClass);
		final IDbEntityManager<?> te = eManagerCache.get(nClass);
		if (te != null) {
			return (IDbEntityManager<T>) te;
		}
		for (final Map.Entry<Class<?>, IDbEntityManager<?>> e : eManagerCache.entrySet()) {
			final Class<?> beanClass2 = e.getKey();
			// 通过父类得到
			if (nClass.isAssignableFrom(beanClass2)) {
				return (IDbEntityManager<T>) e.getValue();
			}
		}
		return null;
	}

	public IDbQueryManager createQueryManager() {
		final DbQueryManager qmgr = new DbQueryManager();
		qmgr.setDbManagerFactory(this);
		return qmgr;
	}

	private IDbQueryManager queryManager;

	public IDbQueryManager getQueryManager() {
		if (queryManager == null) {
			queryManager = createQueryManager();
		}
		return queryManager;
	}
}
