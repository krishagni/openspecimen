package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ServiceRateDetail {
	private Long id;

	private Long serviceId;

	private Long cpId;

	private String cpShortTitle;

	private String serviceCode;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;

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

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
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
		result.setCpId(serviceRate.getCp().getId());
		result.setCpShortTitle(serviceRate.getCp().getShortTitle());
		result.setServiceId(serviceRate.getService().getId());
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
