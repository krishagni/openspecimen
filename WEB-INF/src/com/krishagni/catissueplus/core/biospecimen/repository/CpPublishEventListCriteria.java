package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class CpPublishEventListCriteria extends AbstractListCriteria<CpPublishEventListCriteria> {
	private Long cpId;

	@Override
	public CpPublishEventListCriteria self() {
		return this;
	}

	public Long cpId() {
		return cpId;
	}

	public CpPublishEventListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}
}
