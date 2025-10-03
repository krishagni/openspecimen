package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;

public interface LabServicesRateListFactory {
	LabServicesRateList createRateList(LabServicesRateListDetail input);
}
