
package com.krishagni.catissueplus.rest.controller;

import java.util.Arrays;
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

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.events.SiteDetail;
import com.krishagni.catissueplus.core.administrative.events.SiteQueryCriteria;
import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.administrative.services.SiteService;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/sites")
public class SitesController {

	@Autowired
	private SiteService siteService;
	
	@Autowired
	private FormService formSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SiteSummary> getSites(
			@RequestParam(value = "name", required= false)
			String name,
			
			@RequestParam(value = "exactMatch", required= false, defaultValue = "false")
			boolean exactMatch,
			
			@RequestParam(value = "resource", required = false)
			String resource,
			
			@RequestParam(value = "operation", required = false)
			String operation,
			
			@RequestParam(value = "institute", required = false)
			String institute,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100") 
			int maxResults,
			
			@RequestParam(value = "listAll", required = false, defaultValue = "false")
			boolean listAll,

			@RequestParam(value = "includeType", required = false)
			List<String> includeTypes,

			@RequestParam(value = "excludeType", required = false)
			List<String> excludeTypes,

			@RequestParam(value = "includeStats", required = false, defaultValue = "false")
			boolean includeStats) {
		
		SiteListCriteria crit = new SiteListCriteria()
			.query(name)
			.exactMatch(exactMatch)
			.resource(resource)
			.operation(operation)
			.institute(institute)
			.startAt(startAt)
			.maxResults(maxResults)
			.listAll(listAll)
			.includeTypes(includeTypes)
			.excludeTypes(excludeTypes)
			.includeStat(includeStats);
		
		RequestEvent<SiteListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<List<SiteSummary>> resp = siteService.getSites(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getSitesCount(
			@RequestParam(value = "name", required= false)
			String name,
			
			@RequestParam(value = "institute", required = false)
			String institute) {
		
		SiteListCriteria crit = new SiteListCriteria()
			.query(name)
			.institute(institute);
		
		RequestEvent<SiteListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<Long> resp = siteService.getSitesCount(req);
		resp.throwErrorIfUnsuccessful();
		
		return Collections.singletonMap("count", resp.getPayload());
	}


	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SiteDetail getSite(@PathVariable Long id) {
		SiteQueryCriteria crit = new SiteQueryCriteria();
		crit.setId(id);
		
		RequestEvent<SiteQueryCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<SiteDetail> resp = siteService.getSite(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/byid/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SiteDetail getSiteById(@PathVariable Long id) {
		return this.getSite(id);
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/byname")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public SiteDetail getSiteByName(@RequestParam(value = "name", required = true) String name) {		
		SiteQueryCriteria crit = new SiteQueryCriteria();
		crit.setName(name);

		RequestEvent<SiteQueryCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<SiteDetail> resp = siteService.getSite(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}


	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SiteDetail createSite(@RequestBody SiteDetail siteDetail) {
		RequestEvent<SiteDetail> req = new RequestEvent<>(siteDetail);
		ResponseEvent<SiteDetail> resp = siteService.createSite(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public SiteDetail updateSite(@PathVariable Long id, @RequestBody SiteDetail siteDetail) {
		siteDetail.setId(id);
		
		RequestEvent<SiteDetail> req = new RequestEvent<>(siteDetail);
		ResponseEvent<SiteDetail> resp = siteService.updateSite(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PATCH, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public SiteDetail patchSite(@PathVariable Long id, @RequestBody SiteDetail siteDetail) {
		siteDetail.setId(id);
		
		RequestEvent<SiteDetail> req = new RequestEvent<>(siteDetail);
		ResponseEvent<SiteDetail> resp = siteService.patchSite(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}


	@RequestMapping(method = RequestMethod.PUT, value = "/bulk-update")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<SiteDetail> bulkUpdateSites(@RequestBody BulkEntityDetail<SiteDetail> detail) {
		ResponseEvent<List<SiteDetail>> resp = siteService.bulkUpdateSites(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}
		
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/dependent-entities")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<DependentEntityDetail> getDependentEntities(@PathVariable Long id) {
		RequestEvent<Long> req = new RequestEvent<>(id);
		ResponseEvent<List<DependentEntityDetail>> resp = siteService.getDependentEntities(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public SiteDetail deleteSite(
		@PathVariable
		Long id,

		@RequestParam(value="close", required=false, defaultValue="false")
		boolean close) {

		DeleteEntityOp deleteOp = new DeleteEntityOp(id, close);
		ResponseEvent<SiteDetail> resp = siteService.deleteSite(new RequestEvent<>(deleteOp));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<SiteDetail> deleteSites(
		@RequestParam(value="id")
		Long[] ids,

		@RequestParam(value="close", required=false, defaultValue="false")
		boolean close) {

		Status status = close ? Status.ACTIVITY_STATUS_CLOSED : Status.ACTIVITY_STATUS_DISABLED;
		SiteDetail siteDetail = new SiteDetail();
		siteDetail.setActivityStatus(status.getStatus());

		BulkEntityDetail<SiteDetail> detail = new BulkEntityDetail<>();
		detail.setIds(Arrays.asList(ids));
		detail.setDetail(siteDetail);

		ResponseEvent<List<SiteDetail>> resp = siteService.bulkUpdateSites(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm() {
		return formSvc.getExtensionInfo(-1L, Site.EXTN);
	}
}
