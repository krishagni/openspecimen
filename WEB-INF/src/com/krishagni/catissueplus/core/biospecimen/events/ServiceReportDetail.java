package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;

public class ServiceReportDetail {
	private String code;

	private String description;

	private long specimens;

	private BigDecimal rate;

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

	public long getSpecimens() {
		return specimens;
	}

	public void setSpecimens(long specimens) {
		this.specimens = specimens;
	}

	public BigDecimal getRate() {
		return rate;
	}

	public void setRate(BigDecimal rate) {
		this.rate = rate;
	}
}
