package com.krishagni.catissueplus.core.common.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.UserNotification;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserNotificationDetail;
import com.krishagni.catissueplus.core.common.repository.UserNotifsListCriteria;
import com.krishagni.catissueplus.core.common.service.NotificationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class NotificationServiceImpl implements NotificationService {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<UserNotificationDetail>> getUserNotifications(RequestEvent<UserNotifsListCriteria> req) {
		try {
			UserNotifsListCriteria crit = req.getPayload().userId(AuthUtil.getCurrentUser().getId());

			List<UserNotification> userNotifications = daoFactory.getUserNotificationDao().getUserNotifications(crit);
			return ResponseEvent.response(UserNotificationDetail.from(userNotifications));
		} catch(OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getUnreadNotificationsCount() {
		try {
			UserNotifsListCriteria crit = new UserNotifsListCriteria().userId(AuthUtil.getCurrentUser().getId());
			return ResponseEvent.response(daoFactory.getUserNotificationDao().getUnreadNotificationsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> markNotificationsAsRead(RequestEvent<Date> req) {
		try {
			Date notifsBefore = req.getPayload();
			if (notifsBefore == null) {
				notifsBefore = Calendar.getInstance().getTime();
			}

			int readNotifs = daoFactory.getUserNotificationDao().markUserNotificationsAsRead(
					AuthUtil.getCurrentUser().getId(), notifsBefore);
			return ResponseEvent.response(readNotifs);
		} catch(OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public void addNotification(Notification notification, List<User> users) {
		//
		// serves both as factory and notification persister
		//

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		if (StringUtils.isBlank(notification.getOperation())) {
			// TODO: add error operation cannot be null to ose
		}

		if (StringUtils.isBlank(notification.getEntityType())) {
			// TODO: add the error entity type cannot be null to ose
		}

		if (notification.getEntityId() == null) {
			// TODO: add error entity ID cannot be null to ose
		}

		if (StringUtils.isBlank(notification.getMessage())) {
			// TODO: add error message cannot be null to ose
		}

		ose.checkAndThrow();

		notification.setCreatedBy(AuthUtil.getCurrentUser());
		notification.setCreationTime(Calendar.getInstance().getTime());

		Set<UserNotification> notifiedUsers = users.stream()
			.filter(user -> !user.isContact())
			.map(user -> {
				UserNotification un = new UserNotification();
				un.setNotification(notification);
				un.setUser(user);
				un.setStatus(UserNotification.Status.UNREAD);
				return un;
			})
			.collect(Collectors.toSet());

		if (!notifiedUsers.isEmpty()) {
			notification.setNotifiedUsers(notifiedUsers);
			daoFactory.getUserNotificationDao().saveNotification(notification);
		}
	}
}
