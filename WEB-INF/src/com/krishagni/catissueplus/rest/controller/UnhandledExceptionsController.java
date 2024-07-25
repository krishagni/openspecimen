package com.krishagni.catissueplus.rest.controller;

import java.nio.charset.StandardCharsets;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UnhandledExceptionSummary;
import com.krishagni.catissueplus.core.common.repository.UnhandledExceptionListCriteria;
import com.krishagni.catissueplus.core.common.service.CommonService;
import com.krishagni.catissueplus.core.common.util.Utility;

@Controller
@RequestMapping("/unhandled-exceptions")
public class UnhandledExceptionsController {
	@Autowired
	private CommonService commonSvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UnhandledExceptionSummary> getUnhandledExceptions(
		@RequestParam(value = "fromDate", required = false) 
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date fromDate,
		
		@RequestParam(value = "toDate", required = false) 
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date toDate,
		
		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
		
		@RequestParam(value = "maxResults", required = false, defaultValue = "100") 
		int maxResults) {
		
		UnhandledExceptionListCriteria crit  = new UnhandledExceptionListCriteria()
			.fromDate(fromDate).toDate(toDate)
			.startAt(startAt).maxResults(maxResults);
		return ResponseEvent.unwrap(commonSvc.getUnhandledExceptions(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getUnhandledExceptionsCount(
		@RequestParam(value = "fromDate", required = false)
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date fromDate,

		@RequestParam(value = "toDate", required = false)
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date toDate) {

		UnhandledExceptionListCriteria crit  = new UnhandledExceptionListCriteria()
			.fromDate(fromDate).toDate(toDate);
		return Collections.singletonMap("count", ResponseEvent.unwrap(commonSvc.getUnhandledExceptionsCount(RequestEvent.wrap(crit))));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/stack-trace")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> getStackTrace(@PathVariable("id") Long id) {
		String trace = ResponseEvent.unwrap(commonSvc.getUnhandledExceptionLog(RequestEvent.wrap(id)));
		return Collections.singletonMap("error", trace);
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/{id}/log")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void getUnhandledExceptionLog(
		@PathVariable("id")
		Long id,
		
		HttpServletResponse httpResp) {

		String log = ResponseEvent.unwrap(commonSvc.getUnhandledExceptionLog(RequestEvent.wrap(id)));
		Utility.sendToClient(
			httpResp,
			"unhandled-exception.log",
			"text/plain",
			IOUtils.toInputStream(log, StandardCharsets.UTF_8));
	}
}