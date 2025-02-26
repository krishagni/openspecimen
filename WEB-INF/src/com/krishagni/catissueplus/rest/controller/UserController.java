package com.krishagni.catissueplus.rest.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.domain.UserUiState;
import com.krishagni.catissueplus.core.administrative.events.AnnouncementDetail;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.administrative.events.PasswordDetails;
import com.krishagni.catissueplus.core.administrative.events.UserDetail;
import com.krishagni.catissueplus.core.administrative.repository.UserListCriteria;
import com.krishagni.catissueplus.core.administrative.services.UserService;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.EntityFormRecords;
import com.krishagni.catissueplus.core.de.events.FormCtxtSummary;
import com.krishagni.catissueplus.core.de.events.FormRecordsList;
import com.krishagni.catissueplus.core.de.events.GetEntityFormRecordsOp;
import com.krishagni.catissueplus.core.de.events.GetFormRecordsListOp;
import com.krishagni.rbac.events.SubjectRoleDetail;

@Controller
@RequestMapping("/users")
public class UserController {

	@Autowired
	private UserService userService;
	
	@Autowired
	private UserAuthenticationService userAuthService;

	@Autowired
	private HttpServletRequest httpServletRequest;

	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserSummary> getUsers(
		@RequestParam(value = "start", required = false, defaultValue = "0")
		int start,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults,

		@RequestParam(value = "searchString", required = false)
		String searchString,

		@RequestParam(value = "name", required = false)
		String name,

		@RequestParam(value = "loginName", required = false)
		String loginName,

		@RequestParam(value = "institute", required = false)
		String institute,

		@RequestParam(value = "group", required = false)
		List<String> groups,

		@RequestParam(value = "domainName", required = false)
		String domainName,

		@RequestParam(value = "activityStatus", required = false)
		String activityStatus,

		@RequestParam(value = "listAll", required = false, defaultValue = "true")
		boolean listAll,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "excludeType", required = false)
		String[] excludeTypes,

		@RequestParam(value = "activeSince", required = false)
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date activeSince,

		@RequestParam(value = "site", required = false)
		String siteName,

		@RequestParam(value = "cp", required = false)
		String cpShortTitle,

		@RequestParam(value = "role", required = false)
		String[] roles,

		@RequestParam(value = "resource", required = false)
		String resourceName,

		@RequestParam(value = "op", required = false)
		String[] ops,

		@RequestParam(value = "includeSysUser", required = false, defaultValue = "false")
		boolean includeSysUser) {
		
		UserListCriteria crit = new UserListCriteria()
			.startAt(start)
			.maxResults(maxResults)
			.query(searchString)
			.name(name)
			.loginName(loginName)
			.instituteName(institute)
			.groups(groups)
			.domainName(domainName)
			.activityStatus(activityStatus)
			.listAll(listAll)
			.includeStat(includeStats)
			.type(type)
			.excludeTypes(excludeTypes != null ? Arrays.asList(excludeTypes) : null)
			.activeSince(activeSince)
			.siteName(siteName)
			.cpShortTitle(cpShortTitle)
			.roleNames(roles != null ? Arrays.asList(roles) : Collections.emptyList())
			.resourceName(resourceName)
			.opNames(ops != null ? Arrays.asList(ops) : Collections.emptyList())
			.includeSysUser(includeSysUser);
		
		
		RequestEvent<UserListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<List<UserSummary>> resp = userService.getUsers(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();		
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getUsersCount(
		@RequestParam(value = "searchString", required = false)
		String searchString,
			
		@RequestParam(value = "name", required = false)
		String name,
			
		@RequestParam(value = "loginName", required = false)
		String loginName,
			
		@RequestParam(value = "institute", required = false)
		String institute,

		@RequestParam(value = "group", required = false)
		List<String> groups,

		@RequestParam(value = "domainName", required = false)
		String domainName,
			
		@RequestParam(value = "activityStatus", required = false)
		String activityStatus,
			
		@RequestParam(value = "listAll", required = false, defaultValue = "true")
		boolean listAll,

		@RequestParam(value = "type", required = false)
		String type,

		@RequestParam(value = "excludeType", required = false)
		String[] excludeTypes,

		@RequestParam(value = "activeSince", required = false)
		@DateTimeFormat(pattern="yyyy-MM-dd")
		Date activeSince,

		@RequestParam(value = "includeSysUser", required = false, defaultValue = "false")
		boolean includeSysUser) {
		
		UserListCriteria crit = new UserListCriteria()
			.query(searchString)
			.name(name)
			.loginName(loginName)
			.instituteName(institute)
			.groups(groups)
			.domainName(domainName)
			.activityStatus(activityStatus)
			.type(type)
			.excludeTypes(excludeTypes != null ? Arrays.asList(excludeTypes) : null)
			.listAll(listAll)
			.activeSince(activeSince)
			.includeSysUser(includeSysUser);
		
		RequestEvent<UserListCriteria> req = new RequestEvent<>(crit);
		ResponseEvent<Long> resp = userService.getUsersCount(req);
		resp.throwErrorIfUnsuccessful();
		
		return Collections.singletonMap("count", resp.getPayload());
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserDetail getUser(@PathVariable Long id) {
		ResponseEvent<UserDetail> resp = userService.getUser(new RequestEvent<Long>(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/byid/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserDetail getUserById(@PathVariable Long id) {
		return this.getUser(id);
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserDetail createUser(@RequestBody UserDetail userDetails) {
		RequestEvent<UserDetail> req = new RequestEvent<UserDetail>(userDetails);
		ResponseEvent<UserDetail> resp = userService.createUser(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/sign-up")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserDetail signupUser(@RequestBody UserDetail detail) {
		RequestEvent<UserDetail> req = new RequestEvent<UserDetail>(detail);
		ResponseEvent<UserDetail> resp = userService.createUser(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public UserDetail updateUser(@PathVariable Long id, @RequestBody UserDetail userDetail) {
		userDetail.setId(id);
		
		RequestEvent<UserDetail> req = new RequestEvent<UserDetail>(userDetail);
		ResponseEvent<UserDetail> resp = userService.updateUser(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PATCH, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public UserDetail patchUser(@PathVariable Long id, @RequestBody UserDetail userDetail) {
		userDetail.setId(id);
		
		RequestEvent<UserDetail> req = new RequestEvent<UserDetail>(userDetail);
		ResponseEvent<UserDetail> resp = userService.patchUser(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}


	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/activity-status")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public UserDetail updateUserStatus(@PathVariable Long id, @RequestBody Map<String, String> props) {
		UserDetail detail = new UserDetail();
 		detail.setId(id);
		detail.setActivityStatus(props.get("activityStatus"));
		RequestEvent<UserDetail> req = new RequestEvent<UserDetail>(detail);
		ResponseEvent<UserDetail> resp = userService.updateStatus(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/bulk-update")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<UserDetail> bulkUpdateUsers(@RequestBody BulkEntityDetail<UserDetail> detail) {
		ResponseEvent<List<UserDetail>> resp = userService.bulkUpdateUsers(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/dependent-entities")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<DependentEntityDetail> getDependentEntities(@PathVariable Long id) {
		RequestEvent<Long> req = new RequestEvent<Long>(id);
		ResponseEvent<List<DependentEntityDetail>> resp = userService.getDependentEntities(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public UserDetail deleteUser(@PathVariable Long id) {
		DeleteEntityOp deleteEntityOp = new DeleteEntityOp(id, false);
		RequestEvent<DeleteEntityOp> req = new RequestEvent<DeleteEntityOp>(deleteEntityOp);
		ResponseEvent<UserDetail> resp = userService.deleteUser(req);
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.DELETE)
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<UserDetail> deleteUsers(@RequestParam(value = "id") Long[] ids) {
		UserDetail userDetail = new UserDetail();
		userDetail.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());

		BulkEntityDetail<UserDetail> detail = new BulkEntityDetail<>();
		detail.setIds(Arrays.asList(ids));
		detail.setDetail(userDetail);

		ResponseEvent<List<UserDetail>> resp = userService.bulkUpdateUsers(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();

		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value = "/password")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Boolean changePassword(@RequestBody PasswordDetails passwordDetails) {
		RequestEvent<PasswordDetails> req = new RequestEvent<PasswordDetails>(passwordDetails);
		ResponseEvent<Boolean> resp = userService.changePassword(req);
		resp.throwErrorIfUnsuccessful();
		
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value = "/reset-password")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Boolean resetPassword(@RequestBody PasswordDetails passwordDetails) {
		passwordDetails.setIpAddress(Utility.getRemoteAddress(httpServletRequest));
		return ResponseEvent.unwrap(userService.resetPassword(RequestEvent.wrap(passwordDetails)));
	}

	@RequestMapping(method = RequestMethod.POST, value = "/forgot-password")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Boolean forgotPassword(@RequestBody UserDetail detail) {
		return ResponseEvent.unwrap(userService.forgotPassword(RequestEvent.wrap(detail)));
	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/current-user")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public UserSummary getCurrentUser() {
		return ResponseEvent.unwrap(userAuthService.getCurrentLoggedInUser());
 	}
	
	@RequestMapping(method = RequestMethod.GET, value = "/current-user-roles")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<SubjectRoleDetail> getCurrentUserRoles() {
		ResponseEvent<List<SubjectRoleDetail>> resp = userService.getCurrentUserRoles();
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/current-user-ui-state")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> getCurrentUserUiState(HttpServletRequest httpReq) {
		ResponseEvent<UserUiState> resp = userService.getUiState();
		resp.throwErrorIfUnsuccessful();

		String authToken = AuthUtil.getAuthTokenFromHeader(httpReq);
		if (authToken == null) {
			authToken = AuthUtil.getTokenFromCookie(httpReq);
		}

		Map<String, Object> state = new HashMap<>();
		if (resp.getPayload() != null) {
			state.putAll(resp.getPayload().getState());
		}

		state.put("authToken", authToken);
		return state;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/current-user-ui-state")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Map<String, Object> saveCurrentUserUiState(@RequestBody Map<String, Object> state) {
		ResponseEvent<UserUiState> resp = userService.saveUiState(new RequestEvent<>(state));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload() == null ? Collections.emptyMap() : resp.getPayload().getState();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/institute")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public InstituteDetail getInstitute(@PathVariable Long id) {
		ResponseEvent<InstituteDetail> resp = userService.getInstitute(new RequestEvent<Long>(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.POST, value = "/announcements")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public Boolean sendAnnouncementMail(@RequestBody AnnouncementDetail detail) {
		ResponseEvent<Boolean> resp = userService.broadcastAnnouncement(new RequestEvent<>(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms")
	@ResponseBody
	@ResponseStatus(HttpStatus.OK)
	public List<FormCtxtSummary> getForms(
		@PathVariable("id")
		Long userId,

		@RequestParam(value = "entityType", required = false, defaultValue = "User")
		String entityType) {

		if (StringUtils.isBlank(entityType)) {
			entityType = "User";
		}

		if (!entityType.equals("User") && !entityType.equals("UserProfile")) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, entityType);
		}

		Map<String, Object> req = new HashMap<>();
		req.put("userId", userId);
		req.put("entityType", entityType);
		return ResponseEvent.unwrap(userService.getForms(RequestEvent.wrap(req)));
	}

	@RequestMapping(method = RequestMethod.GET, value="/{id}/form-records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<FormRecordsList> getFormRecords(
		@PathVariable("id")
		Long userId,

		@RequestParam(value = "entityType", required = false, defaultValue = "User")
		String entityType) {

		if (StringUtils.isBlank(entityType)) {
			entityType = "User";
		}

		if (!entityType.equals("User") && !entityType.equals("UserProfile")) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, entityType);
		}

		GetFormRecordsListOp op = new GetFormRecordsListOp();
		op.setEntityType(entityType);
		op.setObjectId(userId);
		return ResponseEvent.unwrap(userService.getAllFormRecords(RequestEvent.wrap(op)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}/forms/{formCtxtId}/records")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public EntityFormRecords getFormRecords(
		@PathVariable("id")
		Long userId,

		@PathVariable("formCtxtId")
		Long formCtxtId) {

		GetEntityFormRecordsOp op = new GetEntityFormRecordsOp();
		op.setEntityId(userId);
		op.setFormCtxtId(formCtxtId);
		return  ResponseEvent.unwrap(userService.getFormRecords(RequestEvent.wrap(op)));
	}
}
