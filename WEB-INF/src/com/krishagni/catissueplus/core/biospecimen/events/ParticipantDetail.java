
package com.krishagni.catissueplus.core.biospecimen.events;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@ListenAttributeChanges
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParticipantDetail extends AttributeModifiedSupport {
	private static final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

	private Long id;

	private String source;
	
	private String firstName;

	private String lastName;
	
	private String middleName;

	private String emailAddress;

	private Boolean emailOptIn;

	private String phoneNumber;

	private Boolean textOptIn;

	private Boolean textOptInConsent;

	private Date birthDate;

	private Date deathDate;

	private String gender;

	private Set<String> races;

	private String vitalStatus;

	private List<PmiDetail> pmis;

	private String sexGenotype;

	private Set<String> ethnicities;

	private String uid;

	private String activityStatus;
	
	private String empi;
	
	private boolean phiAccess;
	
	private Set<CprSummary> registeredCps;
	
	private ExtensionDetail extensionDetail;

	private Long stagedId;

	// For Update participant through BO
	private String cpShortTitle;
	
	private String ppid;

	// Used for CP based custom fields
	private Long cpId = -1L;

	private String dataEntryStatus;

	//
	// Used in matching API to decide whether to populate registration
	// info or not
	//
	private boolean reqRegInfo;
	
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

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
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

	public Boolean getTextOptInConsent() {
		return textOptInConsent;
	}

	public void setTextOptInConsent(Boolean textOptInConsent) {
		this.textOptInConsent = textOptInConsent;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public String getBirthDateStr() {
		return birthDate != null ? sdf.format(birthDate) : null;
	}

	@JsonDeserialize(using = com.krishagni.catissueplus.core.common.util.JsonDateDeserializer.class)
	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public void setBirthDateStr(String birthDateStr) {

	}

	public Date getDeathDate() {
		return deathDate;
	}

	public String getDeathDateStr() {
		return deathDate != null ? sdf.format(deathDate) : null;
	}

	@JsonDeserialize(using = com.krishagni.catissueplus.core.common.util.JsonDateDeserializer.class)
	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public void setDeathDateStr(String deathDateStr) {

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
	
	public void setPmi(PmiDetail pmi) {
		this.pmis = Collections.singletonList(pmi);
	}

	public String getSexGenotype() {
		return sexGenotype;
	}

	public void setSexGenotype(String sexGenotype) {
		this.sexGenotype = sexGenotype;
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

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getEmpi() {
		return empi;
	}

	public void setEmpi(String empi) {
		this.empi = empi;
	}
	
	public boolean getPhiAccess() {
		return phiAccess;
	}

	public void setPhiAccess(boolean phiAccess) {
		this.phiAccess = phiAccess;
	}

	public Set<CprSummary> getRegisteredCps() {
		return registeredCps;
	}

	public void setRegisteredCps(Set<CprSummary> registeredCps) {
		this.registeredCps = registeredCps;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public Long getStagedId() {
		return stagedId;
	}

	public void setStagedId(Long stagedId) {
		this.stagedId = stagedId;
	}

	@JsonIgnore
	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	@JsonIgnore
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

	public String getDataEntryStatus() {
		return dataEntryStatus;
	}

	public void setDataEntryStatus(String dataEntryStatus) {
		this.dataEntryStatus = dataEntryStatus;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public boolean isReqRegInfo() {
		return reqRegInfo;
	}

	public void setReqRegInfo(boolean reqRegInfo) {
		this.reqRegInfo = reqRegInfo;
	}

	public static ParticipantDetail from(Participant participant, boolean excludePhi) {
		return from(participant, excludePhi, null);
	}

	public static ParticipantDetail from(Participant participant, boolean excludePhi, List<CollectionProtocolRegistration> cprs) {
		if (participant == null) {
			return null;
		}

		ParticipantDetail result = new ParticipantDetail();
		result.setId(participant.getId());
		result.setSource(participant.getSource());
		result.setFirstName(excludePhi ? "###" : participant.getFirstName());
		result.setLastName(excludePhi ? "###" : participant.getLastName());
		result.setMiddleName(excludePhi ? "###" : participant.getMiddleName());
		result.setEmailAddress(excludePhi ? "###" : participant.getEmailAddress());
		result.setEmailOptIn(participant.isEmailOptIn());
		result.setPhoneNumber(excludePhi ? "###" : participant.getPhoneNumber());
		result.setTextOptIn(participant.isTextOptIn());
		result.setTextOptInConsent(participant.getTextOptInConsent());
		result.setActivityStatus(participant.getActivityStatus());
		result.setBirthDate(excludePhi ? null : participant.getBirthDate());
		result.setDeathDate(excludePhi ? null : participant.getDeathDate());
		result.setEthnicities(PermissibleValue.toValueSet(participant.getEthnicities()));
		result.setGender(PermissibleValue.getValue(participant.getGender()));
		result.setEmpi(excludePhi ? "###" : participant.getEmpi());
		result.setPmis(PmiDetail.from(participant.getPmisOrderedById(), excludePhi));
		result.setRaces(PermissibleValue.toValueSet(participant.getRaces()));
		result.setSexGenotype(participant.getSexGenotype());
		result.setUid(excludePhi ? "###" : participant.getUid());
		result.setVitalStatus(PermissibleValue.getValue(participant.getVitalStatus()));
		result.setPhiAccess(!excludePhi);
		result.setRegisteredCps(getCprSummaries(cprs));
		result.setExtensionDetail(ExtensionDetail.from(participant.getExtension(), excludePhi));
		return result;
	}
	
	public static List<ParticipantDetail> from(List<Participant> participants, boolean excludePhi) {
		return participants.stream()
			.map(participant -> ParticipantDetail.from(participant, excludePhi))
			.collect(Collectors.toList());
	}
	
	public static Set<CprSummary> getCprSummaries(List<CollectionProtocolRegistration> cprs) {
		if (CollectionUtils.isEmpty(cprs)) {
			return Collections.emptySet();
		}

		return cprs.stream().map(ParticipantDetail::getCprSummary).collect(Collectors.toSet());
	}

	private static CprSummary getCprSummary(CollectionProtocolRegistration cpr) {
		CprSummary cprSummary = new CprSummary();
		cprSummary.setCpId(cpr.getCollectionProtocol().getId());
		cprSummary.setCpShortTitle(cpr.getCollectionProtocol().getShortTitle());
		cprSummary.setCprId(cpr.getId());
		cprSummary.setPpid(cpr.getPpid());
		cprSummary.setRegistrationDate(cpr.getRegistrationDate());
		return cprSummary;
	}
}
