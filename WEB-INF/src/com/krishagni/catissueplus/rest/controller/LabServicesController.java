package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/lab-services")
public class LabServicesController {

	@Autowired
	private RateListService rateListSvc;

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
}
