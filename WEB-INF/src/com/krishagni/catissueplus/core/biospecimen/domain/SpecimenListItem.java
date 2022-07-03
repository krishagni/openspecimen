package com.krishagni.catissueplus.core.biospecimen.domain;

import org.hibernate.envers.Audited;

@Audited
public class SpecimenListItem extends BaseEntity {
	private Specimen specimen;

	private SpecimenList list;

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public SpecimenList getList() {
		return list;
	}

	public void setList(SpecimenList list) {
		this.list = list;
	}
}
