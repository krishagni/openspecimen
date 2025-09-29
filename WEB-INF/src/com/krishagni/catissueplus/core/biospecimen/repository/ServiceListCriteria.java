package com.krishagni.catissueplus.core.biospecimen.repository;

import java.time.LocalDate;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ServiceListCriteria extends AbstractListCriteria<ServiceListCriteria> {
	private List<String> codes;

	private Long cpId;

	private LocalDate rateEffectiveOn;

	@Override
	public ServiceListCriteria self() {
		return this;
	}

	public List<String> codes() {
		return codes;
	}

	public ServiceListCriteria codes(List<String> codes) {
		this.codes = codes;
		return self();
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
