package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditEntityQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

@Controller
@RequestMapping("/audit")
public class AuditController {

	@Autowired
	private AuditService auditService;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public AuditDetail getAuditInfo(
		@RequestParam(value = "objectName")
		String objectName,

		@RequestParam(value = "objectId")
		Long objectId) {

		AuditEntityQueryCriteria criteria = new AuditEntityQueryCriteria();
		criteria.setObjectName(objectName);
		criteria.setObjectId(objectId);

		ResponseEvent<List<AuditDetail>> resp = auditService.getEntityAuditDetail(new RequestEvent<>(Collections.singletonList(criteria)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload().iterator().next();
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<AuditDetail> getAuditInfo(@RequestBody List<AuditEntityQueryCriteria> criteria) {
		ResponseEvent<List<AuditDetail>> resp = auditService.getEntityAuditDetail(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<RevisionDetail> getRevisions(
		@RequestParam("objectName")
		String objectName,

		@RequestParam("objectId")
		Long objectId) {

		AuditEntityQueryCriteria criteria = new AuditEntityQueryCriteria();
		criteria.setObjectName(objectName);
		criteria.setObjectId(objectId);

		ResponseEvent<List<RevisionDetail>> resp = auditService.getEntityRevisions(new RequestEvent<>(Collections.singletonList(criteria)));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<RevisionDetail> getRevisions(@RequestBody List<AuditEntityQueryCriteria> criteria) {
		ResponseEvent<List<RevisionDetail>> resp = auditService.getEntityRevisions(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value="/export-revisions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> exportRevisions(@RequestBody RevisionsListCriteria criteria) {
		ResponseEvent<ExportedFileDetail> resp = auditService.exportRevisions(new RequestEvent<>(criteria));
		resp.throwErrorIfUnsuccessful();

		ExportedFileDetail fileDetail = resp.getPayload();
		return Collections.singletonMap("fileId", fileDetail.getName());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/revisions-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadRevisionsFile(@RequestParam(value = "fileId") String fileId, HttpServletResponse httpResp) {
		ResponseEvent<File> resp = auditService.getExportedRevisionsFile(new RequestEvent<>(fileId));
		resp.throwErrorIfUnsuccessful();

		String[] parts = resp.getPayload().getName().split("_"); // <UUID>_<date>_<time>_<userid>
		String filename = "os_audit_revisions_" + parts[1] + "_" + parts[2]+ ".zip";
		httpResp.setContentType("application/zip");
		httpResp.setHeader("Content-Disposition", "attachment;filename=" + filename);

		InputStream in = null;
		try {
			in = new FileInputStream(resp.getPayload());
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	@RequestMapping(method = RequestMethod.GET, value = "/revision-entities")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, String>> getRevisionEntities() {
		return ResponseEvent.unwrap(auditService.getRevisionEntities());
	}
}
