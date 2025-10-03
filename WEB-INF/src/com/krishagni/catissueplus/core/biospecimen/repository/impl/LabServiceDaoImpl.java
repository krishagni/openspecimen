package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class LabServiceDaoImpl extends AbstractDao<LabService> implements LabServiceDao {

	@Override
	public Class<LabService> getType() {
		return LabService.class;
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
}
