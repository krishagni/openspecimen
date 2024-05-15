package com.krishagni.catissueplus.rest.controller;

import java.util.Date;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobItemListCriteria;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobItemDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.LabelPrintJobService;

@Controller
@RequestMapping("/label-print-jobs")
public class LabelPrintJobsController {

	@Autowired
	private LabelPrintJobService printJobSvc;

	@RequestMapping(method = RequestMethod.GET, value="items")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<LabelPrintJobItemDetail> getPrintJobItems(
		@RequestParam(value = "lastId", required = false, defaultValue = "0")
		Long lastId,

		@RequestParam(value = "from", required = false)
		Date from,

		@RequestParam(value = "printerName", required = false)
		List<String> printerNames,

		@RequestParam(value = "hasContent", required = false)
		Boolean hasContent,

		@RequestParam(value = "includeData", required = false)
		Boolean includeData,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		LabelPrintJobItemListCriteria crit = new LabelPrintJobItemListCriteria()
			.lastId(lastId)
			.printerNames(printerNames)
			.hasContent(hasContent)
			.includeData(includeData)
			.fromDate(from)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(printJobSvc.getPrintJobItems(RequestEvent.wrap(crit)));
	}
}
