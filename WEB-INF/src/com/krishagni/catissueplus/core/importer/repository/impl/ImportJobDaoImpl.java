package com.krishagni.catissueplus.core.importer.repository.impl;

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;

public class ImportJobDaoImpl extends AbstractDao<ImportJob> implements ImportJobDao {
	
	public Class<ImportJob> getType() {
		return ImportJob.class;
	}

	@Override
	public ImportJob getJobForUpdate(Long jobId) {
		String hql = "from " + ImportJob.class.getName() + " where id = :jobId";
		return createQuery(hql, ImportJob.class)
			.setParameter("jobId", jobId)
			.acquirePessimisticWriteLock()
			.uniqueResult();
	}

	@Override
	public List<ImportJob> getImportJobs(ListImportJobsCriteria crit) {
		Criteria<ImportJob> query = createCriteria(ImportJob.class, "job");
		query.join("job.createdBy", "createdBy")
			.join("createdBy.authDomain", "authDomain")
			.join("createdBy.institute", "institute")
			.orderBy(query.desc("job.id"));

		if (StringUtils.isNotBlank(crit.status())) {
			try {
				query.add(query.eq("job.status", ImportJob.Status.valueOf(crit.status())));
			} catch (Exception e) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST, e.getMessage());
			}
		}

		if (crit.instituteId() != null) {
			query.add(query.eq("institute.id", crit.instituteId()));
		}

		if (CollectionUtils.isNotEmpty(crit.userIds())) {
			query.add(query.in("createdBy.id", crit.userIds()));
		}
		
		if (CollectionUtils.isNotEmpty(crit.objectTypes())) {
			query.add(query.in("job.name", crit.objectTypes()));
		}

		if (crit.params() != null && !crit.params().isEmpty()) {
			query.join("job.params", "params");

			Disjunction orCond = query.disjunction();
			for (Map.Entry<String, String> kv : crit.params().entrySet()) {
				orCond.add(query.and(
					query.eq("params.indices", kv.getKey()),
					query.eq("params.elements", kv.getValue())
				));
			}

			query.add(orCond);
		}

		if (crit.fromDate() != null) {
			query.add(query.ge("job.creationTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(query.le("job.creationTime", crit.toDate()));
		}

		int startAt = crit.startAt() <= 0 ? 0 : crit.startAt();
		int maxResults = crit.maxResults() <= 0 || crit.maxResults() > 100 ? 100 : crit.maxResults();
		return query.list(startAt, maxResults);
	}

	@Override
	public int markInProgressJobsAsFailed(String node) {
		return createNativeQuery(MARK_IN_PROGRESS_JOBS_AS_FAILED_SQL)
			.setParameter("node", node)
			.executeUpdate();
	}

	@Override
	public String getActiveImportRunnerNode() {
		Object[] row = createNamedQuery(GET_ACTIVE_IMPORT_RUNNER_NODE, Object[].class).uniqueResult();
		return (String) row[0];
	}

	@Override
	public boolean setActiveImportRunnerNode(String node) {
		return createNamedQuery(SET_ACTIVE_IMPORT_RUNNER_NODE)
			.setParameter("node", node)
			.setParameter("ts", Calendar.getInstance().getTime())
			.executeUpdate() > 0;
	}

	private static final String FQN = ImportJob.class.getName();

	private static final String MARK_IN_PROGRESS_JOBS_AS_FAILED_SQL =
		"update " +
		"  os_bulk_import_jobs " +
		"set " +
		"  status = 'STOPPED' " +
		"where " +
		"  status = 'IN_PROGRESS' and " +
		"  run_by_node = :node";

	private static final String GET_ACTIVE_IMPORT_RUNNER_NODE = FQN + ".getActiveImportRunnerNode";

	private static final String SET_ACTIVE_IMPORT_RUNNER_NODE = FQN + ".setActiveImportRunnerNode";
}
