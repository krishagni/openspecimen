
package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.List;

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.codehaus.jackson.map.annotate.JsonSerialize.Inclusion;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;

@JsonSerialize(include=Inclusion.NON_NULL)
@ListenAttributeChanges
public class CollectionProtocolRegistrationDetail extends AttributeModifiedSupport {
	private Long id;
	
	private ParticipantDetail participant;

	private Long cpId;
	
	private String cpTitle;
	
	private String cpShortTitle;
	
	private String ppid;

	private String barcode;

	private String activityStatus;

	private Date registrationDate;

	private String externalSubjectId;

	private String site;

	private ConsentDetail consentDetails;
	
	/** For UI efficiency **/
	private String specimenLabelFmt;
	
	private String aliquotLabelFmt;
	
	private String derivativeLabelFmt;

	//
	// transient variables specifying action to be performed
	//
	private boolean forceDelete;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public ParticipantDetail getParticipant() {
		return participant;
	}

	public void setParticipant(ParticipantDetail participant) {
		this.participant = participant;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
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

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Date getRegistrationDate() {
		return registrationDate;
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getExternalSubjectId() {
		return externalSubjectId;
	}

	public void setExternalSubjectId(String externalSubjectId) {
		this.externalSubjectId = externalSubjectId;
	}

	public String getSite() {
		return site;
	}

	public void setSite(String site) {
		this.site = site;
	}

	public ConsentDetail getConsentDetails() {
		return consentDetails;
	}

	public void setConsentDetails(ConsentDetail consentDetails) {
		this.consentDetails = consentDetails;
	}

	public String getSpecimenLabelFmt() {
		return specimenLabelFmt;
	}

	public void setSpecimenLabelFmt(String specimenLabelFmt) {
		this.specimenLabelFmt = specimenLabelFmt;
	}

	public String getAliquotLabelFmt() {
		return aliquotLabelFmt;
	}

	public void setAliquotLabelFmt(String aliquotLabelFmt) {
		this.aliquotLabelFmt = aliquotLabelFmt;
	}

	public String getDerivativeLabelFmt() {
		return derivativeLabelFmt;
	}

	public void setDerivativeLabelFmt(String derivativeLabelFmt) {
		this.derivativeLabelFmt = derivativeLabelFmt;
	}
	
	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public static CollectionProtocolRegistrationDetail from(CollectionProtocolRegistration cpr, boolean excludePhi) {
		return from(cpr, excludePhi, null);
	}

	public static CollectionProtocolRegistrationDetail from(CollectionProtocolRegistration cpr, boolean excludePhi, List<CollectionProtocolRegistration> otherCprs) {
		Participant participant = cpr.getParticipant();
		if (participant != null) {
			participant.setCpId(cpr.getCollectionProtocol().getId());
		}

		CollectionProtocolRegistrationDetail detail = new CollectionProtocolRegistrationDetail();
		detail.setParticipant(ParticipantDetail.from(participant, excludePhi, otherCprs));
		detail.setId(cpr.getId());		
		detail.setActivityStatus(cpr.getActivityStatus());
		detail.setBarcode(cpr.getBarcode());
		detail.setPpid(cpr.getPpid());
		detail.setRegistrationDate(cpr.getRegistrationDate());
		detail.setExternalSubjectId(cpr.getExternalSubjectId());

		if (cpr.getSite() != null) {
			detail.setSite(cpr.getSite().getName());
		}
		
		CollectionProtocol cp = cpr.getCollectionProtocol();
		detail.setCpId(cp.getId());
		detail.setCpTitle(cp.getTitle());
		detail.setCpShortTitle(cp.getShortTitle());
		detail.setSpecimenLabelFmt(cp.getSpecimenLabelFormat());
		detail.setAliquotLabelFmt(cp.getAliquotLabelFormatToUse());
		detail.setDerivativeLabelFmt(cp.getDerivativeLabelFormat());
		return detail;
	}
}
