package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface RequestManagerGroupDao extends Dao<RequestManagerGroup> {
	RequestManagerGroup getByName(String name);
}
