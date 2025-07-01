package com.krishagni.catissueplus.core.biospecimen.domain;

public class Service extends BaseEntity {
	private String code;

	private String description;

	private CollectionProtocol cp;

	private String activityStatus;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}
}
