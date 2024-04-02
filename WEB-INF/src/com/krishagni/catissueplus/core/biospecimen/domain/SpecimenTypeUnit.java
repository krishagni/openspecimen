package com.krishagni.catissueplus.core.biospecimen.domain;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;

public class SpecimenTypeUnit extends BaseEntity {
	private CollectionProtocol cp;

	private PermissibleValue specimenClass;

	private PermissibleValue type;

	private PermissibleValue quantityUnit;

	private PermissibleValue concentrationUnit;

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public PermissibleValue getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(PermissibleValue specimenClass) {
		this.specimenClass = specimenClass;
	}

	public PermissibleValue getType() {
		return type;
	}

	public void setType(PermissibleValue type) {
		this.type = type;
	}

	public PermissibleValue getQuantityUnit() {
		return quantityUnit;
	}

	public void setQuantityUnit(PermissibleValue quantityUnit) {
		this.quantityUnit = quantityUnit;
	}

	public PermissibleValue getConcentrationUnit() {
		return concentrationUnit;
	}

	public void setConcentrationUnit(PermissibleValue concentrationUnit) {
		this.concentrationUnit = concentrationUnit;
	}

	public void update(SpecimenTypeUnit other) {
		setCp(other.getCp());
		setSpecimenClass(other.getSpecimenClass());
		setType(other.getType());
		setQuantityUnit(other.getQuantityUnit());
		setConcentrationUnit(other.getConcentrationUnit());
	}
}
