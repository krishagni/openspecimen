package com.krishagni.catissueplus.core.biospecimen.repository;

import java.time.LocalDate;
import java.util.Collection;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class LabServicesRateListCriteria extends AbstractListCriteria<LabServicesRateListCriteria> {

	private Long cpId;

	private String cpShortTitle;

	private LocalDate startDate;

	private LocalDate endDate;

	private LocalDate effectiveDate;

	private String serviceCode;

	private Long creatorId;

	private Collection<SiteCpPair> siteCps;

	@Override
	public LabServicesRateListCriteria self() {
		return this;
	}

	public Long cpId() {
		return cpId;
	}

	public LabServicesRateListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public String cpShortTitle() {
		return cpShortTitle;
	}

	public LabServicesRateListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public LocalDate startDate() {
		return startDate;
	}

	public LabServicesRateListCriteria startDate(LocalDate startDate) {
		this.startDate = startDate;
		return self();
	}

	public LocalDate endDate() {
		return endDate;
	}

	public LabServicesRateListCriteria endDate(LocalDate endDate) {
		this.endDate = endDate;
		return self();
	}

	public LocalDate effectiveDate() {
		return effectiveDate;
	}

	public LabServicesRateListCriteria effectiveDate(LocalDate effectiveDate) {
		this.effectiveDate = effectiveDate;
		return self();
	}

	public String serviceCode() {
		return serviceCode;
	}

	public LabServicesRateListCriteria serviceCode(String serviceCode) {
		this.serviceCode = serviceCode;
		return self();
	}

	public Long creatorId() {
		return creatorId;
	}

	public LabServicesRateListCriteria creatorId(Long creatorId) {
		this.creatorId = creatorId;
		return self();
	}

	public Collection<SiteCpPair> siteCps() {
		return siteCps;
	}

	public LabServicesRateListCriteria siteCps(Collection<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}
}
