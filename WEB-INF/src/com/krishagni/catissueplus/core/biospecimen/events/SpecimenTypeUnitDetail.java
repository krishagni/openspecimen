package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenTypeUnitDetail {
	private Long id;

	private String cpShortTitle;

	private String specimenClass;

	private String type;

	private String quantityUnit;

	private String concentrationUnit;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getQuantityUnit() {
		return quantityUnit;
	}

	public void setQuantityUnit(String quantityUnit) {
		this.quantityUnit = quantityUnit;
	}

	public String getConcentrationUnit() {
		return concentrationUnit;
	}

	public void setConcentrationUnit(String concentrationUnit) {
		this.concentrationUnit = concentrationUnit;
	}

	public static SpecimenTypeUnitDetail from(SpecimenTypeUnit unit) {
		SpecimenTypeUnitDetail result = new SpecimenTypeUnitDetail();
		result.setId(unit.getId());
		result.setCpShortTitle(unit.getCp() != null ? unit.getCp().getShortTitle() : null);
		result.setSpecimenClass(unit.getSpecimenClass().getValue());
		result.setType(unit.getType() != null ? unit.getType().getValue() : null);
		result.setQuantityUnit(unit.getQuantityUnit() != null ? unit.getQuantityUnit().getValue() : null);
		result.setConcentrationUnit(unit.getConcentrationUnit() != null ? unit.getConcentrationUnit().getValue() : null);
		return result;
	}

	public static List<SpecimenTypeUnitDetail> from(Collection<SpecimenTypeUnit> units) {
		return Utility.nullSafeStream(units).map(SpecimenTypeUnitDetail::from).collect(Collectors.toList());
	}
}
