package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.events.JobRunsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledJobDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class ScheduledJobDaoImpl extends AbstractDao<ScheduledJob> implements ScheduledJobDao {

	@Override
	public List<ScheduledJob> getScheduledJobs(ScheduledJobListCriteria criteria) {
		Criteria<ScheduledJob> query = getScheduledJobsListQuery(criteria);
		return query.orderBy(query.desc("job.id")).list(criteria.startAt(), criteria.maxResults());
	}

	@Override
	public Long getScheduledJobsCount(ScheduledJobListCriteria crit) {
		return getScheduledJobsListQuery(crit).getCount("job.id");
	}

	@Override
	public ScheduledJob getJobByName(String name) {
		return createNamedQuery(GET_JOB_BY_NAME, ScheduledJob.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	@Override
	public String getRunByNodeForUpdate(Long jobId) {
		Object[] row = createNamedQuery(GET_RUN_BY_NODE_FOR_UPDATE, Object[].class)
			.setParameter("jobId", jobId)
			.uniqueResult();
		return (String) row[2];
	}

	@Override
	public int updateRunByNode(Long jobId, String node) {
		return createNamedQuery(UPDATE_RUN_BY_NODE)
			.setParameter("jobId", jobId)
			.setParameter("nodeName", node)
			.executeUpdate();
	}

	@Override
	public ScheduledJobRun getJobRun(Long id) {
		return getCurrentSession().get(ScheduledJobRun.class, id);
	}

	@Override
	public void saveOrUpdateJobRun(ScheduledJobRun jobRun) {
		getCurrentSession().saveOrUpdate(jobRun);
	}
	
	@Override
	public List<ScheduledJobRun> getJobRuns(JobRunsListCriteria crit) {
		Criteria<ScheduledJobRun> query = createCriteria(ScheduledJobRun.class, "run");
		query.add(query.in("run.id", getJobRunIdsQuery(crit, query)));
		return query.orderBy(query.desc("run.id")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds) {
		return getJobsLastRunTime(jobIds, ALL_STATUSES);
	}

	@Override
	public Map<Long, Date> getJobsLastRunTime(Collection<Long> jobIds, List<String> statuses) {
		if (jobIds == null || jobIds.isEmpty()) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = createNamedQuery(GET_JOBS_LAST_RUNTIME, Object[].class)
			.setParameterList("jobIds", jobIds)
			.setParameterList("statuses", statuses)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Long) row[0], row -> (Date) row[1]));
	}

	@Override
	public Class<ScheduledJob> getType() {
		return ScheduledJob.class;
	}

	private Criteria<ScheduledJob> getScheduledJobsListQuery(ScheduledJobListCriteria crit) {
		Criteria<ScheduledJob> query = createCriteria(ScheduledJob.class, "job");
		return query.add(query.in("job.id", getJobIdsQuery(crit, query)));
	}

	private SubQuery<Long> getJobIdsQuery(ScheduledJobListCriteria crit, Criteria<ScheduledJob> query) {
		SubQuery<Long> subQuery = query.createSubQuery(ScheduledJob.class, "job");
		subQuery.distinct().select("job.id");

		if (StringUtils.isNotBlank(crit.query())) {
			subQuery.add(subQuery.ilike("job.name", crit.query()));
		}

		if (crit.type() != null) {
			subQuery.add(subQuery.eq("job.type", crit.type()));
		}

		if (crit.userId() != null) {
			subQuery.join("job.createdBy", "createdBy")
				.leftJoin("job.sharedWith", "su")
				.add(
					subQuery.or(
						subQuery.eq("createdBy.id", crit.userId()),
						subQuery.eq("su.id", crit.userId())
					)
				);
		}

		return subQuery;
	}

	private SubQuery<Long> getJobRunIdsQuery(JobRunsListCriteria crit, Criteria<ScheduledJobRun> query) {
		SubQuery<Long> subQuery = query.createSubQuery(ScheduledJobRun.class, "run")
			.distinct().select("run.id");

		if (crit.jobId() != null) {
			subQuery.join("run.scheduledJob", "job")
				.add(subQuery.eq("job.id", crit.jobId()));
		}

		if (crit.fromDate() != null) {
			subQuery.add(subQuery.ge("run.startedAt", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			subQuery.add(subQuery.le("run.finishedAt", crit.toDate()));
		}

		if (crit.status() != null) {
			subQuery.add(subQuery.eq("run.status", crit.status()));
		}

		if (crit.userId() != null) {
			subQuery.join("run.runBy", "runBy")
				.add(subQuery.eq("runBy.id", crit.userId()));

			if (crit.jobId() == null) {
				subQuery.join("run.scheduledJob", "job");
			}

			subQuery.join("job.createdBy", "createdBy")
				.leftJoin("job.sharedWith", "su")
				.add(
					subQuery.or(
						subQuery.eq("createdBy.id", crit.userId()),
						subQuery.eq("su.id", crit.userId())
					)
				);
		}

		return subQuery;
	}


	private static final String FQN = ScheduledJob.class.getName();

	private static final String GET_JOB_BY_NAME = FQN + ".getJobByName";

	private static final String GET_RUN_BY_NODE_FOR_UPDATE = FQN + ".getRunByNodeForUpdate";

	private static final String UPDATE_RUN_BY_NODE = FQN + ".updateRunByNode";

	private static final String GET_JOBS_LAST_RUNTIME = FQN + ".getJobsLastRuntime";

	private static final List<String> ALL_STATUSES = Arrays.asList("IN_PROGRESS", "SUCCEEDED", "FAILED");
}
