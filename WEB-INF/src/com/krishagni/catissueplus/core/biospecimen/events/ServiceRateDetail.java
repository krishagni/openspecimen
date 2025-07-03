package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ServiceRateDetail {
	private Long id;

	private Long serviceId;

	private Long cpId;

	private String cpShortTitle;

	private String serviceCode;

	private Date startDate;

	private Date endDate;

	private BigDecimal rate;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getServiceId() {
		return serviceId;
	}

	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
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

	public static ServiceRateDetail from(ServiceRate serviceRate) {
		ServiceRateDetail result = new ServiceRateDetail();
		result.setId(serviceRate.getId());
		result.setCpShortTitle(serviceRate.getService().getCp().getShortTitle());
		result.setServiceCode(serviceRate.getService().getCode());
		result.setStartDate(serviceRate.getStartDate());
		result.setEndDate(serviceRate.getEndDate());
		result.setRate(serviceRate.getRate());
		result.setActivityStatus(serviceRate.getActivityStatus());
		return result;
	}

	public static List<ServiceRateDetail> from(Collection<ServiceRate> rates) {
		return Utility.nullSafeStream(rates).map(ServiceRateDetail::from).collect(Collectors.toList());
	}
}
