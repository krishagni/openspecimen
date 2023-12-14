package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobErrorCode;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

@Audited
public class ScheduledJob extends BaseEntity {
	private static final LogUtil logger = LogUtil.getLogger(ScheduledJob.class);

	public enum RepeatSchedule { 
		MINUTELY,
		HOURLY,
		DAILY,
		WEEKLY,
		MONTHLY,
		ONDEMAND
	}
	
	public enum DayOfWeek {
		SUNDAY(1),
		MONDAY(2),
		TUESDAY(3),
		WEDNESDAY(4),
		THURSDAY(5),
		FRIDAY(6),
		SATURDAY(7);
		
		private final int value;
		
		DayOfWeek(int newValue) {
			value = newValue;
		}
		
		public int value() {
			return value;
		}
	}
	
	public enum Type {
		INTERNAL,
		EXTERNAL,
		QUERY
	}
	
	private String name;
	
	private User createdBy;
	
	private Date startDate;
	
	private Date endDate;
	
	private Integer scheduledHour;
	
	private Integer scheduledMinute;
	
	private DayOfWeek scheduledDayOfWeek;
	
	private Integer scheduledDayOfMonth;

	private Integer hourlyInterval = 1;

	private Integer minutelyInterval = 1;
	
	private String activityStatus;
	
	private RepeatSchedule repeatSchedule;
	
	private Type type;
	
	private String taskImplfqn;
	
	private String command;

	private String fixedArgs;

	private SavedQuery savedQuery;

	private User runAs;

	private String runByNode;
	
	private Set<User> recipients = new HashSet<>();

	private Set<User> sharedWith = new HashSet<>();
	
	//
	// UI purpose
	//
	private Boolean rtArgsProvided;
	
	private String rtArgsHelpText;

	private transient Date lastRunOn;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public Integer getScheduledHour() {
		return scheduledHour;
	}

	public void setScheduledHour(Integer scheduledHour) {
		this.scheduledHour = scheduledHour;
	}

	public Integer getScheduledMinute() {
		return scheduledMinute;
	}

	public void setScheduledMinute(Integer scheduledMinute) {
		this.scheduledMinute = scheduledMinute;
	}

	public DayOfWeek getScheduledDayOfWeek() {
		return scheduledDayOfWeek;
	}

	public void setScheduledDayOfWeek(DayOfWeek scheduledDayOfWeek) {
		this.scheduledDayOfWeek = scheduledDayOfWeek;
	}

	public Integer getScheduledDayOfMonth() {
		return scheduledDayOfMonth;
	}

	public void setScheduledDayOfMonth(Integer scheduledDayOfMonth) {
		this.scheduledDayOfMonth = scheduledDayOfMonth;
	}

	public Integer getHourlyInterval() {
		return hourlyInterval == null ? 1 : hourlyInterval;
	}

	public void setHourlyInterval(Integer hourlyInterval) {
		this.hourlyInterval = hourlyInterval;
	}

	public Integer getMinutelyInterval() {
		return minutelyInterval == null ? 1 : minutelyInterval;
	}

	public void setMinutelyInterval(Integer minutelyInterval) {
		this.minutelyInterval = minutelyInterval;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public RepeatSchedule getRepeatSchedule() {
		return repeatSchedule;
	}

	public void setRepeatSchedule(RepeatSchedule repeatSchedule) {
		this.repeatSchedule = repeatSchedule;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getTaskImplfqn() {
		return taskImplfqn;
	}

	public void setTaskImplfqn(String fqn) {
		this.taskImplfqn = fqn;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getFixedArgs() {
		return fixedArgs;
	}

	public void setFixedArgs(String fixedArgs) {
		this.fixedArgs = fixedArgs;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public SavedQuery getSavedQuery() {
		return savedQuery;
	}

	public void setSavedQuery(SavedQuery savedQuery) {
		this.savedQuery = savedQuery;
	}

	public User getRunAs() {
		return runAs;
	}

	public void setRunAs(User runAs) {
		this.runAs = runAs;
	}

	@NotAudited
	public String getRunByNode() {
		return runByNode;
	}

	public void setRunByNode(String runByNode) {
		this.runByNode = runByNode;
	}

	@AuditJoinTable(name = "OS_SCHED_JOBS_NOTIF_RCPTS_AUD")
	public Set<User> getRecipients() {
		return recipients;
	}

	public void setRecipients(Set<User> recipients) {
		this.recipients = recipients;
	}

	@AuditJoinTable(name = "OS_SCHED_JOB_SHARED_USERS_AUD")
	public Set<User> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(Set<User> sharedWith) {
		this.sharedWith = sharedWith;
	}

	public Boolean getRtArgsProvided() {
		return rtArgsProvided;
	}

	public void setRtArgsProvided(Boolean rtArgsProvided) {
		this.rtArgsProvided = rtArgsProvided;
	}

	public String getRtArgsHelpText() {
		return rtArgsHelpText;
	}

	public void setRtArgsHelpText(String rtArgsHelpText) {
		this.rtArgsHelpText = rtArgsHelpText;
	}

	public Date getLastRunOn() {
		return lastRunOn;
	}

	public void setLastRunOn(Date lastRunOn) {
		this.lastRunOn = lastRunOn;
	}

	public void update(ScheduledJob other) {
		BeanUtils.copyProperties(other, this, JOB_UPDATE_IGN_PROPS);
		CollectionUpdater.update(getRecipients(), other.getRecipients());
		CollectionUpdater.update(getSharedWith(), other.getSharedWith());
	}

	public boolean isOnDemand() {
		return repeatSchedule.equals(RepeatSchedule.ONDEMAND);
	}
	
	public boolean isActiveJob() {
		return isActiveJob(false);
	}

	public boolean isActiveJob(boolean printReason) {
		if (!Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(activityStatus)) {
			if (printReason) {
				logger.info("Scheduled job " + getName() + " is not active. Activity Status = " + activityStatus);
			}

			return false;
		}

		if (getType() == Type.QUERY && (getSavedQuery() == null || getSavedQuery().getDeletedOn() != null)) {
			if (printReason) {
				String title = (getSavedQuery() != null ? getSavedQuery().getTitle() : "Unknown");
				logger.info("Scheduled query " + title + " is deleted.");
			}

			return false;
		}

		return true;
	}

	public Date getNextRunOn() {
		return switch (repeatSchedule) {
			case MINUTELY -> getNextMinutelyOccurence();
			case HOURLY -> getNextHourlyOccurence();
			case DAILY -> getNextDailyOccurence();
			case MONTHLY -> getNextMonthlyOccurence();
			case WEEKLY -> getNextWeeklyOccurence();
			default -> null;
		};
	}
	
	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		setName(Utility.getDisabledValue(name, 255));
	}
	
	private Date getNextMinutelyOccurence() {
		Integer minutesToAdd = getMinutelyInterval();
		if (minutesToAdd == null || minutesToAdd <= 0) {
			minutesToAdd = 1;
		}

		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, minutesToAdd);
		return calendar.getTime();
	}
	
	private Date getNextHourlyOccurence() {
		Calendar calendar = Calendar.getInstance();
		int minutes = calendar.get(Calendar.MINUTE);
		if (minutes >= scheduledMinute) {
			calendar.add(Calendar.HOUR, getHourlyInterval());
		} 
		
		calendar.set(Calendar.MINUTE, scheduledMinute);
		return calendar.getTime();
	}
	
	private Date getNextDailyOccurence() {
		Calendar current = Calendar.getInstance();
		if (isTimeAfterScheduledTime(current)) {
			current.add(Calendar.DATE, 1);
		}
		
		current.set(Calendar.HOUR_OF_DAY, scheduledHour);
		current.set(Calendar.MINUTE, scheduledMinute);
		return current.getTime();
	}
	
	private Date getNextWeeklyOccurence() {
		int startDayOfWeek = scheduledDayOfWeek.value();
		Calendar current = Calendar.getInstance();
		int currentDayOfWeek = current.get(Calendar.DAY_OF_WEEK);
		
		if (currentDayOfWeek < startDayOfWeek) {
			int diff = startDayOfWeek - currentDayOfWeek;
			current.add(Calendar.DATE, diff);
		} else if (currentDayOfWeek == startDayOfWeek) {
			if (isTimeAfterScheduledTime(current)) {
				current.add(Calendar.DATE, 7);
			}
		} else {
			int diff = (7-currentDayOfWeek) + startDayOfWeek;
			current.add(Calendar.DATE, diff);
		}
		
		current.set(Calendar.HOUR_OF_DAY, scheduledHour);
		current.set(Calendar.MINUTE, scheduledMinute);
		return current.getTime();
	}
	
	private Date getNextMonthlyOccurence() {
		Calendar current = Calendar.getInstance();
		int currentDay = current.get(Calendar.DAY_OF_MONTH);
		
		if (currentDay < scheduledDayOfMonth) {
			int diff = scheduledDayOfMonth - currentDay;
			current.add(Calendar.DATE, diff);
		} else if (currentDay == scheduledDayOfMonth) {
			if (isTimeAfterScheduledTime(current)) {
				current.add(Calendar.MONTH, 1);
			}
		} else {
			int numOfDaysInThisMonth = current.getActualMaximum(Calendar.DAY_OF_MONTH);
			int diff = numOfDaysInThisMonth - currentDay + scheduledDayOfMonth;
			current.add(Calendar.DATE, diff);
		}
		
		current.set(Calendar.HOUR_OF_DAY, scheduledHour);
		current.set(Calendar.MINUTE, scheduledMinute);
		return current.getTime();
	}
	
	private boolean isTimeAfterScheduledTime(Calendar current) {
		int currentHour = current.get(Calendar.HOUR_OF_DAY);
		int currentMinutes = current.get(Calendar.MINUTE);
		
		if (currentHour > scheduledHour) {
			return true;
		}
		
		if (currentHour == scheduledHour && currentMinutes >= scheduledMinute) {
			return true;
		}
		
		return false;
	}
	
	public ScheduledTask newTask() {
		if (getType() == Type.EXTERNAL) {
			throw OpenSpecimenException.userError(ScheduledJobErrorCode.EXT_TASK_FORBIDDEN);
		}
		
		try {
			return (ScheduledTask) Class.forName(getTaskImplfqn()).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			logger.error("Invalid scheduled job class: " + getTaskImplfqn(), e);
			throw OpenSpecimenException.userError(ScheduledJobErrorCode.INVALID_TYPE);
		}
	}
	
	private static final String[] JOB_UPDATE_IGN_PROPS = new String[] {
		"id", 
		"createdBy",
		"recipients",
		"sharedWith"
	};
}
