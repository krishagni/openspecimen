package com.krishagni.catissueplus.rest.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

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

import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("specimen-requirements")
public class SpecimenRequirementsController {

	@Autowired
	private CollectionProtocolService cpSvc;
	
	@Autowired
	private HttpServletRequest httpServletRequest;		

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenRequirementDetail> getRequirements(
			@RequestParam(value = "cpId", required = false)
			Long cpId,

			@RequestParam(value = "eventId", required = false)
			Long eventId,

			@RequestParam(value = "eventLabel", required = false)
			String eventLabel,

			@RequestParam(value = "includeChildReqs", required = false, defaultValue = "true")
			Boolean includeChildReqs) {
		
		return response(cpSvc.getSpecimenRequirments(request(Tuple.make(cpId, eventId, eventLabel, includeChildReqs))));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenRequirementDetail getRequirement(@PathVariable("id") Long id) {
		return response(cpSvc.getSpecimenRequirement(request(id)));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenRequirementDetail addRequirement(@RequestBody SpecimenRequirementDetail requirement) {
		return response(cpSvc.addSpecimenRequirement(request(requirement)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenRequirementDetail updateRequirement(
			@PathVariable("id")
			Long id,

			@RequestBody
			SpecimenRequirementDetail requirement) {
		requirement.setId(id);
		
		return response(cpSvc.updateSpecimenRequirement(request(requirement)));
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{id}/aliquots")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenRequirementDetail> createAliquots(
			@PathVariable("id")
			Long parentSrId,

			@RequestBody
			AliquotSpecimensRequirement requirement) {
		
		requirement.setParentSrId(parentSrId);
		return response(cpSvc.createAliquots(request(requirement)));
	}

	@RequestMapping(method = RequestMethod.POST, value="/{id}/derived")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenRequirementDetail createDerived(
			@PathVariable("id")
			Long parentSrId,

			@RequestBody
			DerivedSpecimenRequirement requirement) {
		
		requirement.setParentSrId(parentSrId);
		return response(cpSvc.createDerived(request(requirement)));
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{id}/copy")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenRequirementDetail copySr(
			@PathVariable("id")
			Long srId) {
		
		return response(cpSvc.copySpecimenRequirement(request(srId)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public SpecimenRequirementDetail deleteSr(@PathVariable("id") Long srId) {
		return response(cpSvc.deleteSpecimenRequirement(request(srId)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/specimens-count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Integer getSpecimensCount(@PathVariable("id") Long srId) {
		return response(cpSvc.getSrSpecimensCount(request(srId)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
