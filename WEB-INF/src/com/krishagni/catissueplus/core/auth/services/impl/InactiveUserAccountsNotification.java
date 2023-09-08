package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class InactiveUserAccountsNotification implements ScheduledTask {
	@Autowired
	private DaoFactory daoFactory;
	
	@Autowired
	private EmailService emailSvc;
	
	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		int inactiveDays = ConfigUtil.getInstance().getIntSetting("auth", "account_inactive_days", 0);
		if (inactiveDays <= 0) {
			return;
		}

		inactiveDays -= 5;
		if (inactiveDays < 0) {
			inactiveDays = 0;
		}

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -inactiveDays);
		Date cutOffDate = Utility.chopTime(cal.getTime());

		List<User> users = daoFactory.getUserDao().getInactiveUsers(cutOffDate);
		if (CollectionUtils.isEmpty(users)) {
			return;
		}

		Map<Long, Date> latestLoginTimes = daoFactory.getUserDao().getLatestLoginTime(users.stream().map(User::getId).toList());

		List<User> lockUsers = new ArrayList<>();
		for (User user : users) {
			Date loginTime = latestLoginTimes.get(user.getId());
			if (loginTime == null) {
				loginTime = user.getCreationDate();
			}

			int daysToLock = 5 - Utility.daysBetween(Utility.chopTime(loginTime), cutOffDate);
			if (daysToLock <= 0) {
				lockUsers.add(user);
			} else {
				Calendar lockTime = Calendar.getInstance();
				lockTime.add(Calendar.DAY_OF_MONTH, daysToLock);
				sendAccountLockWarningNotif(user, loginTime, lockTime.getTime());
			}
		}

		if (lockUsers.isEmpty()) {
			return;
		}

		daoFactory.getUserDao().updateStatus(lockUsers, Status.ACTIVITY_STATUS_LOCKED.getStatus());
		lockUsers.forEach(this::sendAccountLockedNotif);
	}

	private void sendAccountLockedNotif(User user) {
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("ignoreDnd", true);
		String[] rcpts = { user.getEmailAddress() };
		emailSvc.sendEmail(ACCOUNT_LOCKED_NOTIF, rcpts, props);
	}

	private void sendAccountLockWarningNotif(User user, Date lastLoginTime, Date lockTime) {
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("lastLoginTime", lastLoginTime);
		props.put("lockTime", lockTime);
		props.put("ignoreDnd", true);
		String[] rcpts = { user.getEmailAddress() };
		emailSvc.sendEmail(INACTIVE_ACCOUNT_WARN_NOTIF, rcpts, props);
	}

	private static final String ACCOUNT_LOCKED_NOTIF = "inactive_accounts_notification";

	private static final String INACTIVE_ACCOUNT_WARN_NOTIF = "inactive_account_warning_notification";
}
