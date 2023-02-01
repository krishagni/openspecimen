package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ScheduledContainerActivityDetail {
	private Long id;

	private String name;

	private Long containerId;

	private String containerName;

	private Date startDate;

	private Integer cycleInterval;

	private IntervalUnit cycleIntervalUnit;

	private Long taskId;

	private String taskName;

	private Integer reminderInterval;

	private IntervalUnit reminderIntervalUnit;

	private boolean disableReminders;

	private boolean repeatCycle;

	private List<UserSummary> assignedUsers;

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

	public Long getContainerId() {
		return containerId;
	}

	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
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

	public Long getTaskId() {
		return taskId;
	}

	public void setTaskId(Long taskId) {
		this.taskId = taskId;
	}

	public String getTaskName() {
		return taskName;
	}

	public void setTaskName(String taskName) {
		this.taskName = taskName;
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

	public List<UserSummary> getAssignedUsers() {
		return assignedUsers;
	}

	public void setAssignedUsers(List<UserSummary> assignedUsers) {
		this.assignedUsers = assignedUsers;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static ScheduledContainerActivityDetail from(ScheduledContainerActivity activity) {
		ScheduledContainerActivityDetail result = new ScheduledContainerActivityDetail();
		result.setId(activity.getId());
		result.setName(activity.getName());
		result.setContainerId(activity.getContainer().getId());
		result.setContainerName(activity.getContainer().getName());
		result.setStartDate(activity.getStartDate());
		result.setCycleInterval(activity.getCycleInterval());
		result.setCycleIntervalUnit(activity.getCycleIntervalUnit());
		result.setTaskId(activity.getTask().getId());
		result.setTaskName(activity.getTask().getName());
		result.setReminderInterval(activity.getReminderInterval());
		result.setReminderIntervalUnit(activity.getReminderIntervalUnit());
		result.setDisableReminders(activity.isDisableReminders());
		result.setRepeatCycle(activity.isRepeatCycle());
		result.setAssignedUsers(UserSummary.from(activity.getAssignedUsers()));
		result.setActivityStatus(activity.getActivityStatus());
		return result;
	}

	public static List<ScheduledContainerActivityDetail> from(Collection<ScheduledContainerActivity> activities) {
		return Utility.nullSafeStream(activities).map(ScheduledContainerActivityDetail::from).collect(Collectors.toList());
	}
}
