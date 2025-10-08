package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class LabService extends BaseEntity {
	private String code;

	private String description;

	private String activityStatus;

	private Set<SpecimenRequirement> requirements = new HashSet<>();

	private Set<Specimen> specimens = new HashSet<>();


	private Set<LabServiceRate> serviceRates = new HashSet<>();

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

	@NotAudited
	public Set<LabServiceRate> getServiceRates() {
		return serviceRates;
	}

	public void setServiceRates(Set<LabServiceRate> serviceRates) {
		this.serviceRates = serviceRates;
	}

	public void update(LabService other) {
		setCode(other.getCode());
		setDescription(other.getDescription());
		setActivityStatus(other.getActivityStatus());
		if (Status.isDisabledStatus(getActivityStatus())) {
			delete();
		}
	}

	public void delete() {
		setCode(Utility.getDisabledValue(getCode(), 32));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isArchived() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}
}
