package com.krishagni.openspecimen.custom.demo.rest;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.openspecimen.custom.demo.events.SpecimenCollectionDetail;
import com.krishagni.openspecimen.custom.demo.services.SpecimenCollectionService;

//@Controller
//@RequestMapping("/demo/participant-specimens")
public class SpecimenCollectionController {
	
//	@Autowired
	private SpecimenCollectionService specimenCollectionSvc;
	
//	@Autowired
	private HttpServletRequest httpReq;
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenCollectionDetail collectSpecimens(@RequestBody SpecimenCollectionDetail input) {
		RequestEvent<SpecimenCollectionDetail> req = new RequestEvent<SpecimenCollectionDetail>(input);	
		ResponseEvent<SpecimenCollectionDetail> resp = specimenCollectionSvc.collect(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}	
}
