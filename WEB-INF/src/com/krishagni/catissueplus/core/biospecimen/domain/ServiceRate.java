package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.Date;

import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ServiceRate extends BaseEntity {
	private Service service;

	private Date startDate;

	private Date endDate;

	private BigDecimal rate;

	private String activityStatus;

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String intervalString() {
		return Utility.getDateString(startDate) + " - " + (endDate != null ? Utility.getDateString(endDate) : "EOL");
	}

	public void update(ServiceRate other) {
		setStartDate(other.getStartDate());
		setEndDate(other.getEndDate());
		setRate(other.getRate());
		setActivityStatus(other.getActivityStatus());
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
}
