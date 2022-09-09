package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenKit;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecimenKitSummary {

	private Long id;

	private Long cpId;

	private String cpShortTitle;

	private String cpTitle;

	private String sendingSite;

	private String receivingSite;

	private Date sendingDate;

	private UserSummary sender;

	private Long participantCount;

	private Long specimenCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getCpTitle() {
		return cpTitle;
	}

	public void setCpTitle(String cpTitle) {
		this.cpTitle = cpTitle;
	}

	public String getSendingSite() {
		return sendingSite;
	}

	public void setSendingSite(String sendingSite) {
		this.sendingSite = sendingSite;
	}

	public String getReceivingSite() {
		return receivingSite;
	}

	public void setReceivingSite(String receivingSite) {
		this.receivingSite = receivingSite;
	}

	public Date getSendingDate() {
		return sendingDate;
	}

	public void setSendingDate(Date sendingDate) {
		this.sendingDate = sendingDate;
	}

	public UserSummary getSender() {
		return sender;
	}

	public void setSender(UserSummary sender) {
		this.sender = sender;
	}

	public Long getParticipantCount() {
		return participantCount;
	}

	public void setParticipantCount(Long participantCount) {
		this.participantCount = participantCount;
	}

	public Long getSpecimenCount() {
		return specimenCount;
	}

	public void setSpecimenCount(Long specimenCount) {
		this.specimenCount = specimenCount;
	}

	public static void copy(SpecimenKit kit, SpecimenKitSummary summary) {
		summary.setId(kit.getId());
		summary.setCpId(kit.getCollectionProtocol().getId());
		summary.setCpTitle(kit.getCollectionProtocol().getTitle());
		summary.setCpShortTitle(kit.getCollectionProtocol().getShortTitle());
		summary.setSendingSite(kit.getSendingSite().getName());
		summary.setReceivingSite(kit.getReceivingSite().getName());
		summary.setSendingDate(kit.getSendingDate());
		summary.setSender(UserSummary.from(kit.getSender()));
	}

	public static SpecimenKitSummary from(SpecimenKit kit) {
		SpecimenKitSummary summary = new SpecimenKitSummary();
		copy(kit, summary);
		return summary;
	}
}
