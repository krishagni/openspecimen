
package com.krishagni.catissueplus.rest.controller;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.administrative.events.SiteSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolPublishDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentTierOp.OP;
import com.krishagni.catissueplus.core.biospecimen.events.CopyCpOpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CpReportSettingsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MergeCpDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.CpPublishEventListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityDeleteResp;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListDetail;
import com.krishagni.catissueplus.core.query.ListGenerator;

import edu.common.dynamicextensions.nutility.IoUtil;

@Controller
@RequestMapping("/collection-protocols")
public class CollectionProtocolsController {

	@Autowired
	private CollectionProtocolService cpSvc;
	
	@Autowired
	private FormService formSvc;

	@Autowired
	private ListGenerator listGenerator;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@Autowired
	private ImportService importSvc;

	@Autowired
	private ExportService exportSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolSummary> getCollectionProtocols(
			@RequestParam(value = "query", required = false) 
			String searchStr,
			
			@RequestParam(value = "title", required = false)
			String title,

			@RequestParam(value = "irbId", required = false)
			String irbId,
			
			@RequestParam(value = "piId", required = false)
			Long piId,
			
			@RequestParam(value = "repositoryName", required = false)
			String repositoryName,

			@RequestParam(value = "instituteId", required = false)
			Long instituteId,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0") 
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults,
			
			@RequestParam(value = "detailedList", required = false, defaultValue = "false") 
			boolean detailedList,

			@RequestParam(value = "orderByStarred", required = false, defaultValue = "false")
			boolean orderByStarred,

			@RequestParam(value = "onlyParticipantConsentCps", required = false, defaultValue = "false")
			boolean onlyParticipantConsentCps) {
		
		CpListCriteria crit = new CpListCriteria()
			.query(searchStr)
			.title(title)
			.irbId(irbId)
			.piId(piId)
			.repositoryName(repositoryName)
			.instituteId(instituteId)
			.includePi(detailedList)
			.includeStat(detailedList)
			.orderByStarred(orderByStarred)
			.onlyParticipantConsentCps(onlyParticipantConsentCps)
			.startAt(startAt)
			.maxResults(maxResults);

		ResponseEvent<List<CollectionProtocolSummary>> resp = cpSvc.getProtocols(request(crit));
		resp.throwErrorIfUnsuccessful();		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getCollectionProtocolsCount(
			@RequestParam(value = "query", required = false)
			String searchStr,
			
			@RequestParam(value = "title", required = false)
			String title,

			@RequestParam(value = "irbId", required = false)
			String irbId,

			@RequestParam(value = "piId", required = false)
			Long piId,
			
			@RequestParam(value = "repositoryName", required = false)
			String repositoryName,

			@RequestParam(value = "instituteId", required = false)
			Long instituteId,

			@RequestParam(value = "onlyParticipantConsentCps", required = false, defaultValue = "false")
			boolean onlyParticipantConsentCps) {
		
		CpListCriteria crit = new CpListCriteria()
			.query(searchStr)
			.title(title)
			.irbId(irbId)
			.piId(piId)
			.repositoryName(repositoryName)
			.instituteId(instituteId)
			.onlyParticipantConsentCps(onlyParticipantConsentCps);
		
		ResponseEvent<Long> resp = cpSvc.getProtocolsCount(request(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public  CollectionProtocolDetail getCollectionProtocol(@PathVariable("id") Long cpId) {
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);
		
		ResponseEvent<CollectionProtocolDetail> resp = cpSvc.getCollectionProtocol(request(crit));
		resp.throwErrorIfUnsuccessful();		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/sites")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public  List<SiteSummary> getSites(@PathVariable("id") Long cpId) {
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);

		ResponseEvent<List<SiteSummary>> resp = cpSvc.getSites(request(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/definition")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getCpDefFile(
		@PathVariable("id")
		Long cpId,

		@RequestParam(value = "includeIds", required = false, defaultValue = "false")
		boolean includeIds,

		HttpServletResponse httpResp)
	throws JsonProcessingException {
		Date startTime = Calendar.getInstance().getTime();
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);
		crit.setFullObject(true);
		crit.param("includeIds", includeIds);

		String def = ResponseEvent.unwrap(cpSvc.getCollectionProtocolDefinition(RequestEvent.wrap(crit)));
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(def.getBytes());

			httpResp.setContentType("application/json");
			httpResp.setHeader("Content-Disposition", "attachment;filename=CpDef_" + cpId + ".json");
			IoUtil.copy(in, httpResp.getOutputStream());
			exportSvc.saveJob("cpJson", startTime, Collections.singletonMap("cpId", cpId.toString()));
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IoUtil.close(in);
		}				
	}

	@RequestMapping(method = RequestMethod.POST, value="/definition")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public CollectionProtocolDetail importCpDef(@PathVariable("file") MultipartFile file) 
	throws IOException {
		Date startTime = Calendar.getInstance().getTime();
		CollectionProtocolDetail cp = new ObjectMapper().readValue(file.getBytes(), CollectionProtocolDetail.class);
		ResponseEvent<CollectionProtocolDetail> resp = cpSvc.importCollectionProtocol(new RequestEvent<>(cp));
		resp.throwErrorIfUnsuccessful();

		importSvc.saveJob("cpJson", "CREATE", startTime, Collections.singletonMap("cpId", resp.getPayload().getId().toString()));
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/sop-document")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadSopDocument(@PathVariable("id") Long cpId, HttpServletResponse httpResp)
	throws IOException {
		ResponseEvent<File> resp = cpSvc.getSopDocument(request(cpId));
		resp.throwErrorIfUnsuccessful();

		File file = resp.getPayload();
		String fileName = file.getName().split("_", 2)[1];
		Utility.sendToClient(httpResp, fileName, file);
	}

	@RequestMapping(method = RequestMethod.POST, value="/sop-documents")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String uploadSopDocument(@PathVariable("file") MultipartFile file)
	throws IOException {
		InputStream in = null;
		try {
			in = file.getInputStream();

			FileDetail detail = new FileDetail();
			detail.setFilename(file.getOriginalFilename());
			detail.setFileIn(in);

			ResponseEvent<String> resp = cpSvc.uploadSopDocument(request(detail));
			resp.throwErrorIfUnsuccessful();
			return resp.getPayload();
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolDetail createCollectionProtocol(@RequestBody CollectionProtocolDetail cp) {
		ResponseEvent<CollectionProtocolDetail> resp = cpSvc.createCollectionProtocol(request(cp));
		resp.throwErrorIfUnsuccessful();		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolDetail updateCollectionProtocol(@RequestBody CollectionProtocolDetail cp) {
		ResponseEvent<CollectionProtocolDetail> resp = cpSvc.updateCollectionProtocol(request(cp));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{cpId}/copy")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public CollectionProtocolDetail copyCollectionProtocol(
			@PathVariable("cpId") 
			Long cpId,
			
			@RequestBody 
			CollectionProtocolDetail cpDetail) {
		
		CopyCpOpDetail opDetail = new CopyCpOpDetail();
		opDetail.setCpId(cpId);
		opDetail.setCp(cpDetail);

		ResponseEvent<CollectionProtocolDetail> resp = cpSvc.copyCollectionProtocol(request(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/consents-waived")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public CollectionProtocolDetail updateConsentsWaived(@PathVariable Long id, @RequestBody Map<String, String> props) {
		CollectionProtocolDetail cp = new  CollectionProtocolDetail();
		cp.setId(id);
		cp.setConsentsWaived(Boolean.parseBoolean(props.get("consentsWaived")));
		return response(cpSvc.updateConsentsWaived(request(cp)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/consents-source")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public CollectionProtocolDetail updateConsentsSource(@PathVariable Long id, @RequestBody CollectionProtocolSummary source) {
		CollectionProtocolDetail cp = new  CollectionProtocolDetail();
		cp.setId(id);
		cp.setConsentsSource(source);
		return response(cpSvc.updateConsentsSource(request(cp)));
	}


	@RequestMapping(method = RequestMethod.GET, value="/{id}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<DependentEntityDetail> getCpDependentEntities(@PathVariable Long id) {
		ResponseEvent<List<DependentEntityDetail>> resp = cpSvc.getCpDependentEntities(request(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public EntityDeleteResp<CollectionProtocolDetail> deleteCollectionProtocol(
			@PathVariable
			Long id,

			@RequestParam(value = "forceDelete", required = false, defaultValue = "false")
			boolean forceDelete,

			@RequestParam(value = "reason", required = false, defaultValue = "")
			String reason) {

		BulkDeleteEntityOp crit = new BulkDeleteEntityOp();
		crit.setIds(Collections.singleton(id));
		crit.setForceDelete(forceDelete);
		crit.setReason(reason);

		ResponseEvent<BulkDeleteEntityResp<CollectionProtocolDetail>> resp = cpSvc.deleteCollectionProtocols(request(crit));
		resp.throwErrorIfUnsuccessful();
		BulkDeleteEntityResp<CollectionProtocolDetail> payload = resp.getPayload();
		return new EntityDeleteResp<>(payload.getEntities().get(0), payload.isCompleted());
	}
	
	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public BulkDeleteEntityResp<CollectionProtocolDetail> deleteCollectionProtocols(
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

		ResponseEvent<BulkDeleteEntityResp<CollectionProtocolDetail>> resp = cpSvc.deleteCollectionProtocols(request(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/consent-tiers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ConsentTierDetail> getConsentTiers(@PathVariable("id") Long cpId) {
		return response(cpSvc.getConsentTiers(request(cpId)));
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/{id}/consent-tiers")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public ConsentTierDetail addConsentTier(@PathVariable("id") Long cpId, @RequestBody ConsentTierDetail consentTier) {
		return performConsentTierOp(OP.ADD, cpId, consentTier);
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/consent-tiers/{tierId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public ConsentTierDetail updateConsentTier(
			@PathVariable("id") 
			Long cpId,
			
			@PathVariable("tierId") 
			Long tierId,
			
			@RequestBody 
			ConsentTierDetail consentTier) {
		
		consentTier.setId(tierId);
		return performConsentTierOp(OP.UPDATE, cpId, consentTier);
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}/consent-tiers/{tierId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public ConsentTierDetail removeConsentTier(
			@PathVariable("id") 
			Long cpId,
			
			@PathVariable("tierId") 
			Long tierId) {
		
		ConsentTierDetail consentTier = new ConsentTierDetail();
		consentTier.setId(tierId);
		return performConsentTierOp(OP.REMOVE, cpId, consentTier);		
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/consent-tiers/{tierId}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<DependentEntityDetail> getConsentDependentEntities(
			@PathVariable("id") 
			Long cpId,
			
			@PathVariable("tierId") 
			Long tierId) {
		ConsentTierDetail consentTierDetail = new ConsentTierDetail();
		consentTierDetail.setCpId(cpId);
		consentTierDetail.setId(tierId);
		
		ResponseEvent<List<DependentEntityDetail>> resp = cpSvc.getConsentDependentEntities(request(consentTierDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/barcoding-enabled")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Boolean isSpecimenBarcodingEnabled() {
		ResponseEvent<Boolean> resp = cpSvc.isSpecimenBarcodingEnabled();
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/workflows")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public CpWorkflowCfgDetail getWorkflowCfg(@PathVariable("id") Long cpId) {
		ResponseEvent<CpWorkflowCfgDetail> resp = cpSvc.getWorkflows(new RequestEvent<>(cpId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/workflows-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getWorkflowCfg(@PathVariable("id") Long cpId, HttpServletResponse httpResp) {
		Date startTime = Calendar.getInstance().getTime();
		ResponseEvent<CpWorkflowCfgDetail> resp = cpSvc.getWorkflows(new RequestEvent<>(cpId));
		resp.throwErrorIfUnsuccessful();

		InputStream in = null;
		try {
			CpWorkflowCfgDetail workflowDetail = resp.getPayload();
			String filename = (workflowDetail.getShortTitle() +  "_workflows.json")
				.replaceAll("\\\\", "_")  // replace backslash with _
				.replaceAll("/", "_")     // replace forward slash with _
				.replaceAll("\\s+", "_"); // replace whitespace with _

			String workflowsJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(resp.getPayload().getWorkflows().values());
			in = new ByteArrayInputStream(workflowsJson.getBytes());
			Utility.sendToClient(httpResp, filename, "application/json", in);
			exportSvc.saveJob("cpWf", startTime, Collections.singletonMap("cpId", cpId.toString()));
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_SEND_ERROR, e.getMessage());
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/workflows")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public CpWorkflowCfgDetail saveWorkflowCfg(@PathVariable("id") Long cpId, @RequestBody List<WorkflowDetail> workflows) {
		return saveWorkflows(cpId, workflows, false);
	}

	@RequestMapping(method = RequestMethod.POST, value="/{id}/workflows-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpWorkflowCfgDetail saveWorkflowCfg(@PathVariable("id") Long cpId, @PathVariable("file") MultipartFile file) {
		List<WorkflowDetail> workflows;

		Date startTime = Calendar.getInstance().getTime();
		try {
			ObjectMapper mapper = new ObjectMapper();
			workflows = mapper.readValue(file.getInputStream(), new TypeReference<List<WorkflowDetail>>() {});
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST, e.getMessage());
		}

		CpWorkflowCfgDetail result = saveWorkflows(cpId, workflows, false);
		importSvc.saveJob("cpWf", "UPDATE", startTime, Collections.singletonMap("cpId", cpId.toString()));
		return result;
	}

	@RequestMapping(method = RequestMethod.PATCH, value="/{id}/workflows")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpWorkflowCfgDetail patchWorkflowCfg(@PathVariable("id") Long cpId, @RequestBody List<WorkflowDetail> workflows) {
		return saveWorkflows(cpId, workflows, true);
	}

	//
	// Report settings API
	//
	@RequestMapping(method = RequestMethod.GET, value="/{id}/report-settings")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpReportSettingsDetail getReportSettings(@PathVariable("id") Long cpId) {
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);

		RequestEvent<CpQueryCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<CpReportSettingsDetail> resp = cpSvc.getReportSettings(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/report-settings")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpReportSettingsDetail updateReportSettings(
			@PathVariable("id")
			Long cpId,

			@RequestBody
			CpReportSettingsDetail detail) {

		CollectionProtocolSummary cp = new CollectionProtocolSummary();
		cp.setId(cpId);
		detail.setCp(cp);

		RequestEvent<CpReportSettingsDetail> req = new RequestEvent<>(detail);
		ResponseEvent<CpReportSettingsDetail> resp = cpSvc.saveReportSettings(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE, value="/{id}/report-settings")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpReportSettingsDetail deleteReportSettings(@PathVariable("id") Long cpId) {
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);

		RequestEvent<CpQueryCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<CpReportSettingsDetail> resp = cpSvc.deleteReportSettings(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> generateReport(@PathVariable("id") Long cpId) {
		CpQueryCriteria crit = new CpQueryCriteria();
		crit.setId(cpId);

		ResponseEvent<Boolean> resp = cpSvc.generateReport(new RequestEvent<>(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("status", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/report")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadCpReport(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "fileId", required = true)
			String fileId,

			HttpServletResponse httpResp)
	throws IOException {
		ResponseEvent<File> resp = cpSvc.getReportFile(cpId, fileId);
		resp.throwErrorIfUnsuccessful();

		File file = resp.getPayload();

		String extn = ".csv";
		int extnStartIdx = file.getName().lastIndexOf('.');
		if (extnStartIdx != -1) {
			extn = file.getName().substring(extnStartIdx);
		}

		String filename = "CpReport_" + Utility.format(Calendar.getInstance().getTime(), "yyyyMMdd_HHmmss") + extn;
		Utility.sendToClient(httpResp, filename, file);
	}

	//
	// Publish APIs
	//

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/published-events")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolPublishDetail> getPublishedEvents(
		@PathVariable("id")
		Long cpId,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults
	) {
		CpPublishEventListCriteria crit = new CpPublishEventListCriteria();
		crit.cpId(cpId).startAt(startAt).maxResults(maxResults);
		return ResponseEvent.unwrap(cpSvc.getPublishEvents(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/published-events")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolPublishDetail publish(
		@PathVariable("id")
		Long cpId,

		@RequestBody
		CollectionProtocolPublishDetail input) {

		input.setCpId(cpId);
		return ResponseEvent.unwrap(cpSvc.publish(RequestEvent.wrap(input)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/published-versions/{versionId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getPublishedVersion(
		@PathVariable("id")
		Long cpId,

		@PathVariable(value = "versionId")
		Long versionId,

		HttpServletResponse httpResp)
	throws JsonProcessingException {
		EntityQueryCriteria crit = new EntityQueryCriteria(versionId);
		crit.setParams(Collections.singletonMap("cpId", cpId));

		String def = ResponseEvent.unwrap(cpSvc.getPublishedVersion(RequestEvent.wrap(crit)));
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(def.getBytes());

			httpResp.setContentType("application/json");
			httpResp.setHeader("Content-Disposition", "attachment;filename=CpDef_" + cpId + ".json");
			IoUtil.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IoUtil.close(in);
		}
	}

	//
	// For UI work
	//
	@RequestMapping(method = RequestMethod.GET, value="/byop")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody			
	public List<CollectionProtocolSummary> getCpListByOp(
			@RequestParam(value = "resource", required = true)
			String resourceName,
			
			@RequestParam(value = "op", required = true) 
			String opName,
			
			@RequestParam(value = "siteName", required = false)
			String[] siteNames,
			
			@RequestParam(value = "title", required = false)
			String searchTitle,

			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {
				
		List<String> inputSiteList = Collections.emptyList();
		if (siteNames != null) {
			inputSiteList = Arrays.asList(siteNames);
		}
		
		Resource resource = Resource.fromName(resourceName);
		Operation op = Operation.fromName(opName);
		
		List<CollectionProtocolSummary> emptyList = Collections.<CollectionProtocolSummary>emptyList();
		ResponseEvent<List<CollectionProtocolSummary>> resp = new ResponseEvent<List<CollectionProtocolSummary>>(emptyList);
		if (resource == Resource.PARTICIPANT && op == Operation.CREATE) {
			 resp = cpSvc.getRegisterEnabledCps(inputSiteList, searchTitle, maxResults);
		}
		
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/extension-form")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getForm() {
		return formSvc.getExtensionInfo(-1L, CollectionProtocol.EXTN);
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormSummary> getForms(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "entityType", required = true)
			String[] entityTypes) {
		return formSvc.getEntityForms(cpId, entityTypes);
	}

	@RequestMapping(method = RequestMethod.POST, value="/merge")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public MergeCpDetail mergeCollectionProtocol(@RequestBody MergeCpDetail mergeDetail) {
		ResponseEvent<MergeCpDetail> resp = cpSvc.mergeCollectionProtocols(request(mergeDetail));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/list-config")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ListConfig getListConfig(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "listName", required = true)
			String listName) {

		Map<String, Object> listCfgReq = new HashMap<>();
		listCfgReq.put("cpId", cpId);
		listCfgReq.put("listName", listName);

		ResponseEvent<ListConfig> resp = cpSvc.getCpListCfg(new RequestEvent<>(listCfgReq));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/expression-values")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<Object> getExpressionValues(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "listName")
			String listName,

			@RequestParam(value = "expr")
			String expr,

			@RequestParam(value = "searchTerm", required = false, defaultValue = "")
			String searchTerm) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("cpId", cpId);
		listReq.put("listName", listName);
		listReq.put("expr", expr);
		listReq.put("searchTerm", searchTerm);

		ResponseEvent<Collection<Object>> resp = cpSvc.getListExprValues(new RequestEvent<>(listReq));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/list-detail")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ListDetail getListDetail(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "listName")
			String listName,

			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,

			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults,

			@RequestParam(value = "includeCount", required = false, defaultValue = "false")
			boolean includeCount,

			@RequestBody
			List<Column> filters) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("cpId", cpId);
		listReq.put("listName", listName);
		listReq.put("startAt", startAt);
		listReq.put("maxResults", maxResults);
		listReq.put("includeCount", includeCount);
		listReq.put("filters", filters);

		ResponseEvent<ListDetail> resp = cpSvc.getList(request(listReq));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/list-size")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> getListSize(
			@PathVariable("id")
			Long cpId,

			@RequestParam(value = "listName", required = true)
			String listName,

			@RequestBody
			List<Column> filters) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("cpId", cpId);
		listReq.put("listName", listName);
		listReq.put("filters", filters);

		ResponseEvent<Integer> resp = cpSvc.getListSize(request(listReq));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("size", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> addLabel(@PathVariable("id") Long cpId) {
		return Collections.singletonMap("status", cpSvc.toggleStarredCp(cpId, true));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> removeLabel(@PathVariable("id") Long cpId) {
		return Collections.singletonMap("status", cpSvc.toggleStarredCp(cpId, false));
	}

	private ConsentTierDetail performConsentTierOp(OP op, Long cpId, ConsentTierDetail consentTier) {
		ConsentTierOp req = new ConsentTierOp();		
		req.setConsentTier(consentTier);
		req.setCpId(cpId);
		req.setOp(op);
		
		ResponseEvent<ConsentTierDetail> resp = cpSvc.updateConsentTier(request(req));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private CpWorkflowCfgDetail saveWorkflows(Long cpId, List<WorkflowDetail> workflows, boolean patch) {
		CpWorkflowCfgDetail input = new CpWorkflowCfgDetail();
		input.setCpId(cpId);
		input.setPatch(patch);

		for (WorkflowDetail workflow : workflows) {
			input.getWorkflows().put(workflow.getName(), workflow);
		}

		return response(cpSvc.saveWorkflows(request(input)));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
