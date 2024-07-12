
package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

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
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.BulkRegistrationsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CprSummary;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.events.RegistrationQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp;
import com.krishagni.catissueplus.core.de.events.ListEntityFormsOp.EntityType;
import com.krishagni.catissueplus.core.de.services.FormService;

@Controller
@RequestMapping("/collection-protocol-registrations")
public class CollectionProtocolRegistrationsController {
	@Autowired
	private CollectionProtocolRegistrationService cprSvc;

	@Autowired
	private CollectionProtocolService cpSvc;

	@Autowired
	private FormService formSvc;
	
	@RequestMapping(method = RequestMethod.POST, value = "/list")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolRegistrationDetail> getRegistrations(@RequestBody CprListCriteria crit) {
		crit.orderBy("registrationDate").asc(false);
		return ResponseEvent.unwrap(cpSvc.getRegisteredParticipants(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getRegistrationsCount(@RequestBody CprListCriteria crit) {
		ResponseEvent<Long> resp = cpSvc.getRegisteredParticipantsCount(getRequest(crit.includePhi(true)));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.POST, value="/matches")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<MatchedRegistrationsList> getMatches(@RequestBody List<CollectionProtocolRegistrationDetail> regs) {
		return ResponseEvent.unwrap(cprSvc.getMatches(RequestEvent.wrap(regs)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{cprId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolRegistrationDetail getRegistration(@PathVariable("cprId") Long cprId) {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		
		ResponseEvent<CollectionProtocolRegistrationDetail> resp = cprSvc.getRegistration(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value= "/{cprId}/latest-visit")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public VisitDetail getLatestVisit(@PathVariable("cprId") Long cprId) {
		ResponseEvent<VisitDetail> resp = cprSvc.getLatestVisit(getRegQueryReq(cprId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolRegistrationDetail register(@RequestBody CollectionProtocolRegistrationDetail cprDetail) {				
		ResponseEvent<CollectionProtocolRegistrationDetail> resp = cprSvc.createRegistration(getRequest(cprDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/bulk")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolRegistrationDetail> bulkRegister(@RequestBody BulkRegistrationsDetail detail) {
		ResponseEvent<List<CollectionProtocolRegistrationDetail>> resp = cprSvc.bulkRegistration(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolRegistrationDetail updateRegistration(
			@PathVariable("id")
			Long cprId,
			
			@RequestBody 
			CollectionProtocolRegistrationDetail cprDetail) {

		cprDetail.setId(cprId);
		ResponseEvent<CollectionProtocolRegistrationDetail> resp = cprSvc.updateRegistration(getRequest(cprDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/bulk-update")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolRegistrationDetail> bulkUpdateRegistrations(@RequestBody BulkEntityDetail<CollectionProtocolRegistrationDetail> detail) {
		return ResponseEvent.unwrap(cprSvc.bulkUpdateRegistrations(RequestEvent.wrap(detail)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/anonymize")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolRegistrationDetail anonymize(@PathVariable("id") Long cprId) {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);

		ResponseEvent<CollectionProtocolRegistrationDetail> resp = cprSvc.anonymize(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DependentEntityDetail> getDependentEntities(@PathVariable("id") Long cprId) {
		ResponseEvent<List<DependentEntityDetail>> resp = cprSvc.getDependentEntities(getRegQueryReq(cprId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolRegistrationDetail deleteRegistration(
			@PathVariable("id")
			Long cprId,

			@RequestParam(value = "forceDelete", required = false, defaultValue = "false")
			boolean forceDelete,

			@RequestParam(value = "reason", required = false, defaultValue = "")
			String reason) {

		CpEntityDeleteCriteria crit = new CpEntityDeleteCriteria();
		crit.setId(cprId);
		crit.setForceDelete(forceDelete);
		crit.setReason(reason);

		ResponseEvent<CollectionProtocolRegistrationDetail> resp = cprSvc.deleteRegistration(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public BulkDeleteEntityResp<CprSummary> deleteRegistrations(
		@RequestParam(value = "id")
		Long[] ids,

		@RequestParam(value = "forceDelete", required = false, defaultValue = "false")
		boolean forceDelete,

		@RequestParam(value = "reason", required = false, defaultValue = "")
		String reason) {

		BulkDeleteEntityOp crit = new BulkDeleteEntityOp();
		crit.setIds(new HashSet<>(Arrays.asList(ids)));
		crit.setForceDelete(forceDelete);
		crit.setReason(reason);
		return ResponseEvent.unwrap(cprSvc.deleteRegistrations(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/consent-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadConsentForm(@PathVariable("id") Long cprId, HttpServletResponse httpResp) throws IOException {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		
		ResponseEvent<File> resp = cprSvc.getConsentForm(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		
		File file = resp.getPayload();
		String fileName = file.getName().split("_", 2)[1];
		
		Utility.sendToClient(httpResp, fileName, file);
	}

	@RequestMapping(method = RequestMethod.POST, value="/{id}/consent-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String uploadConsentForm(@PathVariable("id") Long cprId, @PathVariable("file") MultipartFile file) 
	throws IOException {
		FileDetail detail = new FileDetail();
		detail.setId(cprId);
		detail.setFilename(file.getOriginalFilename());
		detail.setFileIn(file.getInputStream());
		
		ResponseEvent<String> resp = cprSvc.uploadConsentForm(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}/consent-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public boolean deleteConsentForm(@PathVariable("id") Long cprId) {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		
		ResponseEvent<Boolean> resp = cprSvc.deleteConsentForm(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method=RequestMethod.GET, value="/{id}/consents")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ConsentDetail getConsents(@PathVariable("id") Long cprId) {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		
		ResponseEvent<ConsentDetail> resp = cprSvc.getConsents(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method= RequestMethod.PUT, value="/{id}/consents")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ConsentDetail saveConsents(@PathVariable("id") Long cprId, @RequestBody ConsentDetail detail) {
		detail.setCprId(cprId);
		ResponseEvent<ConsentDetail> resp = cprSvc.saveConsents(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormCtxtSummary> getForms(@PathVariable("id") Long cprId) {
		ListEntityFormsOp opDetail = new ListEntityFormsOp();
		opDetail.setEntityId(cprId);
		opDetail.setEntityType(EntityType.COLLECTION_PROTOCOL_REGISTRATION);

		ResponseEvent<List<FormCtxtSummary>> resp = formSvc.getEntityForms(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms/{formCtxtId}/records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public EntityFormRecords getFormRecords(
		@PathVariable("id")
		Long cprId,

		@PathVariable("formCtxtId")
		Long formCtxtId) {

		GetEntityFormRecordsOp opDetail = new GetEntityFormRecordsOp();
		opDetail.setEntityId(cprId);
		opDetail.setFormCtxtId(formCtxtId);


		ResponseEvent<EntityFormRecords> resp = formSvc.getEntityFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/extension-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<FormRecordsList> getExtensionRecords(@PathVariable("id") Long cprId) {
		GetFormRecordsListOp opDetail = new GetFormRecordsListOp();
		opDetail.setObjectId(cprId);
		opDetail.setEntityType("Participant");
		
		ResponseEvent<List<FormRecordsList>> resp = formSvc.getFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();				
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm(
		@RequestParam(value = "cpId", required = false, defaultValue = "-1")
		Long cpId) {

		return formSvc.getExtensionInfo(cpId, Participant.EXTN);
	}

	private RequestEvent<RegistrationQueryCriteria> getRegQueryReq(Long cprId) {
		RegistrationQueryCriteria crit = new RegistrationQueryCriteria();
		crit.setCprId(cprId);
		return new RequestEvent<>(crit);
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
}
