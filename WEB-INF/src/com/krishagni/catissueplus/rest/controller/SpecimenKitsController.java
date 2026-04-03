package com.krishagni.catissueplus.rest.controller;

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

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenKitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenKitListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenKitService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

@Controller
@RequestMapping("/specimen-kits")
public class SpecimenKitsController {

	@Autowired
	private SpecimenKitService kitSvc;


	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenKitSummary> getSpecimenKits(
		@RequestParam(value="cpId",             required = false)
		Long cpId,

		@RequestParam(value="cpShortTitle",     required = false)
		String cpShortTitle,

		@RequestParam(value="cpTitle",          required = false)
		String cpTitle,

		@RequestParam(value = "startAt",        required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults",     required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "includeStats",   required = false, defaultValue = "false")
		boolean includeStats) {

		SpecimenKitListCriteria listCrit = new SpecimenKitListCriteria()
			.cpId(cpId)
			.cpShortTitle(cpShortTitle)
			.cpTitle(cpTitle)
			.startAt(startAt)
			.maxResults(maxResults)
			.includeStat(includeStats);

		ResponseEvent<List<SpecimenKitSummary>> resp = kitSvc.getSpecimenKits(getRequest(listCrit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenKitDetail getSpecimenKit(@PathVariable("id") Long kitId) {
		ResponseEvent<SpecimenKitDetail> resp = kitSvc.getSpecimenKit(getRequest(kitId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenKitDetail addKit(@RequestBody SpecimenKitDetail detail) {
		ResponseEvent<SpecimenKitDetail> resp = kitSvc.createSpecimenKit(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenKitDetail updateKit(@PathVariable("id") Long id, @RequestBody SpecimenKitDetail detail) {
		detail.setId(id);
		ResponseEvent<SpecimenKitDetail> resp = kitSvc.updateSpecimenKit(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportReport(@PathVariable("id") Long id) {
		ResponseEvent<QueryDataExportResult> resp = kitSvc.exportReport(getRequest(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}

}
