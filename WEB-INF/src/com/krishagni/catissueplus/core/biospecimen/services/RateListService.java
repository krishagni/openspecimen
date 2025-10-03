package com.krishagni.catissueplus.core.biospecimen.services;

import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface RateListService {

	ResponseEvent<LabServiceDetail> createService(RequestEvent<LabServiceDetail> req);

	ResponseEvent<LabServiceDetail> updateService(RequestEvent<LabServiceDetail> req);

	ResponseEvent<LabServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req);
}
