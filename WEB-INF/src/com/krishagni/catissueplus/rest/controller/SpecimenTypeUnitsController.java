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

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenTypeUnitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenTypeUnitsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenTypeUnitsService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/specimen-type-units")
public class SpecimenTypeUnitsController {

	@Autowired
	private SpecimenTypeUnitsService unitsSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenTypeUnitDetail> getUnits(
		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "specimenClass", required = false)
		String specimenClass,

		@RequestParam(value = "specimenType", required = false)
		String type,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults
	) {
		SpecimenTypeUnitsListCriteria crit = new SpecimenTypeUnitsListCriteria()
			.cpShortTitle(cpShortTitle)
			.specimenClass(specimenClass)
			.type(type)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(unitsSvc.getUnits(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getUnitsCount(
		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "specimenClass", required = false)
		String specimenClass,

		@RequestParam(value = "specimenType", required = false)
		String type
	) {
		SpecimenTypeUnitsListCriteria crit = new SpecimenTypeUnitsListCriteria()
			.cpShortTitle(cpShortTitle)
			.specimenClass(specimenClass)
			.type(type);
		Long count = ResponseEvent.unwrap(unitsSvc.getUnitsCount(RequestEvent.wrap(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenTypeUnitDetail createUnit(@RequestBody SpecimenTypeUnitDetail input) {
		return ResponseEvent.unwrap(unitsSvc.createUnit(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenTypeUnitDetail updateUnit(@PathVariable("id") Long id, @RequestBody SpecimenTypeUnitDetail input) {
		input.setId(id);
		return ResponseEvent.unwrap(unitsSvc.updateUnit(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenTypeUnitDetail deleteUnit(@PathVariable("id") Long id) {
		SpecimenTypeUnitDetail input = new SpecimenTypeUnitDetail();
		input.setId(id);
		return ResponseEvent.unwrap(unitsSvc.deleteUnit(RequestEvent.wrap(input)));
	}
}
