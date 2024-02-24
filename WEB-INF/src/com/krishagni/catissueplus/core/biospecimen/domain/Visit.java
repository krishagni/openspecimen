
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol.SpecimenLabelPrePrintMode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;

@Configurable
@Audited
@AuditTable(value="CAT_SPECIMEN_COLL_GROUP_AUD")
public class Visit extends BaseExtensionEntity {
	private static final String ENTITY_NAME = "visit";
	
	public static final String VISIT_STATUS_PENDING = "Pending";
	
	public static final String VISIT_STATUS_COMPLETED = "Complete";

	public static final String VISIT_STATUS_MISSED = "Missed Collection";

	public static final String VISIT_STATUS_NOT_COLLECTED = "Not Collected";

	public static final String EXTN = "VisitExtension";

	private String name;
	
	private Date visitDate;

	private Set<PermissibleValue> clinicalDiagnoses = new HashSet<>();

	private PermissibleValue clinicalStatus;

	private String activityStatus;

	private Site site;

	private String status;

	private String comments;

	private String surgicalPathologyNumber;
	
	private String sprName;
	
	private boolean sprLocked;

	private CollectionProtocolEvent cpEvent;

	private Set<Specimen> specimens = new HashSet<>();

	private CollectionProtocolRegistration registration;
	
	private String defNameTmpl;

	private PermissibleValue missedReason;

	private User missedBy;
	
	private PermissibleValue cohort;
	
	@Autowired
	@Qualifier("visitNameGenerator")
	private LabelGenerator labelGenerator;
	
	@Autowired
	@Qualifier("specimenLabelGenerator")
	private LabelGenerator specimenLabelGenerator;
	
	@Autowired
	private SpecimenService specimenSvc;

	@Autowired
	private VisitService visitSvc;

	@Autowired
	private DaoFactory daoFactory;
	
	private transient boolean forceDelete;

	private transient boolean updated;

	private transient boolean statusChanged;

	private transient Map<Long, String> kitLabels;
	
	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Set<PermissibleValue> getClinicalDiagnoses() {
		return clinicalDiagnoses;
	}

	public void setClinicalDiagnoses(Set<PermissibleValue> clinicalDiagnoses) {
		this.clinicalDiagnoses = clinicalDiagnoses;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(PermissibleValue clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
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

	public Site getSite() {
		return site;
	}

	public void setSite(Site site) {
		this.site = site;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

	public void setSprLocked(boolean sprLocked) {
		this.sprLocked = sprLocked;
	}

	public CollectionProtocolEvent getCpEvent() {
		return cpEvent;
	}

	public void setCpEvent(CollectionProtocolEvent cpEvent) {
		this.cpEvent = cpEvent;
	}

	public boolean isEventClosed() {
		return getCpEvent() != null && getCpEvent().isClosed();
	}

	@NotAudited
	public Set<Specimen> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(Set<Specimen> specimens) {
		this.specimens = specimens;
	}
	
	public Set<Specimen> getTopLevelSpecimens() {
		return Utility.nullSafeStream(getSpecimens()).filter(Specimen::isPrimary).collect(Collectors.toSet());
	}

	public Collection<Specimen> getOrderedTopLevelSpecimens() {
		return Specimen.sort(getTopLevelSpecimens());
	}

	public CollectionProtocolRegistration getRegistration() {
		return registration;
	}

	public void setRegistration(CollectionProtocolRegistration registration) {
		this.registration = registration;
	}

	public String getDefNameTmpl() {
		return defNameTmpl;
	}

	public void setDefNameTmpl(String defNameTmpl) {
		this.defNameTmpl = defNameTmpl;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getMissedReason() {
		return missedReason;
	}

	public void setMissedReason(PermissibleValue missedVisitReason) {
		this.missedReason = missedVisitReason;
	}

	public User getMissedBy() {
		return missedBy;
	}

	public void setMissedBy(User missedBy) {
		this.missedBy = missedBy;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getCohort() {
		return cohort;
	}

	public void setCohort(PermissibleValue cohort) {
		this.cohort = cohort;
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public void setActive() {
		this.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isDeleted() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	public boolean isUpdated() {
		return updated;
	}

	public void setUpdated(boolean updated) {
		this.updated = updated;
	}

	public boolean isStatusChanged() {
		return statusChanged;
	}

	public void setStatusChanged(boolean statusChanged) {
		this.statusChanged = statusChanged;
	}

	public Map<Long, String> getKitLabels() {
		return kitLabels;
	}

	public void setKitLabels(Map<Long, String> kitLabels) {
		this.kitLabels = kitLabels;
	}

	public boolean isCompleted() {
		return isCompleted(getStatus());
	}
	
	public boolean isPending() {
		return isPending(getStatus());
	}
	
	public boolean isMissed() {
		return isMissed(getStatus());
	}

	public boolean isNotCollected() {
		return isNotCollected(getStatus());
	}

	public boolean isMissedOrNotCollected() {
		return isMissed() || isNotCollected();
	}
	
	public boolean isUnplanned() {
		return getCpEvent() == null;
	}

	public boolean isPlanned() {
		return getCpEvent() != null;
	}

	public boolean isOfEvent(String eventLabel) {
		return !isUnplanned() && getCpEvent().getEventLabel().equals(eventLabel);
	}

	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail.singletonList(Specimen.getEntityName(), getActiveSpecimens()); 
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
		delete(!isForceDelete());
	}

	public void delete(boolean checkDependency) {
		if (checkDependency) {
			ensureNoActiveChildObjects();
		}

		Boolean hasDeleteSpmnRights = getRegistration().getHasDeleteSpecimenRights();
		for (Specimen specimen : getSpecimens()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				if (hasDeleteSpmnRights == null) {
					AccessCtrlMgr.getInstance().ensureDeleteSpecimenRights(specimen);
					if (!getCollectionProtocol().storageSiteBasedAccessRightsEnabled()) {
						hasDeleteSpmnRights = true;
						getRegistration().setHasDeleteSpecimenRights(true);
					}
				}
			}

			specimen.disable(checkDependency);
		}
		
		setName(Utility.getDisabledValue(getName(), 255));
		setSurgicalPathologyNumber(Utility.getDisabledValue(getSurgicalPathologyNumber(), 50));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		FormUtil.getInstance().deleteRecords(getCpId(), Arrays.asList("SpecimenCollectionGroup", "VisitExtension"), getId());
	}

	public void update(Visit visit) {
		setForceDelete(visit.isForceDelete());
		setOpComments(visit.getOpComments());

		updateActivityStatus(visit.getActivityStatus());
		if (!isActive()) {
			return;
		}
		
		setName(visit.getName());
		setClinicalStatus(visit.getClinicalStatus());
		updateEvent(visit.getCpEvent());
		updateRegistration(visit.getRegistration());
		setSite(visit.getSite());
		updateStatus(visit.getStatus());		
		setComments(visit.getComments());
		setMissedReason(isMissedOrNotCollected() ? visit.getMissedReason() : null);
		setMissedBy(isMissedOrNotCollected() ? visit.getMissedBy() : null);
		setSurgicalPathologyNumber(visit.getSurgicalPathologyNumber());
		setVisitDate(visit.getVisitDate());
		setCohort(visit.getCohort());
		setDefNameTmpl(visit.getDefNameTmpl());
		setExtension(visit.getExtension());
		CollectionUpdater.update(getClinicalDiagnoses(), visit.getClinicalDiagnoses());
		setUpdated(true);

	}

	public void updateSprName(String sprName) {
		setSprName(sprName);
	}	
	
	public void addSpecimen(Specimen specimen) {
		specimen.setVisit(this);
		getSpecimens().add(specimen);
	}
	
	public CollectionProtocol getCollectionProtocol() {
		return registration.getCollectionProtocol();
	}
	
	public void setNameIfEmpty() {
		if (StringUtils.isNotBlank(name)) {
			return;
		}

		String visitNameFmt = getCollectionProtocol().getVisitNameFormat();
		if (StringUtils.isBlank(visitNameFmt)) {
			visitNameFmt = defNameTmpl;
		}
		
		setName(labelGenerator.generateLabel(visitNameFmt, this));
	}
	
	public void updateStatus(String status) {
		if (StringUtils.isBlank(status)) {
			throw OpenSpecimenException.userError(VisitErrorCode.INVALID_STATUS);
		}
		
		if (status.equals(getStatus())) {
			return;
		}

		setStatus(status);
		if (isMissedOrNotCollected(status) || isPending(status)) {
			updateSpecimenStatus(status);
		} else if (isEventClosed()) {
			throw OpenSpecimenException.userError(CpeErrorCode.CLOSED, getCpEvent().getEventLabel());
		}

		setStatusChanged(true);
	}
	
	public void updateSpecimenStatus(String status) {
		Set<Specimen> topLevelSpmns = getTopLevelSpecimens();
		for (Specimen specimen : topLevelSpmns) {
			specimen.updateCollectionStatus(status);
		}

		if (Specimen.isMissed(status) || Specimen.isNotCollected(status)) {
			createSpecimens(status);
		}
	}

	public void createMissedSpecimens() {
		createSpecimens(Specimen.MISSED_COLLECTION);
	}

	public void createNotCollectedSpecimens() {
		createSpecimens(Specimen.NOT_COLLECTED);
	}

	public void createPendingSpecimens() {
		if (CollectionUtils.isNotEmpty(getSpecimens()) || getCpEvent() == null) {
			//
			// We quit if there is at least one specimen object created for visit or the visit is unplanned
			//
			return;
		}

		for (SpecimenRequirement sr : getCpEvent().getOrderedTopLevelAnticipatedSpecimens()) {
			createPendingSpecimen(sr, null);
		}
	}

	public boolean hasPhiFields() {
		return StringUtils.isNotBlank(getSurgicalPathologyNumber()) || super.hasPhiFields();
	}
	
	public static boolean isCompleted(String status) {
		return Visit.VISIT_STATUS_COMPLETED.equals(status);
	}
	
	public static boolean isPending(String status) {
		return Visit.VISIT_STATUS_PENDING.equals(status);
	}
	
	public static boolean isMissed(String status) {
		return Visit.VISIT_STATUS_MISSED.equals(status);
	}

	public static boolean isNotCollected(String status) {
		return Visit.VISIT_STATUS_NOT_COLLECTED.equals(status);
	}

	public static boolean isMissedOrNotCollected(String status) {
		return isMissed(status) || isNotCollected(status);
	}

	public void printLabels(String prevStatus) {
		printVisitName(prevStatus);
		prePrintSpecimenLabels(prevStatus);
	}

	public void printVisitName(String prevStatus) {
		if (!shouldPrintVisitName(prevStatus)) {
			return;
		}

		Integer copies = getCpEvent().getVisitNamePrintCopiesToUse();
		visitSvc.getLabelPrinter().print(Collections.singletonList(PrintItem.make(this, copies)));
	}

	public boolean isPrePrintSpecimenLabelEnabled() {
		CollectionProtocol cp = getCollectionProtocol();
		SpecimenLabelPrePrintMode mode = cp.getSpmnLabelPrePrintMode();
		return (mode == SpecimenLabelPrePrintMode.ON_REGISTRATION || mode == SpecimenLabelPrePrintMode.ON_VISIT) && !cp.isManualSpecLabelEnabled();
	}

	public boolean shouldPrePrintSpecimenLabels(String prevStatus) {
		if (!isPrePrintSpecimenLabelEnabled()) {
			return false;
		}

		if (StringUtils.isBlank(prevStatus)) {
			return !isMissedOrNotCollected();
		} else {
			return isMissedOrNotCollected(prevStatus) && !isMissedOrNotCollected();
		}
	}
	
	public void prePrintSpecimenLabels(String prevStatus) {
		if (!shouldPrePrintSpecimenLabels(prevStatus)) {
			return;
		}

		//
		// As a first step we create all pending specimens with their labels set
		//
		createPendingSpecimens();

		//
		// Go through individual specimen that have pre print enabled
		// and create queue of print items by putting specimens and copies to print for printing
		//
		specimenSvc.getLabelPrinter().print(getSpecimenPrintItems(getTopLevelSpecimens()));
	}
		
	@Override
	public String getEntityType() {
		return EXTN;
	}

	@Override
	public Long getCpId() {
		return getCollectionProtocol().getId();
	}

	public boolean hasCollectedSpecimens() {
		return getSpecimens().stream().anyMatch(s -> s.isActiveOrClosed() && s.isCollected());
	}

	public String getDescription() {
		String desc = "";
		if (StringUtils.isNotBlank(getName())) {
			desc = getName() + ", ";
		}

		if (getCpEvent() != null) {
			desc += getCpEvent().getEventLabel();
			if (StringUtils.isNotBlank(getCpEvent().getCode())) {
				desc += ", " + getCpEvent().getCode();
			}
		} else {
			desc += "Unplanned";
		}

		if (getId() != null) {
			desc += ", " + getId();
		}

		return desc;
	}

	public static LabelPrinter<Visit> getLabelPrinter() {
		String beanName = ConfigUtil.getInstance().getStrSetting(
			ConfigParams.MODULE, ConfigParams.VISIT_LABEL_PRINTER, "defaultVisitLabelPrinter");
		return (LabelPrinter<Visit>) OpenSpecimenAppCtxProvider.getAppCtx().getBean(beanName);
	}

	private void ensureNoActiveChildObjects() {
		if (hasCollectedSpecimens()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.REF_ENTITY_FOUND);
		}
	}
	
	private int getActiveSpecimens() {
		int count = 0;
		for (Specimen specimen : getSpecimens()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				++count;
			}
		}
		
		return count;
	}

	private void createSpecimens(String status) {
		Set<SpecimenRequirement> anticipated;
		if (getCpEvent() == null) {
			anticipated = new HashSet<>();
		} else {
			anticipated = getCpEvent().getTopLevelAnticipatedSpecimens();
		}

		for (Specimen specimen : getTopLevelSpecimens()) {
			if (specimen.getSpecimenRequirement() != null) {
				anticipated.remove(specimen.getSpecimenRequirement());
			}
		}

		for (SpecimenRequirement sr : anticipated) {
			Specimen specimen = sr.getSpecimen();
			specimen.setVisit(this);
			specimen.updateCollectionStatus(status);
			addSpecimen(specimen);
		}
	}

	private Specimen createPendingSpecimen(SpecimenRequirement sr, Specimen parent) {
		if (sr.isClosed()) {
			return null;
		}

		Specimen specimen = sr.getSpecimen();
		specimen.setParentSpecimen(parent);
		specimen.setVisit(this);
		specimen.setCollectionStatus(Specimen.PENDING);
		specimen.updateAvailableStatus();
		specimen.setLabelIfEmpty();

		addSpecimen(specimen);
		daoFactory.getSpecimenDao().saveOrUpdate(specimen);
		EventPublisher.getInstance().publish(new SpecimenSavedEvent(specimen));

		for (SpecimenRequirement childSr : sr.getOrderedChildRequirements()) {
			Specimen childSpmn = createPendingSpecimen(childSr, specimen);
			if (childSpmn == null) {
				continue;
			}

			specimen.addChildSpecimen(childSpmn);
		}

		return specimen;
	}

	private List<PrintItem<Specimen>> getSpecimenPrintItems(Collection<Specimen> specimens) {
		return Specimen.sort(specimens).stream()
			.map(Specimen::getPrePrintItems)
			.flatMap(List::stream)
			.collect(Collectors.toList());
	}

	private boolean shouldPrintVisitName(String prevStatus) {
		if (isMissedOrNotCollected() || isUnplanned()) {
			return false;
		}

		switch (getCpEvent().getVisitNamePrintModeToUse()) {
			case PRE_PRINT:
				return StringUtils.isBlank(prevStatus) || isMissedOrNotCollected(prevStatus);

			case ON_COMPLETION:
				return isCompleted() && !isCompleted(prevStatus);

			default:
				return false;
		}
	}

	private void updateEvent(CollectionProtocolEvent newEvent) {
		CollectionProtocolEvent existingEvent = getCpEvent();
		setCpEvent(newEvent);

		if (Objects.equals(existingEvent, newEvent)) {
			return;
		}

		// events differ. nullify the specimen requirements
		for (Specimen spmn : getSpecimens()) {
			spmn.setSpecimenRequirement(null);
		}
	}

	private void updateRegistration(CollectionProtocolRegistration newCpr) {
		CollectionProtocolRegistration existingCpr = getRegistration();
		setRegistration(newCpr);

		if (existingCpr.equals(newCpr)) {
			return;
		}

		if (existingCpr.getCollectionProtocol().equals(newCpr.getCollectionProtocol())) {
			return;
		}

		// CPs differ
		for (Specimen spmn : getSpecimens()) {
			spmn.setSpecimenRequirement(null);
			spmn.setCollectionProtocol(newCpr.getCollectionProtocol());
		}
	}
}
