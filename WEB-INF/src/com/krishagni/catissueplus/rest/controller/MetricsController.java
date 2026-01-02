package com.krishagni.catissueplus.rest.controller;

import java.io.File;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;

@Controller
@RequestMapping("/metrics")
public class MetricsController {

	@Autowired
	private PrometheusMeterRegistry registry;

	@RequestMapping(method = RequestMethod.GET, produces = "text/plain")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public String getMetrics() {
		return registry.scrape();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/live-heap")
	@ResponseStatus(HttpStatus.OK)
	public void downloadHeapDump(HttpServletResponse response) {
		if (!AuthUtil.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		File heapDumpFile = Utility.dumpLiveHeap();
		Utility.sendToClient(response, heapDumpFile.getName(), heapDumpFile, true);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/thread-dump")
	@ResponseStatus(HttpStatus.OK)
	public void downloadThreadDump(HttpServletResponse response) {
		if (!AuthUtil.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		File threadDumpFile = Utility.dumpLiveThreadStacks();
		Utility.sendToClient(response, threadDumpFile.getName(), threadDumpFile, true);
	}
}
