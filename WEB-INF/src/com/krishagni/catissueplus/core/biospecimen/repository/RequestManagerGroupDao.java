package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface RequestManagerGroupDao extends Dao<RequestManagerGroup> {
	List<RequestManagerGroup> getGroups(RmgListCriteria crit);

	Long getGroupsCount(RmgListCriteria crit);

	Map<Long, Long> getActiveCpCounts(List<Long> groupIds);

	RequestManagerGroup getByName(String name);
}
