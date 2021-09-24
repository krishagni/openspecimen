package com.krishagni.catissueplus.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;
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
		if (!formDataList.isEmpty()) {
			if (SpecimenReceivedEvent.FORM_NAME.equals(formDataList.get(0).getContainer().getName())) {
				int idx = -1;
				for (FormData formData : formDataList) {
					++idx;
					String label = (String) valueMapList.get(idx).getOrDefault("label", null);
					if (StringUtils.isNotBlank(label)) {
						formData.getAppData().put("newSpecimenlabel", StringUtils.trim(label));
					}
				}
			}
		}

		List<FormData> savedDataList = ResponseEvent.unwrap(specimenEventsSvc.saveSpecimenEvents(RequestEvent.wrap(formDataList)));
		List<Map<String, Object>> savedValueMapList = new ArrayList<>();
		for (FormData data : savedDataList) {
			savedValueMapList.add(data.getFieldNameValueMap(data.isUsingUdn()));
		}
		
		return savedValueMapList;
	}
}
