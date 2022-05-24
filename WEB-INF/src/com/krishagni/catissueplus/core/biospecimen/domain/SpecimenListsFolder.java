package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenListsFolder extends BaseEntity {
	private String name;

	private String description;

	private User owner;

	private Set<UserGroup> userGroups = new HashSet<>();

	private Set<SpecimenList> lists = new HashSet<>();

	private String activityStatus;

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

	public User getOwner() {
		return owner;
	}

	public void setOwner(User owner) {
		this.owner = owner;
	}

	public Set<UserGroup> getUserGroups() {
		return userGroups;
	}

	public void setUserGroups(Set<UserGroup> userGroups) {
		this.userGroups = userGroups;
	}

	public Set<SpecimenList> getLists() {
		return lists;
	}

	public void setLists(Set<SpecimenList> lists) {
		this.lists = lists;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(SpecimenListsFolder folder) {
		setName(folder.getName());
		setDescription(folder.getDescription());
		getUserGroups().retainAll(folder.getUserGroups());
		getUserGroups().addAll(folder.getUserGroups());
		setActivityStatus(folder.getActivityStatus());

		if (Status.isDisabledStatus(getActivityStatus())) {
			delete();
		}
	}

	public void delete() {
		setName(Utility.getDisabledValue(getName(), 128));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
}
