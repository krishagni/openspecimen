package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListsFolderFactory;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListsFolderDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenListsFolderFactoryImpl implements SpecimenListsFolderFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public SpecimenListsFolder createFolder(SpecimenListsFolderDetail input) {
		SpecimenListsFolder folder = new SpecimenListsFolder();

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setName(input, folder, ose);
		setDescription(input, folder, ose);
		setOwner(input, folder, ose);
		setCreationTime(input, folder, ose);
		setUserGroups(input, folder, ose);
		setActivityStatus(input, folder, ose);
		ose.checkAndThrow();

		return folder;
	}

	private void setName(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(SpecimenListsFolderErrorCode.NAME_REQ);
		}

		folder.setName(input.getName());
	}

	private void setDescription(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		folder.setDescription(input.getDescription());
	}

	private void setOwner(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		folder.setOwner(AuthUtil.getCurrentUser());
	}

	private void setCreationTime(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		folder.setCreationTime(Calendar.getInstance().getTime());
	}

	private void setUserGroups(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(input.getUserGroups())) {
			return;
		}

		Set<UserGroup> userGroups = new HashSet<>();

		Set<Long> groupIds = Utility.nullSafeStream(input.getUserGroups())
			.map(UserGroupSummary::getId).filter(Objects::nonNull).collect(Collectors.toSet());
		if (!groupIds.isEmpty()) {
			List<UserGroup> groups = daoFactory.getUserGroupDao().getByIds(groupIds);
			if (groups.size() != groupIds.size()) {
				groups.forEach(group -> groupIds.remove(group.getId()));
				ose.addError(UserGroupErrorCode.NOT_FOUND, Utility.join(groupIds, Object::toString, ", "));
			}

			userGroups.addAll(groups);
		}

		Set<String> groupNames = Utility.nullSafeStream(input.getUserGroups())
			.map(UserGroupSummary::getName).filter(Objects::nonNull).collect(Collectors.toSet());
		if (!groupNames.isEmpty()) {
			List<UserGroup> groups = daoFactory.getUserGroupDao().getByNames(groupNames);
			if (groups.size() != groupNames.size()) {
				groups.forEach(group -> groupNames.remove(group.getName()));
				ose.addError(UserGroupErrorCode.NOT_FOUND, String.join(", ", groupNames));
			}

			userGroups.addAll(groups);
		}

		folder.setUserGroups(userGroups);
	}

	private void setActivityStatus(SpecimenListsFolderDetail input, SpecimenListsFolder folder, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			folder.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}

		if (!Status.isActiveStatus(input.getActivityStatus()) && !Status.isDisabledStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
			return;
		}

		folder.setActivityStatus(input.getActivityStatus());
	}
}
