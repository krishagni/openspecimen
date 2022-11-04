package com.krishagni.catissueplus.core.exporter.repository.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.repository.ExportJobDao;
import com.krishagni.catissueplus.core.exporter.repository.ExportJobsListCriteria;

public class ExportJobDaoImpl extends AbstractDao<ExportJob> implements ExportJobDao {
	@Override
	public Class<ExportJob> getType() {
		return ExportJob.class;
	}

	@Override
	public List<ExportJob> getExportJobs(ExportJobsListCriteria crit) {
		Criteria<ExportJob> query = createCriteria(ExportJob.class, "job");
		query.join("job.createdBy", "createdBy")
			.join("createdBy.authDomain", "authDomain")
			.join("createdBy.institute", "institute")
			.addOrder(query.desc("job.id"));

		if (StringUtils.isNotBlank(crit.status())) {
			try {
				query.add(query.eq("job.status", ExportJob.Status.valueOf(crit.status())));
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
}
