package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedVisit;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StagedVisitDetail extends VisitDetail {
	private Long stagedId;

	private Long stagedParticipantId;

	private Date updatedTime;

	private Map<String, Object> additionalInfo;

	public Long getStagedId() {
		return stagedId;
	}

	public void setStagedId(Long stagedId) {
		this.stagedId = stagedId;
	}

	public Long getStagedParticipantId() {
		return stagedParticipantId;
	}

	public void setStagedParticipantId(Long stagedParticipantId) {
		this.stagedParticipantId = stagedParticipantId;
	}

	public Date getUpdatedTime() {
		return updatedTime;
	}

	public void setUpdatedTime(Date updatedTime) {
		this.updatedTime = updatedTime;
	}

	public Map<String, Object> getAdditionalInfo() {
		return additionalInfo;
	}

	public void setAdditionalInfo(Map<String, Object> additionalInfo) {
		this.additionalInfo = additionalInfo;
	}

	public static StagedVisitDetail from(StagedVisit visit) {
		StagedVisitDetail detail = new StagedVisitDetail();
		detail.setStagedId(visit.getId());
		detail.setName(visit.getName());
		detail.setSurgicalPathologyNumber(visit.getSurgicalPathologyNumber());
		detail.setVisitDate(visit.getVisitDate());
		detail.setClinicalDiagnoses(visit.getClinicalDiagnoses() != null ? new HashSet<>(visit.getClinicalDiagnoses()) : null);
		detail.setClinicalStatus(visit.getClinicalStatus());
		detail.setCohort(visit.getCohort());
		detail.setSite(visit.getCollectionSite());
		detail.setStatus(visit.getStatus());
		detail.setMissedReason(visit.getMissedReason());
		detail.setComments(visit.getComments());
		detail.setStagedParticipantId(visit.getParticipant().getId());
		detail.setUpdatedTime(visit.getUpdateTime());
		detail.setAdditionalInfo(visit.getAdditionalInfo());
		return detail;
	}


}
