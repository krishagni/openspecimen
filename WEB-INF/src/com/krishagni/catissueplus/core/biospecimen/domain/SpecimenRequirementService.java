package com.krishagni.catissueplus.core.biospecimen.domain;

public class SpecimenRequirementService extends BaseEntity {
	private SpecimenRequirement requirement;

	private Service service;

	private int units;

	public SpecimenRequirement getRequirement() {
		return requirement;
	}

	public void setRequirement(SpecimenRequirement requirement) {
		this.requirement = requirement;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}
}
