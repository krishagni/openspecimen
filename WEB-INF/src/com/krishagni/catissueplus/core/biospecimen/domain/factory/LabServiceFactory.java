package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;

public interface LabServiceFactory {
	LabService createService(LabServiceDetail input);
}
