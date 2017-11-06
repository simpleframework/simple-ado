package net.simpleframework.ado.db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import net.simpleframework.ado.ADOException;
import net.simpleframework.ado.IADOListener;
import net.simpleframework.ado.IParamsValue;
import net.simpleframework.ado.UniqueValue;
import net.simpleframework.ado.bean.IIdBeanAware;
import net.simpleframework.ado.bean.IOrderBeanAware;
import net.simpleframework.ado.db.common.EntityInterceptor;
import net.simpleframework.ado.db.common.ExpressionValue;
import net.simpleframework.ado.db.common.SQLValue;
import net.simpleframework.ado.db.event.IDbEntityListener;
import net.simpleframework.ado.db.jdbc.IQueryExtractor;
import net.simpleframework.ado.trans.ITransactionCallback;
import net.simpleframework.ado.trans.TransactionObjectCallback;
import net.simpleframework.common.BeanUtils;
import net.simpleframework.common.ID;
import net.simpleframework.common.coll.ArrayUtils;

/**
 * Licensed under the Apache License, Version 2.0
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         https://github.com/simpleframework
 *         http://www.simpleframework.net
 */
public class DbEntityManager<T> extends AbstractDbManager implements IDbEntityManager<T> {

	private DbEntityTable _table;

	public DbEntityManager(final DbEntityTable table) {
		this._table = table;
	}

	public DbEntityManager() {
	}

	@Override
	public DbEntityTable getEntityTable() {
		return _table;
	}

	public DbEntityManager<T> setEntityTable(final DbEntityTable table) {
		this._table = table;
		return this;
	}

	@SuppressWarnings("unchecked")
	protected Class<T> getBeanClass() {
		return (Class<T>) getEntityTable().getBeanClass();
	}

	@Override
	public Map<String, Object> executeQuery(final String[] columns, final IParamsValue paramsValue) {
		return executeQuery(createSQLValue(columns, paramsValue));
	}

	/* select for map */

	@Override
	public Map<String, Object> queryForMap(final IParamsValue paramsValue) {
		return queryForMap(null, paramsValue);
	}

	@Override
	public Map<String, Object> queryForMap(final String[] columns, final IParamsValue paramsValue) {
		return createQueryMap(columns, createSQLValue(columns, paramsValue));
	}

	@Override
	public IDbDataQuery<Map<String, Object>> queryMapSet(final IParamsValue paramsValue) {
		return queryMapSet((String[]) null, paramsValue);
	}

	protected SQLValue createSQLValue(final String[] columns, final IParamsValue paramsValue) {
		if (paramsValue != null) {
			final Class<?> clazz = paramsValue.getClass();
			if (clazz.equals(SQLValue.class)) {
				return (SQLValue) paramsValue;
			} else if (clazz.equals(UniqueValue.class)) {
				return new SQLValue(SQLBuilder.getSelectUniqueSQL(getEntityTable(), columns),
						paramsValue != null ? paramsValue.getValues() : null);
			} else if (clazz.equals(ExpressionValue.class)) {
				return new SQLValue(
						SQLBuilder.getSelectExpressionSQL(getEntityTable(), columns,
								((ExpressionValue) paramsValue).getExpression()),
						paramsValue != null ? paramsValue.getValues() : null);
			}
		}
		return new SQLValue(SQLBuilder.getSelectExpressionSQL(getEntityTable(), columns, null));
	}

	protected String getSelectUniqueSQL(final String[] columns) {
		return SQLBuilder.getSelectUniqueSQL(getEntityTable(), columns);
	}

	@Override
	public IDbDataQuery<Map<String, Object>> queryMapSet(final String[] columns,
			final IParamsValue paramsValue) {
		return createQueryEntitySet(columns, createSQLValue(columns, paramsValue));
	}

	/* select for object */

	@Override
	public T queryForBean(final IParamsValue paramsValue) {
		return queryForBean(null, paramsValue);
	}

	private Object getIdVal(final Object id) {
		Object id2;
		if (id instanceof ID) {
			id2 = ((ID) id).getValue();
		} else if (id instanceof IIdBeanAware) {
			id2 = ((IIdBeanAware) id).getId().getValue();
		} else {
			id2 = id;
		}
		return id2;
	}

	@Override
	public T getBean(final Object id) {
		final Object id2 = getIdVal(id);
		return id2 == null ? null : queryForBean(new UniqueValue(id2));
	}

	@Override
	public T queryForBean(final String[] columns, final IParamsValue paramsValue) {
		return createQueryObject(columns, createSQLValue(columns, paramsValue), getBeanClass());
	}

	@Override
	public T getBean(final String[] columns, final Object id) {
		final Object id2 = getIdVal(id);
		return id2 == null ? null : queryForBean(columns, new UniqueValue(id2));
	}

	@Override
	public IDbDataQuery<T> queryBeans(final IParamsValue ev) {
		return queryBeans(null, ev);
	}

	@Override
	public IDbDataQuery<T> queryBeans(final String[] columns, final IParamsValue paramsValue) {
		return createQueryEntitySet(columns, createSQLValue(columns, paramsValue), getBeanClass());
	}

	@SuppressWarnings("unchecked")
	private Collection<IDbEntityListener<T>> getListener(final IDbEntityListener<T> l) {
		final ArrayList<IDbEntityListener<T>> listeners = new ArrayList<>();
		if (l != null) {
			listeners.add(l);
		}
		for (final IADOListener l2 : getListeners()) {
			if (l2 instanceof IDbEntityListener) {
				listeners.add((IDbEntityListener<T>) l2);
			}
		}
		final EntityInterceptor interceptor = getBeanClass().getAnnotation(EntityInterceptor.class);
		if (interceptor != null) {
			for (final String lClass : interceptor.listenerTypes()) {
				IDbEntityListener<T> l2 = null;
				try {
					l2 = (IDbEntityListener<T>) singleton(lClass);
				} catch (final Exception e) {
				}
				if (l2 != null) {
					listeners.add(l2);
				}
			}
		}
		return listeners;
	}

	/* delete */

	@Override
	public int delete(final IParamsValue paramsValue) {
		return delete(null, paramsValue);
	}

	protected int delete(final IDbEntityListener<T> l, final IParamsValue paramsValue) {
		if (paramsValue == null) {
			return 0;
		}
		SQLValue sqlVal = null;
		final Class<?> clazz = paramsValue.getClass();
		if (clazz.equals(SQLValue.class)) {
			sqlVal = (SQLValue) paramsValue;
		} else if (clazz.equals(UniqueValue.class)) {
			sqlVal = new SQLValue(SQLBuilder.getDeleteUniqueSQL(getEntityTable()),
					paramsValue.getValues());
		} else if (clazz.equals(ExpressionValue.class)) {
			sqlVal = new SQLValue(SQLBuilder.getDeleteExpressionSQL(getEntityTable(),
					((ExpressionValue) paramsValue).getExpression()), paramsValue.getValues());
		}
		if (sqlVal == null) {
			return 0;
		}

		int ret = 0;
		try {
			final Collection<IDbEntityListener<T>> listeners = getListener(l);
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onBeforeDelete(this, paramsValue);
			}
			ret = executeUpdate(sqlVal);
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onAfterDelete(this, paramsValue);
			}
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
		return ret;
	}

	@Override
	public int deleteTransaction(final IDbEntityListener<T> l, final IParamsValue paramsValue) {
		return doExecuteTransaction(new TransactionObjectCallback<Integer>() {

			@Override
			public Integer onTransactionCallback() throws ADOException {
				return delete(l, paramsValue);
			}
		});
	}

	@Override
	public int deleteTransaction(final IParamsValue paramsValue) {
		return deleteTransaction(null, paramsValue);
	}

	/* insert */

	@Override
	public int insertTransaction(final T... beans) {
		return insertTransaction(null, beans);
	}

	@Override
	public int insertTransaction(final IDbEntityListener<T> l, final T... beans) {
		return doExecuteTransaction(new TransactionObjectCallback<Integer>() {

			@Override
			public Integer onTransactionCallback() throws ADOException {
				return insert(l, beans);
			}
		});
	}

	@Override
	public int insert(final T... beans) {
		return insert(null, beans);
	}

	@SuppressWarnings("unchecked")
	protected int insert(final IDbEntityListener<T> l, T... beans) {
		beans = (T[]) ArrayUtils.removeDuplicatesAndNulls(beans);
		if (beans == null || beans.length == 0) {
			return 0;
		}

		int ret = 0;
		try {
			final Collection<IDbEntityListener<T>> listeners = getListener(l);
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onBeforeInsert(this, beans);
			}

			for (final Object bean : beans) {
				if (bean instanceof IIdBeanAware) {
					final IIdBeanAware idBean = (IIdBeanAware) bean;
					if (idBean.getId() == null) {
						idBean.setId(ID.uuid());
					}
					if (idBean instanceof IOrderBeanAware) {
						final IOrderBeanAware oBean = (IOrderBeanAware) bean;
						if (oBean.getOorder() == 0) {
							int max = max("oorder", null).intValue();
							oBean.setOorder(++max);
						}
					}
				}
				final SQLValue sqlVal = SQLBuilder.getInsertSQLValue(getEntityTable(), bean);
				if (sqlVal != null) {
					ret += executeUpdate(sqlVal);
				}
			}
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onAfterInsert(this, beans);
			}
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
		return ret;
	}

	/* update */

	@Override
	public int update(final T... beans) {
		return update(null, beans);
	}

	@Override
	public int update(final String[] columns, final T... beans) {
		return update(null, columns, beans);
	}

	@SuppressWarnings("unchecked")
	protected int update(final IDbEntityListener<T> l, final String[] columns, T... beans) {
		beans = (T[]) ArrayUtils.removeDuplicatesAndNulls(beans);
		if (beans == null || beans.length == 0) {
			return 0;
		}

		int ret = 0;
		try {
			final Collection<IDbEntityListener<T>> listeners = getListener(l);
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onBeforeUpdate(this, columns, beans);
			}

			for (final Object bean : beans) {
				final SQLValue sqlVal = SQLBuilder.getUpdateSQLValue(getEntityTable(), columns, bean);
				if (sqlVal != null) {
					ret += executeUpdate(sqlVal);
				}
			}
			for (final IDbEntityListener<T> listener : listeners) {
				listener.onAfterUpdate(this, columns, beans);
			}
		} catch (final Exception e) {
			throw ADOException.of(e);
		}
		return ret;
	}

	@Override
	public int updateTransaction(final IDbEntityListener<T> l, final String[] columns,
			final T... beans) {
		return doExecuteTransaction(new TransactionObjectCallback<Integer>() {

			@Override
			public Integer onTransactionCallback() throws ADOException {
				return update(l, columns, beans);
			}
		});
	}

	@Override
	public int updateTransaction(final String[] columns, final T... beans) {
		return updateTransaction(null, columns, beans);
	}

	@Override
	public int updateTransaction(final IDbEntityListener<T> l, final T... beans) {
		return updateTransaction(l, null, beans);
	}

	@Override
	public int updateTransaction(final T... beans) {
		return updateTransaction((IDbEntityListener<T>) null, beans);
	}

	@Override
	public <M> M doExecuteTransaction(final ITransactionCallback<M> callback) {
		return getJdbcProvider().doExecuteTransaction(callback);
	}

	/* utils */

	@Override
	public int count(final IParamsValue paramsValue) {
		return queryMapSet(paramsValue).getCount();
	}

	@Override
	public Number sum(final String column, final IParamsValue paramsValue) {
		return function(column, "sum", paramsValue);
	}

	@Override
	public Number avg(final String column, final IParamsValue paramsValue) {
		return function(column, "avg", paramsValue);
	}

	@Override
	public Number max(final String column, final IParamsValue paramsValue) {
		return function(column, "max", paramsValue);
	}

	private Number function(final String column, final String function,
			final IParamsValue paramsValue) {
		SQLValue sqlVal;
		if (paramsValue instanceof SQLValue) {
			sqlVal = (SQLValue) paramsValue;
		} else {
			final StringBuilder sql = new StringBuilder();
			sql.append("select ").append(function).append("(").append(column).append(") from ")
					.append(getEntityTable().getName());
			if (paramsValue != null) {
				final Class<?> clazz = paramsValue.getClass();
				if (clazz.equals(UniqueValue.class)) {
					sql.append(" where ");
					SQLBuilder.buildUniqueColumns(sql, getEntityTable());
				} else if (clazz.equals(ExpressionValue.class)) {
					sql.append(" where ").append(((ExpressionValue) paramsValue).getExpression());
				}
			}
			sqlVal = new SQLValue(sql, paramsValue != null ? paramsValue.getValues() : null);
		}
		return executeQuery(sqlVal, new IQueryExtractor<Number>() {
			@Override
			public Number extractData(final ResultSet rs) throws SQLException, ADOException {
				Number number = null;
				if (rs.next()) {
					number = (Number) rs.getObject(1);
				}
				return number != null ? number : 0;
			}
		});
	}

	@Override
	public Object queryFor(final String column, final IParamsValue paramsValue) {
		final Map<String, Object> row = executeQuery(new String[] { column }, paramsValue);
		return row != null ? row.get(column) : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void exchange(final DbTableColumn order, final T... beans) {
		if (beans == null || beans.length < 2) {
			return;
		}
		// 顺序交换
		final String orderName = order.getName();
		final int length = beans.length;
		final int[] vals = new int[length];
		for (int i = 0; i < length; i++) {
			vals[i] = ((Number) BeanUtils.getProperty(beans[i], orderName)).intValue();
		}

		final String[] _columns = new String[] { order.getAlias() };
		BeanUtils.setProperty(beans[0], orderName, -1);
		update(_columns, beans[0]);
		for (int i = 1; i < length; i++) {
			BeanUtils.setProperty(beans[i], orderName, vals[i - 1]);
			update(_columns, beans[i]);
		}
		BeanUtils.setProperty(beans[0], orderName, vals[length - 1]);
		update(_columns, beans[0]);
	}

	@Override
	public String toString() {
		return getEntityTable().getName() + " [ " + getClass().getSimpleName() + " ]";
	}
}
