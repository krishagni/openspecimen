package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CprSummary {
	private ParticipantSummary participant;

	private Long id;
	
	private Long cprId;
	
	private Date registrationDate;
	
	private String ppid;

	private Long cpId;

	private String cpShortTitle;
	
	private Long scgCount;
	
	private Long specimenCount;

	public ParticipantSummary getParticipant() {
		return participant;
	}

	public void setParticipant(ParticipantSummary participant) {
		this.participant = participant;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
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

	public Long getScgCount() {
		return scgCount;
	}

	public void setScgCount(Long scgCount) {
		this.scgCount = scgCount;
	}

	public Long getSpecimenCount() {
		return specimenCount;
	}

	public void setSpecimenCount(Long specimenCount) {
		this.specimenCount = specimenCount;
	}

	public static CprSummary from(CollectionProtocolRegistration cpr, boolean excludePhi) {
		CprSummary result = new CprSummary();
		result.setId(cpr.getId());
		result.setCprId(cpr.getId());
		result.setRegistrationDate(cpr.getRegistrationDate());
		result.setPpid(cpr.getPpid());
		result.setCpId(cpr.getCollectionProtocol().getId());
		result.setCpShortTitle(cpr.getCollectionProtocol().getShortTitle());
		result.setParticipant(ParticipantSummary.from(cpr.getParticipant(), excludePhi));
		return result;
	}

	public static List<CprSummary> from(List<CollectionProtocolRegistration> cprs, boolean excludePhi) {
		return Utility.nullSafeStream(cprs).map((cpr) -> from(cpr, excludePhi)).collect(Collectors.toList());
	}
}
