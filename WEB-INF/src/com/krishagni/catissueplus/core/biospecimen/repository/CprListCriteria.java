package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class CprListCriteria extends AbstractListCriteria<CprListCriteria> {
	
	private Long cpId;
	
	private Date registrationDate;
	
	private String ppid;
	
	private String participantId;

	private String name;
	
	private Date dob;

	private String uid;
	
	private String specimen;

	private Set<SiteCpPair> phiSiteCps;

	private Set<SiteCpPair> siteCps;

	private boolean useMrnSites;

	List<String> empis;

	private List<String> ppids;

	private List<String> externalSubjectIds;

	@Override
	public CprListCriteria self() {
		return this;
	}

	public Long cpId() {
		return cpId;
	}

	@JsonProperty("cpId")
	public CprListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public Date registrationDate() {
		return registrationDate;
	}

	@JsonProperty("registrationDate")
	public CprListCriteria registrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
		return self();
	}

	public String ppid() {
		return ppid;
	}

	@JsonProperty("ppid")
	public CprListCriteria ppid(String ppid) {
		this.ppid = ppid;
		return self();
	}

	public String participantId() {
		return participantId;
	}

	@JsonProperty("participantId")
	public CprListCriteria participantId(String participantId) {
		this.participantId = participantId;
		return self();
	}

	public CprListCriteria name(String name) {
		this.name = name;
		return self();
	}

	@JsonProperty("name")
	public String name() {
		return name;
	}

	public CprListCriteria dob(Date dob) {
		this.dob = dob;
		return self();
	}

	@JsonProperty("dob")
	public Date dob() {
		return dob;
	}

	public String uid() {
		return uid;
	}

	@JsonProperty("uid")
	public CprListCriteria uid(String uid) {
		this.uid = uid;
		return self();
	}

	public CprListCriteria specimen(String specimen) {
		this.specimen = specimen;
		return self();
	}

	@JsonProperty("specimen")
	public String specimen() {
		return specimen;
	}

	public Set<SiteCpPair> phiSiteCps() {
		return phiSiteCps;
	}

	public CprListCriteria phiSiteCps(Set<SiteCpPair> phiSiteCps) {
		this.phiSiteCps = phiSiteCps;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public CprListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public boolean useMrnSites() {
		return useMrnSites;
	}

	public CprListCriteria useMrnSites(boolean useMrnSites) {
		this.useMrnSites = useMrnSites;
		return self();
	}

	public boolean hasPhiFields() {
		return StringUtils.isNotBlank(name()) ||
			StringUtils.isNotBlank(participantId()) ||
			StringUtils.isNotBlank(uid()) ||
			CollectionUtils.isNotEmpty(empis()) ||
			dob() != null;
	}

	public Set<Long> phiCps() {
		if (CollectionUtils.isEmpty(phiSiteCps())) {
			return Collections.emptySet();
		}

		return phiSiteCps().stream().map(SiteCpPair::getCpId).collect(Collectors.toSet());
	}

	@JsonProperty("empis")
	public CprListCriteria empis(List<String> empis) {
		this.empis = empis;
		return self();
	}

	public List<String> empis() {
		return empis;
	}

	public List<String> ppids() {
		return ppids;
	}

	@JsonProperty("ppids")
	public CprListCriteria ppids(List<String> ppids) {
		this.ppids = ppids;
		return self();
	}

	public List<String> externalSubjectIds() {
		return externalSubjectIds;
	}

	@JsonProperty("externalSubjectIds")
	public CprListCriteria externalSubjectIds(List<String> externalSubjectIds) {
		this.externalSubjectIds = externalSubjectIds;
		return self();
	}
}