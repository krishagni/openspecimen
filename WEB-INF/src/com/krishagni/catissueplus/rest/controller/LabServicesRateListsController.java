package com.krishagni.catissueplus.rest.controller;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListCollectionProtocolsOp;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListServicesOp;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/lab-services-rate-lists")
public class LabServicesRateListsController {

	@Autowired
	private RateListService rateListSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<LabServicesRateListDetail> getRateLists(
		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "serviceCode", required = false)
		String serviceCode,

		@RequestParam(value = "rateEffectiveOn", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate rateEffectiveOn,

		@RequestParam(value = "startDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate startDate,

		@RequestParam(value = "endDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate endDate,

		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults
	) {
		LabServicesRateListCriteria crit = new LabServicesRateListCriteria()
			.cpId(cpId)
			.cpShortTitle(cpShortTitle)
			.serviceCode(serviceCode)
			.startDate(startDate)
			.endDate(endDate)
			.effectiveDate(rateEffectiveOn)
			.query(query)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(rateListSvc.getRateLists(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getRateListsCount(
		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "serviceCode", required = false)
		String serviceCode,

		@RequestParam(value = "rateEffectiveOn", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate rateEffectiveOn,

		@RequestParam(value = "startDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate startDate,

		@RequestParam(value = "endDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		LocalDate endDate,

		@RequestParam(value = "query", required = false)
		String query
	) {
		LabServicesRateListCriteria crit = new LabServicesRateListCriteria()
			.cpId(cpId)
			.cpShortTitle(cpShortTitle)
			.serviceCode(serviceCode)
			.startDate(startDate)
			.endDate(endDate)
			.effectiveDate(rateEffectiveOn)
			.query(query);
		return Collections.singletonMap("count", ResponseEvent.unwrap(rateListSvc.getRateListsCount(RequestEvent.wrap(crit))));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/id")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServicesRateListDetail getRateList(@PathVariable("id") Long rateListId) {
		return ResponseEvent.unwrap(rateListSvc.getRateList(RequestEvent.wrap(new EntityQueryCriteria(rateListId))));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServicesRateListDetail createRateList(@RequestBody LabServicesRateListDetail input) {
		return ResponseEvent.unwrap(rateListSvc.createRateList(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServicesRateListDetail updateRateList(@PathVariable("id") Long rateListId, @RequestBody LabServicesRateListDetail input) {
		input.setId(rateListId);
		return ResponseEvent.unwrap(rateListSvc.updateRateList(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/service-rates")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> updateRateListServices(@PathVariable("id") Long rateListId, @RequestBody UpdateRateListServicesOp op) {
		op.setRateListId(rateListId);
		return Collections.singletonMap("count", ResponseEvent.unwrap(rateListSvc.updateRateListServices(RequestEvent.wrap(op))));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/collection-protocols")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> updateRateListCps(@PathVariable("id") Long rateListId, @RequestBody UpdateRateListCollectionProtocolsOp op) {
		op.setRateListId(rateListId);
		return Collections.singletonMap("count", ResponseEvent.unwrap(rateListSvc.updateRateListCps(RequestEvent.wrap(op))));
	}
}
