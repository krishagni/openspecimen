
package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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

import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.events.DpConsentTierDetail;
import com.krishagni.catissueplus.core.administrative.events.ReserveSpecimensDetail;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.administrative.services.DistributionOrderService;
import com.krishagni.catissueplus.core.administrative.services.DistributionProtocolService;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/distribution-protocols")
public class DistributionProtocolController {
	@Autowired
	private DistributionProtocolService dpSvc;

	@Autowired
	private DistributionOrderService orderSvc;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionProtocolDetail> getAllDistributionProtocols(
			@RequestParam(value = "query", required = false, defaultValue = "") 
			String searchStr,
			
			@RequestParam(value = "title", required = false)
			String title,
			
			@RequestParam(value = "piId", required = false)
			Long piId,

			@RequestParam(value = "irbId", required = false)
			String irbId,

			@RequestParam(value = "receivingInstitute", required = false)
			String receivingInstitute,

			@RequestParam(value = "receivingSite", required = false)
			String receivingSite,

			@RequestParam(value = "cp", required = false)
			String cpShortTitle,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0") 
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults,

			@RequestParam(value = "includeStats", required = false, defaultValue = "false")
			boolean includeStats,

			@RequestParam(value = "excludeExpiredDps", required = false, defaultValue = "false")
			boolean excludeExpiredDps,

			@RequestParam(value = "orderByStarred", required = false, defaultValue = "false")
			boolean orderByStarred,
			
			@RequestParam(value = "activityStatus", required = false)
			String activityStatus) {
		
		DpListCriteria criteria = new DpListCriteria()
			.startAt(startAt)
			.maxResults(maxResults)
			.query(searchStr)
			.title(title)
			.piId(piId)
			.irbIdLike(irbId)
			.receivingInstitute(receivingInstitute)
			.receivingSite(receivingSite)
			.cpShortTitle(cpShortTitle)
			.includeStat(includeStats)
			.excludeExpiredDps(excludeExpiredDps)
			.orderByStarred(orderByStarred)
			.activityStatus(activityStatus);
		
		ResponseEvent<List<DistributionProtocolDetail>> resp = dpSvc.getDistributionProtocols(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getAllDistributionProtocolsCount(
			@RequestParam(value = "query", required = false, defaultValue = "") 
			String searchStr,
			
			@RequestParam(value = "title", required = false)
			String title,
			
			@RequestParam(value = "piId", required = false)
			Long piId,

			@RequestParam(value = "irbId", required = false)
			String irbId,

			@RequestParam(value = "receivingInstitute", required = false)
			String receivingInstitute,

			@RequestParam(value = "receivingSite", required = false)
			String receivingSite,
			
			@RequestParam(value = "activityStatus", required = false)
			String activityStatus) {
		
		DpListCriteria criteria = new DpListCriteria()
			.query(searchStr)
			.title(title)
			.piId(piId)
			.irbIdLike(irbId)
			.receivingInstitute(receivingInstitute)
			.receivingSite(receivingSite)
			.activityStatus(activityStatus);
		
		ResponseEvent<Long> resp = dpSvc.getDistributionProtocolsCount(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionProtocolDetail getDistributionProtocol(@PathVariable("id") Long id) {
		ResponseEvent<DistributionProtocolDetail> resp = dpSvc.getDistributionProtocol(getRequest(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DistributionProtocolDetail createDistributionProtocol(
			@RequestBody DistributionProtocolDetail detail) {
		ResponseEvent<DistributionProtocolDetail> resp = dpSvc.createDistributionProtocol(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public DistributionProtocolDetail updateDistributionProtocol(@PathVariable("id") Long id,
			@RequestBody DistributionProtocolDetail detail) {
		detail.setId(id);
		ResponseEvent<DistributionProtocolDetail> resp = dpSvc.updateDistributionProtocol(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/dependent-entities")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<DependentEntityDetail> getDependentEntities(@PathVariable("id") Long id) {
		RequestEvent<Long> req = new RequestEvent<Long>(id);
		ResponseEvent<List<DependentEntityDetail>> resp = dpSvc.getDependentEntities(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public DistributionProtocolDetail deleteDistributionProtocol(@PathVariable("id") Long id) {
		return deleteDistributionProtocols(new Long[]{id}).get(0);
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<DistributionProtocolDetail> deleteDistributionProtocols(@RequestParam(value = "id") Long[] ids) {
		BulkDeleteEntityOp op = new BulkDeleteEntityOp();
		op.setIds(new HashSet<>(Arrays.asList(ids)));

		ResponseEvent<List<DistributionProtocolDetail>> resp = dpSvc.deleteDistributionProtocols(getRequest(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value ="/{id}/activity-status")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public DistributionProtocolDetail updateActivityStatus(
			@PathVariable("id")
			Long id,
			
			@RequestBody
			DistributionProtocolDetail detail) {
		
		detail.setId(id);
		RequestEvent<DistributionProtocolDetail> req = new RequestEvent<DistributionProtocolDetail>(detail);
		ResponseEvent<DistributionProtocolDetail> resp = dpSvc.updateActivityStatus(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/reserved-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> getReservedSpecimens(
			@PathVariable("id")
			Long dpId,

			@RequestParam(value = "specimenType", required = false)
			String specimenType,

			@RequestParam(value = "anatomicSite", required = false)
			String anatomicSite,

			@RequestParam(value = "cpShortTitle", required = false)
			String cpShortTitle,

			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,

			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {

		SpecimenListCriteria crit = new SpecimenListCriteria()
			.reservedForDp(dpId)
			.type(specimenType)
			.anatomicSite(anatomicSite)
			.cpShortTitle(cpShortTitle)
			.startAt(startAt)
			.maxResults(maxResults);

		ResponseEvent<List<SpecimenInfo>> resp = orderSvc.getReservedSpecimens(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/reserved-specimens-count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> getReservedSpecimensCount(
			@PathVariable("id")
			Long dpId,

			@RequestParam(value = "specimenType", required = false)
			String specimenType,

			@RequestParam(value = "anatomicSite", required = false)
			String anatomicSite,

			@RequestParam(value = "cpShortTitle", required = false)
			String cpShortTitle) {

		SpecimenListCriteria crit = new SpecimenListCriteria()
			.reservedForDp(dpId)
			.type(specimenType)
			.anatomicSite(anatomicSite)
			.cpShortTitle(cpShortTitle);

		ResponseEvent<Integer> resp = orderSvc.getReservedSpecimensCount(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/reserved-specimens")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> reserveSpecimens(
			@PathVariable("id")
			Long dpId,

			@RequestBody
			ReserveSpecimensDetail detail) {

		detail.setDpId(dpId);
		ResponseEvent<Integer> resp = orderSvc.reserveSpecimens(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("updated", resp.getPayload());
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/orders")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DistributionOrderStat> getOrderStats(
			@RequestParam(value = "dpId", required = false)
			Long dpId,
			
			@RequestParam(value = "groupBy", required = false, defaultValue = "")
			List<String> groupByAttrs) {
		
		DistributionOrderStatListCriteria crit = new DistributionOrderStatListCriteria()
				.dpId(dpId)
				.groupByAttrs(groupByAttrs);
		
		ResponseEvent<List<DistributionOrderStat>> resp = dpSvc.getOrderStats(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/orders-report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void exportOrderStats(
			@RequestParam(value = "dpId", required = false)
			Long dpId,
			
			@RequestParam(value = "groupBy", required = false, defaultValue = "")
			List<String> groupByAttrs,
			
			HttpServletResponse response) {
		
		DistributionOrderStatListCriteria crit = new DistributionOrderStatListCriteria()
				.dpId(dpId)
				.groupByAttrs(groupByAttrs);
		
		ResponseEvent<File> resp = dpSvc.exportOrderStats(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		Utility.sendToClient(response, "dp-order-stat.csv", resp.getPayload(), true);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm() {
		ResponseEvent<Map<String, Object>> resp = dpSvc.getExtensionForm();
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value ="/{id}/order-extension-form")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> getOrderExtnForm(@PathVariable("id") Long dpId) {
		ResponseEvent<Map<String, Object>> resp = dpSvc.getOrderExtensionForm(dpId);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{dpId}/consent-tiers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DpConsentTierDetail> getDpConsentTier(@PathVariable("dpId") Long dpId) {
		ResponseEvent<List<DpConsentTierDetail>> resp = dpSvc.getConsentTiers(getRequest(new EntityQueryCriteria(dpId)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{dpId}/consent-tiers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpConsentTierDetail createDpConsentTier(
		@PathVariable("dpId")
		Long dpId,

		@RequestBody
		DpConsentTierDetail dpConsent) {

		dpConsent.setDpId(dpId);
		ResponseEvent<DpConsentTierDetail> resp = dpSvc.createConsentTier(getRequest(dpConsent));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{dpId}/consent-tiers/{tierId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public DpConsentTierDetail updateDpConsentTier(
		@PathVariable("dpId")
		Long dpId,

		@PathVariable("tierId")
		Long tierId,

		@RequestBody
		DpConsentTierDetail dpConsent) {

		dpConsent.setDpId(dpId);
		dpConsent.setId(tierId);

		ResponseEvent<DpConsentTierDetail> resp  = dpSvc.updateConsentTier(getRequest(dpConsent));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{dpId}/consent-tiers/{tierId}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public DpConsentTierDetail deleteDpConsentTier(
		@PathVariable("dpId")
		Long dpId,

		@PathVariable("tierId")
		Long tierId) {

		DpConsentTierDetail dpConsent = new DpConsentTierDetail();
		dpConsent.setDpId(dpId);
		dpConsent.setId(tierId);

		ResponseEvent<DpConsentTierDetail> resp  = dpSvc.deleteConsentTier(getRequest(dpConsent));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> addLabel(@PathVariable("id") Long dpId) {
		return Collections.singletonMap("status", dpSvc.toggleStarredDp(dpId, true));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> removeLabel(@PathVariable("id") Long dpId) {
		return Collections.singletonMap("status", dpSvc.toggleStarredDp(dpId, false));
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
}
