
package com.krishagni.catissueplus.rest.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.events.ListPvCriteria;
import com.krishagni.catissueplus.core.administrative.events.PvDetail;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.PermissibleValueService;

@Controller
@RequestMapping("/permissible-values")
public class PermissibleValueController {

	@Autowired
	private PermissibleValueService pvSvc;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<PvDetail> getPermissibleValues(
		@RequestParam(value = "attribute", required = false)
		String attribute,
			
		@RequestParam(value = "searchString", required = false)
		String searchStr,

		@RequestParam(value = "includeParentValue", required = false, defaultValue="false")
		boolean includeParentValue,

		@RequestParam(value = "parentAttribute", required = false)
		String parentAttribute,
			
		@RequestParam(value = "parentValue", required = false)
		String parentValue,
			
		@RequestParam(value = "includeOnlyLeafValue", required = false, defaultValue="false")
		boolean includeOnlyLeafValue,

		@RequestParam(value = "includeOnlyRootValue", required = false, defaultValue="false")
		boolean includeOnlyRootValue,

		@RequestParam(value = "includeProps", required = false, defaultValue="false")
		boolean includeProps,

		@RequestParam(value = "activityStatus", required = false)
		String activityStatus,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,
						
		@RequestParam(value = "maxResults", required = false, defaultValue = "1000")
		int maxResults,

		HttpServletRequest httpReq) {

		List<String> values = null;
		String[] searchValues = httpReq.getParameterValues("value");
		if (searchValues != null && searchValues.length > 0) {
			values = Arrays.asList(searchValues);
			searchStr = null;
		}

		ListPvCriteria crit = new ListPvCriteria()
			.attribute(attribute)
			.query(searchStr)
			.values(values)
			.parentValue(parentValue)
			.includeParentValue(includeParentValue)
			.parentAttribute(parentAttribute)
			.includeOnlyLeafValue(includeOnlyLeafValue)
			.includeOnlyRootValue(includeOnlyRootValue)
			.includeProps(includeProps)
			.activityStatus(activityStatus)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(pvSvc.getPermissibleValues(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Long> getPermissibleValuesCount(
		@RequestParam(value = "attribute", required = false)
		String attribute,

		@RequestParam(value = "searchString", required = false)
		String searchStr,

		@RequestParam(value = "parentAttribute", required = false)
		String parentAttribute,

		@RequestParam(value = "parentValue", required = false)
		String parentValue,

		@RequestParam(value = "includeOnlyLeafValue", required = false, defaultValue="false")
		boolean includeOnlyLeafValue,

		@RequestParam(value = "includeOnlyRootValue", required = false, defaultValue="false")
		boolean includeOnlyRootValue,

		@RequestParam(value = "activityStatus", required = false)
		String activityStatus,

		HttpServletRequest httpReq) {

		List<String> values = null;
		String[] searchValues = httpReq.getParameterValues("value");
		if (searchValues != null && searchValues.length > 0) {
			values = Arrays.asList(searchValues);
			searchStr = null;
		}

		ListPvCriteria crit = new ListPvCriteria()
			.attribute(attribute)
			.query(searchStr)
			.values(values)
			.parentValue(parentValue)
			.parentAttribute(parentAttribute)
			.includeOnlyLeafValue(includeOnlyLeafValue)
			.includeOnlyRootValue(includeOnlyRootValue)
			.activityStatus(activityStatus);
		return Collections.singletonMap("count", ResponseEvent.unwrap(pvSvc.getPermissibleValuesCount(RequestEvent.wrap(crit))));
	}


	@RequestMapping(method = RequestMethod.GET, value = "/v")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<PvDetail> getPermissibleValuesV(
		@RequestParam(value = "attribute", required = false)
		String attribute,

		@RequestParam(value = "searchString", required = false)
		String searchStr,

		@RequestParam(value = "includeParentValue", required = false, defaultValue="false")
		boolean includeParentValue,

		@RequestParam(value = "parentAttribute", required = false)
		String parentAttribute,

		@RequestParam(value = "parentValue", required = false)
		String parentValue,

		@RequestParam(value = "includeOnlyLeafValue", required = false, defaultValue="false")
		boolean includeOnlyLeafValue,

		@RequestParam(value = "includeOnlyRootValue", required = false, defaultValue="false")
		boolean includeOnlyRootValue,

		@RequestParam(value = "includeProps", required = false, defaultValue="false")
		boolean includeProps,

		@RequestParam(value = "activityStatus", required = false)
		String activityStatus,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "1000")
		int maxResults,

		HttpServletRequest httpReq) {

		return getPermissibleValues(
			attribute,
			searchStr,
			includeParentValue,
			parentAttribute,
			parentValue,
			includeOnlyLeafValue,
			includeOnlyRootValue,
			includeProps,
			activityStatus,
			startAt,
			maxResults,
			httpReq);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/v/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public PvDetail getPermissibleValue(
		@PathVariable("id")
		Object pv,

		@RequestParam(value = "includeProps", required = false, defaultValue="false")
		boolean includeProps,

		@RequestParam(value = "attribute", required = false)
		String attribute) {

		EntityQueryCriteria criteria = null;
		Map<String, Object> props = new HashMap<>();
		props.put("includeProps", includeProps);

		if (pv instanceof Number) {
			criteria = new EntityQueryCriteria(((Number) pv).longValue());
		} else if (pv instanceof String pvStr) {
			long pvId = NumberUtils.toLong(pvStr, -999L);
			if (pvId < 0) {
				criteria = new EntityQueryCriteria(pv.toString());
				props.put("attribute", attribute);
			} else {
				criteria = new EntityQueryCriteria(pvId);
			}
		} else {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT);
		}

		criteria.setParams(props);
		return ResponseEvent.unwrap(pvSvc.getPermissibleValue(RequestEvent.wrap(criteria)));
	}
}
