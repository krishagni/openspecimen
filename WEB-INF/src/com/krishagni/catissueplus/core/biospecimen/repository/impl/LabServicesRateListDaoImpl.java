package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class LabServicesRateListDaoImpl extends AbstractDao<LabServicesRateList> implements LabServicesRateListDao {
	@Override
	public Class<LabServicesRateList> getType() {
		return LabServicesRateList.class;
	}
}
