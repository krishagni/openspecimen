
package com.krishagni.catissueplus.rest.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
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

import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenStatusDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimensQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp.EntityType;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/specimens")
public class SpecimensController {

	@Autowired
	private SpecimenService specimenSvc;
	
	@Autowired
	private CollectionProtocolRegistrationService cprSvc;
	
	@Autowired
	private FormService formSvc;
	
	@RequestMapping(method = RequestMethod.HEAD)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public Boolean doesSpecimenExists(
		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "label")
		String label) {

		SpecimenQueryCriteria crit = new SpecimenQueryCriteria(cpShortTitle, label);
		ResponseEvent<Boolean> resp = specimenSvc.doesSpecimenExists(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		if (resp.getPayload()) {
			return true;
		}

		throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, label);
	}
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<?> getSpecimens(
			@RequestParam(value = "cpId", required = false)
			Long cpId,

			@RequestParam(value = "cprId", required = false) 
			Long cprId,
			
			@RequestParam(value = "eventId", required = false) 
			Long eventId,
			
			@RequestParam(value = "visitId", required = false) 
			Long visitId,

			@RequestParam(value = "storageLocationSite", required = false)
			String storageLocationSite,
			
			@RequestParam(value = "id", required = false)
			List<Long> ids,

			@RequestParam(value = "label", required = false)
			List<String> labels,

			@RequestParam(value = "barcode", required = false)
			List<String> barcodes,

			@RequestParam(value = "exactMatch", required= false, defaultValue = "false")
			boolean exactMatch,

			@RequestParam(value = "includeExtensions", required = false, defaultValue = "false")
			boolean includeExtensions,

			@RequestParam(value = "minimalInfo", required = false, defaultValue = "false")
			boolean minimalInfo) {
				
		if (cprId != null) { // TODO: Move this to CPR controller
			VisitSpecimensQueryCriteria crit = new VisitSpecimensQueryCriteria();
			crit.setCprId(cprId);
			crit.setEventId(eventId);
			crit.setVisitId(visitId);

			ResponseEvent<List<SpecimenDetail>> resp = cprSvc.getSpecimens(getRequest(crit));
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} else if (CollectionUtils.isNotEmpty(ids)) {
			ResponseEvent<List<? extends SpecimenInfo>> resp = specimenSvc.getSpecimensById(ids, includeExtensions, minimalInfo);
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} else if (CollectionUtils.isNotEmpty(labels) || CollectionUtils.isNotEmpty(barcodes)) {
			SpecimenListCriteria crit = new SpecimenListCriteria()
				.labels(labels).barcodes(barcodes)
				.exactMatch(exactMatch)
				.storageLocationSite(storageLocationSite)
				.includeExtensions(includeExtensions);

			ResponseEvent<List<? extends SpecimenInfo>> resp = specimenSvc.getSpecimens(getRequest(crit));
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} else if (cpId != null) {
			ResponseEvent<List<SpecimenInfo>> resp = specimenSvc.getPrimarySpecimensByCp(getRequest(cpId));
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} else {
			return Collections.emptyList();
		}
	}

	@RequestMapping(method = RequestMethod.POST, value = "/search")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<? extends SpecimenInfo> getSpecimens(@RequestBody SpecimenListCriteria crit) {
		int size = 0;
		if (crit.labels() != null) {
			size += crit.labels().size();
		}

		if (crit.barcodes() != null) {
			size += crit.barcodes().size();
		}

		if (size > 10000) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABELS_SRCH_LIMIT_MAXED, 1000);
		}

		if (crit.maxResults() > 10000) {
			crit.maxResults(10000);
		}

		return ResponseEvent.unwrap(specimenSvc.getSpecimens(RequestEvent.wrap(crit.limitItems(true))));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenDetail getSpecimen(@PathVariable("id") Long id) {
		SpecimenQueryCriteria crit = new SpecimenQueryCriteria(id);
		
		ResponseEvent<SpecimenDetail> resp = specimenSvc.getSpecimen(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public SpecimenDetail createSpecimen(@RequestBody SpecimenDetail detail) {
		ResponseEvent<SpecimenDetail> resp = specimenSvc.createSpecimen(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> updateSpecimens(@RequestBody List<SpecimenDetail> details) {
		ResponseEvent<List<SpecimenInfo>> resp = specimenSvc.updateSpecimens(getRequest(details));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public SpecimenDetail updateSpecimen(
			@PathVariable("id") 
			Long specimenId, 
			
			@RequestBody 
			SpecimenDetail detail) {
		
		detail.setId(specimenId);
		
		ResponseEvent<SpecimenDetail> resp = specimenSvc.updateSpecimen(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/bulk-update")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> bulkUpdateSpecimens(@RequestBody BulkEntityDetail<SpecimenDetail> detail) {
		ResponseEvent<List<SpecimenInfo>> resp = specimenSvc.bulkUpdateSpecimens(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/status")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenDetail updateSpecimenStatus(
			@PathVariable("id")
			Long specimenId,

			@RequestBody
			SpecimenStatusDetail detail) {

		detail.setId(specimenId);

		ResponseEvent<List<SpecimenDetail>>  resp = specimenSvc.updateSpecimensStatus(getRequest(Collections.singletonList(detail)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().get(0);
	}

	@RequestMapping(method = RequestMethod.PUT, value="/status")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenDetail> updateSpecimensStatus(
			@RequestBody
			List<SpecimenStatusDetail> details) {
		ResponseEvent<List<SpecimenDetail>> resp = specimenSvc.updateSpecimensStatus(getRequest(details));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DependentEntityDetail> getDependentEntities(@PathVariable("id") Long specimenId) {
		SpecimenQueryCriteria crit = new SpecimenQueryCriteria(specimenId);
		ResponseEvent<List<DependentEntityDetail>> resp = specimenSvc.getDependentEntities(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenInfo deleteSpecimen(
			@PathVariable("id")
			Long specimenId,

			@RequestParam(value = "forceDelete", required = false, defaultValue = "false")
			boolean forceDelete,

			@RequestParam(value = "reason", required = false, defaultValue = "")
			String reason) {

		CpEntityDeleteCriteria crit = new CpEntityDeleteCriteria();
		crit.setId(specimenId);
		crit.setForceDelete(forceDelete);
		crit.setReason(reason);

		ResponseEvent<List<SpecimenInfo>> resp = specimenSvc.deleteSpecimens(getRequest(Collections.singletonList(crit)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().get(0);
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SpecimenInfo> deleteSpecimens(
			@RequestParam(value = "id")
			Long[] specimenIds,

			@RequestParam(value = "reason", required = false, defaultValue = "")
			String reason) {

		List<CpEntityDeleteCriteria> criteria = new ArrayList<>();
		for (Long specimenId : specimenIds) {
			CpEntityDeleteCriteria criterion = new CpEntityDeleteCriteria();
			criterion.setId(specimenId);
			criterion.setForceDelete(true);
			criterion.setReason(reason);
			criteria.add(criterion);
		}

		ResponseEvent<List<SpecimenInfo>> resp = specimenSvc.deleteSpecimens(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/undelete")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenDetail undelete(@RequestBody Map<String, Object> payload) {
		if (payload == null || payload.get("id") == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.ID_REQUIRED);
		}

		SpecimenQueryCriteria crit = null;
		try {
			Number id = (Number) payload.get("id");
			Boolean includeChildren = (Boolean) payload.get("includeChildren");
			String reason = (String) payload.get("reason");
			crit = new SpecimenQueryCriteria(id.longValue());
			crit.setIncludeChildren(includeChildren != null && includeChildren);
			crit.setParams(Collections.singletonMap("comments", reason));
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, e.getMessage());
		}

		return ResponseEvent.unwrap(specimenSvc.undeleteSpecimen(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormCtxtSummary> getForms(
			@PathVariable("id") 
			Long specimenId,
			
			@RequestParam(value = "entityType", required = false, defaultValue="Specimen")
			String entityType
			) {
		ListEntityFormsOp opDetail = new ListEntityFormsOp();
		opDetail.setEntityId(specimenId);
		
		if (entityType.equals("Specimen")) {
			opDetail.setEntityType(EntityType.SPECIMEN);
		} else {
			opDetail.setEntityType(EntityType.SPECIMEN_EVENT);
		}
		
		ResponseEvent<List<FormCtxtSummary>> resp = formSvc.getEntityForms(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms/{formCtxtId}/records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public EntityFormRecords getFormRecords(@PathVariable("id") Long specimenId,
			@PathVariable("formCtxtId") Long formCtxtId) {

		GetEntityFormRecordsOp opDetail = new GetEntityFormRecordsOp();
		opDetail.setEntityId(specimenId);
		opDetail.setFormCtxtId(formCtxtId);
		
		ResponseEvent<EntityFormRecords> resp = formSvc.getEntityFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/events")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<FormRecordsList> getEvents(@PathVariable("id") Long specimenId) {
		return getRecords(specimenId, "SpecimenEvent");
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/extension-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormRecordsList> getExtensionRecords(@PathVariable("id") Long specimenId) {
		return getRecords(specimenId, "Specimen");
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/collect")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<SpecimenDetail> collectSpecimens(@RequestBody List<SpecimenDetail> specimens) {		
		ResponseEvent<List<SpecimenDetail>> resp = specimenSvc.collectSpecimens(getRequest(specimens));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/pooled")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SpecimenDetail createPooledSpecimen(@RequestBody SpecimenDetail specimen) {
		return ResponseEvent.unwrap(specimenSvc.createPooledSpecimen(RequestEvent.wrap(specimen)));
	}
	
	/** API present for UI purpose **/
	@RequestMapping(method = RequestMethod.GET, value="/{id}/cpr-visit-ids")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public Map<String, Object> getCprAndVisitIds(@PathVariable("id") Long specimenId) {
		ResponseEvent<Map<String, Object>> resp = specimenSvc.getCprAndVisitIds(new RequestEvent<Long>(specimenId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/primary-specimen-id")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getPrimarySpecimenId(@PathVariable("id") Long specimenId) {
		return Collections.singletonMap("id", specimenSvc.getPrimarySpecimen(new SpecimenQueryCriteria(specimenId)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm(
			@RequestParam(value = "cpId", required = false, defaultValue = "-1")
			Long cpId) {

		return formSvc.getExtensionInfo(cpId, Specimen.EXTN);
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}

	private List<FormRecordsList> getRecords(Long specimenId, String entityType) {
		GetFormRecordsListOp opDetail = new GetFormRecordsListOp();
		opDetail.setObjectId(specimenId);
		opDetail.setEntityType(entityType);
		
		ResponseEvent<List<FormRecordsList>> resp = formSvc.getFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();		
	}
}