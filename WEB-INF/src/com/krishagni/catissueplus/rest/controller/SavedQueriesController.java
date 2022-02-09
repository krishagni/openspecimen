package com.krishagni.catissueplus.rest.controller;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.multipart.MultipartFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;


import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.domain.SelectField;
import com.krishagni.catissueplus.core.de.events.ListQueryAuditLogsCriteria;
import com.krishagni.catissueplus.core.de.events.ListSavedQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogSummary;
import com.krishagni.catissueplus.core.de.events.SavedQueriesList;
import com.krishagni.catissueplus.core.de.events.SavedQueryDetail;
import com.krishagni.catissueplus.core.de.services.QueryService;

import edu.common.dynamicextensions.nutility.IoUtil;

@Controller
@RequestMapping("/saved-queries")
public class SavedQueriesController {

	@Autowired
	private QueryService querySvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Object getSavedQueries(
		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "searchString", required = false, defaultValue = "")
		String searchString,

		@RequestParam(value = "start", required = false, defaultValue = "0")
		int start,
			
		@RequestParam(value = "max", required = false, defaultValue = "25")
		int max,

		@RequestParam(value = "countReq", required = false, defaultValue = "false")
		boolean countReq,

		@RequestParam(value = "orderByStarred", required = false, defaultValue = "false")
		boolean orderByStarred,

		@RequestParam(value = "returnList", required = false, defaultValue = "false")
		boolean returnList) {
		
		ListSavedQueriesCriteria crit = new ListSavedQueriesCriteria()
			.cpId(cpId)
			.query(searchString)
			.countReq(countReq)
			.orderByStarred(orderByStarred)
			.startAt(start)
			.maxResults(max);
		SavedQueriesList list = response(querySvc.getSavedQueries(request(crit)));
		return returnList ? list.getQueries() : list;
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getSavedQueriesCount(
		@RequestParam(value = "cpId", required = false)
		Long cpId,

		@RequestParam(value = "searchString", required = false, defaultValue = "")
		String searchString) {

		ListSavedQueriesCriteria crit = new ListSavedQueriesCriteria()
			.cpId(cpId)
			.query(searchString);
		Long count = response(querySvc.getSavedQueriesCount(request(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SavedQueryDetail getQueryDetails(@PathVariable("id") Long queryId) {
		return response(querySvc.getSavedQuery(request(queryId)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/definition-file")
	@ResponseStatus(HttpStatus.OK)	
	public void getQueryDefFile(@PathVariable("id") Long queryId, HttpServletResponse response) {
		String queryDef = response(querySvc.getQueryDef(request(queryId)));

		response.setContentType("application/json");
		response.setHeader("Content-Disposition", "attachment;filename=QueryDef_" + queryId + ".json");
			
		InputStream in = null;
		try {
			in = new ByteArrayInputStream(queryDef.getBytes());
			IoUtil.copy(in, response.getOutputStream());
		} catch (IOException e) {
			throw new RuntimeException("Error sending file", e);
		} finally {
			IoUtil.close(in);
		}				
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/definition-file")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody		
	public SavedQueryDetail importQuery(@PathVariable("file") MultipartFile file)
	throws IOException {
		String json = new String(file.getBytes());
		SavedQueryDetail detail = new Gson().fromJson(json, SavedQueryDetail.class);
		return saveQuery(detail);
	}
	
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SavedQueryDetail saveQuery(@RequestBody SavedQueryDetail detail) {
		curateSavedQueryDetail(detail);
		
		return response(querySvc.saveQuery(request(detail)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SavedQueryDetail updateQuery(@RequestBody SavedQueryDetail detail) {
		curateSavedQueryDetail(detail);
		
		return response(querySvc.updateQuery(request(detail)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Long deleteQuery(@PathVariable("id") Long id) {
		return response(querySvc.deleteQuery(request(id)));
	}

	@RequestMapping(method = RequestMethod.GET,  value = "/{id}/audit-logs")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<QueryAuditLogSummary> getQueryAuditLogs(
			@PathVariable("id")
			Long savedQueryId,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "25")
			int maxResults) {
		
		ListQueryAuditLogsCriteria crit = new ListQueryAuditLogsCriteria()
			.query(savedQueryId.toString()).startAt(startAt).maxResults(maxResults);
		return response(querySvc.getAuditLogs(request(crit)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> addLabel(@PathVariable("id") Long queryId) {
		return Collections.singletonMap("status", querySvc.toggleStarredQuery(queryId, true));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}/labels")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> removeLabel(@PathVariable("id") Long queryId) {
		return Collections.singletonMap("status", querySvc.toggleStarredQuery(queryId, false));
	}

	private void curateSavedQueryDetail(SavedQueryDetail detail) {
		Object[] selectList = detail.getSelectList();
		if (selectList == null) {
			return;
		}
		
		for (int i = 0; i < selectList.length; ++i) {
			if (!(selectList[i] instanceof Map)) {
				continue;
			}
			
			try {
				selectList[i] = getSelectField(getJson((Map<String, Object>)selectList[i]));
			} catch (Exception e) {
				throw new RuntimeException("Bad select field", e);
			}
		}
	}
	
	private String getJson(Map<String, Object> props) 
	throws Exception {
		return new ObjectMapper().writeValueAsString(props);
	}
	
	private SelectField getSelectField(String json) 
	throws Exception {
		return new ObjectMapper().readValue(json, SelectField.class);
	}
	
	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private <T> RequestEvent<T> request(T payload) {
		return new RequestEvent<>(payload);
	}		
}
