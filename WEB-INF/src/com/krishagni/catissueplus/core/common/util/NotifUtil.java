package com.krishagni.catissueplus.core.common.util;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.UserNotification;

@Configurable
public class NotifUtil {
	private static final NotifUtil instance = new NotifUtil();

	@Autowired
	private DaoFactory daoFactory;

	public static NotifUtil getInstance() {
		return instance;
	}

	public void notify(Notification notif, Map<String, Collection<User>> urlKeyUsersMap) {
		Set<UserNotification> userNotifs = new HashSet<>();
		urlKeyUsersMap.forEach((urlKey, users) ->
			users.forEach(user -> {
				if (user.isContact()) {
					return;
				}

				UserNotification userNotif = new UserNotification();
				userNotif.setUrlKey(urlKey);
				userNotif.setUser(user);
				userNotif.setNotification(notif);
				userNotif.setStatus(UserNotification.Status.UNREAD);
				userNotifs.add(userNotif);
			})
		);

		if (!userNotifs.isEmpty()) {
			notif.setNotifiedUsers(userNotifs);
			daoFactory.getUserNotificationDao().saveNotification(notif);
		}
	}
}