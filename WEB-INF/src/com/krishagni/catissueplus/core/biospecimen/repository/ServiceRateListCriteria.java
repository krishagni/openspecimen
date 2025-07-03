package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ServiceRateListCriteria extends AbstractListCriteria<ServiceRateListCriteria> {
	private Date startDate;

	private Date endDate;

	private Long serviceId;

	@Override
	public ServiceRateListCriteria self() {
		return this;
	}

	public Date startDate() {
		return startDate;
	}

	public ServiceRateListCriteria startDate(Date startDate) {
		this.startDate = startDate;
		return self();
	}

	public Date endDate() {
		return endDate;
	}

	public ServiceRateListCriteria endDate(Date endDate) {
		this.endDate = endDate;
		return self();
	}

	public Long serviceId() {
		return serviceId;
	}

	public ServiceRateListCriteria serviceId(Long serviceId) {
		this.serviceId = serviceId;
		return self();
	}
}
