package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.UserNotification;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserNotificationDetail {
	private Long id;

	private String operation;

	private String entityType;

	private Long entityId;

	private String message;

	private UserSummary createdBy;

	private Date creationTime;

	private UserSummary user;

	private String urlKey;
	
	private String status;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getOperation() {
		return operation;
	}

	public void setOperation(String operation) {
		this.operation = operation;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public String getUrlKey() {
		return urlKey;
	}

	public void setUrlKey(String urlKey) {
		this.urlKey = urlKey;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public static UserNotificationDetail from(UserNotification userNotif) {
		Notification notif = userNotif.getNotification();

		UserNotificationDetail detail = new UserNotificationDetail();
		detail.setId(userNotif.getId());
		detail.setOperation(notif.getOperation());
		detail.setEntityId(notif.getEntityId());
		detail.setEntityType(notif.getEntityType());
		detail.setMessage(notif.getMessage());
		detail.setCreatedBy(UserSummary.from(notif.getCreatedBy()));
		detail.setCreationTime(notif.getCreationTime());
		detail.setUser(UserSummary.from(userNotif.getUser()));
		detail.setUrlKey(userNotif.getUrlKey());
		detail.setStatus(userNotif.getStatus().name());
		return detail;
	}

	public static List<UserNotificationDetail> from(Collection<UserNotification> userNotifications) {
		return userNotifications.stream().map(UserNotificationDetail::from).collect(Collectors.toList());
	}
}