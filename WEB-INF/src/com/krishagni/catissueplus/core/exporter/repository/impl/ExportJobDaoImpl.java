package com.krishagni.catissueplus.core.exporter.repository.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.FetchMode;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
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
		int startAt = crit.startAt() <= 0 ? 0 : crit.startAt();
		int maxResults = crit.maxResults() <= 0 || crit.maxResults() > 100 ? 100 : crit.maxResults();

		Criteria query = getCurrentSession().createCriteria(ExportJob.class)
			.createAlias("createdBy", "createdBy")
			.createAlias("createdBy.authDomain", "authDomain")
			.createAlias("createdBy.institute", "institute")
			.setFetchMode("createdBy", FetchMode.JOIN)
			.setFirstResult(startAt)
			.setMaxResults(maxResults)
			.addOrder(Order.desc("id"));

		if (StringUtils.isNotBlank(crit.status())) {
			try {
				query.add(Restrictions.eq("status", ExportJob.Status.valueOf(crit.status())));
			} catch (Exception e) {
				throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST, e.getMessage());
			}
		}

		if (crit.instituteId() != null) {
			query.createAlias("createdBy.institute", "institute")
				.add(Restrictions.eq("institute.id", crit.instituteId()));
		}

		if (CollectionUtils.isNotEmpty(crit.userIds())) {
			query.add(Restrictions.in("createdBy.id", crit.userIds()));
		}

		if (CollectionUtils.isNotEmpty(crit.objectTypes())) {
			query.add(Restrictions.in("name", crit.objectTypes()));
		}

		if (crit.params() != null && !crit.params().isEmpty()) {
			query.createAlias("params", "params");

			Disjunction orCond = Restrictions.disjunction();
			query.add(orCond);

			for (Map.Entry<String, String> kv : crit.params().entrySet()) {
				orCond.add(Restrictions.and(
					Restrictions.eq("params.indices", kv.getKey()),
					Restrictions.eq("params.elements", kv.getValue())
				));
			}
		}

		if (crit.fromDate() != null) {
			query.add(Restrictions.ge("creationTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(Restrictions.le("creationTime", crit.toDate()));
		}

		return query.list();
	}
}
