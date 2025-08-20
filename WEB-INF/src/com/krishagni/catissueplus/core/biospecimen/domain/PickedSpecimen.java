package com.krishagni.catissueplus.core.biospecimen.domain;

public class PickedSpecimen extends BaseEntity {
	private SpecimensPickList pickList;

	private Specimen specimen;

	public SpecimensPickList getPickList() {
		return pickList;
	}

	public void setPickList(SpecimensPickList pickList) {
		this.pickList = pickList;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}
}
