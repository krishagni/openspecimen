package com.krishagni.catissueplus.core.biospecimen.events;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;

@JsonFilter("withoutId")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RequestManagerGroupSummary {
	private Long id;

	private String name;

	private Long userGroupId;

	private String userGroupName;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getUserGroupId() {
		return userGroupId;
	}

	public void setUserGroupId(Long userGroupId) {
		this.userGroupId = userGroupId;
	}

	public String getUserGroupName() {
		return userGroupName;
	}

	public void setUserGroupName(String userGroupName) {
		this.userGroupName = userGroupName;
	}

	public static RequestManagerGroupSummary from(RequestManagerGroup rmg) {
		if (rmg == null) {
			return null;
		}

		RequestManagerGroupSummary result = new RequestManagerGroupSummary();
		result.setId(rmg.getId());
		result.setName(rmg.getName());

		UserGroup userGroup = rmg.getUserGroup();
		if (userGroup != null) {
			result.setUserGroupId(userGroup.getId());
			result.setUserGroupName(userGroup.getName());
		}

		return result;
	}
}
