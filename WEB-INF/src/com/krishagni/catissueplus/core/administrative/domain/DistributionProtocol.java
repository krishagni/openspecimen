
package com.krishagni.catissueplus.core.administrative.domain;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

import krishagni.catissueplus.beans.FormContextBean;

@Configurable
@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
@AuditTable(value = "CAT_DISTRIBUTION_PROTOCOL_AUD")
public class DistributionProtocol extends BaseExtensionEntity {
	public static final String EXTN = "DistributionProtocolExtension";

	public enum NotifAttachmentType {
		NONE,

		CSV_REPORT,

		MANIFEST,

		BOTH
	}

	private static final String ENTITY_NAME = "distribution_protocol";

	private Institute institute;
	
	private Site defReceivingSite;

	private User principalInvestigator;

	private Set<User> coordinators = new HashSet<>();

	private String title;

	private String shortTitle;

	private String irbId;

	private Date startDate;
	
	private Date endDate;

	private String activityStatus;
	
	private SavedQuery report;

	private Form orderExtnForm;

	private Boolean disableEmailNotifs;

	private NotifAttachmentType attachmentType;

	private String orderItemLabelFormat;
	
	private Set<DistributionOrder> distributionOrders = new HashSet<>();
	
	private Set<DpDistributionSite> distributingSites = new HashSet<>();
	
	private Set<DpRequirement> requirements = new HashSet<>();
	
	private Set<DpConsentTier> consentTiers = new HashSet<>();

	private Set<StorageContainer> distributionContainers = new HashSet<>();

	@Autowired
	private DaoFactory daoFactory;
	
	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public Institute getInstitute() {
		return institute;
	}

	public void setInstitute(Institute institute) {
		this.institute = institute;
	}
	
	public Site getDefReceivingSite() {
		return defReceivingSite;
	}
	
	public void setDefReceivingSite(Site defReceivingSite) {
		this.defReceivingSite = defReceivingSite;
	}

	public User getPrincipalInvestigator() {
		return principalInvestigator;
	}

	public void setPrincipalInvestigator(User principalInvestigator) {
		this.principalInvestigator = principalInvestigator;
	}

	public Set<User> getCoordinators() {
		return coordinators;
	}

	public void setCoordinators(Set<User> coordinators) {
		this.coordinators = coordinators;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public String getIrbId() {
		return irbId;
	}

	public void setIrbId(String irbId) {
		this.irbId = irbId;
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

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public boolean isClosed() {
		return Status.isClosedStatus(getActivityStatus());
	}

	public SavedQuery getReport() {
		return report;
	}

	public void setReport(SavedQuery report) {
		this.report = report;
	}

	public Form getOrderExtnForm() {
		return orderExtnForm;
	}

	public void setOrderExtnForm(Form orderExtnForm) {
		this.orderExtnForm = orderExtnForm;
	}

	public Boolean getDisableEmailNotifs() {
		return disableEmailNotifs;
	}

	public void setDisableEmailNotifs(Boolean disableEmailNotifs) {
		this.disableEmailNotifs = disableEmailNotifs;
	}

	public boolean areEmailNotifsDisabled() {
		return Boolean.TRUE.equals(disableEmailNotifs);
	}

	public NotifAttachmentType getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(NotifAttachmentType attachmentType) {
		this.attachmentType = attachmentType;
	}

	public String getOrderItemLabelFormat() {
		return orderItemLabelFormat;
	}

	public void setOrderItemLabelFormat(String orderItemLabelFormat) {
		this.orderItemLabelFormat = orderItemLabelFormat;
	}

	@NotAudited
	public Set<DistributionOrder> getDistributionOrders() {
		return distributionOrders;
	}

	public void setDistributionOrders(Set<DistributionOrder> distributionOrders) {
		this.distributionOrders = distributionOrders;
	}
	
	public Set<DpDistributionSite> getDistributingSites() {
		return distributingSites;
	}
	
	public void setDistributingSites(Set<DpDistributionSite> distributingSites) {
		this.distributingSites = distributingSites;
	}

	@NotAudited
	public Set<SiteCpPair> getAllowedDistributingSites() {
		return getAllowedDistributingSites("DistributionProtocol");
	}

	@NotAudited
	public Set<SiteCpPair> getAllowedDistributingSites(String resource) {
		return getDistributingSites().stream().map(
			(distSite) -> {
				Long siteId = distSite.getSite() != null ? distSite.getSite().getId() : null;
				return SiteCpPair.make(resource, distSite.getInstitute().getId(), siteId, null);
			}
		).collect(Collectors.toSet());
	}

	@NotAudited
	public Set<DpRequirement> getRequirements() {
		return requirements;
	}

	public void setRequirements(Set<DpRequirement> requirements) {
		this.requirements = requirements;
	}

	public Set<DpConsentTier> getConsentTiers() {
		return consentTiers;
	}

	public void setConsentTiers(Set<DpConsentTier> consentTiers) {
		this.consentTiers = consentTiers;
	}

	@NotAudited
	public Set<StorageContainer> getDistributionContainers() {
		return distributionContainers;
	}

	public void setDistributionContainers(Set<StorageContainer> distributionContainers) {
		this.distributionContainers = distributionContainers;
	}

	public void update(DistributionProtocol dp) {
		if (dp.getActivityStatus().equals(Status.ACTIVITY_STATUS_DISABLED.getStatus())) {
			delete();
		} else {
			setShortTitle(dp.getShortTitle());
			setTitle(dp.getTitle());
		}

		setIrbId(dp.getIrbId());
		setInstitute(dp.getInstitute());
		setDefReceivingSite(dp.getDefReceivingSite());
		setPrincipalInvestigator(dp.getPrincipalInvestigator());
		setStartDate(dp.getStartDate());
		setEndDate(dp.getEndDate());
		setActivityStatus(dp.getActivityStatus());
		setReport(dp.getReport());
		setOrderExtnForm(dp.getOrderExtnForm());
		setDisableEmailNotifs(dp.getDisableEmailNotifs());
		setAttachmentType(dp.getAttachmentType());
		setOrderItemLabelFormat(dp.getOrderItemLabelFormat());
		CollectionUpdater.update(getCoordinators(), dp.getCoordinators());
		CollectionUpdater.update(getDistributingSites(), dp.getDistributingSites());
		setExtension(dp.getExtension());
	}
	
	public List<DependentEntityDetail> getDependentEntities() {
		Map<String, Integer> dependents = daoFactory.getDistributionProtocolDao().getDependents(getId());
		return DependentEntityDetail
			.listBuilder()
			.add(DistributionOrder.getEntityName(), dependents.get(DistributionOrder.getEntityName()))
			.add(StorageContainer.getEntityName(), dependents.get(StorageContainer.getEntityName()))
			.add(Specimen.getEntityName(), dependents.get(Specimen.getEntityName()))
			.build();
	}
	
	public void delete() {
		getDistributionContainers().forEach(container -> container.removeDpRestriction(this));
		getDistributionContainers().clear();

		List<DependentEntityDetail> dependentEntities = getDependentEntities();
		if (!dependentEntities.isEmpty()) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.REF_ENTITY_FOUND, getShortTitle());
		}

		setShortTitle(Utility.getDisabledValue(getShortTitle(), 50));
		setTitle(Utility.getDisabledValue(getTitle(), 255));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		if (getOrderExtnForm() != null) {
			FormContextBean  orderExtnFc = getOrderExtnForm().getAssociations().stream()
				.filter(fc -> fc.getDeletedOn() == null && "OrderExtension".equals(fc.getEntityType()) && getId().equals(fc.getEntityId()))
				.findFirst().orElse(null);
			if (orderExtnFc != null) {
				orderExtnFc.setDeletedOn(Calendar.getInstance().getTime());
			}
		}
	}
	
	public Set<Institute> getDistributingInstitutes() {
		return getDistributingSites().stream().map(DpDistributionSite::getInstitute).collect(Collectors.toSet());
	}
	
	public boolean hasRequirement(DpRequirement dpr) {
		return getRequirements().stream().anyMatch(req -> req.equalsSpecimenGroup(dpr));
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	public DpConsentTier addConsentTier(DpConsentTier ct) {
		ct.setId(null);
		ct.setDistributionProtocol(this);
		ct.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		getConsentTiers().add(ct);
		return ct;
	}

	public DpConsentTier updateConsentTier(DpConsentTier ct) {
		DpConsentTier existing = getConsentTierById(ct.getId());
		existing.setStatement(ct.getStatement());
		return existing;
	}

	public DpConsentTier removeConsentTier(Long ctId) {
		DpConsentTier ct = getConsentTierById(ctId);
		ct.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		return ct;
	}

	public DpRequirement getMatchingRequirement(Specimen specimen) {
		return getRequirements().stream()
			.map(req -> Pair.make(req, req.getMatchPoints(specimen))) // {req, req-matching-points}
			.filter(reqPoints -> reqPoints.second() > 0)              // retain only those reqs with more than 0 points
			.max(Comparator.comparingInt(Pair::second))               // find the req with highest points
			.map(Pair::first)                                         // unpack req from {req, req-matching-points} tuple
			.orElse(null);
	}

	public BigDecimal getCost(Specimen specimen) {
		DpRequirement dpReq = getMatchingRequirement(specimen);
		return (dpReq != null) ? dpReq.getCost() : null;
	}

	private DpConsentTier getConsentTierById(Long ctId) {
		DpConsentTier tier = consentTiers.stream().filter(ct -> ct.getId().equals(ctId)).findFirst().orElse(null);
		if (tier == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.CONSENT_NOT_FOUND, ctId, getShortTitle());
		}

		return tier;
	}
}
