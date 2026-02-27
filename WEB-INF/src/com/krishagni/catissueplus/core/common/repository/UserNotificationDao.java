package com.krishagni.catissueplus.core.common.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.UserNotification;

public interface UserNotificationDao extends Dao<UserNotification> {
	List<UserNotification> getUserNotifications(UserNotifsListCriteria crit);

	Long getUnreadNotificationsCount(UserNotifsListCriteria crit);

	int markUserNotificationsAsRead(Long userId, Date notifsBefore);

	//
	// if notification object related methods grow, we can move them to NotificationDao
	//
	void saveOrUpdate(Notification notification);

	int deleteNotificationsOlderThan(int olderThanDay, int maxNotifs);
}
