package com.krishagni.catissueplus.core.administrative.services;

import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.Restriction;

public interface ContainerSelectionRule {
	String getName();

	String getSql(String containerTabAlias, Map<String, Object> params);

	Restriction getRestriction(AbstractCriteria<?, ?> query, String containerObjAlias, Map<String, Object> params);

	boolean eval(StorageContainer container, Map<String, Object> params);
}
