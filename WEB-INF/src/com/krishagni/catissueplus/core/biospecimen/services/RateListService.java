package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface RateListService {

	ResponseEvent<List<LabServiceDetail>> getServices(RequestEvent<LabServiceListCriteria> req);

	ResponseEvent<Long> getServicesCount(RequestEvent<LabServiceListCriteria> req);

	ResponseEvent<LabServiceDetail> getService(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<LabServiceDetail> createService(RequestEvent<LabServiceDetail> req);

	ResponseEvent<LabServiceDetail> updateService(RequestEvent<LabServiceDetail> req);

	ResponseEvent<LabServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<LabServicesRateListDetail> createRateList(RequestEvent<LabServicesRateListDetail> input);
}
