package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;
import com.krishagni.catissueplus.core.de.repository.QueryAuditLogDao;

public class QueryAuditLogDaoImpl extends AbstractDao<QueryAuditLog> implements QueryAuditLogDao {
	@Override
	public Class<QueryAuditLog> getType() {
		return QueryAuditLog.class;
	}

	@Override
	public Long getLogsCount(QueryAuditLogsListCriteria crit) {
		Number count = (Number) getLogsQuery(crit)
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<QueryAuditLog> getLogs(QueryAuditLogsListCriteria crit) {
		return (List<QueryAuditLog>) getLogsQuery(crit)
			.addOrder(crit.asc() ? Order.asc("id") : Order.desc("id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.list();
	}

	private Criteria getLogsQuery(QueryAuditLogsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(QueryAuditLog.class, "al");

		if (crit.lastId() != null) {
			query.add(Restrictions.gt("al.id", crit.lastId()));
		}

		if (crit.startDate() != null) {
			query.add(Restrictions.ge("al.timeOfExecution", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(Restrictions.le("al.timeOfExecution", crit.endDate()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			Disjunction cond = Restrictions.disjunction();
			query.createAlias("al.query", "query");

			if (StringUtils.isNumeric(crit.query())) {
				cond.add(Restrictions.eq("query.id", Long.parseLong(crit.query())));
			}

			cond.add(Restrictions.ilike("query.title", crit.query(), MatchMode.ANYWHERE));
			query.add(cond);
		}

		if (CollectionUtils.isNotEmpty(crit.userIds()) || crit.instituteId() != null) {
			query.createAlias("al.runBy", "rb");

			if (CollectionUtils.isNotEmpty(crit.userIds())) {
				query.add(Restrictions.in("rb.id", crit.userIds()));
			}

			if (crit.instituteId() != null) {
				query.createAlias("rb.institute", "ri")
					.add(Restrictions.eq("ri.id", crit.instituteId()));
			}
		}

		return query;
	}
}
