package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.Set;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.util.Status;

@Audited
public class ScheduledContainerActivity extends BaseEntity {
	private String name;

	private StorageContainer container;

	private Date startDate;

	private Integer cycleInterval;

	private IntervalUnit cycleIntervalUnit;

	private ContainerTask task;

	private Integer reminderInterval;

	private IntervalUnit reminderIntervalUnit;

	private boolean disableReminders;

	private boolean repeatCycle;

	private Set<User> assignedUsers;

	private String activityStatus;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public StorageContainer getContainer() {
		return container;
	}

	public void setContainer(StorageContainer container) {
		this.container = container;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Integer getCycleInterval() {
		return cycleInterval;
	}

	public void setCycleInterval(Integer cycleInterval) {
		this.cycleInterval = cycleInterval;
	}

	public IntervalUnit getCycleIntervalUnit() {
		return cycleIntervalUnit;
	}

	public void setCycleIntervalUnit(IntervalUnit cycleIntervalUnit) {
		this.cycleIntervalUnit = cycleIntervalUnit;
	}

	public ContainerTask getTask() {
		return task;
	}

	public void setTask(ContainerTask task) {
		this.task = task;
	}

	public Integer getReminderInterval() {
		return reminderInterval;
	}

	public void setReminderInterval(Integer reminderInterval) {
		this.reminderInterval = reminderInterval;
	}

	public IntervalUnit getReminderIntervalUnit() {
		return reminderIntervalUnit;
	}

	public void setReminderIntervalUnit(IntervalUnit reminderIntervalUnit) {
		this.reminderIntervalUnit = reminderIntervalUnit;
	}

	public boolean isDisableReminders() {
		return disableReminders;
	}

	public void setDisableReminders(boolean disableReminders) {
		this.disableReminders = disableReminders;
	}

	public boolean isRepeatCycle() {
		return repeatCycle;
	}

	public void setRepeatCycle(boolean repeatCycle) {
		this.repeatCycle = repeatCycle;
	}

	public Set<User> getAssignedUsers() {
		return assignedUsers;
	}

	public void setAssignedUsers(Set<User> assignedUsers) {
		this.assignedUsers = assignedUsers;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(ScheduledContainerActivity other) {
		setName(other.getName());
		setStartDate(other.getStartDate());
		setCycleInterval(other.getCycleInterval());
		setCycleIntervalUnit(other.getCycleIntervalUnit());
		setReminderInterval(other.getReminderInterval());
		setReminderIntervalUnit(other.getReminderIntervalUnit());
		setRepeatCycle(other.isRepeatCycle());
		setAssignedUsers(other.getAssignedUsers());
		setActivityStatus(other.getActivityStatus());

		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus())) {
			setName(getName() + "_" + Calendar.getInstance().getTimeInMillis());
		}
	}
}
