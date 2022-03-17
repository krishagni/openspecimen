package com.krishagni.catissueplus.core.administrative.services;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.PrintDistributionLabelDetail;
import com.krishagni.catissueplus.core.administrative.events.ReserveSpecimensDetail;
import com.krishagni.catissueplus.core.administrative.events.RetrieveSpecimensOp;
import com.krishagni.catissueplus.core.administrative.events.ReturnedSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.EntityCrudListener;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

public interface DistributionOrderService {
	public ResponseEvent<List<DistributionOrderSummary>> getOrders(RequestEvent<DistributionOrderListCriteria> req);

	public ResponseEvent<Long> getOrdersCount(RequestEvent<DistributionOrderListCriteria> req);

	public ResponseEvent<DistributionOrderDetail> getOrder(RequestEvent<Long> req);
	
	public ResponseEvent<DistributionOrderDetail> createOrder(RequestEvent<DistributionOrderDetail> req);
	
	public ResponseEvent<DistributionOrderDetail> updateOrder(RequestEvent<DistributionOrderDetail> req);

	public ResponseEvent<DistributionOrderDetail> deleteOrder(RequestEvent<Long> req);
	
	public ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req);

	ResponseEvent<List<DistributionOrderItemDetail>> getOrderItems(RequestEvent<DistributionOrderItemListCriteria> req);

	ResponseEvent<Integer> deleteOrderItems(Long orderId, String orderName, List<Long> itemIds);
	
	public ResponseEvent<List<DistributionOrderItemDetail>> getDistributedSpecimens(RequestEvent<SpecimenListCriteria> req);

	public ResponseEvent<List<SpecimenInfo>> returnSpecimens(RequestEvent<List<ReturnedSpecimenDetail>> req);

	ResponseEvent<List<SpecimenInfo>> getReservedSpecimens(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<Integer> getReservedSpecimensCount(RequestEvent<SpecimenListCriteria> req);

	ResponseEvent<Integer> reserveSpecimens(RequestEvent<ReserveSpecimensDetail> req);

	ResponseEvent<Integer> retrieveSpecimens(RequestEvent<RetrieveSpecimensOp> req);

	ResponseEvent<LabelPrintJobSummary> printDistributionLabels(RequestEvent<PrintDistributionLabelDetail> req);

	ResponseEvent<List<LabelTokenDetail>> getPrintLabelTokens();

	//
	// Validators
	//
	void addValidator(DistributionValidator validator);

	void removeValidator(String name);

	//
	// Internal APIs for use by plugins and service extenders...
	//
	public void addListener(EntityCrudListener<DistributionOrderDetail, DistributionOrder> listener);

	public void validateSpecimens(DistributionProtocol dp, List<Specimen> specimens);
}
