package com.krishagni.catissueplus.core.biospecimen.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.envers.Audited;

@Audited
public class LabServicesRateList extends BaseEntity {
	private String name;

	private String description;

	private LocalDate startDate;

	private LocalDate endDate;

	private Set<CollectionProtocol> cps = new HashSet<>();

	private Set<LabServiceRate> serviceRates = new HashSet<>();

	private String activityStatus;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public Set<CollectionProtocol> getCps() {
		return cps;
	}

	public void setCps(Set<CollectionProtocol> cps) {
		this.cps = cps;
	}

	public Set<LabServiceRate> getServiceRates() {
		return serviceRates;
	}

	public void setServiceRates(Set<LabServiceRate> serviceRates) {
		this.serviceRates = serviceRates;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}
}
