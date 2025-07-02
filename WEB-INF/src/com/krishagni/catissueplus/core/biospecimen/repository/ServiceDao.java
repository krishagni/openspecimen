package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ServiceDao extends Dao<Service> {
	List<Service> getServices(ServiceListCriteria crit);

	Service getService(String cpShortTitle, String code);

	Service getService(Long cpId, String code);

	long getServiceUsageCount(Long serviceId);
}
