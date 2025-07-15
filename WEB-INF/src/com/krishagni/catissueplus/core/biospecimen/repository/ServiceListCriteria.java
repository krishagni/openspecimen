package com.krishagni.catissueplus.core.biospecimen.repository;

import java.time.LocalDate;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ServiceListCriteria extends AbstractListCriteria<ServiceListCriteria> {
	private Long cpId;

	private LocalDate rateEffectiveOn;

	@Override
	public ServiceListCriteria self() {
		return this;
	}

	public Long cpId() {
		return cpId;
	}

	public ServiceListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public LocalDate rateEffectiveOn() {
		return rateEffectiveOn;
	}

	public ServiceListCriteria rateEffectiveOn(LocalDate rateEffectiveOn) {
		this.rateEffectiveOn = rateEffectiveOn;
		return self();
	}
}
