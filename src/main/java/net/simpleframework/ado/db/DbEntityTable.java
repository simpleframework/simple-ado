package net.simpleframework.ado.db;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import net.simpleframework.ado.ColumnData;
import net.simpleframework.common.StringUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DbEntityTable implements Serializable {

	private final Class<?> beanClass;

	private final String name;

	private String[] uniqueColumns;

	private String[] cacheColumns;

	private boolean noCache;

	private ColumnData defaultOrder;

	/* cache的最大值 */
	private int maxCacheSize;

	public DbEntityTable(final Class<?> beanClass, final String name) {
		this(beanClass, name, 0);
	}

	public DbEntityTable(final Class<?> beanClass, final String name, final int maxCacheSize) {
		this.beanClass = beanClass;
		this.name = name;
		this.uniqueColumns = new String[] { "id" };
		this.maxCacheSize = maxCacheSize;
	}

	public String getName() {
		return name != null ? name.trim().toUpperCase() : null;
	}

	public String[] getUniqueColumns() {
		return uniqueColumns;
	}

	public DbEntityTable setUniqueColumns(final String... uniqueColumns) {
		this.uniqueColumns = uniqueColumns;
		return this;
	}

	public String[] getCacheColumns() {
		return cacheColumns == null ? getUniqueColumns() : cacheColumns;
	}

	public DbEntityTable setCacheColumns(final String... cacheColumns) {
		this.cacheColumns = cacheColumns;
		return this;
	}

	public boolean isNoCache() {
		return noCache;
	}

	public DbEntityTable setNoCache(final boolean noCache) {
		this.noCache = noCache;
		return this;
	}

	public ColumnData getDefaultOrder() {
		return defaultOrder;
	}

	public DbEntityTable setDefaultOrder(final ColumnData defaultOrder) {
		this.defaultOrder = defaultOrder;
		return this;
	}

	public int getMaxCacheSize() {
		return maxCacheSize;
	}

	public void setMaxCacheSize(final int maxCacheSize) {
		this.maxCacheSize = maxCacheSize;
	}

	public Class<?> getBeanClass() {
		return beanClass;
	}

	@SuppressWarnings("unchecked")
	public Map<String, DbTableColumn> getTableColumns() {
		final Class<?> beanClass = getBeanClass();
		return beanClass == null ? Collections.EMPTY_MAP
				: DbTableColumn.getTableColumns(getBeanClass());
	}

	public String getSqlName(final String propertyName) {
		final DbTableColumn tCol = getTableColumns().get(propertyName);
		return tCol != null ? tCol.getAlias() : propertyName;
	}

	public String getBeanPropertyName(final String sqlName) {
		for (final DbTableColumn tCol : getTableColumns().values()) {
			if (sqlName.equalsIgnoreCase(tCol.getAlias())) {
				return tCol.getName();
			}
		}
		return sqlName;
	}

	@Override
	public String toString() {
		return getName() + ", unique[" + StringUtils.join(uniqueColumns, "-") + "]";
	}

	private static final long serialVersionUID = -6445073606291514860L;
}
