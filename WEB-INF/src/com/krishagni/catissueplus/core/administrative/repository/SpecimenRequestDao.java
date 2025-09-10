package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.events.SpecimenRequestSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenRequestDao extends Dao<SpecimenRequest> {
	List<SpecimenRequestSummary> getSpecimenRequests(SpecimenRequestListCriteria crit);

	Map<String, Object> getRequestIds(String key, Object value);

	void removeRequestCart(Long cartId);
}
