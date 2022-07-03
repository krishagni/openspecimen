package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface SpecimenListDao extends Dao<SpecimenList> {
	List<SpecimenListSummary> getSpecimenLists(SpecimenListsCriteria crit);

	Long getSpecimenListsCount(SpecimenListsCriteria crit);

	SpecimenList getSpecimenList(Long listId);
	
	SpecimenList getSpecimenListByName(String name);

	SpecimenList getDefaultSpecimenList(Long userId);
	
	int getListSpecimensCount(Long listId);
	
	void deleteSpecimenList(SpecimenList list);

	boolean isListSharedWithUser(Long listId, Long userId);

	List<Long> getListsSharedWithUser(List<Long> listIds, Long userId);

	//
	// APIs to save specimen list items
	//

	List<SpecimenListItem> getListItems(Long listId, List<Long> specimenIds);

	void saveListItems(List<SpecimenListItem> items);

	int deleteListItems(Long listId, List<Long> specimenIds);

	int clearList(Long listId);

	List<Long> getSpecimenIdsInList(Long listId, List<Long> specimenIds);

	void addChildSpecimens(Long listId, boolean oracle);

	//
	// Deprecated, used mostly by specimen request plugin
	//
	Map<Long, List<Specimen>> getListCpSpecimens(Long listId);

	List<Long> getListSpecimensCpIds(Long listId);
}
