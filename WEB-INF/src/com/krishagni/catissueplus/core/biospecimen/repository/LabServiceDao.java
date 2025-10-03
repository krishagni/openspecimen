package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabServiceDao extends Dao<LabService> {
	LabService getByCode(String code);
}
