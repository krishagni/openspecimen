package com.krishagni.catissueplus.rest.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
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

import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.events.InstituteQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.administrative.repository.InstituteListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.administrative.services.InstituteService;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/institutes")
public class InstitutesController {

	@Autowired
	private InstituteService instituteSvc;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<InstituteDetail> getInstitutes(
		@RequestParam(value = "id", required = false)
		List<Long> ids,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "exactMatch", required = false, defaultValue = "false")
		boolean exactMatch,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats) {

		InstituteListCriteria crit = new InstituteListCriteria()
			.ids(ids)
			.query(name)
			.exactMatch(exactMatch)
			.startAt(startAt)
			.maxResults(maxResults)
			.includeStat(includeStats);

		RequestEvent<InstituteListCriteria> req = new RequestEvent<InstituteListCriteria>(crit);
		ResponseEvent<List<InstituteDetail>> resp = instituteSvc.getInstitutes(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Long> getInstitutesCount(
		@RequestParam(value = "id", required = false)
		List<Long> ids,

		@RequestParam(value = "name", required = false)
		String name) {

		RequestEvent<InstituteListCriteria> req = RequestEvent.wrap(new InstituteListCriteria().ids(ids).query(name));
		ResponseEvent<Long> resp = instituteSvc.getInstitutesCount(req);
		resp.throwErrorIfUnsuccessful();

		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public InstituteDetail getInstitute(@PathVariable("id") Long id) {
		InstituteQueryCriteria crit = new InstituteQueryCriteria();
		crit.setId(id);

		RequestEvent<InstituteQueryCriteria> req = new RequestEvent<InstituteQueryCriteria>(crit);
		ResponseEvent<InstituteDetail> resp = instituteSvc.getInstitute(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/byname")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public InstituteDetail getInstituteByName(@RequestParam(value = "name") String name) {
		InstituteQueryCriteria crit = new InstituteQueryCriteria();
		crit.setName(name);

		RequestEvent<InstituteQueryCriteria> req = new RequestEvent<InstituteQueryCriteria>(crit);
		ResponseEvent<InstituteDetail> resp = instituteSvc.getInstitute(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public InstituteDetail createInstitute(@RequestBody InstituteDetail detail) {
		RequestEvent<InstituteDetail> req = new RequestEvent<InstituteDetail>(detail);
		ResponseEvent<InstituteDetail> resp = instituteSvc.createInstitute(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public InstituteDetail updateInstitute(
		@PathVariable("id")
		Long instituteId,

		@RequestBody
		InstituteDetail instituteDetail) {

		instituteDetail.setId(instituteId);

		RequestEvent<InstituteDetail> req = new RequestEvent<InstituteDetail>(instituteDetail);
		ResponseEvent<InstituteDetail> resp = instituteSvc.updateInstitute(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/dependent-entities")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<DependentEntityDetail> getDependentEntities(@PathVariable("id") Long id) {
		RequestEvent<Long> req = new RequestEvent<Long>(id);
		ResponseEvent<List<DependentEntityDetail>> resp = instituteSvc.getDependentEntities(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public InstituteDetail deleteInstitute(
		@PathVariable("id")
		Long id,

		@RequestParam(value = "close", required = false, defaultValue = "false")
		boolean close) {

		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(Collections.singleton(id));
		op.setClose(close);

		RequestEvent<BulkDeleteEntityOp> req = new RequestEvent<>(op);
		ResponseEvent<List<InstituteDetail>> resp = instituteSvc.deleteInstitutes(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload().get(0);
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<InstituteDetail> deleteInstitutes(
		@RequestParam(value = "id")
		Long[] ids,

		@RequestParam(value = "close", required = false, defaultValue = "false")
		boolean close) {

		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(new HashSet<>(Arrays.asList(ids)));
		op.setClose(close);

		RequestEvent<BulkDeleteEntityOp> req = new RequestEvent<>(op);
		ResponseEvent<List<InstituteDetail>> resp = instituteSvc.deleteInstitutes(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	//
	// Endpoint added to get list of sites without any authentication context
	//
	@RequestMapping(method = RequestMethod.GET, value = "/sites")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<SiteSummary> getSites(
		@RequestParam(value = "name")
		String instituteName,

		@RequestParam(value = "siteName", required = false, defaultValue = "")
		String siteName) {

		SiteListCriteria listCriteria = new SiteListCriteria()
			.institute(instituteName)
			.query(siteName);

		ResponseEvent<List<SiteSummary>> resp = instituteSvc.getSites(new RequestEvent<>(listCriteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}