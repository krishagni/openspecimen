package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreListItem;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ContainerStoreListDao extends Dao<ContainerStoreList> {

	List<ContainerStoreList> getStoreLists(ContainerStoreListCriteria crit);

	List<ContainerStoreListItem> getStoreListItems(Long storeListId, Long lastId, int maxResults);

	Map<ContainerStoreList.Op, Integer> getStoreListItemsCount(Date from, Date to);

	void saveOrUpdateItem(ContainerStoreListItem item);

	List<StorageContainer> getAutomatedFreezers(List<Long> storeListIds);
}
