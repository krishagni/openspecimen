
package com.krishagni.catissueplus.core.biospecimen.events;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.events.PermissibleValueDetails;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@JsonInclude(JsonInclude.Include.NON_NULL)
@ListenAttributeChanges
public class VisitDetail extends VisitSummary {
	private Long cprId;

	private String cpTitle;

	private String cpShortTitle;

	private Set<String> clinicalDiagnoses;

	private List<PermissibleValueDetails> diagnosisList;
	
	private String clinicalStatus;

	private String activityStatus;

	private String site;

	private String comments;

	private String surgicalPathologyNumber;
	
	private String sprName;

	private String missedReason;

	private UserSummary missedBy;
	
	private boolean sprLocked;

	private String code;
	
	private String cohort;
	
	private ExtensionDetail extensionDetail;
	
	//
	// transient variables specifying action to be performed
	//
	private boolean forceDelete;

	private boolean lookupVisitsByName;

	@JsonIgnore
	File sprFile;

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public String getCpTitle() {
		return cpTitle;
	}

	public void setCpTitle(String cpTitle) {
		this.cpTitle = cpTitle;
	}
	
	public String getCpShortTitle() {
		return cpShortTitle;		
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Set<String> getClinicalDiagnoses() {
		return clinicalDiagnoses;
	}

	public void setClinicalDiagnoses(Set<String> clinicalDiagnoses) {
		this.clinicalDiagnoses = clinicalDiagnoses;
	}

	public List<PermissibleValueDetails> getDiagnosisList() {
		return diagnosisList;
	}

	public void setDiagnosisList(List<PermissibleValueDetails> diagnosisList) {
		this.diagnosisList = diagnosisList;
	}

	public String getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(String clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getSurgicalPathologyNumber() {
		return surgicalPathologyNumber;
	}

	public void setSurgicalPathologyNumber(String surgicalPathologyNumber) {
		this.surgicalPathologyNumber = surgicalPathologyNumber;
	}
	
	public String getSprName() {
		return sprName;
	}

	public void setSprName(String sprName) {
		this.sprName = sprName;
	}
	
	public boolean isSprLocked() {
		return sprLocked;
	}

	public void setSprLocked(boolean sprLock) {
		this.sprLocked = sprLock;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getMissedReason() {
		return missedReason;
	}

	public void setMissedReason(String missedReason) {
		this.missedReason = missedReason;
	}

	public UserSummary getMissedBy() {
		return missedBy;
	}

	public void setMissedBy(UserSummary missedBy) {
		this.missedBy = missedBy;
	}

	public String getCohort() {
		return cohort;
	}

	public void setCohort(String cohort) {
		this.cohort = cohort;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	@JsonIgnore
	public boolean isLookupVisitsByName() {
		return lookupVisitsByName;
	}

	@JsonProperty
	public void setLookupVisitsByName(boolean lookupVisitsByName) {
		this.lookupVisitsByName = lookupVisitsByName;
	}

	public File getSprFile() {
		return sprFile;
	}

	public void setSprFile(File sprFile) {
		this.sprFile = sprFile;
	}

	public String getDescription() {
		String result = "";
		if (StringUtils.isBlank(getEventLabel())) {
			result = "Unplanned Visit";
		} else {
			if (getEventPoint() != null) {
				result += (getEventPoint() < 0 ? "-" : "") + "T" + Math.abs(getEventPoint());
				result += getEventPointUnit().name().charAt(0) + ": ";
			}

			result += getEventLabel();
			if (StringUtils.isNotBlank(getEventCode())) {
				result += " (" + getEventCode() + ")";
			}
		}

		if (getVisitDate() != null) {
			result += " / " + Utility.getDateString(getVisitDate());
		}

		return result;
	}

	public static VisitDetail from(Visit visit) {
		return from(visit, true, true);
	}

	public static VisitDetail from(Visit visit, boolean partial, boolean excludePhi) {
		VisitDetail detail = new VisitDetail();
		detail.setActivityStatus(visit.getActivityStatus());
		detail.setClinicalDiagnoses(PermissibleValue.toValueSet(visit.getClinicalDiagnoses()));
		detail.setDiagnosisList(PermissibleValueDetails.from(visit.getClinicalDiagnoses()));
		detail.setClinicalStatus(PermissibleValue.getValue(visit.getClinicalStatus()));
		detail.setStatus(visit.getStatus());
		detail.setComments(visit.getComments());
		detail.setId(visit.getId());
		detail.setName(visit.getName());
		detail.setSprName(visit.getSprName());
		detail.setSprLocked(visit.isSprLocked());
		detail.setVisitDate(visit.getVisitDate());
		detail.setMissedReason(PermissibleValue.getValue(visit.getMissedReason()));
		detail.setCohort(PermissibleValue.getValue(visit.getCohort()));

		if (excludePhi && StringUtils.isNotBlank(visit.getSurgicalPathologyNumber())) {
			detail.setSurgicalPathologyNumber("###");
		} else {
			detail.setSurgicalPathologyNumber(visit.getSurgicalPathologyNumber());
		}

		if (visit.getMissedBy() != null) {
			detail.setMissedBy(UserSummary.from(visit.getMissedBy()));
		}

		if (visit.getSite() != null) {
			detail.setSite(visit.getSite().getName());
		}

		CollectionProtocolRegistration cpr = visit.getRegistration();
		detail.setCprId(cpr.getId());
		detail.setPpid(cpr.getPpid());
		detail.setCpId(cpr.getCollectionProtocol().getId());
		detail.setCpTitle(cpr.getCollectionProtocol().getTitle());
		detail.setCpShortTitle(cpr.getCollectionProtocol().getShortTitle());
		
		if (!visit.isUnplanned()) {
			CollectionProtocolEvent cpe = visit.getCpEvent();
			detail.setEventId(cpe.getId());
			detail.setEventCode(cpe.getCode());
			detail.setEventLabel(cpe.getEventLabel());
			detail.setEventPoint(cpe.getEventPoint());
			detail.setEventPointUnit(cpe.getEventPointUnit());
		}
		
		if (!partial) {
			detail.setExtensionDetail(ExtensionDetail.from(visit.getExtension(), excludePhi));
		}
		return detail;
	}
	
	public static List<VisitDetail> from(Collection<Visit> visits) {
		return Utility.nullSafeStream(visits).map(VisitDetail::from).collect(Collectors.toList());
	}

	public static VisitDetail from(CollectionProtocolEvent event) {
		VisitDetail detail = new VisitDetail();
		detail.setEventId(event.getId());
		detail.setEventCode(event.getCode());
		detail.setEventLabel(event.getEventLabel());
		detail.setEventPoint(event.getEventPoint());
		detail.setEventPointUnit(event.getEventPointUnit());
		detail.setCpId(event.getCollectionProtocol().getId());
		detail.setCpShortTitle(event.getCollectionProtocol().getShortTitle());
		detail.setCpTitle(event.getCollectionProtocol().getTitle());
		detail.setCode(event.getCode());
		detail.setSite(event.getDefaultSite() != null ? event.getDefaultSite().getName() : null);
		detail.setClinicalStatus(PermissibleValue.getValue(event.getClinicalStatus()));

		if (event.getClinicalDiagnosis() != null) {
			detail.setClinicalDiagnoses(PermissibleValue.toValueSet(Collections.singleton(event.getClinicalDiagnosis())));
			detail.setDiagnosisList(PermissibleValueDetails.from(Collections.singleton(event.getClinicalDiagnosis())));
		}

		return detail;
	}

	public static void setAnticipatedVisitDates(Date regDate, Collection<VisitDetail> visits) {
		Pair<Integer, IntervalUnit> minEventPoint = getMinEventPoint(visits);
		visits.forEach(v -> v.setAnticipatedVisitDate(regDate, minEventPoint.first(), minEventPoint.second()));
	}

	private static Pair<Integer, IntervalUnit> getMinEventPoint(Collection<VisitDetail> visits) {
		int minEventPoint = 0, minEventPointInDays = 0;
		IntervalUnit minEventPointUnit = IntervalUnit.DAYS;
		for (VisitDetail visit : visits) {
			if (visit.getEventPoint() == null) {
				continue;
			}

			Integer interval = Utility.getNoOfDays(visit.getEventPoint(), visit.getEventPointUnit());
			if (interval != null && interval < minEventPointInDays) {
				minEventPoint = visit.getEventPoint();
				minEventPointInDays = interval;
				minEventPointUnit = visit.getEventPointUnit();
			}
		}

		return Pair.make(minEventPoint, minEventPointUnit);
	}
}
