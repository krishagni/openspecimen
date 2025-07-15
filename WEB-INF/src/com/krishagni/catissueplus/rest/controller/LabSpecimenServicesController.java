package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;

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

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/lab-specimen-services")
public class LabSpecimenServicesController {

	@Autowired
	private SpecimenService spmnSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenServiceDetail> getServices(
		@RequestParam(value = "specimenId")
		Long specimenId,

		@RequestParam(value = "includeRates", required = false, defaultValue = "false")
		boolean includeRates) {
		SpecimenQueryCriteria criteria = new SpecimenQueryCriteria(specimenId);
		criteria.setParams(Collections.singletonMap("includeRates", includeRates));
		return ResponseEvent.unwrap(spmnSvc.getServices(RequestEvent.wrap(criteria)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenServiceDetail getService(@PathVariable("id") Long serviceId) {
		return ResponseEvent.unwrap(spmnSvc.getService(RequestEvent.wrap(new EntityQueryCriteria(serviceId))));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenServiceDetail addService(@RequestBody SpecimenServiceDetail input) {
		input.setId(null);
		return ResponseEvent.unwrap(spmnSvc.addOrUpdateService(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenServiceDetail updateService(@PathVariable("id") Long serviceId, @RequestBody SpecimenServiceDetail input) {
		input.setId(serviceId);
		return ResponseEvent.unwrap(spmnSvc.addOrUpdateService(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenServiceDetail deleteService(@PathVariable("id") Long serviceId) {
		return ResponseEvent.unwrap(spmnSvc.deleteService(RequestEvent.wrap(new EntityQueryCriteria(serviceId))));
	}
}
