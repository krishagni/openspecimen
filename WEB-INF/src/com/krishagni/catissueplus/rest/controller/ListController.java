package com.krishagni.catissueplus.rest.controller;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListDetail;
import com.krishagni.catissueplus.core.query.ListService;

@Controller
@RequestMapping("/lists")
public class ListController {

	@Autowired
	private ListService listSvc;

	@RequestMapping(method = RequestMethod.GET, value = "/config")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ListConfig getListConfig(
		@RequestParam(value = "listName")
		String listName,

		@RequestParam(value = "objectId")
		Long objectId) {

		Map<String, Object> cfgReq = new HashMap<>();
		cfgReq.put("listName", listName);
		cfgReq.put("objectId", objectId);
		return response(listSvc.getListCfg(request(cfgReq)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/expression-values")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Collection<Object> getExpressionValues(
		@RequestParam(value = "listName")
		String listName,

		@RequestParam(value = "objectId")
		Long objectId,

		@RequestParam(value = "expr")
		String expr,

		@RequestParam(value = "searchTerm", required = false, defaultValue = "")
		String searchTerm) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("listName", listName);
		listReq.put("objectId", objectId);
		listReq.put("expr", expr);
		listReq.put("searchTerm", searchTerm);

		return response(listSvc.getListExprValues(request(listReq)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/data")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ListDetail getListDetail(
		@RequestParam(value = "listName")
		String listName,

		@RequestParam(value = "objectId")
		Long objectId,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "includeCount", required = false, defaultValue = "false")
		boolean includeCount,

		@RequestParam(value = "orderBy", required = false, defaultValue = "")
		String orderBy,

		@RequestParam(value = "orderDirection", required = false, defaultValue = "asc")
		String orderDirection,

		@RequestParam
		Map<String, String> allParams,

		@RequestBody
		List<Column> filters) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("listName", listName);
		listReq.put("objectId", objectId);
		listReq.put("startAt", startAt);
		listReq.put("maxResults", maxResults);
		listReq.put("includeCount", includeCount);
		listReq.put("filters", filters);
		listReq.put("allParams", allParams);

		if (StringUtils.isNotBlank(orderBy)) {
			Column order = new Column();
			order.setExpr(orderBy);
			order.setDirection(orderDirection);
			listReq.put("orderBy", order);
		}

		return response(listSvc.getList(request(listReq)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/size")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Integer> getListSize(
		@RequestParam(value = "listName")
		String listName,

		@RequestParam(value = "objectId")
		Long objectId,

		@RequestBody
		List<Column> filters) {

		Map<String, Object> listReq = new HashMap<>();
		listReq.put("listName", listName);
		listReq.put("objectId", objectId);
		listReq.put("filters", filters);

		return Collections.singletonMap("size", response(listSvc.getListSize(request(listReq))));
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}

	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}
