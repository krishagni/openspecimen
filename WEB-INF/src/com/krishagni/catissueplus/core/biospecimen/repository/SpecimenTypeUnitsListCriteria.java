package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenTypeUnitsListCriteria extends AbstractListCriteria<SpecimenTypeUnitsListCriteria> {
	private String cpShortTitle;

	private String specimenClass;

	private String type;

	public String cpShortTitle() {
		return cpShortTitle;
	}

	public SpecimenTypeUnitsListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public String specimenClass() {
		return specimenClass;
	}

	public SpecimenTypeUnitsListCriteria specimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
		return self();
	}

	public String type() {
		return type;
	}

	public SpecimenTypeUnitsListCriteria type(String type) {
		this.type = type;
		return self();
	}

	@Override
	public SpecimenTypeUnitsListCriteria self() {
		return this;
	}
}
