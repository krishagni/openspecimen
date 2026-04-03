package com.krishagni.catissueplus.rest.controller;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.exporter.events.ExportDetail;
import com.krishagni.catissueplus.core.exporter.events.ExportJobDetail;
import com.krishagni.catissueplus.core.exporter.services.ExportService;

@Controller
@RequestMapping("/export-jobs")
public class ExportJobsController {
	@Autowired
	private ExportService exportSvc;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ExportJobDetail createExportJob(@RequestBody ExportDetail detail) {
		if (detail.getParams() != null) {
			detail.getParams().remove("$$users");
		}

		return response(exportSvc.exportObjects(request(detail)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/output")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getExportJobOutputFile(@PathVariable("id") Long jobId, HttpServletResponse httpResp) {
		String outputFile = response(exportSvc.getExportFile(request(jobId)));
		String fileExtn = FilenameUtils.getExtension(outputFile);
		httpResp.setContentType("application/" + fileExtn);
		httpResp.setHeader("Content-Disposition", "attachment;filename=ExportJob_" + jobId + "." + fileExtn);

		InputStream in = null;
		try {
			in = new FileInputStream(new File(outputFile));
			IOUtils.copy(in, httpResp.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IOUtils.closeQuietly(in);
		}
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
