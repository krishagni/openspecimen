package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenListsFoldersCriteria extends AbstractListCriteria<SpecimenListsFoldersCriteria> {

	private Long userId;

	@Override
	public SpecimenListsFoldersCriteria self() {
		return this;
	}

	public Long userId() {
		return userId;
	}

	public SpecimenListsFoldersCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}
}
