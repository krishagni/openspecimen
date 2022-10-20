package com.krishagni.catissueplus.rest.controller;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupFormsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolGroupService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FormSummary;

@Controller
@RequestMapping("/collection-protocol-groups")
public class CollectionProtocolGroupsController {

	@Autowired
	private CollectionProtocolGroupService groupSvc;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolGroupSummary> getGroups(
		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "cpShortTitle", required = false)
		String cpShortTitle,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		CpGroupListCriteria crit = new CpGroupListCriteria()
			.query(query)
			.cpShortTitle(cpShortTitle)
			.includeStat(includeStats)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(groupSvc.getGroups(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolGroupDetail getGroup(@PathVariable("id") Long groupId) {
		return ResponseEvent.unwrap(groupSvc.getGroup(RequestEvent.wrap(new EntityQueryCriteria(groupId))));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolGroupDetail createGroup(@RequestBody CollectionProtocolGroupDetail group) {
		return ResponseEvent.unwrap(groupSvc.createGroup(RequestEvent.wrap(group)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolGroupDetail updateGroup(
		@PathVariable("id")
		Long groupId,

		@RequestBody
		CollectionProtocolGroupDetail group) {

		group.setId(groupId);
		return ResponseEvent.unwrap(groupSvc.updateGroup(RequestEvent.wrap(group)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CollectionProtocolGroupSummary deleteGroup(
		@PathVariable("id")
		Long id,

		@RequestParam(value = "reason", required = false)
		String reason) {

		BulkDeleteEntityOp crit = new BulkDeleteEntityOp();
		crit.setIds(Collections.singleton(id));
		crit.setReason(reason);
		return ResponseEvent.unwrap(groupSvc.deleteGroups(RequestEvent.wrap(crit))).getEntities().get(0);
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CollectionProtocolGroupSummary> deleteGroups(
		@RequestParam("id")
		Long[] ids,

		@RequestParam(value = "reason", required = false)
		String reason) {

		BulkDeleteEntityOp crit = new BulkDeleteEntityOp();
		crit.setIds(new HashSet<>(Arrays.asList(ids)));
		crit.setReason(reason);
		return ResponseEvent.unwrap(groupSvc.deleteGroups(RequestEvent.wrap(crit))).getEntities();
	}

	@RequestMapping(method = RequestMethod.GET, value = "{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<CpGroupFormsDetail> getForms(
		@PathVariable("id")
		Long groupId,

		@RequestParam(value = "level", required = false)
		String level) {

		EntityQueryCriteria crit = new EntityQueryCriteria(groupId);
		crit.setParams(Collections.singletonMap("level", level));
		return ResponseEvent.unwrap(groupSvc.getForms(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> addForms(
		@PathVariable("id")
		Long groupId,

		@RequestBody
		CpGroupFormsDetail input) {

		input.setGroupId(groupId);
		return Collections.singletonMap("status", ResponseEvent.unwrap(groupSvc.addForms(RequestEvent.wrap(input))));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "{id}/forms")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> removeForms(
		@PathVariable("id")
		Long groupId,

		@RequestParam(value = "level")
		String level,

		@RequestParam(value = "formId")
		List<Long> formIds) {

		CpGroupFormsDetail input = new CpGroupFormsDetail();
		input.setGroupId(groupId);
		input.setLevel(level);
		input.setForms(getForms(formIds));
		return Collections.singletonMap("status", ResponseEvent.unwrap(groupSvc.removeForms(RequestEvent.wrap(input))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/workflows")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpGroupWorkflowCfgDetail getWorkflowCfg(@PathVariable("id") Long groupId) {
		return ResponseEvent.unwrap(groupSvc.getWorkflows(RequestEvent.wrap(new EntityQueryCriteria(groupId))));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/workflows-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getWorkflowCfg(@PathVariable("id") Long groupId, HttpServletResponse httpResp) {
		EntityQueryCriteria crit = new EntityQueryCriteria(groupId);
		crit.setParams(Collections.singletonMap("export", true));

		ResponseEvent<CpGroupWorkflowCfgDetail> resp = groupSvc.getWorkflows(RequestEvent.wrap(crit));
		resp.throwErrorIfUnsuccessful();

		InputStream in = null;
		try {
			CpGroupWorkflowCfgDetail workflowDetail = resp.getPayload();
			String filename = (workflowDetail.getGroupName() +  "_workflows.json")
				.replaceAll("\\\\", "_")  // replace backslash with _
				.replaceAll("/", "_")     // replace forward slash with _
				.replaceAll("\\s+", "_"); // replace whitespace with _

			String workflowsJson = new ObjectMapper().writerWithDefaultPrettyPrinter()
				.writeValueAsString(resp.getPayload().getWorkflows().values());
			in = new ByteArrayInputStream(workflowsJson.getBytes());
			Utility.sendToClient(httpResp, filename, "application/json", in);
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.FILE_SEND_ERROR, e.getMessage());
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{id}/workflows")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpGroupWorkflowCfgDetail saveWorkflowCfg(@PathVariable("id") Long groupId, @RequestBody List<WorkflowDetail> workflows) {
		return saveWorkflows(groupId, workflows, false);
	}

	@RequestMapping(method = RequestMethod.POST, value="/{id}/workflows-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public CpGroupWorkflowCfgDetail saveWorkflowCfg(@PathVariable("id") Long groupId, @PathVariable("file") MultipartFile file) {
		List<WorkflowDetail> workflows;

		try {
			ObjectMapper mapper = new ObjectMapper();
			workflows = mapper.readValue(file.getInputStream(), new TypeReference<List<WorkflowDetail>>() {});
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_REQUEST, e.getMessage());
		}

		return saveWorkflows(groupId, workflows, true);
	}
	private List<FormSummary> getForms(List<Long> formIds) {
		return formIds.stream().map(
			formId -> {
				FormSummary fs = new FormSummary();
				fs.setFormId(formId);
				return fs;
			}
		).collect(Collectors.toList());
	}

	private CpGroupWorkflowCfgDetail saveWorkflows(Long groupId, List<WorkflowDetail> workflows, boolean importWfs) {
		CpGroupWorkflowCfgDetail input = new CpGroupWorkflowCfgDetail();
		input.setGroupId(groupId);
		input.setImportWfs(importWfs);
		for (WorkflowDetail workflow : workflows) {
			input.getWorkflows().put(workflow.getName(), workflow);
		}

		return ResponseEvent.unwrap(groupSvc.saveWorkflows(RequestEvent.wrap(input)));
	}
}
