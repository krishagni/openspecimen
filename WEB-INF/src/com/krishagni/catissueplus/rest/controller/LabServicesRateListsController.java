package com.krishagni.catissueplus.rest.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/lab-services-rate-lists")
public class LabServicesRateListsController {

	@Autowired
	private RateListService rateListSvc;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public LabServicesRateListDetail createRateList(@RequestBody LabServicesRateListDetail input) {
		return ResponseEvent.unwrap(rateListSvc.createRateList(RequestEvent.wrap(input)));
	}
}
