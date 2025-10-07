package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRate;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabServiceRateDetail {
	private Long id;

	private Long serviceId;

	private String serviceCode;

	private String serviceDescription;

	private BigDecimal rate;

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

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}

	public static LabServiceRateDetail from(LabServiceRate svcRate) {
		LabServiceRateDetail result = new LabServiceRateDetail();
		result.setId(svcRate.getId());
		result.setServiceId(svcRate.getService().getId());
		result.setServiceCode(svcRate.getService().getCode());
		result.setServiceDescription(svcRate.getService().getDescription());
		result.setRate(svcRate.getRate());
		return result;
	}

	public static List<LabServiceRateDetail> from(Collection<LabServiceRate> svcRates) {
		return Utility.nullSafeStream(svcRates).map(LabServiceRateDetail::from).collect(Collectors.toList());
	}
}
