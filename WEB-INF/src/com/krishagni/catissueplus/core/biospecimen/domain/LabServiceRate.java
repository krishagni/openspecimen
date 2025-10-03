package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;

import org.hibernate.envers.Audited;

@Audited
public class LabServiceRate extends BaseEntity {
	private LabServicesRateList rateList;

	private LabService service;

	private BigDecimal rate;

	public LabServicesRateList getRateList() {
		return rateList;
	}

	public void setRateList(LabServicesRateList rateList) {
		this.rateList = rateList;
	}

	public LabService getService() {
		return service;
	}

	public void setService(LabService service) {
		this.service = service;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}
}
