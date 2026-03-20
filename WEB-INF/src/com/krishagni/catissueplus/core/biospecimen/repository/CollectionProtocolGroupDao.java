package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;

public interface CollectionProtocolGroupDao extends Dao<CollectionProtocolGroup> {

	List<CollectionProtocolGroup> getGroups(CpGroupListCriteria crit);

	CollectionProtocolGroup getByName(String name);

	Map<Long, Integer> getCpsCount(Collection<Long> groupIds);

	//
	// Filters the input list of CP IDs by removing those that are not present in the CPG identified by group ID.
	//
	List<Long> getGroupCpIds(Long groupId, Collection<Long> cpIds);

	//
	// Returns the list of input CPs that are part CPG different from input group ID.
	//
	List<String> getCpsAssignedToOtherGroup(Long groupId, Collection<Long> cpIds);

	//
	// Returns count of CPs that are part of the group and matches one of the CP sites.
	//
	int getCpsCount(Long groupId, Set<SiteCpPair> siteCps);

	//
	// Adds CPs to the group using batch insertion
	//
	void addCpsToGroup(Long groupId, Collection<Long> cpIds);

	//
	// Removes CPs from the group
	//
	int removeCpsFromGroup(Long groupId, Collection<Long> cpIds);

	//
	// Removes all CPs from the group
	//
	int removeCpsFromGroup(Long groupId);

	//
	// Filters the input CP IDs list by removing those for which none of the site CP pair match
	//
	Set<Long> filterCps(Collection<Long> cpIds, Set<SiteCpPair> siteCps);

	// cpId -> [formId]
	Map<Long, Set<Long>> getCpForms(List<Long> cpIds, String entityType);

	int deleteForms(Collection<Long> formIds);

	int deleteForms(Long groupId);
}
