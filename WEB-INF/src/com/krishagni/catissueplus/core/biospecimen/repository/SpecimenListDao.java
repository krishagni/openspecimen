package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListItem;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimensPickList;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDigestItem;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.common.Tuple;
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
	// Specimen pick lists
	//
	void saveOrUpdate(SpecimensPickList pickList);

	List<SpecimensPickList> getPickLists(PickListsCriteria crit);

	SpecimensPickList getPickList(Long cartId, Long pickListId);

	void deletePickList(Long cartId, Long pickListId);

	Map<String, Long> getPickListSpecimensCount(Long pickListId);

	Map<Long, Map<String, Long>> getPickListSpecimensCount(Collection<Long> pickListIds);

	List<Long> getSpecimenIdsInPickList(Long pickListId, List<Long> specimenIds);

	int savePickListItems(SpecimensPickList pickList, User pickedBy, Date pickTime, List<Long> specimenIds);

	int deleteSpecimensFromPickList(Long pickListId, List<Long> specimenIds);

	List<Tuple> getInactivePickLists(Date activeBefore);

	//
	// Deprecated, used mostly by specimen request plugin
	//
	Map<Long, List<Specimen>> getListCpSpecimens(Long listId);

	List<Long> getListSpecimensCpIds(Long listId);

	List<Long> getDigestEnabledLists();

	List<SpecimenListDigestItem> getListDigest(Long listId, Date startTime, Date endTime);
}
