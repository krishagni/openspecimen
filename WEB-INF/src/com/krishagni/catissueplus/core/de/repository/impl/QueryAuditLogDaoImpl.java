package com.krishagni.catissueplus.core.de.repository.impl;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDigest;
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

	@Override
	public QueryAuditLogDigest getDigest(Date startDate, Date endDate) {
		Object[] row = createNativeQuery(GET_DIGEST_SQL, Object[].class)
			.setParameter("startDate", startDate)
			.setParameter("endDate", endDate)
			.uniqueResult();

		int idx = -1;
		QueryAuditLogDigest result = new QueryAuditLogDigest();
		result.setTotal(toLong(row[++idx]));
		result.setSuccessful(toLong(row[++idx]));
		result.setFailed(toLong(row[++idx]));
		return result;
	}

	@Override
	public List<Long> getSuccessfulRuntimes(Date startDate, Date endDate) {
		return createNativeQuery(GET_SUCCESSFUL_RUNTIMES_SQL, Long.class)
			.addLongScalar("runtime")
			.setParameter("startDate", startDate)
			.setParameter("endDate", endDate)
			.list();
	}

	@Override
	public int deleteLogsOlderThan(int olderThanDays, int maxRows) {
		LocalDate olderThan = LocalDate.now().minus(olderThanDays, ChronoUnit.DAYS);
		String sql = String.format(DELETE_OLDER_QUERY_AUDIT_LOGS_SQL, isMySQL() ? " limit " : " and rownum < ") + maxRows;
		return getCurrentSession().createNativeQuery(sql)
			.setParameter("olderThan", java.sql.Date.valueOf(olderThan))
			.executeUpdate();
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

		if (crit.failed()) {
			query.add(query.eq("al.recordCount", -1L))
				.add(query.isNotNull("al.error"))
				.add(query.ne("al.error", ""));
		}

		return query;
	}

	private long toLong(Object value) {
		return value != null ? ((Number) value).longValue() : 0L;
	}

	private static final String FAILED_LOG_COND =
		"record_count = -1 and error_message is not null and length(error_message) > 0";

	private static final String SUCCESS_LOG_COND =
		"(record_count <> -1 or record_count is null or error_message is null or length(error_message) = 0)";

	private static final String GET_DIGEST_SQL =
		"select " +
		"  count(*) as total_queries, " +
		"  sum(case when " + SUCCESS_LOG_COND + " then 1 else 0 end) as successful_queries, " +
		"  sum(case when " + FAILED_LOG_COND + " then 1 else 0 end) as failed_queries " +
		"from " +
		"  catissue_query_audit_logs " +
		"where " +
		"  time_of_exec >= :startDate and time_of_exec <= :endDate";

	private static final String GET_SUCCESSFUL_RUNTIMES_SQL =
		"select " +
		"  time_to_finish as runtime " +
		"from " +
		"  catissue_query_audit_logs " +
		"where " +
		"  time_of_exec >= :startDate and time_of_exec <= :endDate and " + SUCCESS_LOG_COND + " and " +
		"  time_to_finish is not null " +
		"order by " +
		"  time_to_finish";

	private static final String DELETE_OLDER_QUERY_AUDIT_LOGS_SQL =
		"delete from catissue_query_audit_logs where time_of_exec < :olderThan %s";
}
