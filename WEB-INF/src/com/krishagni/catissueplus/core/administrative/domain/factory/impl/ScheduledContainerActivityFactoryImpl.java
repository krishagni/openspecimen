package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerTask;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerActivityErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTaskErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledContainerActivityFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ScheduledContainerActivityDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Status;

public class ScheduledContainerActivityFactoryImpl implements ScheduledContainerActivityFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ScheduledContainerActivity createActivity(ScheduledContainerActivityDetail input) {
		ScheduledContainerActivity activity = new ScheduledContainerActivity();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setName(input, activity, ose);
		setContainer(input, activity, ose);
		setStartDate(input, activity, ose);
		setCycleInterval(input, activity, ose);
		setContainerTask(input, activity, ose);
		setReminderInterval(input, activity, ose);
		setRepeatCycle(input, activity, ose);
		setAssignedto(input, activity, ose);
		setActivityStatus(input, activity, ose);
		ose.checkAndThrow();
		return activity;
	}

	private void setName(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(ContainerActivityErrorCode.NAME_REQ);
		}

		activity.setName(input.getName());
	}

	private void setContainer(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		StorageContainer container = null;
		Object key = null;

		if (input.getContainerId() != null) {
			container = daoFactory.getStorageContainerDao().getById(input.getContainerId());
			key = input.getContainerId();
		} else if (StringUtils.isNotBlank(input.getContainerName())) {
			container = daoFactory.getStorageContainerDao().getByName(input.getContainerName());
			key = input.getContainerName();
		}

		if (key == null) {
			ose.addError(StorageContainerErrorCode.NAME_REQUIRED);
		} else if (container == null) {
			ose.addError(StorageContainerErrorCode.NOT_FOUND, key, 1);
		} else if (container.getParentContainer() != null) {
			ose.addError(ContainerActivityErrorCode.TL_CONT_REQ, container.getName());
		}

		activity.setContainer(container);
	}

	private void setStartDate(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		Date now = Calendar.getInstance().getTime();

		if (input.getStartDate() == null) {
			ose.addError(ContainerActivityErrorCode.ST_DATE_REQ);
		}

		activity.setStartDate(input.getStartDate());
	}

	private void setCycleInterval(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		if (input.getCycleInterval() == null) {
			ose.addError(ContainerActivityErrorCode.CYCLE_INT_REQ);
		} else if (input.getCycleInterval() <= 0) {
			ose.addError(ContainerActivityErrorCode.INVALID_CYCLE_INT, input.getCycleInterval());
		}

		activity.setCycleInterval(input.getCycleInterval());

		if (input.getCycleIntervalUnit() == null) {
			ose.addError(ContainerActivityErrorCode.CYCLE_INT_UNIT_REQ);
		}

		activity.setCycleIntervalUnit(input.getCycleIntervalUnit());
	}

	private void setContainerTask(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		ContainerTask task = null;
		Object key = null;

		if (input.getTaskId() != null) {
			task = daoFactory.getContainerTaskDao().getById(input.getTaskId());
			key = input.getTaskId();
		} else if (StringUtils.isNotBlank(input.getTaskName())) {
			task = daoFactory.getContainerTaskDao().getByName(input.getTaskName());
			key = input.getTaskName();
		}

		if (key == null) {
			ose.addError(ContainerTaskErrorCode.NAME_REQ);
		} else if (task == null) {
			ose.addError(ContainerTaskErrorCode.NOT_FOUND, key);
		}

		activity.setTask(task);
	}

	private void setReminderInterval(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		activity.setDisableReminders(input.isDisableReminders());
		if (input.isDisableReminders()) {
			activity.setReminderInterval(-1);
			activity.setReminderIntervalUnit(IntervalUnit.DAYS);
			return;
		}

		if (input.getReminderInterval() == null) {
			ose.addError(ContainerActivityErrorCode.REM_INT_REQ);
		} else if (input.getReminderInterval() <= 0) {
			ose.addError(ContainerActivityErrorCode.INVALID_REM_INT, input.getReminderInterval());
		}

		activity.setReminderInterval(input.getReminderInterval());

		if (input.getReminderIntervalUnit() == null) {
			ose.addError(ContainerActivityErrorCode.REM_INT_UNIT_REQ);
		}

		activity.setReminderIntervalUnit(input.getReminderIntervalUnit());
	}

	private void setRepeatCycle(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		activity.setRepeatCycle(input.isRepeatCycle());
	}

	private void setAssignedto(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		if (input.getAssignedUsers() == null || input.getAssignedUsers().isEmpty()) {
			ose.addError(ContainerActivityErrorCode.ASSIGNED_TO_REQ);
			return;
		}

		Set<User> users = new HashSet<>();
		for (UserSummary assignedUser : input.getAssignedUsers()) {
			User user = null;
			Object key = null;

			if (assignedUser.getId() != null) {
				user = daoFactory.getUserDao().getById(assignedUser.getId());
				key = assignedUser.getId();
			} else if (StringUtils.isNotBlank(assignedUser.getEmailAddress())) {
				user = daoFactory.getUserDao().getUserByEmailAddress(assignedUser.getEmailAddress());
				key = assignedUser.getEmailAddress();
			} else {
				continue;
			}

			if (user == null) {
				ose.addError(UserErrorCode.NOT_FOUND, key);
				return;
			}

			users.add(user);
		}

		if (users.isEmpty()) {
			ose.addError(ContainerActivityErrorCode.ASSIGNED_TO_REQ);
		}

		activity.setAssignedUsers(users);
	}

	private void setActivityStatus(ScheduledContainerActivityDetail input, ScheduledContainerActivity activity, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			activity.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(input.getActivityStatus())) {
			activity.setActivityStatus(input.getActivityStatus());
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		}
	}
}
