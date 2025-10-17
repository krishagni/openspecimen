package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
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

import com.krishagni.catissueplus.core.biospecimen.events.PickListSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PickListSpecimensOp;
import com.krishagni.catissueplus.core.biospecimen.events.ShareSpecimenListOp;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimensPickListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateListSpecimensOp;
import com.krishagni.catissueplus.core.biospecimen.repository.PickListSpecimensCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.PickListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListsCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenListService;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;

@Controller
@RequestMapping("/specimen-lists")
public class SpecimenListsController {
	
	@Autowired
	private HttpServletRequest httpServletRequest;

	@Autowired
	private SpecimenListService specimenListSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenListSummary> getSpecimenLists(
		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "folderId", required = false)
		Long folderId,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "orderByStarred", required = false, defaultValue = "false")
		boolean orderByStarred) {

		SpecimenListsCriteria crit = new SpecimenListsCriteria()
			.query(name)
			.folderId(folderId)
			.includeStat(includeStats)
			.orderByStarred(orderByStarred)
			.startAt(startAt < 0 ? 0 : startAt)
			.maxResults(maxResults <=0 ? 100 : maxResults);
		return response(specimenListSvc.getSpecimenLists(request(crit)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getSpecimenListsCount(
		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "folderId", required = false)
		Long folderId) {
		SpecimenListsCriteria crit = new SpecimenListsCriteria().query(name).folderId(folderId);
		return Collections.singletonMap("count", response(specimenListSvc.getSpecimenListsCount(request(crit))));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail getSpecimenList(@PathVariable("listId") Long listId) {
		return response(specimenListSvc.getSpecimenList(request(new EntityQueryCriteria(listId))));
	}
		
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail createSpecimenList(@RequestBody SpecimenListDetail details) {
		return response(specimenListSvc.createSpecimenList(request(details)));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail updateSpecimenList(@PathVariable Long listId, @RequestBody SpecimenListDetail details) {
		details.setId(listId);
		return response(specimenListSvc.updateSpecimenList(request(details)));
	}

	@RequestMapping(method = RequestMethod.PATCH, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail patchSpecimenList(@PathVariable Long listId, @RequestBody SpecimenListDetail details) {
		details.setId(listId);
		return response(specimenListSvc.patchSpecimenList(request(details)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenListDetail deleteSpecimenList(@PathVariable Long listId) {
		return response(specimenListSvc.deleteSpecimenList(request(listId)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{listId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getListSpecimens(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "label", required = false)
		String label,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "ppid", required = false)
		String ppid,

		@RequestParam(value = "lineage", required = false)
		String lineage,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "anatomicSite", required = false)
		String anatomicSite,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "available", required = false, defaultValue = "false")
		boolean available,

		@RequestParam(value = "noQty", required = false, defaultValue = "false")
		boolean noQty,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "50")
		int maxResults,

		@RequestParam(value = "includeListCount", required = false, defaultValue = "false")
		boolean includeListCount) {

		SpecimenListCriteria criteria = new SpecimenListCriteria()
			.specimenListId(listId)
			.labels(StringUtils.isNotBlank(label) ? Collections.singletonList(label) : null)
			.cpId(cpId)
			.ppid(ppid)
			.lineages(StringUtils.isNotBlank(lineage) ? new String[] {lineage} : null)
			.type(type)
			.anatomicSite(anatomicSite)
			.container(container)
			.exactMatch(false)
			.startAt(startAt)
			.maxResults(maxResults)
			.available(available)
			.noQty(noQty)
			.includeStat(includeListCount)
			.limitItems(true);
		return response(specimenListSvc.getListSpecimens(request(criteria)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{listId}/specimens-count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> getListSpecimensCount(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "label", required = false)
		String label,

		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "ppid", required = false)
		String ppid,

		@RequestParam(value = "lineage", required = false)
		String lineage,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "anatomicSite", required = false)
		String anatomicSite,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "available", required = false, defaultValue = "false")
		boolean available,

		@RequestParam(value = "noQty", required = false, defaultValue = "false")
		boolean noQty) {

		SpecimenListCriteria criteria = new SpecimenListCriteria()
			.specimenListId(listId)
			.labels(StringUtils.isNotBlank(label) ? Collections.singletonList(label) : null)
			.cpId(cpId)
			.ppid(ppid)
			.lineages(StringUtils.isNotBlank(lineage) ? new String[] {lineage} : null)
			.type(type)
			.anatomicSite(anatomicSite)
			.container(container)
			.exactMatch(false)
			.available(available)
			.noQty(noQty);
		return Collections.singletonMap("count", response(specimenListSvc.getListSpecimensCount(request(criteria))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{listId}/specimens-sorted-by-rel")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getListSpecimens(@PathVariable("listId") Long listId) {
		RequestEvent<EntityQueryCriteria> req = new RequestEvent<>(new EntityQueryCriteria(listId));
		return response(specimenListSvc.getListSpecimensSortedByRel(req));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{listId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> updateListSpecimens(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "operation", required = false, defaultValue = "ADD")
		String operation,

		@RequestBody
		List<Long> specimenIds) {
		
		UpdateListSpecimensOp opDetail = new UpdateListSpecimensOp();
		opDetail.setListId(listId);
		opDetail.setSpecimens(specimenIds);
		opDetail.setOp(UpdateListSpecimensOp.Operation.valueOf(operation));

		return Collections.singletonMap("count", response(specimenListSvc.updateListSpecimens(request(opDetail))));
	}

	@RequestMapping(method = RequestMethod.POST, value="/{listId}/add-child-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> addChildSpecimens(@PathVariable("listId") Long listId) {
		Boolean status = response(specimenListSvc.addChildSpecimens(request(listId)));
		return Collections.singletonMap("status", status.toString());
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{listId}/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserSummary> shareSpecimenList(
		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "operation", required = false, defaultValue = "UPDATE")
		String operation,

		@RequestBody
		List<Long> userIds) {

		ShareSpecimenListOp opDetail = new ShareSpecimenListOp();
		opDetail.setListId(listId);
		opDetail.setOp(com.krishagni.catissueplus.core.biospecimen.events.ShareSpecimenListOp.Operation.valueOf(operation));
		opDetail.setUserIds(userIds);
		
		return response(specimenListSvc.shareSpecimenList(request(opDetail)));
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryDataExportResult exportList(
		@PathVariable("id")
		Long listId,

		@RequestParam(value = "specimenId", required = false)
		List<Long> specimenIds) {

		SpecimenListCriteria crit = new SpecimenListCriteria().specimenListId(listId).ids(specimenIds);
		return response(specimenListSvc.exportSpecimenList(request(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> addLabel(@PathVariable("id") Long listId) {
		return Collections.singletonMap("status", specimenListSvc.toggleStarredSpecimenList(listId, true));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> removeLabel(@PathVariable("id") Long listId) {
		return Collections.singletonMap("status", specimenListSvc.toggleStarredSpecimenList(listId, false));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/pick-lists")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimensPickListDetail> getPickLists(
		@PathVariable("id")
		Long cartId,

		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		PickListsCriteria crit = new PickListsCriteria()
			.cartId(cartId)
			.query(name)
			.includeStat(includeStats)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(specimenListSvc.getPickLists(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/pick-lists/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimensPickListDetail getPickList(@PathVariable("id") Long cartId, @PathVariable("listId") Long listId) {
		EntityQueryCriteria crit = new EntityQueryCriteria(listId);
		crit.setParams(Collections.singletonMap("cartId", cartId));
		return ResponseEvent.unwrap(specimenListSvc.getPickList(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/pick-lists")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimensPickListDetail createPickList(@PathVariable("id") Long cartId, @RequestBody SpecimensPickListDetail input) {
		input.setCart(new SpecimenListSummary());
		input.getCart().setId(cartId);
		return ResponseEvent.unwrap(specimenListSvc.createPickList(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/pick-lists/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimensPickListDetail updatePickList(
		@PathVariable("id")
		Long cartId,

		@PathVariable("listId")
		Long listId,

		@RequestBody
		SpecimensPickListDetail input) {

		input.setId(listId);
		input.setCart(new SpecimenListSummary());
		input.getCart().setId(cartId);
		return ResponseEvent.unwrap(specimenListSvc.updatePickList(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/pick-lists/{listId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimensPickListDetail deletePickList(@PathVariable("id") Long cartId, @PathVariable("listId") Long listId) {
		EntityQueryCriteria crit = new EntityQueryCriteria(listId);
		crit.setParams(Collections.singletonMap("cartId", cartId));
		return ResponseEvent.unwrap(specimenListSvc.deletePickList(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/pick-lists/{listId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<PickListSpecimenDetail> getPickListSpecimens(
		@PathVariable("id")
		Long cartId,

		@PathVariable("listId")
		Long listId,

		@RequestParam(value = "picked", required = false, defaultValue = "false")
		Boolean picked,

		@RequestParam(value = "cp", required = false)
		String cp,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "container", required = false)
		String container,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		PickListSpecimensCriteria criteria = new PickListSpecimensCriteria()
			.cartId(cartId)
			.pickListId(listId)
			.picked(picked)
			.cp(cp)
			.type(type)
			.container(container)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(specimenListSvc.getPickListSpecimens(RequestEvent.wrap(criteria)));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/pick-lists/{pickListId}/specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> updatePickListSpecimens(
		@PathVariable("id")
		Long listId,

		@PathVariable("pickListId")
		Long pickListId,

		@RequestBody
		PickListSpecimensOp input) {

		input.setCartId(listId);
		input.setPickListId(pickListId);
		return response(specimenListSvc.updatePickListSpecimens(request(input)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
 }
