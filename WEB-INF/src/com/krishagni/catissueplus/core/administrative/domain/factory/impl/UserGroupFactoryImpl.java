package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupFactory;
import com.krishagni.catissueplus.core.administrative.events.UserGroupDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class UserGroupFactoryImpl implements UserGroupFactory {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public UserGroup createGroup(UserGroupDetail input) {
		return createGroup(input, null);
	}

	@Override
	public UserGroup createGroup(UserGroupDetail input, UserGroup existing) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		UserGroup group = new UserGroup();
		group.setId(existing != null ? existing.getId() : input.getId());
		setName(input, existing, group, ose);
		setDescription(input, existing, group, ose);
		setInstitute(input, existing, group, ose);
		setActivityStatus(input, existing, group, ose);
		setUsers(input, existing, group, ose);

		ose.checkAndThrow();
		return group;
	}

	@Override
	public Set<User> getUsers(List<UserSummary> inputUsers) {
		return Utility.nullSafeStream(inputUsers).map(this::getUser).collect(Collectors.toSet());
	}

	private void setName(UserGroupDetail input, UserGroup existing, UserGroup group, OpenSpecimenException ose) {
		if (existing == null || input.isAttrModified("name")) {
			setName(input, group, ose);
		} else {
			group.setName(existing.getName());
		}
	}

	private void setName(UserGroupDetail input, UserGroup group, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(UserGroupErrorCode.NAME_REQ);
		}

		group.setName(input.getName());
	}

	private void setDescription(UserGroupDetail input, UserGroup existing, UserGroup group, OpenSpecimenException ose) {
		if (existing == null || input.isAttrModified("description")) {
			setDescription(input, group, ose);
		} else {
			group.setDescription(existing.getDescription());
		}
	}

	private void setDescription(UserGroupDetail input, UserGroup group, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getDescription())) {
			ose.addError(UserGroupErrorCode.DESC_REQ);
		}

		group.setDescription(input.getDescription());
	}

	private void setInstitute(UserGroupDetail input, UserGroup existing, UserGroup group, OpenSpecimenException ose) {
		if (existing == null || input.isAttrModified("institute")) {
			setInstitute(input, group, ose);
		} else {
			group.setInstitute(existing.getInstitute());
		}
	}

	private void setInstitute(UserGroupDetail input, UserGroup group, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getInstitute())) {
			ose.addError(UserGroupErrorCode.INST_REQ);
			return;
		}

		Institute institute = daoFactory.getInstituteDao().getInstituteByName(input.getInstitute());
		if (institute == null) {
			ose.addError(InstituteErrorCode.NOT_FOUND, input.getInstitute());
		}

		group.setInstitute(institute);
	}

	private void setActivityStatus(UserGroupDetail input, UserGroup existing, UserGroup group, OpenSpecimenException ose) {
		if (existing == null || input.isAttrModified("activityStatus")) {
			setActivityStatus(input, group, ose);
		} else {
			group.setActivityStatus(existing.getActivityStatus());
		}
	}

	private void setActivityStatus(UserGroupDetail input, UserGroup group, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			group.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}

		if (!Status.isValidActivityStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		}

		group.setActivityStatus(input.getActivityStatus());
	}

	private void setUsers(UserGroupDetail input, UserGroup existing, UserGroup group, OpenSpecimenException ose) {
		if (existing == null || input.isAttrModified("users")) {
			setUsers(input, group, ose);
		} else {
			group.setUsers(existing.getUsers());
			ensureUsersOfSameInstitute(group.getInstitute(), group.getUsers(), ose);
		}
	}

	private void setUsers(UserGroupDetail input, UserGroup group, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(input.getUsers()) || group.getInstitute() == null) {
			return;
		}

		Set<User> users = new HashSet<>();
		for (UserSummary user : input.getUsers()) {
			users.add(getUser(user));
		}

		group.setUsers(users);
		ensureUsersOfSameInstitute(group.getInstitute(), group.getUsers(), ose);
	}

	private User getUser(UserSummary input) {
		User user = null;
		Object key = null;

		if (input.getId() != null) {
			user = daoFactory.getUserDao().getById(input.getId());
			key = input.getId();
		} else if (StringUtils.isNotBlank(input.getEmailAddress())) {
			user = daoFactory.getUserDao().getUserByEmailAddress(input.getEmailAddress());
			key = input.getEmailAddress();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(UserErrorCode.EMAIL_REQUIRED);
		} else if (user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.ONE_OR_MORE_NOT_FOUND, key);
		}

		return user;
	}

	private void ensureUsersOfSameInstitute(Institute institute, Collection<User> users, OpenSpecimenException ose) {
		if (institute == null || users == null) {
			return;
		}

		User diffInst = users.stream()
			.filter(u -> !u.getInstitute().equals(institute))
			.findFirst().orElse(null);

		if (diffInst != null) {
			ose.addError(UserGroupErrorCode.USER_NOT_OF_INST, diffInst.formattedName(), institute.getName());
		}
	}
}
