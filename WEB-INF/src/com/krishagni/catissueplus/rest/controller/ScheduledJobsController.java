package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.events.JobExportDetail;
import com.krishagni.catissueplus.core.administrative.events.JobRunsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobDetail;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobRunDetail;
import com.krishagni.catissueplus.core.administrative.services.ScheduledJobService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

@Controller
@RequestMapping("/scheduled-jobs")
public class ScheduledJobsController {
	
	@Autowired
	private ScheduledJobService jobSvc;
	
	@Autowired
	private HttpServletRequest httpReq;

	@Autowired
	private ExportService exportSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ScheduledJobDetail> getJobs(
		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "type", required = false)
		ScheduledJob.Type type,
			
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
			
		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {
		
		ScheduledJobListCriteria criteria = new ScheduledJobListCriteria()
			.query(query)
			.type(type)
			.startAt(startAt)
			.maxResults(maxResults);
		return response(jobSvc.getScheduledJobs(request(criteria)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getJobsCount(
		@RequestParam(value = "query", required = false)
		String query,

		@RequestParam(value = "type", required = false)
		ScheduledJob.Type type) {

		ScheduledJobListCriteria criteria = new ScheduledJobListCriteria().query(query).type(type);
		Long count = response(jobSvc.getScheduledJobsCount(request(criteria)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobDetail getJob(@PathVariable("id") Long jobId) {
		return response(jobSvc.getScheduledJob(request(jobId)));
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobDetail createJob(@RequestBody ScheduledJobDetail detail) {
		return response(jobSvc.createScheduledJob(request(detail)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobDetail updateJob(@PathVariable("id") Long jobId, @RequestBody ScheduledJobDetail detail) {
		detail.setId(jobId);
		return response(jobSvc.updateScheduledJob(request(detail)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value="{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobDetail deleteJob(@PathVariable("id") Long jobId) {
		return response(jobSvc.deleteScheduledJob(request(jobId)));
	}

	@RequestMapping(method = RequestMethod.GET, value="{jobId}/runs")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ScheduledJobRunDetail> getJobRuns(
		@PathVariable(value = "jobId")
		Long jobId,

		@RequestParam(value = "fromDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
		Date fromDate,

		@RequestParam(value = "toDate", required = false)
		@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm")
		Date toDate,

		@RequestParam(value = "status", required = false)
		ScheduledJobRun.Status status,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		JobRunsListCriteria criteria = new JobRunsListCriteria()
			.jobId(jobId)
			.fromDate(fromDate)
			.toDate(toDate)
			.status(status)
			.startAt(0)
			.maxResults(maxResults);
		return response(jobSvc.getJobRuns(request(criteria)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "{jobId}/runs/{runId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobRunDetail getJobRun(
		@PathVariable("jobId")
		Long jobId,
			
		@PathVariable("runId")
		Long runId) {
		return response(jobSvc.getJobRun(request(runId)));
	}
	
	@RequestMapping(method = RequestMethod.POST, value="{jobId}/runs")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ScheduledJobDetail executeJob(
		@PathVariable("jobId")
		Long jobId,
			
		@RequestBody
		Map<String, String> body) {

		ScheduledJobRunDetail detail = new ScheduledJobRunDetail();
		detail.setJobId(jobId);
		detail.setRtArgs(body.get("args"));
		return response(jobSvc.executeJob(request(detail)));
	}
		
	
	@RequestMapping(method = RequestMethod.GET, value = "{jobId}/runs/{runId}/result-file")
	@ResponseStatus(HttpStatus.OK)
	public void downloadExportDataFile(
		@PathVariable("jobId")
		Long jobId,
			
		@PathVariable("runId")
		Long runId,
			
		HttpServletResponse httpResp) {

		Date startTime = Calendar.getInstance().getTime();
		JobExportDetail detail = response(jobSvc.getJobResultFile(request(runId)));
		File file = detail.getFile();
		Utility.sendToClient(httpResp, file.getName(), file);

		Map<String, String> params = new HashMap<>();
		params.put("jobId", detail.getJobRun().getJobId().toString());
		params.put("runId", detail.getJobRun().getId().toString());
		exportSvc.saveJob("scheduled_job_report", startTime, params);
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
