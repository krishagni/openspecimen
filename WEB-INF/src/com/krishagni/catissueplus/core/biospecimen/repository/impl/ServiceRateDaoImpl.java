package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateDao;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;

public class ServiceRateDaoImpl extends AbstractDao<ServiceRate> implements ServiceRateDao {
	public Class<ServiceRate> getType() {
		return ServiceRate.class;
	}

	@Override
	public List<ServiceRate> getRates(ServiceRateListCriteria crit) {
		Criteria<ServiceRate> query = createCriteria(ServiceRate.class, "svcRate")
			.join("svcRate.service", "svc");

		if (crit.serviceId() != null) {
			query.add(query.eq("svc.id", crit.serviceId()));
		}

		if (crit.startDate() != null) {
			query.add(query.ge("svcRate.startDate", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(query.le("svcRate.endDate", crit.endDate()));
		}

		return query.addOrder(query.desc("svcRate.startDate")).list(crit.startAt(), crit.maxResults());
	}

	public List<ServiceRate> getOverlappingRates(ServiceRate rate) {
		Criteria<ServiceRate> query = createCriteria(ServiceRate.class, "svcRate")
			.join("svcRate.service", "svc");

		query.add(query.eq("svc.id", rate.getService().getId()));

		Disjunction orCond = query.disjunction();
		if (rate.getStartDate() != null) {
			orCond.add(
				query.and(
					query.le("svcRate.startDate", rate.getStartDate()),
					query.or(
						query.isNull("svcRate.endDate"),
						query.ge("svcRate.endDate", rate.getStartDate()))
				)
			);
		}

		if (rate.getEndDate() != null) {
			orCond.add(
				query.and(
					query.le("svcRate.startDate", rate.getEndDate()),
					query.or(
						query.isNull("svcRate.endDate"),
						query.ge("svcRate.endDate", rate.getEndDate()))
				)
			);
		} else {
			orCond.add(query.isNull("svcRate.endDate"));
		}

		query.add(orCond);
		if (rate.getId() != null) {
			query.add(query.ne("svcRate.id", rate.getId()));
		}

		return query.list();
	}
}
