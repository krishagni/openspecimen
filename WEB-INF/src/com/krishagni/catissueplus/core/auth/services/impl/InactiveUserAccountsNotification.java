package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Calendar;
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

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.DATE, -inactiveDays);
		List<User> users = daoFactory.getUserDao().getInactiveUsers(Utility.chopTime(cal.getTime()));
		if (CollectionUtils.isEmpty(users)) {
			return;
		}

		daoFactory.getUserDao().updateStatus(users, Status.ACTIVITY_STATUS_LOCKED.getStatus());
		users.forEach(this::sendAccountLockedNotif);
	}

	private void sendAccountLockedNotif(User user) {
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("ignoreDnd", true);
		String[] rcpts = {user.getEmailAddress()};
		emailSvc.sendEmail(ACCOUNT_LOCKED_NOTIF, rcpts, props);
	}

	private static final String ACCOUNT_LOCKED_NOTIF = "inactive_accounts_notification";
}
