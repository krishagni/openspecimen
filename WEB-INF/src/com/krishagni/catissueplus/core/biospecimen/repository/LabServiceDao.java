package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabServiceDao extends Dao<LabService> {

	List<LabService> getServices(LabServiceListCriteria criteria);

	Long getServicesCount(LabServiceListCriteria criteria);

	LabService getByCode(String code);

	Map<Long, Long> getRateListsCount(Collection<Long> serviceIds);
}
