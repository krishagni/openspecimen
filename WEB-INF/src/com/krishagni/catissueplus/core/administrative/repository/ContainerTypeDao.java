package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ContainerTypeDao extends Dao<ContainerType> {
	List<ContainerType> getTypes(ContainerTypeListCriteria crit);

	Long getTypesCount(ContainerTypeListCriteria crit);

	List<ContainerType> getByNames(Collection<String> names);

	ContainerType getByName(String name);

	List<Long> getLeafTypeIds();

	List<ContainerType> getActiveParentTypes(Long typeId);

	List<DependentEntityDetail> getDependentEntities(Long typeId);
}
