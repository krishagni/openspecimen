package com.krishagni.catissueplus.core.administrative.services;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

public interface AutomatedFreezer {
	default String listName(StorageContainer container, ContainerStoreList.Op op, Specimen specimen) {
		return container.getId() + "-" + op.name();
	}

	ContainerStoreList.Status processList(ContainerStoreList list);
}
