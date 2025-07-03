package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ServiceRateDao extends Dao<ServiceRate> {
	List<ServiceRate> getRates(ServiceRateListCriteria crit);

	List<ServiceRate> getOverlappingRates(ServiceRate rate);
}
