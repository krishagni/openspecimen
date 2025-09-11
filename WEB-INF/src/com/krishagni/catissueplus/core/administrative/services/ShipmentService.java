package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.ShipmentCartDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimensPickListDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

public interface ShipmentService {
	ResponseEvent<List<ShipmentDetail>> getShipments(RequestEvent<ShipmentListCriteria> req);

	ResponseEvent<Long> getShipmentsCount(RequestEvent<ShipmentListCriteria> req);

	ResponseEvent<ShipmentDetail> getShipment(RequestEvent<Long> req);

	ResponseEvent<List<ShipmentContainerDetail>> getShipmentContainers(RequestEvent<ShipmentItemsListCriteria> req);

	ResponseEvent<List<ShipmentSpecimenDetail>> getShipmentSpecimens(RequestEvent<ShipmentItemsListCriteria> req);
	
	ResponseEvent<ShipmentDetail> createShipment(RequestEvent<ShipmentDetail> req);
	
	ResponseEvent<ShipmentDetail> updateShipment(RequestEvent<ShipmentDetail> req);

	ResponseEvent<ShipmentDetail> deleteShipment(RequestEvent<Long> req);

	ResponseEvent<ShipmentDetail> updateShipmentRequestStatus(RequestEvent<ShipmentDetail> req);
	
	ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req);

	List<StorageContainerSummary> getContainers(List<String> names, boolean request, String sendingSiteName, String receivingSiteName);

	ResponseEvent<SpecimensPickListDetail> createPickList(RequestEvent<ShipmentCartDetail> req);
}
