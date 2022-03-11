package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface CollectionProtocolGroupDao extends Dao<CollectionProtocolGroup> {

	List<CollectionProtocolGroup> getGroups(CpGroupListCriteria crit);

	CollectionProtocolGroup getByName(String name);

	Map<Long, Integer> getCpsCount(Collection<Long> groupIds);

	List<String> getCpsUsedInOtherGroups(CollectionProtocolGroup group);

	// cpId -> [formId]
	Map<Long, Set<Long>> getCpForms(List<Long> cpIds, String entityType);

	void deleteForms(Collection<Long> formIds);
}
