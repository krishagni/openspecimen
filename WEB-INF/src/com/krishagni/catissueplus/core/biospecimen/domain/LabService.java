package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.common.util.Status;

@Audited
public class LabService extends BaseEntity {
	private String code;

	private String description;

	private String activityStatus;

	private Set<SpecimenRequirement> requirements = new HashSet<>();

	private Set<Specimen> specimens = new HashSet<>();

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	@NotAudited
	public Set<SpecimenRequirement> getRequirements() {
		return requirements;
	}

	public void setRequirements(Set<SpecimenRequirement> requirements) {
		this.requirements = requirements;
	}

	@NotAudited
	public Set<Specimen> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(Set<Specimen> specimens) {
		this.specimens = specimens;
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isArchived() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}
}
