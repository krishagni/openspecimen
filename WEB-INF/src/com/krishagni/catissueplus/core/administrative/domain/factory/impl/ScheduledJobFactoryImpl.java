package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob.DayOfWeek;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob.RepeatSchedule;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob.Type;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ScheduledJobDetail;
import com.krishagni.catissueplus.core.administrative.services.impl.ScheduledQueryTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;

public class ScheduledJobFactoryImpl implements ScheduledJobFactory {

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	@Override
	public ScheduledJob createScheduledJob(ScheduledJobDetail detail) {
		ScheduledJob job = new ScheduledJob();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		setName(detail, job, ose);
		setRepeatSchedule(detail, job, ose);
		setStartAndEndDates(detail, job, ose);
		setActivityStatus(detail, job, ose);
		setType(detail, job, ose);
		setFixedArgs(detail, job, ose);
		setSavedQuery(detail, job, ose);
		setRunAs(detail, job, ose);
		setRecipients(detail, job, ose);
		setSharedWith(detail, job, ose);
		
		job.setRtArgsProvided(detail.getRtArgsProvided());
		job.setRtArgsHelpText(detail.getRtArgsHelpText());
			
		ose.checkAndThrow();
		return job;
	}
	
	private void setName(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		String name = detail.getName();
		if (StringUtils.isBlank(name)) {
			ose.addError(ScheduledJobErrorCode.NAME_REQUIRED);
			return;
		}
		
		job.setName(name);
	}

	private void setStartAndEndDates(ScheduledJobDetail detail,	ScheduledJob job, OpenSpecimenException ose) {
		if (detail.getStartDate() == null) {
			detail.setStartDate(Calendar.getInstance().getTime());
		}
		
		if (detail.getEndDate() != null && detail.getEndDate().before(detail.getStartDate())) {
			ose.addError(ScheduledJobErrorCode.END_DATE_BEFORE_START_DATE);
			return;
		}
		
		job.setStartDate(detail.getStartDate());
		job.setEndDate(detail.getEndDate());
		
	}
	
	private void setActivityStatus(ScheduledJobDetail detail, ScheduledJob job,	OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}

		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}

		job.setActivityStatus(activityStatus);
	}
	
	private void setRepeatSchedule(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		String repeatSchedule = detail.getRepeatSchedule();
		if (StringUtils.isBlank(repeatSchedule)) {
			ose.addError(ScheduledJobErrorCode.REPEAT_SCHEDULE_REQUIRED);
			return;
		}
		
		try {
			job.setRepeatSchedule(RepeatSchedule.valueOf(detail.getRepeatSchedule()));
		} catch (IllegalArgumentException ile) {
			ose.addError(ScheduledJobErrorCode.INVALID_REPEAT_SCHEDULE, detail.getRepeatSchedule());
			return;
		}
		
		if (job.getRepeatSchedule().equals(RepeatSchedule.ONDEMAND)) {
			job.setScheduledMinute(0);
			return;
		}
		
		setScheduledMinute(detail, job, ose);
		setScheduledHour(detail, job, ose);
		setScheduledDayOfWeek(detail, job, ose);
		setScheduledDayOfMonth(detail, job, ose);
		setHourlyInterval(detail, job, ose);
		setMinutelyInterval(detail, job, ose);
	}

	private void setScheduledMinute(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		Integer scheduledMinute = detail.getScheduledMinute();
		if (job.getRepeatSchedule() == RepeatSchedule.MINUTELY) {
			job.setScheduledMinute(0);
			return;
		}

		if (scheduledMinute == null || scheduledMinute < 0 || scheduledMinute > 59) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		}
		
		job.setScheduledMinute(scheduledMinute);
	}
	
	private void setScheduledHour(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getRepeatSchedule() == RepeatSchedule.MINUTELY || job.getRepeatSchedule() == RepeatSchedule.HOURLY) {
			return;
		}
		
		Integer scheduledHour = detail.getScheduledHour();
		if (scheduledHour == null || scheduledHour < 0 || scheduledHour > 23) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		}
		
		job.setScheduledHour(detail.getScheduledHour());
	}
	
	private void setScheduledDayOfWeek(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getRepeatSchedule() != RepeatSchedule.WEEKLY) {
			return;
		}
		
		DayOfWeek dow = null;
		try {
			dow = DayOfWeek.valueOf(detail.getScheduledDayOfWeek());
		} catch (Exception e) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		} 
	
		job.setScheduledDayOfWeek(dow);
	}
	
	private void setScheduledDayOfMonth(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getRepeatSchedule() != RepeatSchedule.MONTHLY) {
			return;
		}
		
		Integer dayOfMonth = detail.getScheduledDayOfMonth();

		if (dayOfMonth == null || dayOfMonth < 1 || dayOfMonth > 31) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		}
	
		job.setScheduledDayOfMonth(dayOfMonth);
	}

	private void setHourlyInterval(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getRepeatSchedule() != RepeatSchedule.HOURLY) {
			return;
		}

		Integer hourlyInterval = detail.getHourlyInterval();
		if (hourlyInterval == null) {
			hourlyInterval = 1;
		}

		if (hourlyInterval <= 0) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		}

		job.setHourlyInterval(hourlyInterval);
	}

	private void setMinutelyInterval(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getRepeatSchedule() != RepeatSchedule.MINUTELY) {
			return;
		}

		Integer minutelyInterval = detail.getMinutelyInterval();
		if (minutelyInterval == null) {
			minutelyInterval = 1;
		}

		if (minutelyInterval < 0) {
			ose.addError(ScheduledJobErrorCode.INVALID_SCHEDULED_TIME);
			return;
		}

		job.setMinutelyInterval(minutelyInterval);
	}
	
	private void setType(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		Type type = null;
		try {
			type = Type.valueOf(detail.getType());
		} catch (Exception e) {
			ose.addError(ScheduledJobErrorCode.INVALID_TYPE);
			return;
		}
		
		job.setType(type);
		
		if (type == Type.EXTERNAL) {
			ose.addError(ScheduledJobErrorCode.EXT_TASK_FORBIDDEN);
		} else {
			setTaskImplFqn(detail, job, ose);
		}
	}

	private void setTaskImplFqn(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getType() == Type.QUERY) {
			job.setTaskImplfqn(ScheduledQueryTask.class.getName());
			return;
		}

		String fqn = detail.getTaskImplFqn();
		if (StringUtils.isBlank(fqn)) {
			ose.addError(ScheduledJobErrorCode.TASK_IMPL_FQN_REQUIRED);
			return;
		}
		
		try {
			Class.forName(fqn);
		} catch (Exception e) {
			ose.addError(ScheduledJobErrorCode.INVALID_TASK_IMPL_FQN);
			return;
		}
		
		job.setTaskImplfqn(fqn);
	}

	private void setFixedArgs(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		job.setFixedArgs(detail.getFixedArgs());
	}

	private void setSavedQuery(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		if (job.getType() != Type.QUERY) {
			return;
		}

		SavedQuerySummary queryDetail = detail.getSavedQuery();
		if (queryDetail == null || queryDetail.getId() == null) {
			ose.addError(ScheduledJobErrorCode.QUERY_REQ);
			return;
		}

		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryDetail.getId());
		if (query == null) {
			ose.addError(SavedQueryErrorCode.NOT_FOUND, queryDetail.getId());
			return;
		}

		job.setSavedQuery(query);
	}

	private void setRunAs(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		UserSummary runAs = detail.getRunAs();
		if (runAs == null || runAs.getId() == null) {
			return;
		}

		User user = daoFactory.getUserDao().getById(runAs.getId());
		if (user == null) {
			ose.addError(UserErrorCode.NOT_FOUND, runAs.getId());
			return;
		}

		job.setRunAs(user);
	}
	
	private void setRecipients(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		job.setRecipients(getUsers(detail.getRecipients(), ose));
	}

	private void setSharedWith(ScheduledJobDetail detail, ScheduledJob job, OpenSpecimenException ose) {
		job.setSharedWith(getUsers(detail.getSharedWith(), ose));
	}
	
	private Set<User> getUsers(List<UserSummary> inputList, OpenSpecimenException ose) {
		if (inputList == null) {
			return Collections.emptySet();
		}

		Set<Long> userIds = inputList.stream().map(UserSummary::getId).collect(Collectors.toSet());
		if (userIds.isEmpty()) {
			return Collections.emptySet();
		}

		List<User> users = daoFactory.getUserDao().getUsersByIds(userIds);
		if (users.size() != userIds.size()) {
			users.forEach(u -> userIds.remove(u.getId()));
			ose.addError(UserErrorCode.ONE_OR_MORE_NOT_FOUND, userIds);
			return null;
		}

		return new HashSet<>(users);
	}
}
