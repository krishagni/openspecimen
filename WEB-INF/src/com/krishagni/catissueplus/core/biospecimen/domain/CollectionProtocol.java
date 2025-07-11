
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.AuditTable;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolRegistrationFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;

@Configurable
@Audited
@AuditTable(value="CAT_COLLECTION_PROTOCOL_AUD")
public class CollectionProtocol extends BaseExtensionEntity {
	public enum SpecimenLabelPrePrintMode {
		ON_REGISTRATION,
		ON_VISIT,
		ON_PRIMARY_COLL,
		ON_PRIMARY_RECV,
		ON_SHIPMENT_RECV,
		NONE
	}

	public enum SpecimenLabelAutoPrintMode {
		PRE_PRINT,
		ON_COLLECTION,
		ON_RECEIVE,
		NONE
	}


	public enum VisitNamePrintMode {
		PRE_PRINT,
		ON_COMPLETION,
		NONE
	}

	public enum VisitCollectionMode {
		PRIMARY_SPMNS,
		ALL_SPMNS
	}

	public enum LabelSequenceKey {
		ID,
		LABEL
	};

	public static final String EXTN = "CollectionProtocolExtension";

	private static final String ENTITY_NAME = "collection_protocol";

	private static final String CP_REG_PPID_FMT = "$$cp_reg_%d$$";

	private static final String CP_VISIT_NAME_FMT = "$$cp_visit_%d$$";

	private static final String CP_EVENT_FMT = "$$cp_event_%d$$";
	
	private String title;

	private String shortTitle;
	
	private String code;

	private Date startDate;

	private Date endDate;
	
	private String activityStatus;

	private User principalInvestigator;
	
	private String irbIdentifier;
	
	private Long enrollment;
	
	private String sopDocumentUrl;

	private String sopDocumentName;

	private Boolean storeSprEnabled;

	private Boolean extractSprText;

	private String descriptionURL;
	
	private String specimenLabelFormat;
	
	private String derivativeLabelFormat;
	
	private String aliquotLabelFormat;

	private String additionalLabelFormat;

	private String specimenBarcodeFormat;

	private String ppidFormat;
	
	private String visitNameFormat;

	private String unsignedConsentDocumentURL;
	
	private Boolean manualPpidEnabled;
	
	private Boolean manualVisitNameEnabled;
	
	private Boolean manualSpecLabelEnabled;

	private Boolean kitLabelsEnabled;

	private Boolean bulkPartRegEnabled;

	private Boolean specimenCentric;

	private Boolean barcodingEnabled;

	private Boolean closeParentSpecimens;

	private Boolean setQtyToZero;

	private String containerSelectionStrategy;

	private Boolean aliquotsInSameContainer;

	private Boolean storageSiteBasedAccess;

	private Boolean draftDataEntry;

	private LabelSequenceKey labelSequenceKey = LabelSequenceKey.ID;

	private VisitCollectionMode visitCollectionMode = VisitCollectionMode.ALL_SPMNS;

	private VisitNamePrintMode visitNamePrintMode = VisitNamePrintMode.NONE;

	private Integer visitNamePrintCopies;
	
	private SpecimenLabelPrePrintMode spmnLabelPrePrintMode = SpecimenLabelPrePrintMode.NONE;
	
	private Set<CpSpecimenLabelPrintSetting> spmnLabelPrintSettings = new HashSet<>();
	
	private Boolean consentsWaived;

	private Boolean visitLevelConsents;

	private CollectionProtocol consentsSource;

	private String serviceRateCurrency;

	private Set<CpConsentTier> consentTier = new LinkedHashSet<>();
	
	private Set<User> coordinators = new HashSet<>();
	
	private Set<CollectionProtocolSite> sites = new HashSet<>();
	
	private Set<CollectionProtocolEvent> collectionProtocolEvents = new LinkedHashSet<>();

	private Set<StorageContainer> storageContainers = new HashSet<>();
	
	private Set<CollectionProtocolRegistration> collectionProtocolRegistrations = new HashSet<>();

	private Set<DistributionProtocol> distributionProtocols = new HashSet<>();

	private Set<User> starred = new HashSet<>();

	private boolean draftMode = false;

	private Long catalogId;

	@Autowired
	private DaoFactory daoFactory;

	@Autowired
	private CollectionProtocolRegistrationFactory cprFactory;

	@Autowired
	private VisitFactory visitFactory;

	public static String getEntityName() {
		return ENTITY_NAME;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
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

	public boolean isDeleted() {
		return Status.isDisabledStatus(getActivityStatus());
	}

	public User getPrincipalInvestigator() {
		return principalInvestigator;
	}

	public void setPrincipalInvestigator(User principalInvestigator) {
		this.principalInvestigator = principalInvestigator;
	}

	public String getIrbIdentifier() {
		return irbIdentifier;
	}

	public void setIrbIdentifier(String irbIdentifier) {
		this.irbIdentifier = irbIdentifier;
	}

	public Long getEnrollment() {
		return enrollment;
	}

	public void setEnrollment(Long enrollment) {
		this.enrollment = enrollment;
	}

	public String getSopDocumentUrl() {
		return sopDocumentUrl;
	}

	public void setSopDocumentUrl(String sopDocumentUrl) {
		this.sopDocumentUrl = sopDocumentUrl;
	}

	public String getSopDocumentName() {
		return sopDocumentName;
	}

	public void setSopDocumentName(String sopDocumentName) {
		this.sopDocumentName = sopDocumentName;
	}

	public Boolean getStoreSprEnabled() {
		return storeSprEnabled;
	}

	public void setStoreSprEnabled(Boolean storeSprEnabled) {
		this.storeSprEnabled = storeSprEnabled;
	}

	public Boolean getExtractSprText() {
		return extractSprText;
	}

	public void setExtractSprText(Boolean extractSprText) {
		this.extractSprText = extractSprText;
	}

	public String getDescriptionURL() {
		return descriptionURL;
	}

	public void setDescriptionURL(String descriptionURL) {
		this.descriptionURL = descriptionURL;
	}

	public String getSpecimenLabelFormat() {
		return specimenLabelFormat;
	}

	public void setSpecimenLabelFormat(String specimenLabelFormat) {
		this.specimenLabelFormat = specimenLabelFormat;
	}

	public String getDerivativeLabelFormat() {
		return derivativeLabelFormat;
	}

	public void setDerivativeLabelFormat(String derivativeLabelFormat) {
		this.derivativeLabelFormat = derivativeLabelFormat;
	}

	public String getAliquotLabelFormat() {
		return aliquotLabelFormat;
	}

	public String getAliquotLabelFormatToUse() {
		if (StringUtils.isNotBlank(getAliquotLabelFormat())) {
			return getAliquotLabelFormat();
		} else {
			return ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.ALIQUOT_LABEL_FORMAT, null);
		}
	}

	public void setAliquotLabelFormat(String aliquotLabelFormat) {
		this.aliquotLabelFormat = aliquotLabelFormat;
	}

	public String getAdditionalLabelFormat() {
		return additionalLabelFormat;
	}

	public String getAdditionalLabelFormatToUse() {
		if (StringUtils.isNotBlank(getAdditionalLabelFormat())) {
			return getAdditionalLabelFormat();
		} else {
			return ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.SPMN_ADDL_LABEL_FORMAT, null);
		}
	}

	public void setAdditionalLabelFormat(String additionalLabelFormat) {
		this.additionalLabelFormat = additionalLabelFormat;
	}

	public String getSpecimenBarcodeFormat() {
		return specimenBarcodeFormat;
	}

	public String getSpecimenBarcodeFormatToUse() {
		if (StringUtils.isNotBlank(getSpecimenBarcodeFormat())) {
			return getSpecimenBarcodeFormat();
		} else {
			return ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.SPMN_BARCODE_FORMAT, null);
		}
	}

	public void setSpecimenBarcodeFormat(String specimenBarcodeFormat) {
		this.specimenBarcodeFormat = specimenBarcodeFormat;
	}

	public String getPpidFormat() {
		return ppidFormat;
	}

	public void setPpidFormat(String ppidFormat) {
		this.ppidFormat = ppidFormat;
	}

	public String getVisitNameFormat() {
		return visitNameFormat;
	}

	public void setVisitNameFormat(String visitNameFormat) {
		this.visitNameFormat = visitNameFormat;
	}

	public String getUnsignedConsentDocumentURL() {
		return unsignedConsentDocumentURL;
	}

	public void setUnsignedConsentDocumentURL(String unsignedConsentDocumentURL) {
		this.unsignedConsentDocumentURL = unsignedConsentDocumentURL;
	}

	public void setManualPpidEnabled(Boolean manualPpidEnabled) {
		this.manualPpidEnabled = manualPpidEnabled;
	}
	
	public boolean isManualPpidEnabled() {
		return manualPpidEnabled != null ? manualPpidEnabled : false;
	}

	public void setManualVisitNameEnabled(Boolean manualVisitNameEnabled) {
		this.manualVisitNameEnabled = manualVisitNameEnabled;
	}
	
	public boolean isManualVisitNameEnabled() {
		return manualVisitNameEnabled != null ? manualVisitNameEnabled : false;
	}
	
	public void setManualSpecLabelEnabled(Boolean manualSpecLabelEnabled) {
		this.manualSpecLabelEnabled = manualSpecLabelEnabled;
	}
	
	public boolean isManualSpecLabelEnabled() {
		return manualSpecLabelEnabled != null ? manualSpecLabelEnabled : false;
	}

	public void setKitLabelsEnabled(Boolean kitLabelsEnabled) {
		this.kitLabelsEnabled = kitLabelsEnabled;
	}

	public boolean isKitLabelsEnabled() {
		return Boolean.TRUE.equals(kitLabelsEnabled);
	}

	public boolean isBulkPartRegEnabled() {
		return bulkPartRegEnabled != null ? bulkPartRegEnabled : false;
	}

	public void setBulkPartRegEnabled(Boolean bulkPartRegEnabled) {
		this.bulkPartRegEnabled = bulkPartRegEnabled;
	}

	public boolean isSpecimenCentric() {
		return specimenCentric != null ? specimenCentric : false;
	}

	public void setSpecimenCentric(Boolean specimenCentric) {
		this.specimenCentric = specimenCentric;
	}

	public boolean isBarcodingEnabled() {
		return barcodingEnabled != null ? barcodingEnabled : false;
	}

	public void setBarcodingEnabled(Boolean barcodingEnabled) {
		this.barcodingEnabled = barcodingEnabled;
	}

	public boolean isBarcodingEnabledToUse() {
		if (isBarcodingEnabled()) {
			return true;
		}

		return ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.ENABLE_SPMN_BARCODING, false);
	}

	public boolean isCloseParentSpecimens() {
		return closeParentSpecimens != null && closeParentSpecimens;
	}

	public void setCloseParentSpecimens(Boolean closeParentSpecimens) {
		this.closeParentSpecimens = (closeParentSpecimens != null && closeParentSpecimens);
	}

	public Boolean getSetQtyToZero() {
		return setQtyToZero;
	}

	public void setSetQtyToZero(Boolean setQtyToZero) {
		this.setQtyToZero = setQtyToZero;
	}

	public boolean isZeroOutQtyEnabled() {
		if (setQtyToZero != null) {
			return setQtyToZero;
		}

		return ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.SET_QTY_TO_ZERO, false);
	}

	public boolean areAllSpecimenLabelsAutoGenerated() {
		return !isManualSpecLabelEnabled() && StringUtils.isNotBlank(getSpecimenLabelFormat()) &&
			StringUtils.isNotBlank(getDerivativeLabelFormat()) && StringUtils.isNotBlank(getAliquotLabelFormat());
	}

	public String getContainerSelectionStrategy() {
		return containerSelectionStrategy;
	}

	public void setContainerSelectionStrategy(String containerSelectionStrategy) {
		this.containerSelectionStrategy = containerSelectionStrategy;
	}

	public Boolean getAliquotsInSameContainer() {
		return aliquotsInSameContainer;
	}

	public void setAliquotsInSameContainer(Boolean aliquotsInSameContainer) {
		this.aliquotsInSameContainer = aliquotsInSameContainer;
	}

	public Boolean getStorageSiteBasedAccess() {
		return storageSiteBasedAccess;
	}

	public void setStorageSiteBasedAccess(Boolean storageSiteBasedAccess) {
		this.storageSiteBasedAccess = storageSiteBasedAccess;
	}

	@NotAudited
	public boolean storageSiteBasedAccessRightsEnabled() {
		return Boolean.TRUE.equals(getStorageSiteBasedAccess());
	}

	public Boolean getDraftDataEntry() {
		return draftDataEntry;
	}

	public void setDraftDataEntry(Boolean draftDataEntry) {
		this.draftDataEntry = draftDataEntry;
	}

	@NotAudited
	public boolean draftDataEntryEnabled() {
		return Boolean.TRUE.equals(draftDataEntry);
	}

	public LabelSequenceKey getLabelSequenceKey() {
		return labelSequenceKey;
	}

	public void setLabelSequenceKey(LabelSequenceKey labelSequenceKey) {
		this.labelSequenceKey = labelSequenceKey;
	}

	public boolean useLabelsAsSequenceKey() {
		return getLabelSequenceKey() == LabelSequenceKey.LABEL;
	}

	public VisitCollectionMode getVisitCollectionMode() {
		return visitCollectionMode == null ? VisitCollectionMode.ALL_SPMNS : visitCollectionMode;
	}

	public void setVisitCollectionMode(VisitCollectionMode visitCollectionMode) {
		this.visitCollectionMode = visitCollectionMode;
	}

	public VisitNamePrintMode getVisitNamePrintMode() {
		return visitNamePrintMode != null ? visitNamePrintMode : VisitNamePrintMode.NONE;
	}

	public void setVisitNamePrintMode(VisitNamePrintMode visitNamePrintMode) {
		this.visitNamePrintMode = visitNamePrintMode;
	}

	public Integer getVisitNamePrintCopies() {
		return visitNamePrintCopies;
	}

	public void setVisitNamePrintCopies(Integer visitNamePrintCopies) {
		this.visitNamePrintCopies = visitNamePrintCopies;
	}

	public SpecimenLabelPrePrintMode getSpmnLabelPrePrintMode() {
		return spmnLabelPrePrintMode != null ? spmnLabelPrePrintMode : SpecimenLabelPrePrintMode.NONE;
	}

	public void setSpmnLabelPrePrintMode(SpecimenLabelPrePrintMode spmnLabelPrePrintMode) {
		this.spmnLabelPrePrintMode = spmnLabelPrePrintMode;
	}

	public Set<CpSpecimenLabelPrintSetting> getSpmnLabelPrintSettings() {
		return spmnLabelPrintSettings;
	}

	public void setSpmnLabelPrintSettings(Set<CpSpecimenLabelPrintSetting> spmnLabelPrintSettings) {
		this.spmnLabelPrintSettings = spmnLabelPrintSettings;
	}
	
	public CpSpecimenLabelPrintSetting getSpmnLabelPrintSetting(String lineage) {
		Optional<CpSpecimenLabelPrintSetting> setting = getSpmnLabelPrintSettings()
			.stream()
			.filter((s) -> s.getLineage().equals(lineage))
			.findFirst();
		return setting.isPresent() ? setting.get() : null;
	}

	public void addSpmnLabelPrintSetting(CpSpecimenLabelPrintSetting setting) {
		setting.setCollectionProtocol(this);
		getSpmnLabelPrintSettings().add(setting);
	}
	
	public boolean isConsentsWaived() {
		return consentsWaived != null ? consentsWaived : false;
	}

	public void setConsentsWaived(Boolean consentsWaived) {
		this.consentsWaived = consentsWaived;
	}

	public Boolean getVisitLevelConsents() {
		return visitLevelConsents;
	}

	public void setVisitLevelConsents(Boolean visitLevelConsents) {
		this.visitLevelConsents = visitLevelConsents;
	}

	public CollectionProtocol getConsentsSource() {
		return consentsSource;
	}

	public void setConsentsSource(CollectionProtocol consentsSource) {
		this.consentsSource = consentsSource;
	}

	public String getServiceRateCurrency() {
		return serviceRateCurrency;
	}

	public void setServiceRateCurrency(String serviceRateCurrency) {
		this.serviceRateCurrency = serviceRateCurrency;
	}

	public Set<CpConsentTier> getConsentTier() {
		return consentTier;
	}

	public void setConsentTier(Set<CpConsentTier> consentTier) {
		this.consentTier = consentTier;
	}

	public CpConsentTier addConsentTier(CpConsentTier ct) {
		ct.setId(null);
		ct.setCollectionProtocol(this);
		ct.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		getConsentTier().add(ct);
		return ct;
	}

	public Set<User> getCoordinators() {
		return coordinators;
	}

	public void setCoordinators(Set<User> coordinators) {
		this.coordinators = coordinators;
	}
	
	public Set<Site> getRepositories() {
		return getSites().stream().map(CollectionProtocolSite::getSite).collect(Collectors.toSet());
	}

	@NotAudited
	public Set<CollectionProtocolSite> getSites() {
		return sites;
	}

	public void setSites(Set<CollectionProtocolSite> sites) {
		this.sites = sites;
	}

	public void addSite(CollectionProtocolSite site) {
		site.setCollectionProtocol(this);
		getSites().add(site);
	}

	@NotAudited
	public Set<CollectionProtocolEvent> getCollectionProtocolEvents() {
		return collectionProtocolEvents;
	}

	public void setCollectionProtocolEvents(Set<CollectionProtocolEvent> collectionProtocolEvents) {
		this.collectionProtocolEvents = collectionProtocolEvents;
	}

	@NotAudited
	public Set<StorageContainer> getStorageContainers() {
		return storageContainers;
	}

	public void setStorageContainers(Set<StorageContainer> storageContainers) {
		this.storageContainers = storageContainers;
	}

	@NotAudited
	public Set<CollectionProtocolRegistration> getCollectionProtocolRegistrations() {
		return collectionProtocolRegistrations;
	}

	public void setCollectionProtocolRegistrations(Set<CollectionProtocolRegistration> collectionProtocolRegistrations) {
		this.collectionProtocolRegistrations = collectionProtocolRegistrations;
	}

	public Set<DistributionProtocol> getDistributionProtocols() {
		return distributionProtocols;
	}

	public void setDistributionProtocols(Set<DistributionProtocol> distributionProtocols) {
		this.distributionProtocols = distributionProtocols;
	}

	@NotAudited
	public Set<User> getStarred() {
		return starred;
	}

	public void setStarred(Set<User> starred) {
		this.starred = starred;
	}

	public boolean isDraftMode() {
		return draftMode;
	}

	public void setDraftMode(boolean draftMode) {
		this.draftMode = draftMode;
	}

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public void update(CollectionProtocol cp) {
		setTitle(cp.getTitle()); 
		setShortTitle(cp.getShortTitle());
		setCode(cp.getCode());
		setStartDate(cp.getStartDate());
		setEndDate(cp.getEndDate());
		setActivityStatus(cp.getActivityStatus());
		setPrincipalInvestigator(cp.getPrincipalInvestigator());
		setIrbIdentifier(cp.getIrbIdentifier());
		setEnrollment(cp.getEnrollment());
		setSopDocumentUrl(cp.getSopDocumentUrl());
		setSopDocumentName(cp.getSopDocumentName());
		setStoreSprEnabled(cp.getStoreSprEnabled());
		setExtractSprText(cp.getExtractSprText());
		setDescriptionURL(cp.getDescriptionURL());
		setPpidFormat(cp.getPpidFormat());
		setManualPpidEnabled(cp.isManualPpidEnabled());
		setVisitNameFormat(cp.getVisitNameFormat());
		setManualVisitNameEnabled(cp.isManualVisitNameEnabled());
		setSpecimenLabelFormat(cp.getSpecimenLabelFormat());
		setDerivativeLabelFormat(cp.getDerivativeLabelFormat());
		setAliquotLabelFormat(cp.getAliquotLabelFormat());
		setAdditionalLabelFormat(cp.getAdditionalLabelFormat());
		setSpecimenBarcodeFormat(cp.getSpecimenBarcodeFormat());
		setManualSpecLabelEnabled(cp.isManualSpecLabelEnabled());
		setKitLabelsEnabled(cp.isKitLabelsEnabled());
		setBulkPartRegEnabled(cp.isBulkPartRegEnabled());
		setBarcodingEnabled(cp.isBarcodingEnabled());
		setCloseParentSpecimens(cp.isCloseParentSpecimens());
		setSetQtyToZero(cp.getSetQtyToZero());
		setContainerSelectionStrategy(cp.getContainerSelectionStrategy());
		setAliquotsInSameContainer(cp.getAliquotsInSameContainer());
		setStorageSiteBasedAccess(cp.getStorageSiteBasedAccess());
		setDraftDataEntry(cp.draftDataEntryEnabled());
		setLabelSequenceKey(cp.getLabelSequenceKey());
		setVisitCollectionMode(cp.getVisitCollectionMode());
		setVisitNamePrintMode(cp.getVisitNamePrintMode());
		setVisitNamePrintCopies(cp.getVisitNamePrintCopies());
		setUnsignedConsentDocumentURL(cp.getUnsignedConsentDocumentURL());
		setServiceRateCurrency(cp.getServiceRateCurrency());
		setExtension(cp.getExtension());
//		setCatalogId(cp.getCatalogId());
		
		updateSites(cp.getSites());
		updateSpecimenLabelPrintSettings(cp.getSpmnLabelPrintSettings());
		CollectionUpdater.update(getCoordinators(), cp.getCoordinators());
		updateLabelPrePrintMode(cp.getSpmnLabelPrePrintMode());
		CollectionUpdater.update(getDistributionProtocols(), cp.getDistributionProtocols());
	}
	
	public void copyTo(CollectionProtocol cp) {
		copySitesTo(cp);

		cp.setTitle(getTitle());
		cp.setShortTitle(getShortTitle());
		cp.setCode(getCode());
		cp.setPrincipalInvestigator(getPrincipalInvestigator());
		cp.setCoordinators(new HashSet<>(getCoordinators()));

		cp.setStartDate(getStartDate());
		cp.setEndDate(getEndDate());
		cp.setIrbIdentifier(getIrbIdentifier());
		cp.setEnrollment(getEnrollment());
		cp.setDescriptionURL(getDescriptionURL());
		cp.setEnrollment(getEnrollment());
		cp.setSpecimenCentric(isSpecimenCentric());
		cp.setStoreSprEnabled(getStoreSprEnabled());
		cp.setExtractSprText(getExtractSprText());

		cp.setPpidFormat(getPpidFormat());
		cp.setManualPpidEnabled(isManualPpidEnabled());
		cp.setVisitNameFormat(getVisitNameFormat());
		cp.setManualVisitNameEnabled(isManualVisitNameEnabled());
		cp.setSpecimenLabelFormat(getSpecimenLabelFormat());
		cp.setAliquotLabelFormat(getAliquotLabelFormat());
		cp.setDerivativeLabelFormat(getDerivativeLabelFormat());
		cp.setAdditionalLabelFormat(getAdditionalLabelFormat());
		cp.setSpecimenBarcodeFormat(getSpecimenBarcodeFormat());
		cp.setManualSpecLabelEnabled(isManualSpecLabelEnabled());
		cp.setKitLabelsEnabled(isKitLabelsEnabled());
		cp.setBulkPartRegEnabled(isBulkPartRegEnabled());
		cp.setBarcodingEnabled(isBarcodingEnabled());
		cp.setCloseParentSpecimens(isCloseParentSpecimens());
		cp.setSetQtyToZero(getSetQtyToZero());
		cp.setStorageSiteBasedAccess(getStorageSiteBasedAccess());
		cp.setDraftDataEntry(draftDataEntryEnabled());
		cp.setLabelSequenceKey(getLabelSequenceKey());
		cp.setVisitCollectionMode(getVisitCollectionMode());
		cp.setVisitNamePrintMode(getVisitNamePrintMode());
		cp.setVisitNamePrintCopies(getVisitNamePrintCopies());
		cp.setSpmnLabelPrePrintMode(getSpmnLabelPrePrintMode());
		cp.setCatalogId(getCatalogId());

		copyLabelPrintSettingsTo(cp);
		copyExtensionTo(cp);

		cp.setConsentsWaived(isConsentsWaived());
		cp.setVisitLevelConsents(getVisitLevelConsents());
		cp.setConsentsSource(getConsentsSource());
		cp.setUnsignedConsentDocumentURL(getUnsignedConsentDocumentURL());
		copyConsentTierTo(cp);

		copyEventsTo(cp);

		cp.setContainerSelectionStrategy(getContainerSelectionStrategy());
		cp.setAliquotsInSameContainer(getAliquotsInSameContainer());
		cp.setStorageSiteBasedAccess(getStorageSiteBasedAccess());
		cp.setServiceRateCurrency(getServiceRateCurrency());
		cp.setDraftDataEntry(draftDataEntryEnabled());
		cp.setActivityStatus(getActivityStatus());
	}
	
	public void copyConsentTierTo(CollectionProtocol cp) {
		if (cp.getConsentsSource() != null) {
			return;
		}

		for (CpConsentTier consentTier : getConsentTier()) {
			cp.addConsentTier(consentTier.copy());
		}
	}
	
	public void copyLabelPrintSettingsTo(CollectionProtocol cp) {
		for (CpSpecimenLabelPrintSetting setting : getSpmnLabelPrintSettings()) {
			cp.addSpmnLabelPrintSetting(setting.copy());
		}
	}

	public void copySitesTo(CollectionProtocol cp) {
		for (CollectionProtocolSite site : getSites()) {
			cp.addSite(site.copy());
		}
	}
	
	public void copyEventsTo(CollectionProtocol cp) {
		for (CollectionProtocolEvent cpe : getOrderedCpeList()) {
			if (cpe.isClosed()) {
				continue;
			}

			cp.addCpe(cpe.deepCopy());
		}
	}
	
	public CpConsentTier updateConsentTier(CpConsentTier ct) {
		if (ct.getId() == null) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_TIER_NOT_FOUND);
		}

		CpConsentTier existing = getConsentTierById(ct.getId());
		if (existing == null) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_TIER_NOT_FOUND);
		}
		
		existing.setStatement(ct.getStatement());
		return ct;		
	}
	
	public CpConsentTier removeConsentTier(Long ctId) {		
		CpConsentTier ct = getConsentTierById(ctId);
		if (ct == null) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_TIER_NOT_FOUND);
		}
		
		List<DependentEntityDetail> dependentEntities = ct.getDependentEntities();
		if (!dependentEntities.isEmpty()) {
			throw OpenSpecimenException.userError(CpErrorCode.CONSENT_REF_ENTITY_FOUND, ct.getStatement().getCode());
		}
		ct.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		return ct;
	}	
	
	public void addCpe(CollectionProtocolEvent cpe) {
		CollectionProtocolEvent existing = getCpe(cpe.getEventLabel());
		if (existing != null) {
			throw OpenSpecimenException.userError(CpeErrorCode.DUP_LABEL, cpe.getEventLabel());
		}
		
		if (StringUtils.isNotBlank(cpe.getCode()) && getCpeByCode(cpe.getCode()) != null) {
			throw OpenSpecimenException.userError(CpeErrorCode.DUP_CODE, cpe.getCode());
		}
				
		cpe.setId(null);
		cpe.setCollectionProtocol(this);
		getCollectionProtocolEvents().add(cpe);
	}
	
	public void updateCpe(CollectionProtocolEvent cpe) {
		Object key = null;
		CollectionProtocolEvent existing = null;
		if (cpe.getId() != null) {
			key = cpe.getId();
			existing = getCpe(cpe.getId());
		} else if (StringUtils.isNotBlank(cpe.getEventLabel())) {
			key = cpe.getEventLabel();
			existing = getCpe(cpe.getEventLabel());
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CpeErrorCode.LABEL_REQUIRED);
		} else if (existing == null) {
			throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, key, 1);
		}

		if (!existing.getEventLabel().equalsIgnoreCase(cpe.getEventLabel())) {
			if (getCpe(cpe.getEventLabel()) != null) {
				throw OpenSpecimenException.userError(CpeErrorCode.DUP_LABEL, cpe.getEventLabel());
			}			
		}
		
		if (StringUtils.isNotBlank(cpe.getCode()) && !cpe.getCode().equals(existing.getCode())) {
			if (getCpeByCode(cpe.getCode()) != null) {
				throw OpenSpecimenException.userError(CpeErrorCode.DUP_CODE, cpe.getCode());
			}
		}
		
		existing.update(cpe);
	}
	
	public CollectionProtocolEvent getCpe(Long cpeId) {
		for (CollectionProtocolEvent cpe : getCollectionProtocolEvents()) {
			if (cpe.getId().equals(cpeId)) {
				return cpe;
			}
		}
		
		return null;		
	}
	
	public CollectionProtocolEvent getCpe(String eventLabel) {
		for (CollectionProtocolEvent cpe : getCollectionProtocolEvents()) {
			if (cpe.getEventLabel().equalsIgnoreCase(eventLabel)) {
				return cpe;
			}
		}
		
		return null;
	}
	
	public CollectionProtocolEvent getCpeByCode(String code) {
		for (CollectionProtocolEvent cpe : getCollectionProtocolEvents()) {
			if (code.equals(cpe.getCode())) {
				return cpe;
			}
		}
		
		return null;
	}
	
	//TODO: need to check few more dependencies like user role 
	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail
				.listBuilder()
				.add(CollectionProtocolRegistration.getEntityName(), getCollectionProtocolRegistrations().size())
				.add(StorageContainer.getEntityName(), getStorageContainers().size())
				.build();
	}
	
	public void delete() {
		List<DependentEntityDetail> dependentEntities = getDependentEntities();
		if (!dependentEntities.isEmpty()) {
			throw OpenSpecimenException.userError(CpErrorCode.REF_ENTITY_FOUND);
		}

		getCollectionProtocolEvents().forEach(CollectionProtocolEvent::delete);
		setTitle(Utility.getDisabledValue(getTitle(), 255));
		setShortTitle(Utility.getDisabledValue(getShortTitle(), 50));
		setCode(Utility.getDisabledValue(getCode(), 32));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		FormUtil.getInstance().deleteRecords(getId(), Collections.singletonList("CollectionProtocolExtension"), getId());
		FormUtil.getInstance().deleteCpEntityForms(getId());
	}
	
	@Override
	public String getEntityType() {
		return EXTN;
	}
	
	public CollectionProtocolEvent firstEvent() {
		if (!getCollectionProtocolEvents().isEmpty()) {
			return getOrderedCpeList().get(0);
		}
		
		return null;
	}
	
	public List<CollectionProtocolEvent> getOrderedCpeList() {
		return getCollectionProtocolEvents().stream().sorted().collect(Collectors.toList());
	}

	public String getPpid() {
		return String.format(CP_REG_PPID_FMT, getId());
	}

	public String getVisitName() {
		return String.format(CP_VISIT_NAME_FMT, getId());
	}

	public String getEventName() {
		return String.format(CP_EVENT_FMT, getId());
	}

	public Visit getVisit() {
		String visitName = getVisitName();
		Visit cpVisit = daoFactory.getVisitsDao().getByName(visitName);
		if (cpVisit != null) {
			return cpVisit;
		}

		String ppid = getPpid();
		CollectionProtocolRegistration cpReg = daoFactory.getCprDao().getCprByPpid(getId(), ppid);
		if (cpReg == null) {
			CollectionProtocolRegistrationDetail cprInput = new CollectionProtocolRegistrationDetail();
			cprInput.setCpId(getId());
			cprInput.setPpid(ppid);
			cprInput.setRegistrationDate(Calendar.getInstance().getTime());
			cpReg = cprFactory.createCpr(cprInput);

			daoFactory.getParticipantDao().saveOrUpdate(cpReg.getParticipant());
			daoFactory.getCprDao().saveOrUpdate(cpReg);
		}

		VisitDetail visitInput = new VisitDetail();
		visitInput.setCpId(getId());
		visitInput.setPpid(ppid);
		visitInput.setName(visitName);
		visitInput.setVisitDate(Calendar.getInstance().getTime());
		visitInput.setSite(getRepositories().iterator().next().getName());
		if (firstEvent() != null) {
			visitInput.setEventLabel(firstEvent().getEventLabel());
		}

		cpVisit = visitFactory.createVisit(visitInput);
		daoFactory.getVisitsDao().saveOrUpdate(cpVisit);
		return cpVisit;
	}

	private CpConsentTier getConsentTierById(Long ctId) {
		for (CpConsentTier ct : consentTier) {
			if (ct.getId().equals(ctId)) {
				return ct;
			}
		}
		
		return null;
	}
	
	private void updateSites(Set<CollectionProtocolSite> newSites) {
		Map<Site, CollectionProtocolSite> existingSites = new HashMap<Site, CollectionProtocolSite>();
		for (CollectionProtocolSite cpSite: getSites()) {
			existingSites.put(cpSite.getSite(), cpSite);
		}
		
		for (CollectionProtocolSite newSite: newSites) {
			CollectionProtocolSite oldSite = existingSites.get(newSite.getSite());
			if (oldSite != null) {
				oldSite.update(newSite);
				existingSites.remove(oldSite.getSite());
			} else {
				getSites().add(newSite);
			}
		}
		
		getSites().removeAll(existingSites.values());
	}
	
	private void updateLabelPrePrintMode(SpecimenLabelPrePrintMode prePrintMode) {
		if (getSpmnLabelPrePrintMode() == prePrintMode) {
			//
			// Nothing has changed
			//
			return;
		}

		setSpmnLabelPrePrintMode(prePrintMode);
		if (prePrintMode != SpecimenLabelPrePrintMode.NONE) {
			//
			// pre-printing is not disabled
			//
			return;
		}

		//
		// Disable pre-print for all specimen requirements
		//
		for (CollectionProtocolEvent cpe : getCollectionProtocolEvents()) {
			for (SpecimenRequirement sr : cpe.getSpecimenRequirements()) {
				if (sr.getLabelAutoPrintMode() == SpecimenLabelAutoPrintMode.PRE_PRINT) {
					sr.setLabelAutoPrintMode(null);
				}
			}
		}
	}
	
	private void updateSpecimenLabelPrintSettings(Set<CpSpecimenLabelPrintSetting> newSpmnLblPrintSettings) {
		Map<String, CpSpecimenLabelPrintSetting> existingSettings = new HashMap<String, CpSpecimenLabelPrintSetting>();
		for (CpSpecimenLabelPrintSetting spmnLblPrintSetting : getSpmnLabelPrintSettings()) {
			existingSettings.put(spmnLblPrintSetting.getLineage(), spmnLblPrintSetting);
		}
		
		for (CpSpecimenLabelPrintSetting newSpmnLblPrintSetting : newSpmnLblPrintSettings) {
			CpSpecimenLabelPrintSetting oldSetting = existingSettings.get(newSpmnLblPrintSetting.getLineage());
			if (oldSetting != null) {
				oldSetting.update(newSpmnLblPrintSetting);
				existingSettings.remove(oldSetting.getLineage());
			} else {
				getSpmnLabelPrintSettings().add(newSpmnLblPrintSetting);
			}
		}
		
		getSpmnLabelPrintSettings().removeAll(existingSettings.values());
	}
}
