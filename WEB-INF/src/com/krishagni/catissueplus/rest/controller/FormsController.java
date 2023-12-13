package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
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
import org.springframework.web.multipart.MultipartFile;

import com.krishagni.catissueplus.core.administrative.repository.FormListCriteria;
import com.krishagni.catissueplus.core.auth.domain.UserRequestData;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormContextRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormDataDetail;
import com.krishagni.catissueplus.core.de.events.FormFieldSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordCriteria;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.FormRevisionDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.GetFormFieldPvsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.catissueplus.core.de.events.ListFormFields;
import com.krishagni.catissueplus.core.de.events.MoveFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp.RemoveType;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.services.ImportService;

import edu.common.dynamicextensions.domain.nui.Container;
import edu.common.dynamicextensions.domain.nui.PermissibleValue;
import edu.common.dynamicextensions.napi.FormData;
import edu.common.dynamicextensions.nutility.ContainerJsonSerializer;
import edu.common.dynamicextensions.nutility.ContainerSerializer;
import edu.common.dynamicextensions.nutility.FormDefinitionExporter;
import edu.common.dynamicextensions.nutility.IoUtil;


@Controller
@RequestMapping("/forms")
public class FormsController {

	private static AtomicInteger formCnt = new AtomicInteger();
	
	@Autowired
	private FormService formSvc;

	@Autowired
	private ImportService importSvc;

	@Autowired
	private ExportService exportSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormSummary> getForms(
		@RequestParam(value = "name", required= false, defaultValue = "")
		String name,

		@RequestParam(value = "cpId", required = false)
		List<Long> cpIds,

		@RequestParam(value = "entityId", required = false)
		List<Long> entityIds,

		@RequestParam(value = "formType", required = false)
		List<String> formTypes,

		@RequestParam(value = "excludeSysForms", required = false)
		Boolean excludeSysForms,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStat,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		FormListCriteria crit = new FormListCriteria()
			.query(name)
			.cpIds(cpIds)
			.entityIds(entityIds)
			.entityTypes(formTypes)
			.excludeSysForm(excludeSysForms)
			.includeStat(includeStat)
			.startAt(startAt)
			.maxResults(maxResults);
		
		ResponseEvent<List<FormSummary>> resp = formSvc.getForms(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getFormsCount(
		@RequestParam(value = "name", required= false, defaultValue = "")
		String name,

		@RequestParam(value = "cpId", required = false)
		List<Long> cpIds,

		@RequestParam(value = "entityId", required = false)
		List<Long> entityIds,

		@RequestParam(value="formType", required = false)
		List<String> formTypes,

		@RequestParam(value="excludeSysForms", required=false)
		Boolean excludeSysForms) {
		
		FormListCriteria crit = new FormListCriteria()
			.query(name)
			.cpIds(cpIds)
			.entityIds(entityIds)
			.entityTypes(formTypes)
			.excludeSysForm(excludeSysForms);
		
		ResponseEvent<Long> resp = formSvc.getFormsCount(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.DELETE, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Boolean deleteForm(@PathVariable("id") Long formId) {
		return deleteForms(new Long[] {formId});
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Boolean deleteForms(@RequestParam(value = "id") Long[] ids) {
		BulkDeleteEntityOp op = new BulkDeleteEntityOp(new HashSet<>(Arrays.asList(ids)));
		ResponseEvent<Boolean> resp = formSvc.deleteForms(getRequest(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/definition")
	@ResponseStatus(HttpStatus.OK)
	public void getFormDefinition(
		@PathVariable("id")
		Long formId,
			
		@RequestParam(value = "maxPvs", required = false, defaultValue = "0")
		int maxPvListSize,
			
		HttpServletResponse httpResp)
	throws IOException {
		Container form = ResponseEvent.unwrap(formSvc.getFormDefinition(RequestEvent.wrap(formId)));
		serializeToJson(form, maxPvListSize, httpResp);
	}

	@RequestMapping(method = RequestMethod.GET, value="/definition")
	@ResponseStatus(HttpStatus.OK)
	public void getFormDefinition(
		@RequestParam(value = "name")
		String name,

		@RequestParam(value = "maxPvs", required = false, defaultValue = "0")
		int maxPvListSize,

		HttpServletResponse httpResp)
	throws IOException {
		Container form = ResponseEvent.unwrap(formSvc.getFormDefinitionByName(RequestEvent.wrap(name)));
		serializeToJson(form, maxPvListSize, httpResp);
	}

	@RequestMapping(method = RequestMethod.PUT, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> saveForm(
		@PathVariable("id")
		Long formId,

		@RequestBody
		Map<String, Object> props
	) {
		if (formId != null && !formId.equals(-1L)) {
			props.put("id", formId);
		} else {
			props.remove("id");
		}

		formId = ResponseEvent.unwrap(formSvc.saveForm(RequestEvent.wrap(props)));
		return Collections.singletonMap("id", formId);
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/fields")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormFieldSummary> getFormFields(
		@PathVariable("id")
		Long formId,
			
		@RequestParam(value = "prefixParentCaption", required = false, defaultValue = "false")
		boolean prefixParentCaption,
			
		@RequestParam(value = "cpId", required = false, defaultValue = "-1")
		Long cpId,

		@RequestParam(value = "cpGroupId", required = false)
		Long cpGroupId,
			
		@RequestParam(value = "extendedFields", required = false, defaultValue = "false")
		boolean extendedFields) {
		
		ListFormFields crit = new ListFormFields();
		crit.setFormId(formId);
		crit.setPrefixParentFormCaption(prefixParentCaption);
		crit.setCpId(cpId);
		crit.setCpGroupId(cpGroupId);
		crit.setExtendedFields(extendedFields);
		
		ResponseEvent<List<FormFieldSummary>> resp = formSvc.getFormFields(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
		
	@RequestMapping(method = RequestMethod.GET, value="{id}/data/{recordId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getFormData(
		@PathVariable("id")
		Long formId,
			
		@PathVariable("recordId")
		Long recordId,
			
		@RequestParam(value = "includeUdn", required = false, defaultValue = "false")
		String includeUdn,

		@RequestParam(value="includeMetadata", required = false, defaultValue = "false")
		boolean includeMetadata) {
		
		FormRecordCriteria crit = new FormRecordCriteria();
		crit.setFormId(formId);
		crit.setRecordId(recordId);
		
		ResponseEvent<FormDataDetail> resp = formSvc.getFormData(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		if (includeMetadata) {
			return resp.getPayload().getFormData().getFieldValueMap();
		} else {
			//
			// StringUtils.equals(Utility.escapeXss ...) is done to avoid reporting of XSS violation by Snyk
			// Changing it to boolean doesn't seem to help
			//
			return resp.getPayload().getFormData().getFieldNameValueMap(StringUtils.equals(Utility.escapeXss(includeUdn), "true"));
		}
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/latest-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, Object>> getLatestRecords(
		@PathVariable("id")
		Long formId,

		@RequestParam(value="entityType")
		String entityType,

		@RequestParam(value="objectId")
		List<Long> objectIds,

		@RequestParam(value="includeUdn", required=false, defaultValue="false")
		String includeUdn) {

		FormRecordCriteria crit = new FormRecordCriteria();
		crit.setFormId(formId);
		crit.setEntityType(entityType);
		crit.setObjectIds(objectIds);

		ResponseEvent<List<FormDataDetail>> resp = formSvc.getLatestRecords(getRequest(crit));
		resp.throwErrorIfUnsuccessful();

		boolean useUdn = Utility.escapeXss(includeUdn).equalsIgnoreCase("true");
		return resp.getPayload().stream().map(r -> r.getFormData().getFieldNameValueMap(useUdn)).collect(Collectors.toList());
	}
	
	@RequestMapping(method = RequestMethod.POST, value="{id}/data")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> saveFormData(
		@PathVariable("id")
		Long formId,

		@RequestBody
		Map<String, Object> valueMap) {
		return saveOrUpdateFormData(formId, valueMap);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="{id}/data")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> updateFormData(
		@PathVariable("id")
		Long formId,
			
		@RequestBody
		Map<String, Object> valueMap) {
		return saveOrUpdateFormData(formId, valueMap);
	}
		
	@RequestMapping(method = RequestMethod.GET, value="{id}/contexts")	
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormContextDetail> getFormContexts(@PathVariable("id") Long formId) {
		ResponseEvent<List<FormContextDetail>> resp = formSvc.getFormContexts(getRequest(formId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="{id}/contexts")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormContextDetail> addFormContexts(
		@PathVariable("id")
		Long formId,
			
		@RequestBody
		List<FormContextDetail> formCtxts) {
		
		formCtxts.forEach(fc -> fc.setFormId(formId));
		return ResponseEvent.unwrap(formSvc.addFormContexts(RequestEvent.wrap(formCtxts)));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="{id}/contexts")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> removeFormContext(
		@PathVariable("id")
		Long formId,
			
		@RequestParam(value = "entityType")
		String entityType,
			
		@RequestParam(value = "cpId")
		Long cpId,

		@RequestParam(value = "entityId", required = false)
		Long entityId) {
		
		RemoveFormContextOp op = new RemoveFormContextOp();
		op.setCpId(cpId);
		op.setFormId(formId);
		op.setEntityType(entityType);
		op.setEntityId(entityId);
		op.setRemoveType(RemoveType.SOFT_REMOVE);
		return Collections.singletonMap("status", ResponseEvent.unwrap(formSvc.removeFormContext(RequestEvent.wrap(op))));
    }
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<FormRecordsList> getRecords(
		@PathVariable("id")
		Long formId,
			
		@RequestParam(value = "objectId", required = true)
		Long objectId,
			
		@RequestParam(value = "entityType", required = true)
		String entityType) {
		
		GetFormRecordsListOp opDetail = new GetFormRecordsListOp();
		opDetail.setObjectId(objectId);
		opDetail.setEntityType(entityType);
		opDetail.setFormId(formId);
		
		ResponseEvent<List<FormRecordsList>> resp = formSvc.getFormRecords(getRequest(opDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
		
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/dependent-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<DependentEntityDetail> getDependentEntities(@PathVariable("id") Long formId) {
		ResponseEvent<List<DependentEntityDetail>> resp = formSvc.getDependentEntities(getRequest(formId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="{id}/data/{recordId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Long deleteRecords(@PathVariable("id") Long formId, @PathVariable("recordId") Long recId) {
		FormRecordCriteria crit = new FormRecordCriteria();
		crit.setFormId(formId);
		crit.setRecordId(recId);

		ResponseEvent<Long> resp = formSvc.deleteRecord(getRequest(crit));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/definition-zip")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public FormSummary importForm(@PathVariable("file") MultipartFile file)
	throws IOException {
		Date startDate = Calendar.getInstance().getTime();
		File tmpDir = new File(getTmpDirName());
		try {
			String contentType = file.getContentType();
			String filename    = file.getOriginalFilename();
			if (StringUtils.equals(contentType, "application/zip") || filename.endsWith(".zip")) {
				if (!tmpDir.exists()) {
					tmpDir.mkdirs();
				}

				Utility.inflateZip(file.getInputStream(), tmpDir.getAbsolutePath());
			} else {
				Utility.downloadFile(file.getInputStream(), tmpDir.getAbsolutePath(), "forms.xml");
			}

			FormSummary result = ResponseEvent.unwrap(formSvc.importForm(RequestEvent.wrap(tmpDir.getAbsolutePath())));
			importSvc.saveJob("form", "UPSERT", startDate, Collections.singletonMap("formId", result.getFormId().toString()));
			return result;
		} finally {
			try {
				FileUtils.deleteDirectory(tmpDir);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/definition-zip")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void exportFormZip(@PathVariable("id") Long containerId, HttpServletResponse httpResp) {
		Date startDate = Calendar.getInstance().getTime();
		ResponseEvent<Container> resp = formSvc.getFormDefinition(getRequest(containerId));
		resp.throwErrorIfUnsuccessful();

		Container form = resp.getPayload();
		exportFormZip(form, httpResp);
		exportSvc.saveJob("form", startDate, Collections.singletonMap("formId", form.getId().toString()));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{id}/permissible-values")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<PermissibleValue> getFormFieldPvs(
		@PathVariable("id")
		Long formId,
			
		@RequestParam(value = "controlName", required = true)
		String controlName,

		@RequestParam(value = "useUdn", required = false, defaultValue = "false")
		boolean useUdn,
			
		@RequestParam(value = "searchString", required = false, defaultValue = "")
		List<String> queries,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		GetFormFieldPvsOp op = new GetFormFieldPvsOp();
		op.setFormId(formId);
		op.setControlName(controlName);
		op.setUseUdn(useUdn);
		op.setQueries(queries);
		op.setMaxResults(maxResults);

		ResponseEvent<List<PermissibleValue>> resp = formSvc.getPvs(getRequest(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/permissible-values")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<PermissibleValue> getFieldPvs(
		@RequestParam(value = "formId", required = false)
		Long formId,

		@RequestParam(value = "formName", required = false)
		String formName,

		@RequestParam(value = "controlName", required = true)
		String controlName,

		@RequestParam(value = "useUdn", required = false, defaultValue = "false")
		boolean useUdn,

		@RequestParam(value = "searchString", required = false, defaultValue = "")
		List<String> queries,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		GetFormFieldPvsOp op = new GetFormFieldPvsOp();
		op.setFormId(formId);
		op.setFormName(formName);
		op.setControlName(controlName);
		op.setUseUdn(useUdn);
		op.setQueries(queries);
		op.setMaxResults(maxResults);

		ResponseEvent<List<PermissibleValue>> resp = formSvc.getPvs(getRequest(op));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value="/move-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> moveRecords(@RequestBody MoveFormRecordsOp input) {
		return Collections.singletonMap("count", ResponseEvent.unwrap(formSvc.moveRecords(RequestEvent.wrap(input))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormRevisionDetail> getRevisions(@PathVariable("id") Long formId) {
		return ResponseEvent.unwrap(formSvc.getFormRevisions(RequestEvent.wrap(formId)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/revisions/{revId}/definition")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getFormAtRevision(
		@PathVariable("id")
		Long formId,

		@PathVariable("revId")
		Long revId,

		@RequestParam(value = "maxPvs", required = false, defaultValue = "0")
		int maxPvListSize,

		HttpServletResponse httpResp)
	throws IOException {
		Container form = ResponseEvent.unwrap(formSvc.getFormAtRevision(RequestEvent.wrap(Pair.make(formId, revId))));
		if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formId + ":" + revId);
		}

		serializeToJson(form, maxPvListSize, httpResp);
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/revisions/{revId}/definition-zip")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getFormAtRevision(@PathVariable("id") Long formId, @PathVariable("revId") Long revId, HttpServletResponse httpResp) {
		Container form = ResponseEvent.unwrap(formSvc.getFormAtRevision(RequestEvent.wrap(Pair.make(formId, revId))));
		if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, formId + ":" + revId);
		}

		exportFormZip(form, httpResp);
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/context-revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormContextRevisionDetail> getContextRevisions(@PathVariable("id") Long formId) {
		return ResponseEvent.unwrap(formSvc.getFormContextRevisions(RequestEvent.wrap(formId)));
	}

	private Map<String, Object> saveOrUpdateFormData(Long formId, Map<String, Object> valueMap) {
		Map<String, Object> appData = (Map<String, Object>) valueMap.computeIfAbsent(
			"appData",
			(k) -> new HashMap<String, Object>()
		);

		Object useUdnInput = appData.get("useUdn");
		boolean useUdn = false;
		if (useUdnInput instanceof String) {
			useUdn =  Utility.escapeXss((String) useUdnInput).equals("true");
		} else if (useUdnInput instanceof Boolean) {
			useUdn = Utility.escapeXss(useUdnInput.toString()).toLowerCase().equals("true");
		} else if (useUdnInput instanceof Number) {
			useUdn = ((Number) useUdnInput).intValue() == 1;
		}

		appData.putAll(UserRequestData.getInstance().getData());
		boolean includeMetadata = Boolean.TRUE.equals(appData.get("includeMetadata"));

		FormData formData = FormData.fromValueMap(formId, valueMap);
		
		FormDataDetail detail = new FormDataDetail();
		detail.setFormId(formId);
		detail.setFormData(formData);
		detail.setRecordId(formData.getRecordId());
		
		ResponseEvent<FormDataDetail> resp = formSvc.saveFormData(getRequest(detail));
		resp.throwErrorIfUnsuccessful();

		formData.getAppData().put("nextSurveyToken", UserRequestData.getInstance().getDataItem("nextSurveyToken"));
		formData.getAppData().entrySet().removeIf(kv -> kv.getKey().startsWith("$"));

		if (includeMetadata) {
			return resp.getPayload().getFormData().getFieldValueMap();
		} else {
			return resp.getPayload().getFormData().getFieldNameValueMap(useUdn);
		}
	}

	private String zipFiles(String dir) {
		String zipFile = new StringBuilder(System.getProperty("java.io.tmpdir"))
			.append(File.separator).append("export-form-")
			.append(formCnt.incrementAndGet()).append(".zip")
			.toString();
		
		IoUtil.zipFiles(dir, zipFile);
		return zipFile;
	}
	
	private String getTmpDirName() {
		return new StringBuilder().append(System.getProperty("java.io.tmpdir")).append(File.separator)
				.append(System.currentTimeMillis()).append(formCnt.incrementAndGet()).append("create").toString();
	}
  
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}

	private void serializeToJson(Container form, int maxPvListSize, HttpServletResponse httpResp)
	throws IOException {
		httpResp.setCharacterEncoding("UTF-8");
		Writer writer = httpResp.getWriter();

		ContainerSerializer serializer = new ContainerJsonSerializer(form, writer);
		serializer.serialize(maxPvListSize);
		writer.flush();
	}

	private void exportFormZip(Container form, HttpServletResponse httpResp) {
		String tmpDir = getTmpDirName();
		String zipFileName = null;
		FileInputStream fin = null;

		try {
			FormDefinitionExporter formExporter = new FormDefinitionExporter();
			formExporter.export(form, tmpDir);

			String fileName = form.getName() + ".zip";
			zipFileName = zipFiles(tmpDir);

			httpResp.setContentType("application/zip");
			httpResp.setHeader("Content-Disposition", "attachment; filename=" + fileName);

			OutputStream out = httpResp.getOutputStream();
			fin = new FileInputStream(zipFileName);
			IoUtil.copy(fin, out);
		} catch (Exception e) {
			throw new RuntimeException("Error occurred when exporting form", e);
		} finally {
			IoUtil.delete(tmpDir);
			IoUtil.delete(zipFileName);
			IoUtil.close(fin);
		}
	}
}
