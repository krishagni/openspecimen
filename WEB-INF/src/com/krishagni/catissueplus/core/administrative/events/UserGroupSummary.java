package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.util.Utility;

@ListenAttributeChanges
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserGroupSummary extends AttributeModifiedSupport {
	private Long id;

	private String name;

	private String description;

	private String institute;

	private Integer noOfUsers;

	private String activityStatus;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getInstitute() {
		return institute;
	}

	public void setInstitute(String institute) {
		this.institute = institute;
	}

	public Integer getNoOfUsers() {
		return noOfUsers;
	}

	public void setNoOfUsers(Integer noOfUsers) {
		this.noOfUsers = noOfUsers;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static UserGroupSummary from(UserGroup group) {
		if (group == null) {
			return null;
		}

		UserGroupSummary result = new UserGroupSummary();
		result.setId(group.getId());
		result.setName(group.getName());
		result.setDescription(group.getDescription());
		result.setInstitute(group.getInstitute().getName());
		result.setActivityStatus(group.getActivityStatus());
		return result;
	}

	public static List<UserGroupSummary> from(Collection<UserGroup> groups) {
		return Utility.nullSafeStream(groups).map(UserGroupSummary::from).collect(Collectors.toList());
	}
}
