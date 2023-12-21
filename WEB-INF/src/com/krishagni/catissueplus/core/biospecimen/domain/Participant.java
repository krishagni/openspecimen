
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantUtil;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.MpiGenerator;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;

@Audited
public class Participant extends BaseExtensionEntity {
	private static final String ENTITY_NAME = "participant";

	public static final String DEF_SOURCE = "OpenSpecimen";

	public static final String EXTN = "ParticipantExtension";

	private String source = DEF_SOURCE;

	private String lastName;

	private String firstName;

	private String middleName;

	private Date birthDate;

	private String emailAddress;

	private Boolean emailOptIn;

	private String phoneNumber;

	private Boolean textOptIn;

	private Boolean textOptInConsent;

	private PermissibleValue gender;

	private String sexGenotype;

	private Set<PermissibleValue> races = new HashSet<>();

	private Set<PermissibleValue> ethnicities = new HashSet<>();

	private String uid;

	private String activityStatus;

	private Date deathDate;

	private PermissibleValue vitalStatus;
	
	private String empi;
	
	private Set<ParticipantMedicalIdentifier> pmis = new HashSet<>();

	private Set<CollectionProtocolRegistration> cprs = new HashSet<>();

	private transient Long cpId = -1L;

	private transient Set<Long> oldCprIds;

	private transient Set<Long> newCprIds;

	private transient Set<Long> kwAddedCprIds;

	public String getSource() {
		return StringUtils.isBlank(source) ? DEF_SOURCE : source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getMiddleName() {
		return middleName;
	}

	public void setMiddleName(String middleName) {
		this.middleName = middleName;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		if (StringUtils.isBlank(emailAddress)) {
			//
			// this is done to avoid UQ violation because of ''
			// For more details, refer to OPSMN-5344
			//
			this.emailAddress = null;
		} else {
			this.emailAddress = emailAddress;
		}
	}

	public boolean isEmailOptIn() {
		return Boolean.TRUE.equals(emailOptIn);
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

	public boolean isTextOptIn() {
		return Boolean.TRUE.equals(textOptIn);
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

	public PermissibleValue getGender() {
		return gender;
	}

	public void setGender(PermissibleValue gender) {
		this.gender = gender;
	}

	public String getSexGenotype() {
		return sexGenotype;
	}

	public void setSexGenotype(String sexGenotype) {
		this.sexGenotype = sexGenotype;
	}

	public Set<PermissibleValue> getRaces() {
		return races;
	}

	public void setRaces(Set<PermissibleValue> races) {
		this.races = races;
	}

	public Set<PermissibleValue> getEthnicities() {
		return ethnicities;
	}

	public void setEthnicities(Set<PermissibleValue> ethnicities) {
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
		if (this.activityStatus != null && this.activityStatus.equals(activityStatus)) {
			return;
		}
		
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (this.activityStatus != null && Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus)) {
			delete();
		}		

		this.activityStatus = activityStatus;
	}

	public Date getDeathDate() {
		return deathDate;
	}

	public void setDeathDate(Date deathDate) {
		this.deathDate = deathDate;
	}

	public PermissibleValue getVitalStatus() {
		return vitalStatus;
	}

	public void setVitalStatus(PermissibleValue vitalStatus) {
		this.vitalStatus = vitalStatus;
	}

	public String getEmpi() {
		return empi;
	}

	public void setEmpi(String empi) {
		this.empi = empi;
	}
	
	public Set<ParticipantMedicalIdentifier> getPmis() {
		return pmis;
	}

	public void setPmis(Set<ParticipantMedicalIdentifier> pmis) {
		this.pmis = pmis;
	}

	@NotAudited
	public Set<CollectionProtocolRegistration> getCprs() {
		return cprs;
	}

	public void setCprs(Set<CollectionProtocolRegistration> cprs) {
		this.cprs = cprs;
	}

	public Set<Long> getOldCprIds() {
		return oldCprIds;
	}

	public void setOldCprIds() {
		oldCprIds = getCprs().stream().map(CollectionProtocolRegistration::getId).collect(Collectors.toSet());
	}

	public Set<Long> getNewCprIds() {
		return newCprIds;
	}

	public void setNewCprIds(Set<Long> newCprIds) {
		this.newCprIds = newCprIds;
	}

	public Set<Long> getKwAddedCprIds() {
		return kwAddedCprIds;
	}

	public void setKwAddedCprIds(Set<Long> kwAddedCprIds) {
		this.kwAddedCprIds = kwAddedCprIds;
	}

	public void addKwAddedCprId(Long cprId) {
		if (kwAddedCprIds == null) {
			kwAddedCprIds = new HashSet<>();
		}

		kwAddedCprIds.add(cprId);
	}

	public void update(Participant participant) {
		setFirstName(participant.getFirstName());
		setLastName(participant.getLastName());
		setMiddleName(participant.getMiddleName());
		setUid(participant.getUid());
		setEmpi(participant.getEmpi());
		setEmailAddress(participant.getEmailAddress());
		setEmailOptIn(participant.isEmailOptIn());
		setPhoneNumber(participant.getPhoneNumber());
		setTextOptIn(participant.isTextOptIn());
		setActivityStatus(participant.getActivityStatus());
		setSexGenotype(participant.getSexGenotype());
		setVitalStatus(participant.getVitalStatus());
		setGender(participant.getGender());
		setBirthDate(participant.getBirthDate());
		setDeathDate(participant.getDeathDate());
		setExtension(participant.getExtension());
		CollectionUpdater.update(getEthnicities(), participant.getEthnicities());
		CollectionUpdater.update(getRaces(), participant.getRaces());
		updatePmis(participant);

		if (Boolean.FALSE.equals(getTextOptInConsent())) {
			if (isTextOptIn()) {
				throw OpenSpecimenException.userError(ParticipantErrorCode.TEXT_OPT_OUT);
			}
		}
	}

	public void updateActivityStatus(String activityStatus) {
		setActivityStatus(activityStatus);
	}
	
	public void setActive() {
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isDeleted() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	public void delete() {
		checkActiveDependents();

		disableMrns();		
		setUid(Utility.getDisabledValue(getUid(), 50));
		setEmpi(Utility.getDisabledValue(getEmpi(), 50));
		setEmailAddress(Utility.getDisabledValue(getEmailAddress(), 255));
		activityStatus = Status.ACTIVITY_STATUS_DISABLED.getStatus();
		FormUtil.getInstance().deleteRecords(-1L, Collections.singletonList("CommonParticipant"), getId());
	}

	public void updatePmi(ParticipantMedicalIdentifier pmi) {
		ParticipantMedicalIdentifier existing = getPmiBySite(getPmis(), pmi.getSite().getName());
		if (existing == null) {
			pmi.setParticipant(this);
			getPmis().add(pmi);
		} else {
			existing.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
		}
	}
	
	public Set<Site> getMrnSites() {
		return getPmis().stream().map(ParticipantMedicalIdentifier::getSite).collect(Collectors.toSet());
	}
	
	public void setEmpiIfEmpty() {
		MpiGenerator generator = ParticipantUtil.getMpiGenerator();
		if (generator == null) {
			return;
		}

		if (StringUtils.isNotBlank(empi)) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.MANUAL_MPI_NOT_ALLOWED);
		}
		
		setEmpi(generator.generateMpi());
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	@Override
	public Long getCpId() {
		return cpId;
	}

	public List<ParticipantMedicalIdentifier> getPmisOrderedById() {
		return getPmis().stream()
			.sorted((p1, p2) -> ObjectUtils.compare(p1.getId(), p2.getId()))
			.collect(Collectors.toList());
	}
	
	public CollectionProtocolRegistration getCpr(CollectionProtocol cp) {
		return getCprs().stream().filter(cpr -> cpr.getCollectionProtocol().equals(cp)).findFirst().orElse(null);
	}

	public String formattedName() {
		StringBuilder name = new StringBuilder();
		if (StringUtils.isNotBlank(firstName)) {
			name.append(firstName);
		}

		if (StringUtils.isNotBlank(lastName)) {
			if (name.length() > 0) {
				name.append(" ");
			}

			name.append(lastName);
		}

		return name.toString();
	}

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	private void updatePmis(Participant participant) {
		for (ParticipantMedicalIdentifier pmi : participant.getPmis()) {
			ParticipantMedicalIdentifier existing = getPmiBySite(getPmis(), pmi.getSite().getName());
			if (existing == null) {
				ParticipantMedicalIdentifier newPmi = new ParticipantMedicalIdentifier();
				newPmi.setParticipant(this);
				newPmi.setSite(pmi.getSite());
				newPmi.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
				getPmis().add(newPmi);				
			} else {
				existing.setMedicalRecordNumber(pmi.getMedicalRecordNumber());
			}
		}

		getPmis().removeIf(pmi -> getPmiBySite(participant.getPmis(), pmi.getSite().getName()) == null);
	}

	private void disableMrns() {
		for (ParticipantMedicalIdentifier pmi : getPmis()) {
			pmi.setMedicalRecordNumber(Utility.getDisabledValue(pmi.getMedicalRecordNumber(), 255));
		}
	}

	private void checkActiveDependents() {
		for (CollectionProtocolRegistration cpr : getCprs()) {
			if (cpr.isActive()) {
				throw OpenSpecimenException.userError(ParticipantErrorCode.REF_ENTITY_FOUND);
			}
		}
	}
	
	private ParticipantMedicalIdentifier getPmiBySite(Collection<ParticipantMedicalIdentifier> pmis, String siteName) {
		ParticipantMedicalIdentifier result = null;
		
		for (ParticipantMedicalIdentifier pmi : pmis) {
			if (pmi.getSite().getName().equals(siteName)) {
				result = pmi;
				break;				
			}
		}
		
		return result;
	}
}
