package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceDao;
import com.krishagni.catissueplus.core.biospecimen.repository.ServiceListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ServiceDaoImpl extends AbstractDao<Service> implements ServiceDao {

	public Class<Service> getType() {
		return Service.class;
	}

	@Override
	public List<Service> getServices(ServiceListCriteria crit) {
		Criteria<Service> query = createCriteria(Service.class, "s")
			.join("s.cp", "cp");

		if (crit.cpId() > 0L) {
			query.add(query.eq("cp.id", crit.cpId()));
		}

		if (StringUtils.isNotBlank(crit.query())) {
			if (isMySQL()) {
				query.add(query.or(query.like("s.code", crit.query()), query.like("s.description", crit.query())));
			} else {
				query.add(query.or(query.ilike("s.code", crit.query()), query.ilike("s.description", crit.query())));
			}
		}

		return query.addOrder(query.asc("s.code")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Service getService(String cpShortTitle, String code) {
		Criteria<Service> query = createCriteria(Service.class, "s")
			.join("s.cp", "cp");

		query.add(query.eq("cp.shortTitle", cpShortTitle));
		if (isMySQL()) {
			query.add(query.eq("s.code", code));
		} else {
			query.add(query.eq(query.lower("s.code"), code.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public Service getService(Long cpId, String code) {
		Criteria<Service> query = createCriteria(Service.class, "s")
			.join("s.cp", "cp");

		query.add(query.eq("cp.id", cpId));
		if (isMySQL()) {
			query.add(query.eq("s.code", code));
		} else {
			query.add(query.eq(query.lower("s.code"), code.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public long getServiceUsageCount(Long serviceId) {
		Criteria<Long> query = createCriteria(Service.class, Long.class, "svc")
			.join("svc.specimens", "spmnSvc")
			.join("spmnSvc.specimen", "spmn");

		return query.add(query.eq("svc.id", serviceId))
			.add(query.ne("spmn.activityStatus", "Disabled"))
			.select(query.distinctCount("spmn.id"))
			.uniqueResult();
	}
}
