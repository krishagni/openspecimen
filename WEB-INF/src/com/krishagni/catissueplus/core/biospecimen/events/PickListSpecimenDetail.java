package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.events.UserSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PickListSpecimenDetail {
	private Long pickupId;

	private SpecimenInfo specimen;

	private UserSummary pickedBy;

	private Date pickupTime;

	public Long getPickupId() {
		return pickupId;
	}

	public void setPickupId(Long pickupId) {
		this.pickupId = pickupId;
	}

	public SpecimenInfo getSpecimen() {
		return specimen;
	}

	public void setSpecimen(SpecimenInfo specimen) {
		this.specimen = specimen;
	}

	public UserSummary getPickedBy() {
		return pickedBy;
	}

	public void setPickedBy(UserSummary pickedBy) {
		this.pickedBy = pickedBy;
	}

	public Date getPickupTime() {
		return pickupTime;
	}

	public void setPickupTime(Date pickupTime) {
		this.pickupTime = pickupTime;
	}
}
