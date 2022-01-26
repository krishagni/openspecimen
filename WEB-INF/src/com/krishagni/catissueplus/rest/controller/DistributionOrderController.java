package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.events.DistributionOrderDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.RetrieveSpecimensOp;
import com.krishagni.catissueplus.core.administrative.events.ReturnedSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.services.DistributionOrderService;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

@Controller
@RequestMapping("/distribution-orders")
public class DistributionOrderController {
	@Autowired
	private HttpServletRequest httpServletRequest;
	
	@Autowired
	private DistributionOrderService distributionService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionOrderSummary> getDistributionOrders(			
		@RequestParam(value = "query", required = false, defaultValue = "") 
		String searchTerm,
		
		@RequestParam(value = "dpShortTitle", required = false, defaultValue = "")
		String dpShortTitle,
		
		@RequestParam(value = "dpId", required = false)
		Long dpId,
		
		@RequestParam(value = "requestor", required = false, defaultValue = "")
		String requestor,
		
		@RequestParam(value = "requestorId", required = false)
		Long requestorId,

		@RequestParam(value = "requestId", required = false)
		Long requestId,
		
		@RequestParam(value = "executionDate", required = false) 
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date executionDate,
		
		@RequestParam(value = "receivingSite", required = false, defaultValue = "")
		String receivingSite,
		
		@RequestParam(value = "receivingInstitute", required = false, defaultValue = "")
		String receivingInstitute,

		@RequestParam(value = "status", required = false, defaultValue = "")
		String status,
		
		@RequestParam(value = "startAt", required = false, defaultValue = "0") 
		int startAt,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "50") 
		int maxResults,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false") 
		boolean includeStats) {
		
		
		DistributionOrderListCriteria listCrit = new DistributionOrderListCriteria()
			.query(searchTerm)
			.dpShortTitle(dpShortTitle)
			.dpId(dpId)
			.requestor(requestor)
			.requestorId(requestorId)
			.requestId(requestId)
			.executionDate(executionDate)
			.receivingSite(receivingSite)
			.receivingInstitute(receivingInstitute)
			.status(status)
			.startAt(startAt)
			.maxResults(maxResults)
			.includeStat(includeStats);

		return ResponseEvent.unwrap(distributionService.getOrders(RequestEvent.wrap(listCrit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getDistributionOrdersCount(
		@RequestParam(value = "query", required = false, defaultValue = "")
		String searchTerm,
		
		@RequestParam(value = "dpShortTitle", required = false, defaultValue = "")
		String dpShortTitle,
		
		@RequestParam(value = "dpId", required = false)
		Long dpId,
		
		@RequestParam(value = "requestor", required = false, defaultValue = "")
		String requestor,
		
		@RequestParam(value = "requestorId", required = false)
		Long requestorId,

		@RequestParam(value = "requestId", required = false)
		Long requestId,

		@RequestParam(value = "executionDate", required = false)
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date executionDate,
		
		@RequestParam(value = "receivingSite", required = false, defaultValue = "")
		String receivingSite,
		
		@RequestParam(value = "receivingInstitute", required = false, defaultValue = "")
		String receivingInstitute,

		@RequestParam(value = "status", required = false, defaultValue = "")
		String status) {
		
		
		DistributionOrderListCriteria listCrit = new DistributionOrderListCriteria()
			.query(searchTerm)
			.dpShortTitle(dpShortTitle)
			.dpId(dpId)
			.requestor(requestor)
			.requestorId(requestorId)
			.requestId(requestId)
			.executionDate(executionDate)
			.receivingSite(receivingSite)
			.receivingInstitute(receivingInstitute)
			.status(status);
			
		Long count = ResponseEvent.unwrap(distributionService.getOrdersCount(RequestEvent.wrap(listCrit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionOrderDetail getDistribution(@PathVariable("id") Long id) {
		return ResponseEvent.unwrap(distributionService.getOrder(RequestEvent.wrap(id)));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionOrderDetail createDistribution(@RequestBody DistributionOrderDetail order) {
		order.setId(null);
		return ResponseEvent.unwrap(distributionService.createOrder(RequestEvent.wrap(order)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionOrderDetail updateDistribution(
		@PathVariable("id") 
		Long distributionId,
		
		@RequestBody 
		DistributionOrderDetail order) {
		
		order.setId(distributionId);
		return ResponseEvent.unwrap(distributionService.updateOrder(RequestEvent.wrap(order)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionOrderDetail deleteOrder(@PathVariable("id") Long orderId) {
		return ResponseEvent.unwrap(distributionService.deleteOrder(RequestEvent.wrap(orderId)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportDistributionReport(@PathVariable("id") Long orderId) {
		return ResponseEvent.unwrap(distributionService.exportReport(RequestEvent.wrap(orderId)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/items")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionOrderItemDetail> getOrderItems(
		@PathVariable("id")
		Long orderId,

		@RequestParam(value = "storedInDistributionContainer", required = false, defaultValue = "false")
		boolean storedInDistributionContainer,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		DistributionOrderItemListCriteria crit = new DistributionOrderItemListCriteria()
			.orderId(orderId)
			.storedInContainers(storedInDistributionContainer)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(distributionService.getOrderItems(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method =  RequestMethod.POST, value = "/{id}/retrieve")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> retrieveOrderItems(
		@PathVariable("id")
		Long orderId,

		@RequestBody
		RetrieveSpecimensOp detail) {

		detail.setListId(orderId);
		Integer count = ResponseEvent.unwrap(distributionService.retrieveSpecimens(RequestEvent.wrap(detail)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionOrderItemDetail> getDistributedSpecimens(
		@RequestParam(value = "label", required = false)
		List<String> labels,

		@RequestParam(value = "barcode", required = false)
		List<String> barcodes) {

		return searchDistributedSpecimens(new SpecimenListCriteria().labels(labels).barcodes(barcodes));
	}

	@RequestMapping(method = RequestMethod.POST, value="/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionOrderItemDetail> searchDistributedSpecimens(@RequestBody SpecimenListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.ids()) &&
			CollectionUtils.isEmpty(crit.labels()) &&
			CollectionUtils.isEmpty(crit.barcodes())) {
			return Collections.emptyList();
		}

		crit.exactMatch(true);
		return ResponseEvent.unwrap(distributionService.getDistributedSpecimens(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/return-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> returnSpecimens(@RequestBody List<ReturnedSpecimenDetail> returnedSpecimens) {
		return ResponseEvent.unwrap(distributionService.returnSpecimens(RequestEvent.wrap(returnedSpecimens)));
	}
}
