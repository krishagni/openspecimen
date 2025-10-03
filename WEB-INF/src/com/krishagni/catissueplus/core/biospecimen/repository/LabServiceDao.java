package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabServiceDao extends Dao<LabService> {

	List<LabService> getServices(LabServiceListCriteria criteria);

	Long getServicesCount(LabServiceListCriteria criteria);

	LabService getByCode(String code);
}
