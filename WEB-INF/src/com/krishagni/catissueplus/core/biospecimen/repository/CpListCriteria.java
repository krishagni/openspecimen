
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class CpListCriteria extends AbstractListCriteria<CpListCriteria> {

	private List<String> shortTitles;
	
	private String title;

	private String irbId;
	
	private Long piId;
	
	private String repositoryName;

	private Long instituteId;
	
	private boolean includePi;

	private boolean onlyParticipantConsentCps;

	private Set<SiteCpPair> siteCps;
	
	@Override
	public CpListCriteria self() {
		return this;
	}

	public List<String> shortTitles() {
		return shortTitles;
	}

	public CpListCriteria shortTitles(List<String> shortTitles) {
		this.shortTitles = shortTitles;
		return self();
	}
	
	public String title() {
		return this.title;
	}
	
	public CpListCriteria title(String title) {
		this.title = title;
		return self();
	}

	public String irbId() {
		return irbId;
	}

	public CpListCriteria irbId(String irbId) {
		this.irbId = irbId;
		return self();
	}
	
	public Long piId() {
		return this.piId;
	}
	
	public CpListCriteria piId(Long piId) {
		this.piId = piId;
		return self();
	}
	
	public String repositoryName() {
		return this.repositoryName;
	}
	
	public CpListCriteria repositoryName(String repositoryName) {
		this.repositoryName = repositoryName;
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public CpListCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}
	
	public boolean includePi() {
		return includePi;
	}
	
	public CpListCriteria includePi(boolean includePi) {
		this.includePi = includePi;
		return self();
	}

	public boolean onlyParticipantConsentCps() {
		return onlyParticipantConsentCps;
	}

	public CpListCriteria onlyParticipantConsentCps(boolean onlyParticipantConsentCps) {
		this.onlyParticipantConsentCps = onlyParticipantConsentCps;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public CpListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}
}
