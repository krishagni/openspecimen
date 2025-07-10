package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class Service extends BaseEntity {
	private String code;

	private String description;

	private CollectionProtocol cp;

	private Set<ServiceRate> serviceRatesList = new HashSet<>();

	private Set<Specimen> specimens = new HashSet<>();

	private Set<SpecimenRequirement> requirements = new HashSet<>();

	private String activityStatus;

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

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	@NotAudited
	public Set<ServiceRate> getServiceRatesList() {
		return serviceRatesList;
	}

	public void setServiceRatesList(Set<ServiceRate> serviceRatesList) {
		this.serviceRatesList = serviceRatesList;
	}

	@NotAudited
	public Set<Specimen> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(Set<Specimen> specimens) {
		this.specimens = specimens;
	}

	@NotAudited
	public Set<SpecimenRequirement> getRequirements() {
		return requirements;
	}

	public void setRequirements(Set<SpecimenRequirement> requirements) {
		this.requirements = requirements;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(Service service) {
		setCode(service.getCode());
		setDescription(service.getDescription());
		setActivityStatus(service.getActivityStatus());
		if (Status.isDisabledStatus(getActivityStatus())) {
			delete();
		}
	}

	public void delete() {
		setCode(Utility.getDisabledValue(getCode(), 32));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}
}
