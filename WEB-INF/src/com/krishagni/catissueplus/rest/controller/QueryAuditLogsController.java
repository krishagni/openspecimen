package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDetail;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogSummary;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;
import com.krishagni.catissueplus.core.de.services.QueryService;

@Controller
@RequestMapping("/query-audit-logs")
public class QueryAuditLogsController {

	@Autowired
	private QueryService querySvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public List<QueryAuditLogSummary> getAuditLogs(
		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "userId", required = false)
		Long userId,

		@RequestParam(value = "failed", required = false, defaultValue = "false")
		boolean failed,

		@RequestParam(value = "startDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		Date startDate,

		@RequestParam(value = "endDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		Date endDate,

		@RequestParam(value = "orderDirection", required = false, defaultValue = "desc")
		String orderDirection,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "25")
		int maxResults) {
		
		QueryAuditLogsListCriteria crit = new QueryAuditLogsListCriteria()
			.query(query).userId(userId).failed(failed)
			.startDate(startDate != null ? Utility.chopTime(startDate) : null)
			.endDate(endDate != null ? Utility.getEndOfDay(endDate) : null)
			.asc("asc".equalsIgnoreCase(orderDirection))
			.startAt(startAt).maxResults(maxResults);
		return ResponseEvent.unwrap(querySvc.getAuditLogs(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getAuditLogsCount(
		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "userId", required = false)
		Long userId,

		@RequestParam(value = "failed", required = false, defaultValue = "false")
		boolean failed,

		@RequestParam(value = "startDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		Date startDate,

		@RequestParam(value = "endDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd")
		Date endDate) {

		QueryAuditLogsListCriteria crit = new QueryAuditLogsListCriteria()
			.query(query).userId(userId).failed(failed)
			.startDate(startDate != null ? Utility.chopTime(startDate) : null)
			.endDate(endDate != null ? Utility.getEndOfDay(endDate) : null);
		Long count = ResponseEvent.unwrap(querySvc.getAuditLogsCount(RequestEvent.wrap(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody	
	public QueryAuditLogDetail getAuditLog(@PathVariable("id") Long id) {
		return ResponseEvent.unwrap(querySvc.getAuditLog(RequestEvent.wrap(id)));
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}/diagnostics")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadAuditLogDiagnostics(@PathVariable("id") Long id, HttpServletResponse httpResp) {
		File file = ResponseEvent.unwrap(querySvc.getAuditLogDiagnosticFile(RequestEvent.wrap(id)));
		Utility.sendToClient(httpResp, "query_audit_log_" + id + ".json.zip", file);
	}

	@RequestMapping(method = RequestMethod.POST, value="/export")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> exportAuditLogs(@RequestBody QueryAuditLogsListCriteria crit) {
		ExportedFileDetail file = ResponseEvent.unwrap(querySvc.exportAuditLogs(RequestEvent.wrap(crit)));
		return Collections.singletonMap("fileId", file != null ? file.getName() : null);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/exported-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void downloadAuditLogs(@RequestParam(value = "fileId") String fileId, HttpServletResponse httpResp) {
		File file = ResponseEvent.unwrap(querySvc.getExportedAuditLogsFile(RequestEvent.wrap(fileId)));
		String[] parts = file.getName().split("_"); // <UUID>_<date>_<time>_<userid>
		String filename = "os_query_audit_logs" + parts[1] + "_" + parts[2]+ ".zip";
		httpResp.setContentType("application/zip");
		httpResp.setHeader("Content-Disposition", "attachment;filename=" + filename);

		InputStream in = null;
		try {
			in = new FileInputStream(file);
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}
}
