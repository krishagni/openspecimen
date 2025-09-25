package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonFilter("withoutId")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecimenTypeUnitDetail {
	private Long id;

	private Long cpId;

	private String cpShortTitle;

	private String specimenClass;

	private String type;

	private String quantityUnit;

	private String concentrationUnit;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
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

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getKey() {
		List<String> keyParts = new ArrayList<>();
		if (cpId != null && cpId > 0L) {
			keyParts.add("CP ID = " + cpId);
		}

		if (StringUtils.isNotBlank(cpShortTitle)) {
			keyParts.add(("CP = " + cpShortTitle));
		}

		if (StringUtils.isNotBlank(specimenClass)) {
			keyParts.add("Specimen Class = " + specimenClass);
		}

		if (StringUtils.isNotBlank(type)) {
			keyParts.add("Specimen Type = " + type);
		}

		return String.join(", ", keyParts);
	}

	public static SpecimenTypeUnitDetail from(SpecimenTypeUnit unit) {
		SpecimenTypeUnitDetail result = new SpecimenTypeUnitDetail();
		result.setId(unit.getId());
		result.setCpId(unit.getCp() != null ? unit.getCp().getId() : null);
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
