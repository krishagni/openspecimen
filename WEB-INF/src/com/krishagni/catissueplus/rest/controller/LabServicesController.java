package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/lab-services")
public class LabServicesController {

	@Autowired
	private RateListService rateListSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<LabServiceDetail> getServices(
		@RequestParam(value = "code", required = false)
		List<String> codes,

		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "notInRateListId", required = false)
		Long notInRateListId,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		LabServiceListCriteria crit = new LabServiceListCriteria()
			.codes(codes)
			.query(query)
			.notInRateListId(notInRateListId)
			.cpId(cpId)
			.includeStat(includeStats)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(rateListSvc.getServices(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getServicesCount(
		@RequestParam(value = "code", required = false)
		List<String> codes,

		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "notInRateListId", required = false)
		Long notInRateListId) {

		LabServiceListCriteria crit = new LabServiceListCriteria().codes(codes).query(query).cpId(cpId).notInRateListId(notInRateListId);
		return Collections.singletonMap("count", ResponseEvent.unwrap(rateListSvc.getServicesCount(RequestEvent.wrap(crit))));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServiceDetail getService(@PathVariable("id") Long svcId) {
		return ResponseEvent.unwrap(rateListSvc.getService(RequestEvent.wrap(new EntityQueryCriteria(svcId))));
	}


	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServiceDetail createService(@RequestBody LabServiceDetail input) {
		return ResponseEvent.unwrap(rateListSvc.createService(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServiceDetail updateService(@PathVariable("id") Long svcId, @RequestBody LabServiceDetail input) {
		input.setId(svcId);
		return ResponseEvent.unwrap(rateListSvc.updateService(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServiceDetail deleteService(@PathVariable("id") Long svcId) {
		return ResponseEvent.unwrap(rateListSvc.deleteService(RequestEvent.wrap(new EntityQueryCriteria(svcId))));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/rate-lists")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<LabServicesRateListDetail> getRateLists(@PathVariable("id") Long svcId) {
		return ResponseEvent.unwrap(rateListSvc.getServiceRates(RequestEvent.wrap(new EntityQueryCriteria(svcId))));
	}
}
