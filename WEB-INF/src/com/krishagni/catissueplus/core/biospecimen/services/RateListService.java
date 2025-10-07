package com.krishagni.catissueplus.core.biospecimen.services;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListCollectionProtocolsOp;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListServicesOp;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListCriteria;
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

	ResponseEvent<List<LabServicesRateListDetail>> getRateLists(RequestEvent<LabServicesRateListCriteria> req);

	ResponseEvent<Long> getRateListsCount(RequestEvent<LabServicesRateListCriteria> req);

	ResponseEvent<LabServicesRateListDetail> getRateList(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<LabServicesRateListDetail> createRateList(RequestEvent<LabServicesRateListDetail> req);

	ResponseEvent<LabServicesRateListDetail> updateRateList(RequestEvent<LabServicesRateListDetail> req);

	ResponseEvent<List<LabServiceRateDetail>> getRateListServices(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<Integer> updateRateListServices(RequestEvent<UpdateRateListServicesOp> req);

	ResponseEvent<List<CollectionProtocolSummary>> getRateListCps(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<Long> getRateListCpsCount(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<Integer> updateRateListCps(RequestEvent<UpdateRateListCollectionProtocolsOp> req);
}
