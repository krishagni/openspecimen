package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceDao;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class LabServiceDaoImpl extends AbstractDao<LabService> implements LabServiceDao {

	@Override
	public Class<LabService> getType() {
		return LabService.class;
	}

	@Override
	public List<LabService> getServices(LabServiceListCriteria criteria) {
		Criteria<LabService> query = getLabServicesQuery(criteria);
		return query.addOrder(query.asc("labSvc.code")).list(criteria.startAt(), criteria.maxResults());
	}

	@Override
	public Long getServicesCount(LabServiceListCriteria criteria) {
		Criteria<LabService> query = getLabServicesQuery(criteria);
		return query.getCount("labSvc.id");
	}

	@Override
	public LabService getByCode(String code) {
		Criteria<LabService> query = createCriteria(LabService.class, "labSvc");
		if (isMySQL()) {
			query.add(query.eq("labSvc.code", code));
		} else {
			query.add(query.eq(query.lower("labSvc.code"), code.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public Map<Long, Long> getRateListsCount(Collection<Long> serviceIds) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_RATE_LISTS_COUNT)
			.setParameterList("serviceIds", serviceIds)
			.list();

		Map<Long, Long> result = new LinkedHashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Long) row[1]);
		}

		return result;
	}

	private Criteria<LabService> getLabServicesQuery(LabServiceListCriteria criteria) {
		Criteria<LabService> query = createCriteria(LabService.class, "labSvc");
		if (CollectionUtils.isNotEmpty(criteria.codes())) {
			query.add(query.in("labSvc.code", criteria.codes()));
		}

		if (StringUtils.isNotBlank(criteria.query())) {
			if (isMySQL()) {
				query.add(query.or(query.like("labSvc.code", criteria.query()), query.like("labSvc.description", criteria.query())));
			} else {
				query.add(query.or(query.ilike("labSvc.code", criteria.query()), query.ilike("labSvc.description", criteria.query())));
			}
		}
		return query;
	}

	private static final String FQN = LabService.class.getName();

	private static final String GET_RATE_LISTS_COUNT = FQN + ".getRateListsCount";
}
