package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class UserGroup extends BaseEntity {
	private String name;

	private String description;

	private Institute institute;

	private String activityStatus;

	private Set<User> users = new HashSet<>();

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

	public Institute getInstitute() {
		return institute;
	}

	public void setInstitute(Institute institute) {
		this.institute = institute;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Set<User> getUsers() {
		return users;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	public void update(UserGroup other) {
		setName(other.getName());
		setDescription(other.getDescription());
		setInstitute(other.getInstitute());
		setUsers(other.getUsers());
		setActivityStatus(other.getActivityStatus());

		if (Status.isDisabledStatus(other.getActivityStatus())) {
			setName(Utility.getDisabledValue(getName(), 64));
		}
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		setName(Utility.getDisabledValue(getName(), 64));
	}

	public int addUsers(Collection<User> users) {
		int count = 0;
		for (User user : users) {
			if (!user.getInstitute().equals(getInstitute())) {
				throw OpenSpecimenException.userError(UserGroupErrorCode.USER_NOT_OF_INST, user.formattedName(), getInstitute().getName());
			}

			if (getUsers().add(user)) {
				++count;
			}
		}

		return count;
	}

	public int removeUsers(Collection<User> users) {
		int count = 0;
		for (User user : users) {
			if (getUsers().remove(user)) {
				++count;
			}
		}

		return count;
	}

	public boolean isDeleted() {
		return Status.isDisabledStatus(getActivityStatus());
	}
}
