package com.krishagni.catissueplus.core.common.repository;

import java.util.Collection;
import java.util.List;

import javax.persistence.LockModeType;

import org.apache.commons.lang3.ClassUtils;
import org.hibernate.LockMode;
import org.hibernate.Session;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.internal.QueryImpl;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.Type;

public class Query<R> {
	private org.hibernate.query.Query<R> query;

	private boolean nativeQuery;

	private Query(org.hibernate.query.Query<R> query) {
		this.query = query;
	}

	public static <R> Query<R> createNamedQuery(Session session, String name, Class<R> returnType) {
		if (ClassUtils.isPrimitiveOrWrapper(returnType) || returnType.isArray() || returnType == String.class) {
			return createNamedQuery(session, name);
		}

		return new Query<>(session.createNamedQuery(name, returnType));
	}

	@SuppressWarnings("unchecked")
	public static <R> Query<R> createNamedQuery(Session session, String name) {
		return new Query<R>(session.createNamedQuery(name));
	}

	public static <R> Query<R> createQuery(Session session, String hql, Class<R> returnType) {
		return new Query<>(session.createQuery(hql, returnType));
	}

	@SuppressWarnings("unchecked")
	public static <R> Query<R> createQuery(Session session, String hql) {
		return new Query<R>(session.createQuery(hql));
	}

	@SuppressWarnings("unchecked")
	public static <R> Query<R> createNativeQuery(Session session, String sql) {
		Query<R> query = new Query<R>(session.createNativeQuery(sql));
		query.nativeQuery = true;
		return query;
	}

	public static <R> Query<R> createNativeQuery(Session session, String sql, Class<R> returnType) {
		if (ClassUtils.isPrimitiveOrWrapper(returnType) || returnType.isArray() || returnType == String.class) {
			return createNativeQuery(session, sql);
		}

		Query<R> query = new Query<>(session.createNativeQuery(sql, returnType));
		query.nativeQuery = true;
		return query;
	}

	public Query<R> setParameter(String name, Object value) {
		query.setParameter(name, value);
		return this;
	}

	public Query<R> setParameterList(String name, Collection<?> values) {
		query.setParameterList(name, values);
		return this;
	}

	public Query<R> addIntScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.INTEGER);
	}

	public Query<R> addLongScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.LONG);
	}

	public Query<R> addStringScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.STRING);
	}

	public Query<R> addTimestampScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.TIMESTAMP);
	}

	public Query<R> addBlobScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.BLOB);
	}

	public Query<R> addBigDecimalScalar(String columnAlias) {
		return addScalar(columnAlias, StandardBasicTypes.BIG_DECIMAL);
	}

	public Query<R> acquirePessimisticWriteLock() {
		query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
		return this;
	}

	public Query<R> acquirePessimisticWriteLock(String tableAlias) {
		query.setLockMode(tableAlias, LockMode.PESSIMISTIC_WRITE);
		return this;
	}

	public Query<R> setFirstResult(int startAt) {
		query.setFirstResult(startAt);
		return this;
	}

	public Query<R> setMaxResults(int maxResults) {
		query.setMaxResults(maxResults);
		return this;
	}

	public R uniqueResult() {
		return query.uniqueResult();
	}

	public List<R> list() {
		return query.list();
	}

	public int executeUpdate() {
		return query.executeUpdate();
	}

	public String getQueryString() {
		return query.getQueryString();
	}

	public String getSqlString() {
		String[] sqlPlans = ((QueryImpl<Object>) query).getQueryPlan().getSqlStrings();
		return sqlPlans[0];
	}

	//


	private Query<R> addScalar(String alias, Type type) {
		if (nativeQuery) {
			((NativeQuery<?>)query).addScalar(alias, type);
		}

		return this;
	}
}
