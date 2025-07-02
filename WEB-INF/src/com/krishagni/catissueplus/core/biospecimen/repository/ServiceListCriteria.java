package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ServiceListCriteria extends AbstractListCriteria<ServiceListCriteria> {
	private Long cpId;

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
}
