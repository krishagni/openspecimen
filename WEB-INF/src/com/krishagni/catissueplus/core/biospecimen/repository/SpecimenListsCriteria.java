package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenListsCriteria extends AbstractListCriteria<SpecimenListsCriteria> {

	private Long userId;

	private Long folderId;

	@Override
	public SpecimenListsCriteria self() {
		return this;
	}

	public Long userId() {
		return userId;
	}

	public SpecimenListsCriteria userId(Long userId) {
		this.userId = userId;
		return self();
	}

	public Long folderId() {
		return folderId;
	}

	public SpecimenListsCriteria folderId(Long folderId) {
		this.folderId = folderId;
		return self();
	}
}
