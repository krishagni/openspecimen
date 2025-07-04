package com.krishagni.catissueplus.core.biospecimen.repository;

import java.time.LocalDate;
import java.util.Collection;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ServiceRateListCriteria extends AbstractListCriteria<ServiceRateListCriteria> {
	private LocalDate startDate;

	private LocalDate endDate;

	private Long serviceId;

	private Collection<Long> serviceIds;

	private Long cpId;

	private LocalDate effectiveDate;

	@Override
	public ServiceRateListCriteria self() {
		return this;
	}

	public LocalDate startDate() {
		return startDate;
	}

	public ServiceRateListCriteria startDate(LocalDate startDate) {
		this.startDate = startDate;
		return self();
	}

	public LocalDate endDate() {
		return endDate;
	}

	public ServiceRateListCriteria endDate(LocalDate endDate) {
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

	public Collection<Long> serviceIds() {
		return serviceIds;
	}

	public ServiceRateListCriteria serviceIds(Collection<Long> serviceIds) {
		this.serviceIds = serviceIds;
		return self();
	}

	public Long cpId() {
		return cpId;
	}

	public ServiceRateListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public LocalDate effectiveDate() {
		return effectiveDate;
	}

	public ServiceRateListCriteria effectiveDate(LocalDate effectiveDate) {
		this.effectiveDate = effectiveDate;
		return self();
	}
}
