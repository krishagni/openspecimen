package com.krishagni.catissueplus.rest.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.services.SpecimenEventsService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

import edu.common.dynamicextensions.napi.FormData;

@Controller
@RequestMapping("/specimen-events")
public class SpecimenEventsController {

	@Autowired
	private SpecimenEventsService specimenEventsSvc;

	@RequestMapping(method = RequestMethod.POST, value = "/{formId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<Map<String, Object>> saveSpecimenEvents(
		@PathVariable(value = "formId")
		Long formId,
			
		@RequestBody
		List<Map<String, Object>> valueMapList) {

		List<FormData> formDataList = FormData.fromValueMap(formId, valueMapList);
		List<FormData> savedDataList = ResponseEvent.unwrap(specimenEventsSvc.saveSpecimenEvents(RequestEvent.wrap(formDataList)));
		return savedDataList.stream().map(data -> data.getFieldNameValueMap(data.isUsingUdn())).collect(Collectors.toList());
	}
}
