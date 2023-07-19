
package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@JsonFilter("withoutId")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionProtocolSummary extends AttributeModifiedSupport implements Comparable<CollectionProtocolSummary> {
	private Long id;

	private String shortTitle;

	private String title;
	
	private String code;
	
	private UserSummary principalInvestigator;
	
	private Date startDate;
	
	private Date endDate;
	
	private Long participantCount;
	
	private Long specimenCount;
	
	private String ppidFmt;

	private Boolean manualPpidEnabled;

	private Boolean specimenCentric;

	private Long catalogId;

	private Boolean starred;

	private boolean draftMode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public UserSummary getPrincipalInvestigator() {
		return principalInvestigator;
	}

	public void setPrincipalInvestigator(UserSummary principalInvestigator) {
		this.principalInvestigator = principalInvestigator;
	}

	public Date getStartDate() {
		return startDate;
	}

	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}

	public Date getEndDate() {
		return endDate;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
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
	
	public String getPpidFmt() {
		return ppidFmt;
	}

	public void setPpidFmt(String ppidFmt) {
		this.ppidFmt = ppidFmt;
	}

	public Boolean isManualPpidEnabled() {
		return manualPpidEnabled != null ? manualPpidEnabled : false;
	}

	public void setManualPpidEnabled(Boolean manualPpidEnabled) {
		this.manualPpidEnabled = manualPpidEnabled;
	}

	public Boolean isSpecimenCentric() {
		return specimenCentric != null ? specimenCentric : false;
	}

	public void setSpecimenCentric(Boolean specimenCentric) {
		this.specimenCentric = specimenCentric;
	}

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public Boolean getStarred() {
		return starred;
	}

	public void setStarred(Boolean starred) {
		this.starred = starred;
	}

	public boolean isDraftMode() {
		return draftMode;
	}

	public void setDraftMode(boolean draftMode) {
		this.draftMode = draftMode;
	}

	@Override
	public int compareTo(CollectionProtocolSummary cpSummary) {
		return this.shortTitle.toUpperCase().compareTo(cpSummary.getShortTitle().toUpperCase());
	}
	
	public static CollectionProtocolSummary from(CollectionProtocol cp) {
		CollectionProtocolSummary result = new CollectionProtocolSummary();
		copy(cp, result);
		return result;		
	}
	
	public static <T extends CollectionProtocolSummary> void copy(CollectionProtocol cp, T detail) {
		detail.setId(cp.getId());
		detail.setShortTitle(cp.getShortTitle());
		detail.setTitle(cp.getTitle());
		detail.setCode(cp.getCode());
		detail.setStartDate(cp.getStartDate());
		detail.setEndDate(cp.getEndDate());
		detail.setPrincipalInvestigator(UserSummary.from(cp.getPrincipalInvestigator()));
		detail.setPpidFmt(cp.getPpidFormat());
		detail.setManualPpidEnabled(cp.isManualPpidEnabled());
		detail.setSpecimenCentric(cp.isSpecimenCentric());
		detail.setCatalogId(cp.getCatalogId());
		detail.setDraftMode(cp.isDraftMode());
	}
}
