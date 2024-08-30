
package com.krishagni.catissueplus.core.common.repository;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.SessionFactory;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.init.AppProperties;

public class AbstractDao<T> implements Dao<T> {
	private static final String LIMIT_SQL = "select * from (select tab.*, rownum rnum from (%s) tab where rownum <= %d) where rnum >= %d";

	protected SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public void saveOrUpdate(T obj) {
		saveOrUpdate(obj, false);
	}
	
	@Override
	public void saveOrUpdate(T obj, boolean flush) {
		try {
			sessionFactory.getCurrentSession().saveOrUpdate(obj);
			if (flush) {
				flush();
			}

			if (!(obj instanceof BaseEntity)) {
				return;
			}

			BaseEntity entity = (BaseEntity) obj;
			if (CollectionUtils.isEmpty(entity.getOnSaveProcs())) {
				return;
			}

			entity.getOnSaveProcs().forEach(Runnable::run);
		} catch (HibernateException he) {
			throw OpenSpecimenException.serverError(he);
		}
	}

	@Override
	public <R> void delete(R obj) {
		getCurrentSession().delete(obj);
	}

	@Override
	public T getById(Long id) {
		Criteria<?> query = createCriteria(getType(), "t");
		return (T) query.add(query.eq("t.id", id)).uniqueResult();
	}

	public List<T> getByIds(Collection<Long> ids) {
		Criteria<?> query = createCriteria(getType(), "t");
		return (List<T>) query.add(query.in("t.id", ids)).list();
	}

	public Class<?> getType() {
		throw new UnsupportedOperationException("Override the dao method getType() to use getById()");
	}
	
	public void flush() {
		sessionFactory.getCurrentSession().flush();
	}

	protected void applyIdsFilter(AbstractCriteria<?, ?> criteria, String attrName, List<Long> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}

		Junction or = criteria.disjunction();
		for (int i = 0; i < ids.size(); i += 500) {
			List<Long> params = ids.subList(i, Math.min(i + 500, ids.size()));
			or.add(criteria.in(attrName, params));
		}

		criteria.add(or);
	}

	protected Session getCurrentSession() {
		Session session = sessionFactory.getCurrentSession();
		session.enableFilter("activeEntity");
		session.enableFilter("activeForms");
		return session;
	}

	protected Map<String, Object> getObjectIds(String propName, String key, Object value) {
		Criteria<Long> criteria = Criteria.create(getCurrentSession(), getType(),  Long.class, "t")
			.select("t.id");

		List<Long> rows = criteria.add(criteria.eq("t." + key, value)).list();
		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		return Collections.singletonMap(propName, rows.iterator().next());
	}

	protected String getLimitSql(String baseSql, int startAt, int maxResults, boolean oracle) {
		if (oracle) {
			return String.format(LIMIT_SQL, baseSql, maxResults, startAt);
		} else {
			return baseSql + "limit " + startAt + ", " + maxResults;
		}
	}

	protected Collection<String> toUpper(Collection<String> inputList) {
		return Utility.nullSafeStream(inputList).map(String::toUpperCase).collect(Collectors.toList());
	}

	protected boolean isOracle() {
		return getDbType().equals("oracle");
	}

	protected boolean isMySQL() {
		return getDbType().equals("mysql");
	}

	protected <R> Criteria<R> createCriteria(Class<R> fromClass, String alias) {
		return createCriteria(fromClass, fromClass, alias);
	}

	protected <R> Criteria<R> createCriteria(Class<?> fromClass, Class<R> returnClass, String alias) {
		return Criteria.create(getCurrentSession(), fromClass, returnClass, alias);
	}

	protected Query<?> createNamedQuery(String name) {
		return Query.createNamedQuery(getCurrentSession(), name);
	}

	protected <R> Query<R> createNamedQuery(String name, Class<R> returnType) {
		return Query.createNamedQuery(getCurrentSession(), name, returnType);
	}

	public <R> Query<R> createQuery(String hql) {
		return Query.createQuery(getCurrentSession(), hql);
	}

	public <R> Query<R> createQuery(String hql, Class<R> returnType) {
		return Query.createQuery(getCurrentSession(), hql, returnType);
	}

	protected <R> Query<R> createNativeQuery(String sql) {
		return Query.createNativeQuery(getCurrentSession(), sql);
	}

	protected <R> Query<R> createNativeQuery(String sql, Class<R> returnType) {
		return Query.createNativeQuery(getCurrentSession(), sql, returnType);
	}

	private String getDbType() {
		return AppProperties.getInstance().getProperties()
			.getProperty("database.type", "mysql").toLowerCase();
	}
}
