package com.krishagni.catissueplus.core.administrative.services.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class ContainerMaintenanceReminderTask implements ScheduledTask {
	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		boolean endOfScheduledActivities = false;
		int startAt = 0;
		while (!endOfScheduledActivities) {
			int activities = sendReminders(startAt);
			startAt += activities;
			endOfScheduledActivities = (activities < 100);
		}
	}

	@PlusTransactional
	private int sendReminders(int startAt) {
		List<ScheduledContainerActivity> schedActivities = getScheduledActivities(startAt);
		Map<Long, Date> latestActivityDates = getLatestActivityDates(schedActivities);
		for (ScheduledContainerActivity activity : schedActivities) {
			if (activity.isDisableReminders()) {
				continue;
			}

			sendReminder(activity, latestActivityDates.get(activity.getId()));
		}

		return schedActivities.size();
	}

	private List<ScheduledContainerActivity> getScheduledActivities(int startAt) {
		ScheduledContainerActivityListCriteria crit = new ScheduledContainerActivityListCriteria().startAt(startAt).activityStatus("Active");
		return daoFactory.getScheduledContainerActivityDao().getActivities(crit);
	}

	private Map<Long, Date> getLatestActivityDates(List<ScheduledContainerActivity> activities) {
		List<Long> activityIds = activities.stream().map(ScheduledContainerActivity::getId).collect(Collectors.toList());
		return daoFactory.getContainerActivityLogDao().getLatestScheduledActivityDate(activityIds);
	}

	private void sendReminder(ScheduledContainerActivity activity, Date lastActivityDate) {
		Date cycleStartDate = activity.getStartDate();
		if (lastActivityDate != null && lastActivityDate.after(cycleStartDate)) {
			cycleStartDate = lastActivityDate;
		}

		Date currentTime = Calendar.getInstance().getTime();
		Date nextCycleDate = null;
		while (nextCycleDate == null || nextCycleDate.before(currentTime)) {
			nextCycleDate = addInterval(cycleStartDate, activity.getCycleInterval(), activity.getCycleIntervalUnit());
			cycleStartDate = nextCycleDate;
		}

		nextCycleDate = Utility.chopTime(nextCycleDate);
		Date remStartDate = addInterval(nextCycleDate, -1 * activity.getReminderInterval(), activity.getReminderIntervalUnit());
		if (Utility.chopTime(remStartDate).before(currentTime)) {
			sendReminder(activity, lastActivityDate, nextCycleDate);
		}
	}

	private void sendReminder(ScheduledContainerActivity activity, Date lastActivityDate, Date nextCycleDate) {
		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] { activity.getContainer().getName(), activity.getName() });
		props.put("container", activity.getContainer());
		props.put("activity", activity);
		props.put("task", activity.getTask());
		props.put("lastActivityDate", lastActivityDate != null ? Utility.getDateString(lastActivityDate) : null);
		props.put("nextCycleDate", Utility.getDateString(nextCycleDate));

		for (User user : activity.getAssignedUsers()) {
			props.put("rcpt", user);

			String[] to = {user.getEmailAddress()};
			EmailUtil.getInstance().sendEmail(REM_EMAIL_TMPL, to, null, props);
		}
	}

	private Date addInterval(Date refDate, int interval, IntervalUnit intervalUnit) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(refDate);

		switch (intervalUnit) {
			case DAYS:
				cal.add(Calendar.DATE, interval);
				break;

			case WEEKS:
				cal.add(Calendar.DATE, interval * 7);
				break;

			case MONTHS:
				cal.add(Calendar.MONTH, interval);
				break;

			case YEARS:
				cal.add(Calendar.YEAR, interval);
				break;
		}

		return cal.getTime();
	}

	private static final String REM_EMAIL_TMPL = "container_maintenance_reminder";
}
