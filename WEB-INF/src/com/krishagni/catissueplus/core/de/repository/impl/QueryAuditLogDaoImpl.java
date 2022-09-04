package com.krishagni.catissueplus.core.de.repository.impl;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
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
		return getLogsQuery(crit).getCount("al.id");
	}

	@Override
	public List<QueryAuditLog> getLogs(QueryAuditLogsListCriteria crit) {
		Criteria<QueryAuditLog> query = getLogsQuery(crit);
		return query.orderBy(crit.asc() ? query.asc("al.id") : query.desc("al.id"))
			.list(crit.startAt(), crit.maxResults());
	}

	private Criteria<QueryAuditLog> getLogsQuery(QueryAuditLogsListCriteria crit) {
		Criteria<QueryAuditLog> query = createCriteria(QueryAuditLog.class, "al");
		if (crit.lastId() != null) {
			query.add(query.gt("al.id", crit.lastId()));
		}

		if (crit.startDate() != null) {
			query.add(query.ge("al.timeOfExecution", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(query.le("al.timeOfExecution", crit.endDate()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			Disjunction cond = query.disjunction();
			query.join("al.query", "query");

			if (StringUtils.isNumeric(crit.query())) {
				cond.add(query.eq("query.id", Long.parseLong(crit.query())));
			}

			cond.add(query.ilike("query.title", crit.query()));
			query.add(cond);
		}

		if (CollectionUtils.isNotEmpty(crit.userIds()) || crit.instituteId() != null) {
			query.join("al.runBy", "rb");

			if (CollectionUtils.isNotEmpty(crit.userIds())) {
				query.add(query.in("rb.id", crit.userIds()));
			}

			if (crit.instituteId() != null) {
				query.join("rb.institute", "ri")
					.add(query.eq("ri.id", crit.instituteId()));
			}
		}

		return query;
	}
}
