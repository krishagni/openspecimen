package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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

import com.krishagni.catissueplus.core.administrative.events.UserGroupDetail;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupListCriteria;
import com.krishagni.catissueplus.core.administrative.services.UserGroupService;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@Controller
@RequestMapping("/user-groups")
public class UserGroupController {

	@Autowired
	private UserGroupService groupSvc;


	@RequestMapping(method = RequestMethod.GET)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<UserGroupSummary> getGroups(
		@RequestParam(value = "query", required = false, defaultValue = "")
		String searchStr,

		@RequestParam(value = "site", required = false, defaultValue = "")
		String site,

		@RequestParam(value = "institute", required = false, defaultValue = "")
		String institute,

		@RequestParam(value = "listAll", required = false, defaultValue = "false")
		boolean listAll,

		@RequestParam(value = "includeStats", required = false, defaultValue = "false")
		boolean includeStats,

		@RequestParam(value = "startAt", required = false, defaultValue = "0")
		int startAt,

		@RequestParam(value = "maxResults", required = false, defaultValue = "100")
		int maxResults) {

		UserGroupListCriteria crit = new UserGroupListCriteria()
			.query(searchStr)
			.institute(institute)
			.site(site)
			.listAll(listAll)
			.includeStat(includeStats)
			.startAt(startAt)
			.maxResults(maxResults);
		return ResponseEvent.unwrap(groupSvc.getGroups(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.GET, value = "/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getGroupsCount(
		@RequestParam(value = "query", required = false, defaultValue = "")
		String searchStr,

		@RequestParam(value = "institute", required = false, defaultValue = "")
		String institute,

		@RequestParam(value = "site", required = false, defaultValue = "")
		String site,

		@RequestParam(value = "listAll", required = false, defaultValue = "false")
		boolean listAll) {

		UserGroupListCriteria crit = new UserGroupListCriteria()
			.query(searchStr)
			.institute(institute)
			.site(site)
			.listAll(listAll);

		Long count = ResponseEvent.unwrap(groupSvc.getGroupsCount(RequestEvent.wrap(crit)));
		return Collections.singletonMap("count", count);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGroupDetail getGroup(
		@PathVariable("id")
		Long groupId,

		@RequestParam(value = "includeUsers", required = false, defaultValue = "false")
		boolean includeUsers) {

		EntityQueryCriteria crit = new EntityQueryCriteria(groupId);
		crit.setParams(Collections.singletonMap("includeUsers", includeUsers));
		return ResponseEvent.unwrap(groupSvc.getGroup(RequestEvent.wrap(crit)));
	}

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGroupDetail createGroup(@RequestBody UserGroupDetail groupDetail) {
		return ResponseEvent.unwrap(groupSvc.createGroup(RequestEvent.wrap(groupDetail)));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGroupDetail updateGroup(
		@PathVariable("id")
		Long groupId,

		@RequestBody
		UserGroupDetail groupDetail) {

		groupDetail.setId(groupId);
		return ResponseEvent.unwrap(groupSvc.updateGroup(RequestEvent.wrap(groupDetail)));
	}

	@RequestMapping(method = RequestMethod.DELETE, value = "/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGroupDetail deleteGroup(@PathVariable("id") Long groupId) {
		return ResponseEvent.unwrap(groupSvc.deleteGroup(RequestEvent.wrap(new EntityQueryCriteria(groupId))));
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/{id}/users")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public UserGroupDetail addRemoveUsers(
		@PathVariable("id")
		Long groupId,

		@RequestParam(value = "op")
		String op,

		@RequestBody
		List<UserSummary> users) {

		UserGroupDetail input = new UserGroupDetail();
		input.setId(groupId);
		input.setUsers(users);

		if ("ADD".equals(op)) {
			return ResponseEvent.unwrap(groupSvc.addUsers(RequestEvent.wrap(input)));
		} else if ("REMOVE".equals(op)) {
			return ResponseEvent.unwrap(groupSvc.removeUsers(RequestEvent.wrap(input)));
		}

		throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, "Invalid user group op: " + op);
	}

	@RequestMapping(method = RequestMethod.GET, value = "/member-of")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Boolean> isMemberOf(@RequestParam(value = "groupName") String groupName) {
		Boolean status = ResponseEvent.unwrap(groupSvc.isMemberOf(RequestEvent.wrap(groupName)));
		return Collections.singletonMap("status", status);
	}
}
