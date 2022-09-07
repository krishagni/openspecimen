package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.common.CollectionUpdater;

public class StagedVisit extends BaseEntity {
	private String name;

	private String clinicalStatus;

	private String status;

	private Date visitDate;

	private String surgicalPathologyNumber;

	private String comments;

	private String missedReason;

	private Set<String> clinicalDiagnoses = new HashSet<>();

	private String eventLabel;

	private String collectionSite;

	private String cohort;

	private Date updateTime;

	private StagedParticipant participant;

	private String additionalInfoJson;

	private String activityStatus;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(String clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public String getSurgicalPathologyNumber() {
		return surgicalPathologyNumber;
	}

	public void setSurgicalPathologyNumber(String surgicalPathologyNumber) {
		this.surgicalPathologyNumber = surgicalPathologyNumber;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getMissedReason() {
		return missedReason;
	}

	public void setMissedReason(String missedReason) {
		this.missedReason = missedReason;
	}

	public Set<String> getClinicalDiagnoses() {
		return clinicalDiagnoses;
	}

	public void setClinicalDiagnoses(Set<String> clinicalDiagnoses) {
		this.clinicalDiagnoses = clinicalDiagnoses;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public String getCollectionSite() {
		return collectionSite;
	}

	public void setCollectionSite(String collectionSite) {
		this.collectionSite = collectionSite;
	}

	public String getCohort() {
		return cohort;
	}

	public void setCohort(String cohort) {
		this.cohort = cohort;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public StagedParticipant getParticipant() {
		return participant;
	}

	public void setParticipant(StagedParticipant participant) {
		this.participant = participant;
	}

	public String getAdditionalInfoJson() {
		return additionalInfoJson;
	}

	public Map<String, Object> getAdditionalInfo() {
		if (StringUtils.isBlank(additionalInfoJson)) {
			return Collections.emptyMap();
		}

		try {
			return getReadMapper().readValue(additionalInfoJson, new TypeReference<Map<String, Object>>() { });
		} catch (Exception e) {
			throw new IllegalArgumentException("Error parsing visit additional info to JSON", e);
		}
	}

	public void setAdditionalInfoJson(String additionalInfoJson) {
		this.additionalInfoJson = additionalInfoJson;
	}

	public void setAdditionalInfo(Map<String, Object> additionalInfo) {
		if (additionalInfo == null || additionalInfo.isEmpty()) {
			setAdditionalInfoJson(null);
		}

		try {
			setAdditionalInfoJson(getWriteMapper().writeValueAsString(additionalInfo));
		} catch (Exception e) {
			throw new IllegalArgumentException("Error converting visit additional info to JSON", e);
		}
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(StagedVisit other) {
		setName(other.getName());
		setClinicalStatus(other.getClinicalStatus());
		setStatus(other.getStatus());
		setVisitDate(other.getVisitDate());
		setSurgicalPathologyNumber(other.getSurgicalPathologyNumber());
		setComments(other.getComments());
		setMissedReason(other.getMissedReason());
		CollectionUpdater.update(getClinicalDiagnoses(), other.getClinicalDiagnoses());
		setEventLabel(other.getEventLabel());
		setCollectionSite(other.getCollectionSite());
		setCohort(other.getCohort());
		setUpdateTime(other.getUpdateTime());
		setAdditionalInfoJson(other.getAdditionalInfoJson());
		setActivityStatus(other.getActivityStatus());
	}

	private static ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}

	private static ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper();
		return mapper.setVisibility(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
	}
}
