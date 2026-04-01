
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
import com.krishagni.catissueplus.core.biospecimen.domain.Identifiable;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.init.AppProperties;

public class AbstractDao<T> implements Dao<T> {
	private static LogUtil logger = LogUtil.getLogger(AbstractDao.class);

	private static final String LIMIT_SQL = "select * from (select tab.*, rownum rnum from (%s) tab where rownum <= %d) where rnum >= %d";

	protected SessionFactory sessionFactory;

	public SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	public static <T> void saveOrUpdate(SessionFactory sessionFactory, T obj, boolean flush) {
		try {
			if (!(obj instanceof Identifiable<?> idObj)) {
				throw OpenSpecimenException.userError(CommonErrorCode.SERVER_ERROR, "AbstractDao.saveOrUpdate can be invoked only with Identifiable objects as input. Input: " + obj.getClass().getName());
			}

			Session session = sessionFactory.getCurrentSession();
			if (idObj.getId() != null) {
				if (session.contains(idObj)) {
					session.merge(obj);
				} else {
					throw OpenSpecimenException.userError(CommonErrorCode.SERVER_ERROR, "AbstractDao.saveOrUpdate can be invoked on new object or existing object that is already attached to the session. Input: " + obj.getClass().getName());
				}
			} else {
				session.persist(obj);
			}

			if (flush) {
				session.flush();
			}

			if (idObj instanceof BaseEntity beObj && CollectionUtils.isNotEmpty(beObj.getOnSaveProcs())) {
				beObj.getOnSaveProcs().forEach(Runnable::run);
			}
		} catch (HibernateException he) {
			throw OpenSpecimenException.serverError(he);
		}
	}

	@Override
	public void saveOrUpdate(T obj) {
		saveOrUpdate(obj, false);
	}

	@Override
	public void saveOrUpdate(T obj, boolean flush) {
		saveOrUpdate(sessionFactory, obj, flush);
	}

	@Override
	public T merge(T obj) {
		return merge(obj, false);
	}

	@Override
	public T merge(T obj, boolean flush) {
		T mergedObj = getCurrentSession().merge(obj);
		if (flush) {
			flush();
		}

		return mergedObj;
	}

	@Override
	public void save(Object obj) {
		getCurrentSession().persist(obj);
	}

	@Override
	public void update(Object obj) {
		if (!getCurrentSession().contains(obj)) {
			throw OpenSpecimenException.userError(CommonErrorCode.SERVER_ERROR, "Attempting to update unmanaged object. Input: " + obj.getClass().getName());
		}

		getCurrentSession().merge(obj);
	}

	@Override
	public <R> void delete(R obj) {
		if (!getCurrentSession().contains(obj)) {
			throw OpenSpecimenException.userError(CommonErrorCode.SERVER_ERROR, "AbstractDao.delete can be invoked only with managed object. Input: " + obj.getClass().getName());
		}

		getCurrentSession().remove(obj);
	}

	@Override
	public T getById(Long id) {
		try {
			return (T) getCurrentSession().find(getType(), id);
		} catch (Exception e) {
			logger.error("Error obtaining the object of type " + getType().getName() + " and ID = " + id, e);
		}

		return null;
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

	protected void addInClauses(AbstractCriteria<?, ?> criteria, String attrName, List<?> ids) {
		if (CollectionUtils.isEmpty(ids)) {
			return;
		}

		Junction or = criteria.disjunction();
		for (int i = 0; i < ids.size(); i += 500) {
			List<?> params = ids.subList(i, Math.min(i + 500, ids.size()));
			or.add(criteria.in(attrName, params));
		}

		criteria.add(or);
	}

	protected void applyIdsFilter(AbstractCriteria<?, ?> criteria, String attrName, List<Long> ids) {
		addInClauses(criteria, attrName, ids);
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
