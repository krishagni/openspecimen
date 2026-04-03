package com.krishagni.catissueplus.rest.controller;

import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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

import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.events.DpRequirementDetail;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/distribution-protocol-requirements")
public class DpRequirementController {
	@Autowired
	private HttpServletRequest httpServletRequest;
	
	@Autowired
	private DistributionProtocolService dpSvc;

	@Autowired
	private FormService formSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DpRequirementDetail> getAllRequirements(@RequestParam(value = "dpId") Long dpId) {
		ResponseEvent<List<DpRequirementDetail>> resp = dpSvc.getRequirements(getRequest(dpId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpRequirementDetail getRequirement(@PathVariable("id") Long reqId) {
		ResponseEvent<DpRequirementDetail> resp = dpSvc.getRequirement(getRequest(reqId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpRequirementDetail createRequirement(
			@RequestBody
			DpRequirementDetail detail) {
		
		ResponseEvent<DpRequirementDetail> resp = dpSvc.createRequirement(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpRequirementDetail updateRequirement(
			@PathVariable("id")
			Long dprId,
			
			@RequestBody
			DpRequirementDetail detail) {
		
		detail.setId(dprId);
		ResponseEvent<DpRequirementDetail> resp = dpSvc.updateRequirement(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpRequirementDetail deleteRequirement(@PathVariable("id") Long dprId) {
		ResponseEvent<DpRequirementDetail> resp = dpSvc.deleteRequirement(getRequest(dprId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm() {
		return formSvc.getExtensionInfo(-1L, DpRequirement.EXTN);
	}
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
}
