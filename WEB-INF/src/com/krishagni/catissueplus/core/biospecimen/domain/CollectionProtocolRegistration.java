
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

@Configurable
@Audited
public class CollectionProtocolRegistration extends BaseExtensionEntity {
	private static final String ENTITY_NAME = "collection_protocol_registration";

	private static final String EXTN = "ParticipantExtension";
	
	private String ppid;

	private Date registrationDate;

	private String externalSubjectId;

	private Site site;

	private Participant participant;

	private CollectionProtocol collectionProtocol;

	private Collection<Visit> visits = new HashSet<>();

	private String activityStatus;

	private Date consentSignDate;

	private User consentWitness;
	
	private String consentComments;
	
	private String signedConsentDocumentName;

	private Set<ConsentTierResponse> consentResponses = new HashSet<>();

	private String barcode;

	private Integer extensionRev;
	
	@Autowired
	@Qualifier("ppidGenerator")
	private LabelGenerator labelGenerator;

	@Autowired
	private DaoFactory daoFactory;

	private transient boolean forceDelete;

	private transient Boolean hasDeleteSpecimenRights;

	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
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
		if (StringUtils.isBlank(externalSubjectId)) {
			//
			// this is done to avoid UQ violation because of ''
			// For more details, refer to OPSMN-5348
			//
			this.externalSubjectId = null;
		} else {
			this.externalSubjectId = externalSubjectId;
		}
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public Participant getParticipant() {
		return participant;
	}

	public void setParticipant(Participant participant) {
		this.participant = participant;
		if (participant != null) {
			participant.setCpr(this);
		}
	}

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public String getCpShortTitle() {
		return getCollectionProtocol().getShortTitle();
	}

	public void setExtensionRev(Integer extensionRev) {
		this.extensionRev = extensionRev;
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	@Override
	public Long getRecordId(Long formId, Long formCtxtId) {
		return daoFactory.getCprDao().getCustomFieldRecordId(getId(), formId, formCtxtId);
	}

    @Override
    public Map<Long, Long> getRecordIds(Collection<Long> cprIds, Long formId, Long formCtxtId) {
		return daoFactory.getCprDao().getCustomFieldRecordIds(cprIds, formId, formCtxtId);
	}

	@Override
	public boolean saveOrUpdateRecordEntry(boolean insert, Long formId, Long formCtxtId, Long recordId) {
		String status = getDataEntryStatus() != null ? getDataEntryStatus().name() : "COMPLETE";
		if (insert) {
			daoFactory.getCprDao().insertCustomFieldRecordId(getId(), formId, formCtxtId, recordId, status);
		} else {
			daoFactory.getCprDao().updateCustomFieldRecStatus(getId(), formId, recordId, status);
		}

		return true;
	}

	@Override
	public Long getCpId() {
		return getCollectionProtocol().getId();
	}

	@NotAudited
	public Collection<Visit> getVisits() {
		return visits;
	}

	public void setVisits(Collection<Visit> visits) {
		this.visits = visits;
	}

	public List<Visit> getOrderedVisits() {
		if (getVisits() == null) {
			return Collections.emptyList();
		}

		return getVisits().stream()
			.sorted((v1, v2) -> {
				if (v1.getVisitDate() != null && v2.getVisitDate() != null) {
					return v1.getVisitDate().compareTo(v2.getVisitDate());
				} else if (v1.getVisitDate() != null) {
					return -1;
				} else if (v2.getVisitDate() != null) {
					return 1;
				} else {
					return v1.getId().compareTo(v2.getId());
				}
			})
			.collect(Collectors.toList());
	}

	public Visit getLatestVisit() {
		List<Visit> sortedVisits = getOrderedVisits();
		if (CollectionUtils.isEmpty(sortedVisits)) {
			return null;
		}

		return sortedVisits.get(sortedVisits.size() - 1);
	}

	public Visit getVisitByName(String visitName) {
		return getVisits().stream().filter(v -> v.getName().equals(visitName)).findFirst().orElse(null);
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		this.activityStatus = activityStatus;
	}
	
	public Date getConsentSignDate() {
		return consentSignDate;
	}

	public void setConsentSignDate(Date consentSignDate) {
		this.consentSignDate = consentSignDate;
	}

	public User getConsentWitness() {
		return consentWitness;
	}

	public void setConsentWitness(User consentWitness) {
		this.consentWitness = consentWitness;
	}

	public String getConsentComments() {
		return consentComments;
	}

	public void setConsentComments(String consentComments) {
		this.consentComments = consentComments;
	}

	public String getSignedConsentDocumentName() {
		return signedConsentDocumentName;
	}

	public void setSignedConsentDocumentName(String signedConsentDocumentName) {
		this.signedConsentDocumentName = signedConsentDocumentName;
	}

	public Set<ConsentTierResponse> getConsentResponses() {
		return consentResponses;
	}

	public void setConsentResponses(Set<ConsentTierResponse> consentResponses) {
		this.consentResponses = consentResponses;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public Boolean getHasDeleteSpecimenRights() {
		return hasDeleteSpecimenRights;
	}

	public void setHasDeleteSpecimenRights(Boolean hasDeleteSpecimenRights) {
		this.hasDeleteSpecimenRights = hasDeleteSpecimenRights;
	}

	public boolean isDeleted() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public void setActive() {
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}

	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail.singletonList(Visit.getEntityName(), getActiveVisits()); 
	}

	public void updateActivityStatus(String activityStatus) {
		if (this.activityStatus != null && this.activityStatus.equals(activityStatus)) {
			return;
		}
		
		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus)) {
			delete();
		}
	}
	
	public void delete() {
		delete(!isForceDelete(), false);
	}

	public void delete(boolean checkDependency) {
		delete(checkDependency, false);
	}

	public void delete(boolean checkDependency, boolean checkOnlyCollectedSpmns) {
		if (checkDependency) {
			ensureNoActiveChildObjects(checkOnlyCollectedSpmns);
		}

		Boolean hasDeleteVisitRights = null;
		for (Visit visit : getVisits()) {
			if (visit.isActive() && visit.isCompleted()) {
				if (hasDeleteVisitRights == null) {
					hasDeleteVisitRights = AccessCtrlMgr.getInstance().hasDeleteVisitRights(this);
				}

				if (!hasDeleteVisitRights) {
					throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
				}
			}

			visit.delete(checkDependency);
		}
		
		setBarcode(Utility.getDisabledValue(getBarcode(), 255));
		setPpid(Utility.getDisabledValue(getPpid(), 255));
		setExternalSubjectId(Utility.getDisabledValue(getExternalSubjectId(), 255));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		FormUtil.getInstance().deleteRecords(getCollectionProtocol().getId(), Collections.singletonList("Participant"), getId());
		FormUtil.getInstance().deleteRecords(getCollectionProtocol().getId(), Collections.singletonList("ParticipantExtension"), getParticipant().getId());

		//
		// If participant is registered to only this deleted registration
		// then delete participant as well
		//
		int regCount = getParticipant().getCprs().size();
		CollectionProtocolRegistration cpr = getParticipant().getCprs().iterator().next();
		if (regCount == 1 && this.equals(cpr)) {
			getParticipant().delete();
		}
	}

	public void update(CollectionProtocolRegistration cpr) {
		setForceDelete(cpr.isForceDelete());
		setOpComments(cpr.getOpComments());
		setDataEntryStatus(cpr.getDataEntryStatus());
		if (getDataEntryStatus() == DataEntryStatus.DRAFT && !getCollectionProtocol().draftDataEntryEnabled()) {
			throw OpenSpecimenException.userError(CprErrorCode.DRAFT_NOT_ALLOWED, getCollectionProtocol().getShortTitle());
		}

		updateActivityStatus(cpr.getActivityStatus());
		if (!isActive()) {
			return;
		}

		if (StringUtils.isNotBlank(cpr.getPpid())) {
			setPpid(cpr.getPpid());
		}

		if (cpr.getRegistrationDate() != null) {
			setRegistrationDate(cpr.getRegistrationDate());
		}

		if (cpr.getParticipant() != null) {
			setParticipant(cpr.getParticipant());
		}

		setBarcode(cpr.getBarcode());
		setExternalSubjectId(cpr.getExternalSubjectId());
		setSite(cpr.getSite());
		setExtension(cpr.getExtension());
	}
	
	public void updateConsents(ConsentResponses consentResponses) {
		setConsentSignDate(consentResponses.getConsentSignDate());
		setConsentWitness(consentResponses.getConsentWitness());
		setConsentComments(consentResponses.getConsentComments());
		updateConsentResponses(consentResponses.getConsentResponses());
	}
	
	public void setPpidIfEmpty() {
		if (StringUtils.isNotBlank(ppid)) {
			return;
		}
		
		CollectionProtocol cp = getCollectionProtocol();
		String ppidFmt = cp.getPpidFormat();
		if (StringUtils.isNotBlank(ppidFmt)) {
			setPpid(labelGenerator.generateLabel(ppidFmt, this));
		} else {
			setPpid(cp.getId() + "_" + participant.getId());
		}
	}

	public boolean isSpecimenLabelPrePrintOnRegEnabled() {
		return !getCollectionProtocol().isManualSpecLabelEnabled() &&
			getCollectionProtocol().getSpmnLabelPrePrintMode() == CollectionProtocol.SpecimenLabelPrePrintMode.ON_REGISTRATION;
	}

	public void addVisit(Visit visit) {
		visit.setRegistration(this);
		getVisits().add(visit);
	}
	
	public void addVisits(Collection<Visit> visits) {
		visits.forEach(this::addVisit);
	}

	private void updateConsentResponses(Collection<ConsentTierResponse> consentResponses) {
		Map<String, ConsentTierResponse> existingResps = getConsentResponses().stream()
			.collect(Collectors.toMap(ConsentTierResponse::getStatementCode, resp -> resp));

		for(ConsentTierResponse newResp : consentResponses) {
			ConsentTierResponse existingResp = existingResps.remove(newResp.getStatementCode());
			if (existingResp == null) {
				getConsentResponses().add(newResp);
			} else {
				existingResp.setResponse(newResp.getResponse());
			}
		}
		
		getConsentResponses().removeAll(existingResps.values());
	}

	private void ensureNoActiveChildObjects(boolean checkOnlyCollectedSpmns) {
		for (Visit visit : getVisits()) {
			if (checkOnlyCollectedSpmns && visit.hasCollectedSpecimens()) {
				throw OpenSpecimenException.userError(ParticipantErrorCode.REF_ENTITY_FOUND);
			} else if (!checkOnlyCollectedSpmns && visit.isActive() && visit.isCompleted()) {
				throw OpenSpecimenException.userError(ParticipantErrorCode.REF_ENTITY_FOUND);
			}
		}
	}	
	
	private int getActiveVisits() {
		int count = 0;
		for (Visit visit : getVisits()) {
			if (visit.isActive() && visit.isCompleted()) {
				++count;
			}
		}
		
		return count;
	}
}
