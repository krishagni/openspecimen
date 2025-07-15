package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceRateDetail;

public interface ServiceRateFactory {
	ServiceRate createServiceRate(ServiceRateDetail input);
}
