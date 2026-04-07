package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

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

import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ListFolderQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetails;
import com.krishagni.catissueplus.core.de.events.QueryFolderSummary;
import com.krishagni.catissueplus.core.de.events.SavedQueriesList;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.events.ShareQueryFolderOp;
import com.krishagni.catissueplus.core.de.events.UpdateFolderQueriesOp;
import com.krishagni.catissueplus.core.de.events.UpdateFolderQueriesOp.Operation;
import com.krishagni.catissueplus.core.de.services.QueryService;

@Controller
@RequestMapping("/query-folders")
public class QueryFoldersController {
	
	@Autowired
	private HttpServletRequest httpServletRequest;

	@Autowired
	private QueryService querySvc;
	
	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<QueryFolderSummary> getFoldersForUser() {
		return response(querySvc.getUserFolders(getRequest(null)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryFolderDetails getFolder(
		@PathVariable("folderId")
		Long folderId,

		@RequestParam(value = "includeQueries", defaultValue = "true", required = false)
		Boolean includeQueries) {

		EntityQueryCriteria crit = new EntityQueryCriteria(folderId);
		crit.setParams(Collections.singletonMap("includeQueries", includeQueries));
		return response(querySvc.getFolder(getRequest(crit)));
	}
		
	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryFolderDetails createFolder(@RequestBody QueryFolderDetails folderDetails) {
		return response(querySvc.createFolder(getRequest(folderDetails)));
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public QueryFolderDetails updateFolder(@PathVariable("folderId") Long folderId, @RequestBody QueryFolderDetails folderDetails) {
		folderDetails.setId(folderId);
		
		return response(querySvc.updateFolder(getRequest(folderDetails)));
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/{folderId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Long deleteFolder(@PathVariable("folderId") Long folderId) {
		return response(querySvc.deleteFolder(getRequest(folderId)));
	}
			
	@RequestMapping(method = RequestMethod.GET, value="/{folderId}/saved-queries")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SavedQueriesList getFolderQueries(
			@PathVariable("folderId") 
			Long folderId,
			
			@RequestParam(value = "start", required = false, defaultValue = "0") 
			int start,
			
			@RequestParam(value = "max", required = false, defaultValue = "25") 
			int max,
			
			@RequestParam(value = "countReq", required = false, defaultValue = "false") 
			boolean countReq,
			
			@RequestParam(value = "searchString", required = false, defaultValue = "") 
			String searchString) {
		
		ListFolderQueriesCriteria crit = new ListFolderQueriesCriteria()
			.folderId(folderId)
			.startAt(start)
			.maxResults(max)
			.query(searchString)
			.countReq(countReq);
		
		return response(querySvc.getFolderQueries(getRequest(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{folderId}/saved-queries-count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> getFolderQueriesCount(
		@PathVariable("folderId")
		Long folderId,

		@RequestParam(value = "searchString", required = false, defaultValue = "")
		String searchString) {

		ListFolderQueriesCriteria crit = new ListFolderQueriesCriteria()
			.folderId(folderId)
			.query(searchString);
		Long count = response(querySvc.getFolderQueriesCount(getRequest(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.PUT, value="/{folderId}/saved-queries")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SavedQuerySummary> updateFolderQueries(
			@PathVariable("folderId") 
			Long folderId,
			
			@RequestParam(value = "operation", required = false, defaultValue = "UPDATE") 
			String operation,
			
			@RequestBody 
			List<Long> queries) {
		
		UpdateFolderQueriesOp opDetail = new UpdateFolderQueriesOp();
		opDetail.setFolderId(folderId);
		opDetail.setQueries(queries);
		opDetail.setOp(Operation.valueOf(operation));
		
		return response(querySvc.updateFolderQueries(getRequest(opDetail)));
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/{folderId}/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserSummary> shareFolder(
			@PathVariable("folderId") 
			Long folderId,
			
			@RequestParam(value = "operation", required = false, defaultValue = "UPDATE") 
			String operation,
			
			@RequestBody List<Long> 
			userIds) {
		
		ShareQueryFolderOp opDetail = new ShareQueryFolderOp();
		opDetail.setFolderId(folderId);
		opDetail.setUserIds(userIds);
		opDetail.setOp(ShareQueryFolderOp.Operation.valueOf(operation));
		
		return response(querySvc.shareFolder(getRequest(opDetail)));
	}
	
	private <T> T response(ResponseEvent<T> resp) {
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);				
	}	
}
