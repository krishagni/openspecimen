package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;


import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.NameValuePair;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@ListenAttributeChanges
public class MasterSpecimenDetail extends AttributeModifiedSupport {
	private String cpShortTitle;
	
	private String ppid;
	
	private Date registrationDate;

	private String regSite;

	private String externalSubjectId;
	
	private String firstName;

	private String lastName;
	
	private String middleName;

	private String emailAddress;

	private Boolean emailOptIn;

	private String phoneNumber;

	private Boolean textOptIn;

	private Date birthDate;

	private Date deathDate;

	private String gender;

	private Set<String> races;

	private String vitalStatus;

	private List<PmiDetail> pmis;

	private String sexGenotype;

	private Set<String> ethnicities;

	private String uid;

	private String empi;
	
	private Long visitId;
	
	private String visitName;
	
	private String eventLabel;
	
	private Date visitDate;
	
	private String collectionSite;
	
	private String status;
	
	private Set<String> clinicalDiagnoses;
	
	private String clinicalStatus;
	
	private String surgicalPathologyNumber;
	
	private String visitComments;
	
	private String reqCode;
	
	private String label;

	private String barcode;

	private String imageId;
	
	private String specimenClass;
	
	private String type;
		
	private String lineage;
	
	private String parentLabel;

	private String anatomicSite;

	private String laterality;
	
	private String pathology;
	
	private BigDecimal initialQty;

	private BigDecimal availableQty;
	
	private BigDecimal concentration;

	private Integer freezeThawCycles;
	
	private Date createdOn;

	private UserSummary createdBy;
	
	private String comments;

	private String collectionStatus;
	
	private String container;
	
	private String positionX;

	private String positionY;

	private int position;

	private Date collectionDate;
	
	private String collectionProcedure;
	
	private String collectionContainer;
	
	private String collector;
	
	private Date receivedDate;
	
	private String receivedQuality;
	
	private String receiver;

	private List<NameValuePair> externalIds;

	private ExtensionDetail extensionDetail;

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

	public Date getRegistrationDate() {
		if (registrationDate != null) {
			return registrationDate;
		} else if (visitDate != null) {
			return visitDate;
		} else {
			return collectionDate;
		}
	}

	public void setRegistrationDate(Date registrationDate) {
		this.registrationDate = registrationDate;
	}

	public String getRegSite() {
		return regSite;
	}

	public void setRegSite(String regSite) {
		this.regSite = regSite;
	}

	public String getExternalSubjectId() {
		return externalSubjectId;
	}

	public void setExternalSubjectId(String externalSubjectId) {
		this.externalSubjectId = externalSubjectId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public Boolean getEmailOptIn() {
		return emailOptIn;
	}

	public void setEmailOptIn(Boolean emailOptIn) {
		this.emailOptIn = emailOptIn;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public Boolean getTextOptIn() {
		return textOptIn;
	}

	public void setTextOptIn(Boolean textOptIn) {
		this.textOptIn = textOptIn;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public Date getDeathDate() {
		return deathDate;
	}

	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public Set<String> getRaces() {
		return races;
	}

	public void setRaces(Set<String> races) {
		this.races = races;
	}

	public String getVitalStatus() {
		return vitalStatus;
	}

	public void setVitalStatus(String vitalStatus) {
		this.vitalStatus = vitalStatus;
	}

	public List<PmiDetail> getPmis() {
		return pmis;
	}

	public void setPmis(List<PmiDetail> pmis) {
		this.pmis = pmis;
	}

	public String getSexGenotype() {
		return sexGenotype;
	}

	public void setSexGenotype(String sexGenotype) {
		this.sexGenotype = sexGenotype;
	}

	public String getEthnicity() {
		return CollectionUtils.isNotEmpty(ethnicities) ? ethnicities.iterator().next() : null;
	}

	public void setEthnicity(String ethnicity) {
		if (ethnicities == null) {
			ethnicities = new HashSet<>();
		}

		ethnicities.add(ethnicity);
	}

	public Set<String> getEthnicities() {
		return ethnicities;
	}

	public void setEthnicities(Set<String> ethnicities) {
		this.ethnicities = ethnicities;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmpi() {
		return empi;
	}

	public void setEmpi(String empi) {
		this.empi = empi;
	}

	public Long getVisitId() {
		return visitId;
	}

	public void setVisitId(Long visitId) {
		this.visitId = visitId;
	}

	public String getVisitName() {
		return visitName;
	}

	public void setVisitName(String visitName) {
		this.visitName = visitName;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public Date getVisitDate() {
		if (visitDate != null) {
			return visitDate;
		} else {
			return collectionDate;
		}
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public String getCollectionSite() {
		return collectionSite;
	}

	public void setCollectionSite(String collectionSite) {
		this.collectionSite = collectionSite;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Set<String> getClinicalDiagnoses() {
		return clinicalDiagnoses;
	}

	public void setClinicalDiagnoses(Set<String> clinicalDiagnoses) {
		this.clinicalDiagnoses = clinicalDiagnoses;
	}

	public String getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(String clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
	}

	public String getSurgicalPathologyNumber() {
		return surgicalPathologyNumber;
	}

	public void setSurgicalPathologyNumber(String surgicalPathologyNumber) {
		this.surgicalPathologyNumber = surgicalPathologyNumber;
	}

	public String getVisitComments() {
		return visitComments;
	}

	public void setVisitComments(String visitComments) {
		this.visitComments = visitComments;
	}

	public String getReqCode() {
		return reqCode;
	}

	public void setReqCode(String reqCode) {
		this.reqCode = reqCode;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	public String getParentLabel() {
		return parentLabel;
	}

	public void setParentLabel(String parentLabel) {
		this.parentLabel = parentLabel;
	}

	public String getAnatomicSite() {
		return anatomicSite;
	}

	public void setAnatomicSite(String anatomicSite) {
		this.anatomicSite = anatomicSite;
	}

	public String getLaterality() {
		return laterality;
	}

	public void setLaterality(String laterality) {
		this.laterality = laterality;
	}

	public String getPathology() {
		return pathology;
	}

	public void setPathology(String pathology) {
		this.pathology = pathology;
	}

	public BigDecimal getInitialQty() {
		return initialQty;
	}

	public void setInitialQty(BigDecimal initialQty) {
		this.initialQty = initialQty;
	}

	public BigDecimal getAvailableQty() {
		return availableQty;
	}

	public void setAvailableQty(BigDecimal availableQty) {
		this.availableQty = availableQty;
	}

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		this.concentration = concentration;
	}

	public Integer getFreezeThawCycles() {
		return freezeThawCycles;
	}

	public void setFreezeThawCycles(Integer freezeThawCycles) {
		this.freezeThawCycles = freezeThawCycles;
	}

	public Date getCreatedOn() {
		return createdOn;
	}

	public void setCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
	}

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public String getCollectionStatus() {
		return collectionStatus;
	}

	public void setCollectionStatus(String collectionStatus) {
		this.collectionStatus = collectionStatus;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public String getPositionX() {
		return positionX;
	}

	public void setPositionX(String positionX) {
		this.positionX = positionX;
	}

	public String getPositionY() {
		return positionY;
	}

	public void setPositionY(String positionY) {
		this.positionY = positionY;
	}

	public int getPosition() {
		return position;
	}

	public void setPosition(int position) {
		this.position = position;
	}

	public Date getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(Date collectionDate) {
		this.collectionDate = collectionDate;
	}

	public String getCollectionProcedure() {
		return collectionProcedure;
	}

	public void setCollectionProcedure(String collectionProcedure) {
		this.collectionProcedure = collectionProcedure;
	}

	public String getCollectionContainer() {
		return collectionContainer;
	}

	public void setCollectionContainer(String collectionContainer) {
		this.collectionContainer = collectionContainer;
	}

	public String getCollector() {
		return collector;
	}

	public void setCollector(String collector) {
		this.collector = collector;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	public String getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(String receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public String getReceiver() {
		return receiver;
	}

	public void setReceiver(String receiver) {
		this.receiver = receiver;
	}

	public List<NameValuePair> getExternalIds() {
		return externalIds;
	}

	public void setExternalIds(List<NameValuePair> externalIds) {
		this.externalIds = externalIds;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

}
