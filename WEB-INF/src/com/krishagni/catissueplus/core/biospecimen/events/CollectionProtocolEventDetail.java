package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;

@JsonFilter("withoutId")
@JsonInclude(Include.NON_NULL)
public class CollectionProtocolEventDetail {
	private Long id;

	private String code;
	
	private String eventLabel;
	
	private Integer eventPoint;

	private String eventPointUnit;

	private Integer sortOrder;

	private Long cpId;
	
	private String collectionProtocol;
	
	private String cpShortTitle;
	
	private String defaultSite;
	
	private String clinicalDiagnosis;
	
	private String clinicalStatus;

	private String visitNamePrintMode;

	private Integer visitNamePrintCopies;
	
	private String activityStatus;
	

	
	//
	// mostly used for export
	//
	private List<SpecimenRequirementDetail> specimenRequirements;

	private int offset;

	private String offsetUnit;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public Integer getEventPoint() {
		return eventPoint;
	}

	public void setEventPoint(Integer eventPoint) {
		this.eventPoint = eventPoint;
	}

	public String getEventPointUnit() {
		return eventPointUnit;
	}

	public void setEventPointUnit(String eventPointUnit) {
		this.eventPointUnit = eventPointUnit;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(String collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getDefaultSite() {
		return defaultSite;
	}

	public void setDefaultSite(String defaultSite) {
		this.defaultSite = defaultSite;
	}

	public String getClinicalDiagnosis() {
		return clinicalDiagnosis;
	}

	public void setClinicalDiagnosis(String clinicalDiagnosis) {
		this.clinicalDiagnosis = clinicalDiagnosis;
	}

	public String getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(String clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
	}

	public String getVisitNamePrintMode() {
		return visitNamePrintMode;
	}

	public void setVisitNamePrintMode(String visitNamePrintMode) {
		this.visitNamePrintMode = visitNamePrintMode;
	}

	public Integer getVisitNamePrintCopies() {
		return visitNamePrintCopies;
	}

	public void setVisitNamePrintCopies(Integer visitNamePrintCopies) {
		this.visitNamePrintCopies = visitNamePrintCopies;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}
	
	public List<SpecimenRequirementDetail> getSpecimenRequirements() {
		return specimenRequirements;
	}

	public void setSpecimenRequirements(List<SpecimenRequirementDetail> specimenRequirements) {
		this.specimenRequirements = specimenRequirements;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public String getOffsetUnit() {
		return offsetUnit;
	}

	public void setOffsetUnit(String offsetUnit) {
		this.offsetUnit = offsetUnit;
	}

	public static CollectionProtocolEventDetail from(CollectionProtocolEvent event) {
		return from(event, false);
	}
	
	public static CollectionProtocolEventDetail from(CollectionProtocolEvent event, boolean fullObject) {
		CollectionProtocolEventDetail detail = new CollectionProtocolEventDetail();
		
		detail.setId(event.getId());
		detail.setCode(event.getCode());
		detail.setEventLabel(event.getEventLabel());
		detail.setEventPoint(event.getEventPoint());
		detail.setEventPointUnit(event.getEventPointUnit() != null ? event.getEventPointUnit().name(): null);
		detail.setSortOrder(event.getSortOrder());
		detail.setClinicalDiagnosis(PermissibleValue.getValue(event.getClinicalDiagnosis()));
		detail.setClinicalStatus(PermissibleValue.getValue(event.getClinicalStatus()));
		detail.setCpId(event.getCollectionProtocol().getId());
		detail.setCollectionProtocol(event.getCollectionProtocol().getTitle());
		detail.setCpShortTitle(event.getCollectionProtocol().getShortTitle());
		detail.setVisitNamePrintCopies(event.getVisitNamePrintCopies());
		detail.setActivityStatus(event.getActivityStatus());

		detail.setOffset(event.getOffset());
		detail.setOffsetUnit(event.getOffsetUnit() != null ? event.getOffsetUnit().name() : null);
		
		if (event.getDefaultSite() != null) {
			detail.setDefaultSite(event.getDefaultSite().getName());
		}

		if (event.getVisitNamePrintMode() != null) {
			detail.setVisitNamePrintMode(event.getVisitNamePrintMode().name());
		}
		
		if (fullObject) {
			Set<SpecimenRequirement> srs = new HashSet<SpecimenRequirement>();
			for (SpecimenRequirement sr : event.getOrderedTopLevelAnticipatedSpecimens()) {
				srs.add(sr);
			}

			detail.setSpecimenRequirements(SpecimenRequirementDetail.from(srs));
		}
		
		return detail;
	}
	
	public static List<CollectionProtocolEventDetail> from(Collection<CollectionProtocolEvent> events) {
		return from(events, false);
	}
		
	public static List<CollectionProtocolEventDetail> from(Collection<CollectionProtocolEvent> events, boolean fullObject) {
		List<CollectionProtocolEventDetail> result = new ArrayList<CollectionProtocolEventDetail>();
		
		for (CollectionProtocolEvent event : events) {
			result.add(CollectionProtocolEventDetail.from(event, fullObject));
		}
		
		return result;
	}
}
