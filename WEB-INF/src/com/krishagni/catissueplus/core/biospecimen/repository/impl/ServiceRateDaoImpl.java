package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateDao;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceRateListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Conjunction;
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

		if (crit.cpId() != null) {
			query.join("svc.cp", "cp")
				.add(query.eq("cp.id", crit.cpId()));
		}

		if (crit.serviceId() != null) {
			query.add(query.eq("svc.id", crit.serviceId()));
		}

		if (crit.serviceIds() != null && !crit.serviceIds().isEmpty()) {
			query.add(query.in("svc.id", crit.serviceIds()));
		}

		if (crit.startDate() != null) {
			query.add(query.ge("svcRate.startDate", crit.startDate()));
		}

		if (crit.endDate() != null) {
			query.add(query.le("svcRate.endDate", crit.endDate()));
		}

		if (crit.effectiveDate() != null) {
			query.add(query.le("svcRate.startDate", crit.effectiveDate()))
				.add(
					query.or(
						query.isNull("svcRate.endDate"),
						query.ge("svcRate.endDate", crit.effectiveDate())
					)
				);
		}

		query.addOrder(query.desc("svcRate.startDate"));
		if (crit.limitItems()) {
			return query.list(crit.startAt(), crit.maxResults());
		} else {
			return query.list();
		}
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
						query.gt("svcRate.endDate", rate.getStartDate()))
				)
			);
		}

		if (rate.getEndDate() != null) {
			orCond.add(
				query.and(
					query.lt("svcRate.startDate", rate.getEndDate()),
					query.or(
						query.isNull("svcRate.endDate"),
						query.ge("svcRate.endDate", rate.getEndDate()))
				)
			);
		} else {
			orCond.add(query.isNull("svcRate.endDate"));
		}

		Conjunction encompassCond = query.conjunction();
		encompassCond.add(query.ge("svcRate.startDate", rate.getStartDate()));
		if (rate.getEndDate() != null) {
			encompassCond.add(query.lt("svcRate.endDate", rate.getEndDate()));
		}

		orCond.add(encompassCond);

		query.add(orCond);
		if (rate.getId() != null) {
			query.add(query.ne("svcRate.id", rate.getId()));
		}

		return query.list();
	}
}
