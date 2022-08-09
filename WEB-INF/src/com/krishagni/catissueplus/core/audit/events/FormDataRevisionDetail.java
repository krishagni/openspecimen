package com.krishagni.catissueplus.core.audit.events;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class FormDataRevisionDetail {
	private Long id;

	private Date time;

	private String ipAddress;

	private UserSummary user;

	private String op;

	private String entityType;

	private String formName;

	private Long entityId;

	private Long recordId;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public String getOp() {
		return op;
	}

	public void setOp(String op) {
		this.op = op;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getFormName() {
		return formName;
	}

	public void setFormName(String formName) {
		this.formName = formName;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}
}
