package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceDetail;

public interface ServiceFactory {
	Service createService(ServiceDetail input);
}
