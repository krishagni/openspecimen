package com.krishagni.catissueplus.core.common.events;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class EntityStatusDetail {
	private Long id;
	
	private String name;

	private String status;

	private UserSummary user;

	private Date date;
	
	private String reason;

	private String comments;

	private boolean forceUpdate;

	private List<UserSummary> notifyUsers;

	private Map<String, Object> extraAttrs;

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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public boolean isForceUpdate() {
		return forceUpdate;
	}

	public void setForceUpdate(boolean forceUpdate) {
		this.forceUpdate = forceUpdate;
	}

	public List<UserSummary> getNotifyUsers() {
		return notifyUsers;
	}

	public void setNotifyUsers(List<UserSummary> notifyUsers) {
		this.notifyUsers = notifyUsers;
	}

	public Map<String, Object> getExtraAttrs() {
		return extraAttrs;
	}

	public void setExtraAttrs(Map<String, Object> extraAttrs) {
		this.extraAttrs = extraAttrs;
	}
}
