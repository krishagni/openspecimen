package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
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

import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;
import com.krishagni.catissueplus.core.importer.events.FileRecordsDetail;
import com.krishagni.catissueplus.core.importer.events.ImportDetail;
import com.krishagni.catissueplus.core.importer.events.ImportJobDetail;
import com.krishagni.catissueplus.core.importer.events.ObjectSchemaCriteria;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.catissueplus.core.importer.services.ObjectReader;

@Controller
@RequestMapping("/import-jobs")
public class BulkObjectImportController {
	
	@Autowired
	private ImportService importSvc;

	@RequestMapping(method = RequestMethod.GET, value="/input-file-template")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public void getInputFileTemplate(
			@RequestParam(value = "schema")
			String schemaName,

			@RequestParam
			Map<String, String> params,

			HttpServletResponse httpResp) {

		params.remove("schema");

		String formName   = params.get("formName");
		String entityType = params.get("entityType");

		String filename = schemaName + ".csv";
		if (StringUtils.isNotBlank(formName) && StringUtils.isNotBlank(entityType)) {
			filename = formName + "_" + entityType + ".csv";
		}

		filename = Utility.cleanPath(Utility.stripWs(filename));
		
		ObjectSchemaCriteria detail = new ObjectSchemaCriteria();
		detail.setObjectType(schemaName);
		detail.setParams(params);
		
		RequestEvent<ObjectSchemaCriteria> req = new RequestEvent<>(detail);
		ResponseEvent<String> resp = importSvc.getInputFileTemplate(req);
		resp.throwErrorIfUnsuccessful();
		
		httpResp.setContentType("application/csv");
		httpResp.setHeader("Content-Disposition", "attachment;filename=" + filename);
			
		try {
			httpResp.getOutputStream().write(resp.getPayload().getBytes());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} 			
	}
		
	@RequestMapping(method = RequestMethod.POST, value = "/input-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public Map<String, String> uploadJobInputFile(@PathVariable("file") MultipartFile file) 
	throws IOException {
		RequestEvent<InputStream> req = new RequestEvent<>(file.getInputStream());
		ResponseEvent<String> resp = importSvc.uploadImportJobFile(req);
		resp.throwErrorIfUnsuccessful();

		Map<String, String> response = new HashMap<>();
		response.put("fileId", resp.getPayload());
		response.put("filename", file.getOriginalFilename());
		return response;
	}

	@RequestMapping(method = RequestMethod.POST, value = "/process-file-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, Object>> processFileRecords(@RequestBody FileRecordsDetail detail) {
		ResponseEvent<List<Map<String, Object>>> resp = importSvc.processFileRecords(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/record-fields-csv")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String getRecordFieldsCsv(@RequestBody List<ObjectSchema.Field> fields) {
		ObjectSchema.Record schemaRec = new ObjectSchema.Record();
		schemaRec.setFields(fields);

		ObjectSchema schema = new ObjectSchema();
		schema.setRecord(schemaRec);
		return ObjectReader.getSchemaFields(schema);
	}

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<ImportJobDetail> getImportJobs(
			@RequestParam(value = "startAt", required = false, defaultValue = "0") 
			int startAt,

			@RequestParam(value = "maxResults", required = false, defaultValue = "25") 
			int maxResults,
			
			@RequestParam(value = "objectType", required = false) 
			String[] objectTypes,

			@RequestParam
			Map<String, String> params) {

		String[] nonParams = {"startAt", "maxResults", "objectType"};
		for (String nonParam : nonParams) {
			params.remove(nonParam);
		}

		ListImportJobsCriteria crit = new ListImportJobsCriteria()
			.startAt(startAt)
			.maxResults(maxResults)
			.objectTypes(objectTypes != null ? Arrays.asList(objectTypes) : null)
			.params(params);

		RequestEvent<ListImportJobsCriteria> req = new RequestEvent<ListImportJobsCriteria>(crit);
		ResponseEvent<List<ImportJobDetail>> resp = importSvc.getImportJobs(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ImportJobDetail getImportJob(@PathVariable("id") Long jobId) {
		RequestEvent<Long> req = new RequestEvent<Long>(jobId);
		ResponseEvent<ImportJobDetail> resp = importSvc.getImportJob(req);
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
			
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/output")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getImportJobOutputFile(@PathVariable("id") Long jobId, HttpServletResponse httpResp) {		
		RequestEvent<Long> req = new RequestEvent<Long>(jobId);
		ResponseEvent<String> resp = importSvc.getImportJobFile(req);
		resp.throwErrorIfUnsuccessful();
		
		httpResp.setContentType("application/csv");
		httpResp.setHeader("Content-Disposition", "attachment;filename=BulkImportJob_" + jobId + ".csv");
			
		InputStream in = null;
		try {
			in = new FileInputStream(new File(resp.getPayload()));
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}				
	}
			
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public ImportJobDetail createImportJob(@RequestBody ImportDetail detail) {
		RequestEvent<ImportDetail> req = new RequestEvent<ImportDetail>(detail);
		ResponseEvent<ImportJobDetail> resp = importSvc.importObjects(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "{id}/stop")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ImportJobDetail stopImportJob(@PathVariable("id") Long jobId) {
		ResponseEvent<ImportJobDetail> resp = importSvc.stopJob(new RequestEvent<>(jobId));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "schedule")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> scheduleJobs(@PathVariable("file") MultipartFile file)
	throws IOException {
		FileDetail input = new FileDetail();
		input.setFilename(file.getOriginalFilename());
		input.setFileIn(file.getInputStream());
		Integer scheduled = ResponseEvent.unwrap(importSvc.scheduleImportJobs(RequestEvent.wrap(input)));
		return Collections.singletonMap("scheduled", scheduled);
	}
}
