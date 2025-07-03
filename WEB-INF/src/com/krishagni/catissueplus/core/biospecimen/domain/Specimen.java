
package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenReservedEvent;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenReturnEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.impl.CpWorkflowTxnCache;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.services.impl.FormUtil;

@Configurable
@Audited
public class Specimen extends BaseExtensionEntity {
	private static final LogUtil logger = LogUtil.getLogger(Specimen.class);

	public static final String NEW = "New";
	
	public static final String ALIQUOT = "Aliquot";
	
	public static final String DERIVED = "Derived";
	
	public static final String COLLECTED = "Collected";
	
	public static final String PENDING = "Pending";

	public static final String MISSED_COLLECTION = "Missed Collection";

	public static final String NOT_COLLECTED = "Not Collected";

	public static final String AVAILABLE = "Available";

	public static final String DISPOSED = "Closed";

	public static final String DISTRIBUTED = "Distributed";

	public static final String RESERVED = "Reserved";

	public static final String ACCEPTABLE = "Acceptable";

	public static final String TO_BE_RECEIVED = "To be Received";
	
	public static final String NOT_SPECIFIED = "Not Specified";

	public static final String PROCESSED = "Processed";

	public static final String EXTN = "SpecimenExtension";
	
	private static final String ENTITY_NAME = "specimen";

	private PermissibleValue tissueSite;

	private PermissibleValue tissueSide;

	private PermissibleValue pathologicalStatus;

	private String lineage;

	private BigDecimal initialQuantity;

	private PermissibleValue specimenClass;

	private PermissibleValue specimenType;

	private BigDecimal concentration;

	private String label;

	private String additionalLabel;

	private String activityStatus;

	private String barcode;

	private String comment;

	private Date createdOn;

	private String imageId;

	private BigDecimal availableQuantity;

	private String collectionStatus;

	private String availabilityStatus;
	
	private Set<PermissibleValue> biohazards = new HashSet<>();

	private Integer freezeThawCycles;

	private CollectionProtocol collectionProtocol;

	private Visit visit;

	private SpecimenRequirement specimenRequirement;

	private StorageContainerPosition position;

	private StorageContainerPosition checkoutPosition;

	private Specimen parentSpecimen;

	private Set<Specimen> childCollection = new HashSet<>();

	private SpecimenPooledEvent pooledEvent;

	private boolean poolItem;

	private Set<SpecimenPooledEvent> poolItemEvents;

	private Set<SpecimenExternalIdentifier> externalIds = new HashSet<>();

	//
	// records aliquot or derivative events that have occurred on this specimen
	//
	private Set<SpecimenChildrenEvent> childrenEvents = new LinkedHashSet<>();

	//
	// records the event through which this specimen got created
	//
	private SpecimenChildrenEvent parentEvent;

	//
	// collectionEvent and receivedEvent are valid only for primary specimens
	//
	private SpecimenCollectionEvent collectionEvent;
	
	private SpecimenReceivedEvent receivedEvent;

	//
	// record the DP for which this specimen is currently reserved
	//
	private SpecimenReservedEvent reservedEvent;

	//
	// Available for all specimens in hierarchy based on values set for primary specimens
	//
	private Set<SpecimenCollectionReceiveDetail> collRecvDetailsList;

	private List<SpecimenTransferEvent> transferEvents;
	
	private Set<SpecimenListItem> specimenListItems =  new HashSet<>();
	
	private boolean concentrationInit = false;

	private Set<SpecimenDeleteEvent> deleteEvents = new HashSet<>();

	private Set<SpecimenService> services = new HashSet<>();

	@Autowired
	@Qualifier("specimenLabelGenerator")
	private LabelGenerator labelGenerator;

	@Autowired
	@Qualifier("specimenAddlLabelGenerator")
	private LabelGenerator addlLabelGenerator;


	@Autowired
	@Qualifier("specimenBarcodeGenerator")
	private LabelGenerator barcodeGenerator;

	@Autowired
	private DaoFactory daoFactory;

	private transient boolean forceDelete;
	
	private transient boolean printLabel;

	private transient boolean freezeThawIncremented;

	private transient StorageContainerPosition oldPosition;

	private transient User transferUser;

	private transient Date transferTime;

	private transient String transferComments;

	private transient Boolean checkout;

	private transient Boolean retrievedNotif;

	private transient boolean autoCollectParents;

	private transient boolean updated;

	private transient boolean statusChanged;

	private transient String uid;

	private transient String parentUid;

	private transient User createdBy;

	private transient String shipmentReceiveQuality;

	//
	// holdingLocation and dp are used during distribution to record the location
	// where the specimen will be stored temporarily post distribution.
	//
	private transient StorageContainerPosition holdingLocation;

	private transient DistributionProtocol dp;

	//
	// Records the derivatives or aliquots created from this specimen in current action/transaction
	//
	private transient SpecimenChildrenEvent aliquotEvent;

	//
	// OPSMN-4636: To ensure the same set of specimens are not created twice
	//
	private transient Map<Long, Specimen> preCreatedSpmnsMap;

	private transient Long ancestorId;

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getTissueSite() {
		return tissueSite;
	}

	public void setTissueSite(PermissibleValue tissueSite) {
		if (!Objects.equals(this.tissueSite, tissueSite)) {
			getChildCollection().stream()
				.filter(child -> child.isAliquot() || Objects.equals(this.tissueSite, child.getTissueSite()))
				.forEach(child -> child.setTissueSite(tissueSite));
		}
		
		this.tissueSite = tissueSite;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getTissueSide() {
		return tissueSide;
	}

	public void setTissueSide(PermissibleValue tissueSide) {
		if (!Objects.equals(this.tissueSide, tissueSide)) {
			getChildCollection().stream()
				.filter(child -> child.isAliquot() || Objects.equals(this.tissueSide, child.getTissueSide()))
				.forEach(child -> child.setTissueSide(tissueSide));
		}
		
		this.tissueSide = tissueSide;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getPathologicalStatus() {
		return pathologicalStatus;
	}

	public void setPathologicalStatus(PermissibleValue pathologicalStatus) {
		if (!Objects.equals(this.pathologicalStatus, pathologicalStatus)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setPathologicalStatus(pathologicalStatus);
				}
			}
		}
				
		this.pathologicalStatus = pathologicalStatus;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	public BigDecimal getInitialQuantity() {
		return initialQuantity;
	}

	public void setInitialQuantity(BigDecimal initialQuantity) {
		this.initialQuantity = initialQuantity;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(PermissibleValue specimenClass) {
		if (!Objects.equals(this.specimenClass, specimenClass)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setSpecimenClass(specimenClass);
				}				
			}
		}
		
		this.specimenClass = specimenClass;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getSpecimenType() {
		return specimenType;
	}

	public void setSpecimenType(PermissibleValue specimenType) {
		if (!Objects.equals(this.specimenType, specimenType)) {
			for (Specimen child : getChildCollection()) {
				if (child.isAliquot()) {
					child.setSpecimenType(specimenType);
				}				
			}
		}
				
		this.specimenType = specimenType;
	}

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		if (concentrationInit) {
			if (Objects.equals(this.concentration, concentration)) {
				return;
			}

			if (this.concentration == null || !this.concentration.equals(concentration)) {
				for (Specimen child : getChildCollection()) {
					if (Objects.equals(this.concentration, child.getConcentration()) && child.isAliquot()) {
						child.setConcentration(concentration);
					}
				}
			}
		}
		
		this.concentration = concentration;
		this.concentrationInit = true;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getAdditionalLabel() {
		return additionalLabel;
	}

	public void setAdditionalLabel(String additionalLabel) {
		this.additionalLabel = additionalLabel;
	}

	public String getActivityStatus() {
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}

		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		this.activityStatus = activityStatus;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public Date getCreatedOn() {
		return  Utility.chopSeconds(createdOn);
	}

	public void setCreatedOn(Date createdOn) {
		// For all specimens, the created on seconds and milliseconds should be reset to 0
		this.createdOn = Utility.chopSeconds(createdOn);
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public BigDecimal getAvailableQuantity() {
		return availableQuantity;
	}

	public void setAvailableQuantity(BigDecimal availableQuantity) {
		this.availableQuantity = availableQuantity;
	}

	public String getCollectionStatus() {
		return collectionStatus;
	}

	public void setCollectionStatus(String collectionStatus) {
		this.collectionStatus = collectionStatus;
	}

	public String getAvailabilityStatus() {
		return availabilityStatus;
	}

	public void setAvailabilityStatus(String availabilityStatus) {
		this.availabilityStatus = availabilityStatus;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public Set<PermissibleValue> getBiohazards() {
		return biohazards;
	}

	public void setBiohazards(Set<PermissibleValue> biohazards) {
		this.biohazards = biohazards;
	}
	
	public void updateBiohazards(Set<PermissibleValue> biohazards) {
		getBiohazards().addAll(biohazards);
		getBiohazards().retainAll(biohazards);
		
		for (Specimen child : getChildCollection()) {
			if (child.isAliquot()) {
				child.updateBiohazards(biohazards);
			}
		}
	}

	public Integer getFreezeThawCycles() {
		return freezeThawCycles;
	}

	public void setFreezeThawCycles(Integer freezeThawCycles) {
		this.freezeThawCycles = freezeThawCycles;
	}

	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public Visit getVisit() {
		return visit;
	}

	public void setVisit(Visit visit) {
		this.visit = visit;
	}

	public SpecimenRequirement getSpecimenRequirement() {
		return specimenRequirement;
	}

	public void setSpecimenRequirement(SpecimenRequirement specimenRequirement) {
		this.specimenRequirement = specimenRequirement;
	}

	public Long getReqId() {
		return specimenRequirement.getId();
	}

	public StorageContainerPosition getPosition() {
		return position;
	}

	public void setPosition(StorageContainerPosition position) {
		this.position = position;
	}

	public StorageContainerPosition getCheckoutPosition() {
		return checkoutPosition;
	}

	public void setCheckoutPosition(StorageContainerPosition checkoutPosition) {
		this.checkoutPosition = checkoutPosition;
	}

	@NotAudited
	public Site getStorageSite() {
		return position != null && position.getContainer() != null ? position.getContainer().getSite() : null;
	}

	public Specimen getParentSpecimen() {
		return parentSpecimen;
	}

	public void setParentSpecimen(Specimen parentSpecimen) {
		this.parentSpecimen = parentSpecimen;
	}

	@NotAudited
	public Set<Specimen> getChildCollection() {
		return childCollection;
	}

	public void setChildCollection(Set<Specimen> childSpecimenCollection) {
		this.childCollection = childSpecimenCollection;
	}

	@NotAudited
	public SpecimenPooledEvent getPooledEvent() {
		return pooledEvent;
	}

	public void setPooledEvent(SpecimenPooledEvent pooledEvent) {
		this.pooledEvent = pooledEvent;
	}

	public boolean isPoolItem() {
		return poolItem;
	}

	public void setPoolItem(boolean poolItem) {
		this.poolItem = poolItem;
	}

	@NotAudited
	public Set<SpecimenPooledEvent> getPoolItemEvents() {
		return poolItemEvents;
	}

	public void setPoolItemEvents(Set<SpecimenPooledEvent> poolItemEvents) {
		this.poolItemEvents = poolItemEvents;
	}

	public Set<SpecimenExternalIdentifier> getExternalIds() {
		return externalIds;
	}

	public void setExternalIds(Set<SpecimenExternalIdentifier> externalIds) {
		this.externalIds = externalIds;
	}

	@NotAudited
	public Set<SpecimenChildrenEvent> getChildrenEvents() {
		return childrenEvents;
	}

	public void setChildrenEvents(Set<SpecimenChildrenEvent> childrenEvents) {
		this.childrenEvents = childrenEvents;
	}

	@NotAudited
	public SpecimenChildrenEvent getParentEvent() {
		return parentEvent;
	}

	public void setParentEvent(SpecimenChildrenEvent parentEvent) {
		this.parentEvent = parentEvent;
	}

	@NotAudited
	public SpecimenCollectionEvent getCollectionEvent() {
		if (isAliquot() || isDerivative()) {
			return null;
		}
				
		if (this.collectionEvent == null) {
			this.collectionEvent = SpecimenCollectionEvent.getFor(this); 
		}
		
		if (this.collectionEvent == null) {
			this.collectionEvent = SpecimenCollectionEvent.createFromSr(this);
		}
		
		return this.collectionEvent;
	}

	public void setCollectionEvent(SpecimenCollectionEvent collectionEvent) {
		this.collectionEvent = collectionEvent;
	}

	@NotAudited
	public SpecimenReceivedEvent getReceivedEvent() {
		if (isAliquot() || isDerivative()) {
			return null;
		}
		
		if (this.receivedEvent == null) {
			this.receivedEvent = SpecimenReceivedEvent.getFor(this); 			 
		}
		
		if (this.receivedEvent == null) {
			this.receivedEvent = SpecimenReceivedEvent.createFromSr(this);
		}
		
		return this.receivedEvent; 
	}

	public void setReceivedEvent(SpecimenReceivedEvent receivedEvent) {
		this.receivedEvent = receivedEvent;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public SpecimenReservedEvent getReservedEvent() {
		return reservedEvent;
	}

	public void setReservedEvent(SpecimenReservedEvent reservedEvent) {
		this.reservedEvent = reservedEvent;
	}

	@NotAudited
	public SpecimenCollectionReceiveDetail getCollRecvDetails() {
		return collRecvDetailsList != null && !collRecvDetailsList.isEmpty() ? collRecvDetailsList.iterator().next() : null;
	}

	@NotAudited
	public Set<SpecimenCollectionReceiveDetail> getCollRecvDetailsList() {
		return collRecvDetailsList;
	}

	public void setCollRecvDetailsList(Set<SpecimenCollectionReceiveDetail> collRecvDetailsList) {
		this.collRecvDetailsList = collRecvDetailsList;
	}

	@NotAudited
	public List<String> getReqNames() {
		return getReqAttrs(SpecimenRequirement::getName);
	}

	@NotAudited
	public List<String> getReqCodes() {
		return getReqAttrs(SpecimenRequirement::getCode);
	}

	@NotAudited
	public List<SpecimenTransferEvent> getTransferEvents() {
		if (this.transferEvents == null) {
			this.transferEvents = SpecimenTransferEvent.getFor(this);
		}		
		return this.transferEvents;
	}
	
	@NotAudited
	public Set<SpecimenListItem> getSpecimenListItems() {
		return specimenListItems;
	}

	public void setSpecimenListItems(Set<SpecimenListItem> specimenListItems) {
		this.specimenListItems = specimenListItems;
	}

	@NotAudited
	public Set<SpecimenDeleteEvent> getDeleteEvents() {
		return deleteEvents;
	}

	public void setDeleteEvents(Set<SpecimenDeleteEvent> deleteEvents) {
		this.deleteEvents = deleteEvents;
	}

	public Set<SpecimenService> getServices() {
		return services;
	}

	public void setServices(Set<SpecimenService> services) {
		this.services = services;
	}

	public Set<DistributionProtocol> getDistributionProtocols() {
		return getCollectionProtocol().getDistributionProtocols();
	}

	public LabelGenerator getLabelGenerator() {
		return labelGenerator;
	}

	public Specimen getPrimarySpecimen() {
		Specimen specimen = this;
		while (specimen.getParentSpecimen() != null) {
			specimen = specimen.getParentSpecimen();
		}

		return specimen;
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	@Override
	public Long getCpId() {
		return getCollectionProtocol().getId();
	}

	public String getCpShortTitle() {
		return getCollectionProtocol().getShortTitle();
	}

	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	public User getTransferUser() {
		return transferUser;
	}

	public void setTransferUser(User transferUser) {
		this.transferUser = transferUser;
	}

	public Date getTransferTime() {
		return transferTime;
	}

	public void setTransferTime(Date transferTime) {
		this.transferTime = transferTime;
	}

	public String getTransferComments() {
		return transferComments;
	}

	public void setTransferComments(String transferComments) {
		this.transferComments = transferComments;
	}

	public Boolean getCheckout() {
		return checkout;
	}

	public void setCheckout(Boolean checkout) {
		this.checkout = checkout;
	}

	public Boolean getRetrievedNotif() {
		return retrievedNotif;
	}

	public void setRetrievedNotif(Boolean retrievedNotif) {
		this.retrievedNotif = retrievedNotif;
	}

	public boolean isAutoCollectParents() {
		return autoCollectParents;
	}

	public void setAutoCollectParents(boolean autoCollectParents) {
		this.autoCollectParents = autoCollectParents;
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

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getParentUid() {
		return parentUid;
	}

	public void setParentUid(String parentUid) {
		this.parentUid = parentUid;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public String getShipmentReceiveQuality() {
		return shipmentReceiveQuality;
	}

	public void setShipmentReceiveQuality(String shipmentReceiveQuality) {
		this.shipmentReceiveQuality = shipmentReceiveQuality;
	}

	public StorageContainerPosition getHoldingLocation() {
		return holdingLocation;
	}

	public void setHoldingLocation(StorageContainerPosition holdingLocation) {
		this.holdingLocation = holdingLocation;
	}

	public DistributionProtocol getDp() {
		return dp;
	}

	public void setDp(DistributionProtocol dp) {
		this.dp = dp;
	}

	public Map<Long, Specimen> getPreCreatedSpmnsMap() {
		return preCreatedSpmnsMap;
	}

	public boolean isPrintLabel() {
		return printLabel;
	}

	public void setPrintLabel(boolean printLabel) {
		this.printLabel = printLabel;
	}

	public boolean isActive() {
		return Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(getActivityStatus());
	}

	public boolean isTxnActive() {
		return ClosedSpecimensTracker.getInstance().isActive(this);
	}
	
	public boolean isClosed() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}
	
	public boolean isActiveOrClosed() {
		return isActive() || isClosed();
	}

	public boolean isDeleted() {
		return Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(getActivityStatus());
	}

	public boolean isReserved() {
		return getReservedEvent() != null;
	}

	public boolean isEditAllowed() {
		return !isReserved() && ClosedSpecimensTracker.getInstance().isActive(this);
	}
	
	public boolean isAliquot() {
		return ALIQUOT.equals(lineage);
	}
	
	public boolean isDerivative() {
		return DERIVED.equals(lineage);
	}
	
	public boolean isPrimary() {
		return NEW.equals(lineage);
	}
	
	public boolean isCollected() {
		return isCollected(getCollectionStatus());
	}
	
	public boolean isPending() {
		return isPending(getCollectionStatus());
	}

	public boolean isMissed() {
		return isMissed(getCollectionStatus());
	}

	public boolean isNotCollected() {
		return isNotCollected(getCollectionStatus());
	}

	public boolean isMissedOrNotCollected() {
		return isMissed() || isNotCollected();
	}

	public Boolean isAvailable() {
		return getAvailableQuantity() == null || NumUtil.greaterThanZero(getAvailableQuantity());
	}

	public boolean isReceived() {
		return isPrimary() && isCollected() && getReceivedEvent() != null && getReceivedEvent().isReceived();
	}

	public boolean isStorageSiteBasedAccessRightsEnabled() {
		return getCollectionProtocol().storageSiteBasedAccessRightsEnabled();
	}

	public boolean isStoredInAutoFreezer() {
		return getPosition() != null && getPosition().getContainer() != null && getPosition().getContainer().isAutomated();
	}

	public void disable() {
		disable(!isForceDelete());
	}

	public void disable(boolean checkChildSpecimens) {
		if (isStoredInAutoFreezer()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_DELETE_NA, getLabel(), getPosition().toString());
		}

		if (getActivityStatus().equals(Status.ACTIVITY_STATUS_DISABLED.getStatus())) {
			return;
		}

		if (checkChildSpecimens) {
			ensureNoActiveChildSpecimens();
		}
		
		for (Specimen child : getChildCollection()) {
			child.setOpComments(getOpComments());
			child.disable(checkChildSpecimens);
		}

		setLabel(Utility.getDisabledValue(getLabel(), 255));
		setAdditionalLabel(Utility.getDisabledValue(getAdditionalLabel(), 255));
		setBarcode(Utility.getDisabledValue(getBarcode(), 255));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		virtualize(null, "Specimen deleted");
		updateAvailableStatus();
		FormUtil.getInstance().deleteRecords(getCpId(), Arrays.asList("Specimen", "SpecimenEvent", "SpecimenExtension"), getId());
		getDeleteEvents().add(SpecimenDeleteEvent.deleteEvent(this, getOpComments()));
	}

	public void undelete(boolean includeChildren) {
		if (getParentSpecimen() != null && getParentSpecimen().isDeleted()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PARENT_DELETED, getLabel(), getParentSpecimen().getId());
		}

		setLabel(Utility.stripTs(getLabel()));
		setAdditionalLabel(Utility.stripTs(getAdditionalLabel()));
		setBarcode(Utility.stripTs(getBarcode()));
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		updateAvailableStatus();
		FormUtil.getInstance().undeleteRecords(getCpId(), Arrays.asList("Specimen", "SpecimenEvent", "SpecimenExtension"), getId());
		getDeleteEvents().add(SpecimenDeleteEvent.undeleteEvent(this, getOpComments()));

		if (includeChildren) {
			for (Specimen child : getChildCollection()) {
				child.setOpComments(getOpComments());
				child.undelete(includeChildren);
			}
		}
	}
	
	public static boolean isCollected(String status) {
		return COLLECTED.equals(status);
	}
	
	public static boolean isPending(String status) {
		return PENDING.equals(status);
	}

	public static boolean isMissed(String status) {
		return MISSED_COLLECTION.equals(status);
	}

	public static boolean isNotCollected(String status) {
		return NOT_COLLECTED.equals(status);
	}

	public static boolean isReceived(String quality) {
		return StringUtils.isNotBlank(quality) && !quality.equals(TO_BE_RECEIVED);
	}

	public Integer getCopiesToPrint() {
		if (getSpecimenRequirement() != null) {
			return getSpecimenRequirement().getLabelPrintCopiesToUse();
		}

		CpSpecimenLabelPrintSetting setting = getCollectionProtocol().getSpmnLabelPrintSetting(getLineage());
		return setting != null ? setting.getCopies() : null;
	}

	public boolean isPrePrintEnabled() {
		return getSpecimenRequirement() != null &&
			getSpecimenRequirement().getLabelAutoPrintModeToUse() == CollectionProtocol.SpecimenLabelAutoPrintMode.PRE_PRINT;
	}

	public List<Specimen> createPendingSpecimens() {
		if (getSpecimenRequirement() == null || !isPrimary() || !isCollected() || CollectionUtils.isNotEmpty(getChildCollection())) {
			return Collections.emptyList();
		}

		return createPendingSpecimens(getSpecimenRequirement(), this);
	}

	public void prePrintChildrenLabels(String prevStatus, String prevRecv) {
		prePrintChildrenLabels(prevStatus, prevRecv, getLabelPrinter());
	}

	public void prePrintChildrenLabels(String prevStatus, String prevRecv, LabelPrinter<Specimen> printer) {
		if (getSpecimenRequirement() == null) {
			//
			// We pre-print child specimen labels of only planned specimens
			//
			return;
		}

		if (!isPrimary()) {
			//
			// We pre-print child specimen labels of only primary specimens
			//
			return;
		}

		if (!isCollected()) {
			//
			// We pre-print child specimen labels of only collected specimens
			//
			return;
		}

		switch (getCollectionProtocol().getSpmnLabelPrePrintMode()) {
			case ON_PRIMARY_COLL:
				if (Specimen.isCollected(prevStatus)) {
					//
					// specimen was previously collected. no need to print the child specimen labels
					//
					return;
				}
				break;

			case ON_PRIMARY_RECV:
				if (Specimen.isReceived(prevRecv)) {
					//
					// specimen was previously received. no need to print the child specimen labels
					//
					return;
				}

				if (!isReceived()) {
					//
					// specimen is not received, yet
					//
					return;
				}
				break;

			case ON_SHIPMENT_RECV:
				if (StringUtils.isBlank(getShipmentReceiveQuality())) {
					return;
				}

				String recvQuality = ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.PP_SHIPMENT_RECV_QUALITY);
				if (StringUtils.isNotBlank(recvQuality) && !Pattern.matches(recvQuality, getShipmentReceiveQuality())) {
					return;
				}
				break;

			default:
				//
				// the other values - on registration and on visit means the labels
				// were pre-printed when pending visits and specimens got created.
				// none means no pre-printing.
				// therefore returning from this point is the right thing to do.
				//
				return;
		}

		if (getCollectionProtocol().isManualSpecLabelEnabled()) {
			//
			// no child labels are pre-printed in specimen labels are manually scanned
			//
			return;
		}

		if (CollectionUtils.isNotEmpty(getChildCollection())) {
			//
			// We quit if there is at least one child specimen created underneath the primary specimen
			//
			return;
		}


		List<Specimen> pendingSpecimens = createPendingSpecimens(getSpecimenRequirement(), this);
		preCreatedSpmnsMap = pendingSpecimens.stream().collect(Collectors.toMap(Specimen::getReqId, s -> s));

		List<PrintItem<Specimen>> printItems = pendingSpecimens.stream()
			.filter(spmn -> spmn.getParentSpecimen().equals(this))
			.map(Specimen::getPrePrintItems)
			.flatMap(List::stream)
			.collect(Collectors.toList());
		if (!printItems.isEmpty()) {
			printer.print(printItems);
		}
	}

	public List<PrintItem<Specimen>> getPrePrintItems() {
		SpecimenRequirement requirement = getSpecimenRequirement();
		if (requirement == null) {
			//
			// OPSMN-4227: We won't pre-print unplanned specimens
			// This can happen when following state change transition happens:
			// visit -> completed -> planned + unplanned specimens collected -> visit missed -> pending
			//
			return Collections.emptyList();
		}

		List<PrintItem<Specimen>> result = new ArrayList<>();
		if (requirement.getLabelAutoPrintModeToUse() == CollectionProtocol.SpecimenLabelAutoPrintMode.PRE_PRINT) {
			Integer printCopies = requirement.getLabelPrintCopiesToUse();
			result.add(PrintItem.make(this, printCopies));
		}

		for (Specimen childSpmn : sort(getChildCollection())) {
			result.addAll(childSpmn.getPrePrintItems());
		}

		return result;
	}

	public void close(User user, Date time, String reason, String comments) {
		if (!getActivityStatus().equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus())) {
			return;
		}

		if (isStoredInAutoFreezer()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_CLOSE_NA, getLabel(), getPosition().toString());
		}

		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
		transferTo(holdingLocation, user, time, reason, false);
		addDisposalEvent(user, time, reason, comments);
		updateAvailableStatus();
		ClosedSpecimensTracker.getInstance().add(this);
	}
	
	public List<DependentEntityDetail> getDependentEntities() {
		return DependentEntityDetail.singletonList(Specimen.getEntityName(), getActiveChildSpecimens()); 
	}
		
	public void activate() {
		if (getActivityStatus().equals(Status.ACTIVITY_STATUS_ACTIVE.getStatus())) {
			return;
		}
		
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		updateAvailableStatus();
	}
		
	public CollectionProtocolRegistration getRegistration() {
		return getVisit().getRegistration();
	}

	public List<Specimen> getDescendants() {
		List<Specimen> result = new ArrayList<>();
		result.add(this);

		for (Specimen specimen : getChildCollection()) {
			result.addAll(specimen.getDescendants());
		}

		return result;
	}

	public void update(Specimen specimen) {
		if (!StringUtils.equals(getLineage(), specimen.getLineage())) {
			throw OpenSpecimenException.userError(
				SpecimenErrorCode.CANNOT_CHG_LINEAGE, getLineage(), specimen.getLineage());
		}

		boolean wasCollected = isCollected();

		setForceDelete(specimen.isForceDelete());
		setAutoCollectParents(specimen.isAutoCollectParents());
		setOpComments(specimen.getOpComments());

		String reason = null;
		if (!StringUtils.equals(getComment(), specimen.getComment())) {
			reason = specimen.getComment();
		}

		updateStatus(specimen, reason);

		//
		// NOTE: This has been commented to allow retrieving distributed specimens from the holding tanks
		//
		//	if (!isActive()) {
		//		return;
		//	}
		if (!isDeleted()) {
			setLabel(specimen.getLabel());
			setAdditionalLabel(specimen.getAdditionalLabel());
			setBarcode(specimen.getBarcode());
		}

		setImageId(specimen.getImageId());
		setInitialQuantity(specimen.getInitialQuantity());
		setAvailableQuantity(specimen.getAvailableQuantity());
		setConcentration(specimen.getConcentration());

		if (!getVisit().equals(specimen.getVisit())) {
			if (isPrimary()) {
				updateVisit(specimen.getVisit(), specimen.getSpecimenRequirement());
			} else {
				throw OpenSpecimenException.userError(SpecimenErrorCode.VISIT_CHG_NOT_ALLOWED, getLabel());
			}
		} else {
			updateRequirement(specimen.getSpecimenRequirement());
		}

		updateExternalIds(specimen.getExternalIds());
		updateEvent(getCollectionEvent(), specimen.getCollectionEvent());
		updateEvent(getReceivedEvent(), specimen.getReceivedEvent());

		setCreatedOn(specimen.getCreatedOn()); // required for auto-collection of parent specimens
		updateCollectionStatus(specimen.getCollectionStatus());

		setCheckout(specimen.getCheckout());
		updatePosition(specimen.getPosition(), specimen.getTransferUser(), specimen.getTransferTime(), specimen.getTransferComments());
		updateCreatedBy(specimen.getCreatedBy());

		if (isCollected()) {
			Date createdOn = specimen.getCreatedOn();
			if (isPrimary()) {
				updateCreatedOn(createdOn != null ? createdOn : getReceivedEvent().getTime());
			} else {
				updateCreatedOn(createdOn != null ? createdOn : Calendar.getInstance().getTime());

				if (!wasCollected) {
					getParentSpecimen().addToChildrenEvent(this);
				}
			}
		} else {
			updateCreatedOn(null);
		}

		// TODO: Specimen class/type should not be allowed to change
		Specimen spmnToUpdateFrom = null;
		if (isAliquot()) {
			spmnToUpdateFrom = getParentSpecimen();
		} else {
			spmnToUpdateFrom = specimen;
		}

		setTissueSite(spmnToUpdateFrom.getTissueSite());
		setTissueSide(spmnToUpdateFrom.getTissueSide());
		setSpecimenClass(spmnToUpdateFrom.getSpecimenClass());
		setSpecimenType(spmnToUpdateFrom.getSpecimenType());
		updateBiohazards(spmnToUpdateFrom.getBiohazards());
		setPathologicalStatus(spmnToUpdateFrom.getPathologicalStatus());
		setComment(specimen.getComment());

		setExtension(specimen.getExtension());
		setPrintLabel(specimen.isPrintLabel());
		setFreezeThawCycles(specimen.getFreezeThawCycles());
		setShipmentReceiveQuality(specimen.getShipmentReceiveQuality());
		setUpdated(true);
	}
	
	public void updateStatus(Specimen otherSpecimen, String comments) {
		updateStatus(otherSpecimen.getActivityStatus(), AuthUtil.getCurrentUser(), Calendar.getInstance().getTime(), "Not Specified", comments, isForceDelete());

		//
		// OPSMN-4629
		// the specimen is in closed state and has no position.
		// ensure the new updatable specimen has no position either.
		//
		if (!isActive()) {
			otherSpecimen.setPosition(null);
		}

		//
		// OPSMN-5615
		//
		if (isClosed()) {
			otherSpecimen.zeroOutAvailableQty();
		}
	}

	public void updateStatus(String activityStatus, User user, Date date, String reason, String comments, boolean isForceDelete) {
		if (isReserved()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.RESV_EDIT_NOT_ALLOWED, getLabel());
		}

		if (this.activityStatus != null && this.activityStatus.equals(activityStatus)) {
			return;
		}
		
		if (Status.ACTIVITY_STATUS_DISABLED.getStatus().equals(activityStatus)) {
			disable(!isForceDelete);
		} else if (Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(activityStatus)) {
			close(user, date, reason, comments);
		} else if (Status.ACTIVITY_STATUS_ACTIVE.getStatus().equals(activityStatus)) {
			activate();
		}
	}
	
	public void updateCollectionStatus(String collectionStatus) {
		if (collectionStatus.equals(getCollectionStatus())) {
			//
			// no change in collection status; therefore nothing needs to be done
			//
			return;
		}

		if (isMissed(collectionStatus)) {
			if (!getVisit().isCompleted() && !getVisit().isMissed()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_OR_MISSED_VISIT_REQ);
			} else if (getParentSpecimen() != null && !getParentSpecimen().isCollected() && !getParentSpecimen().isMissed()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_OR_MISSED_PARENT_REQ);
			} else {
				updateHierarchyStatus(collectionStatus);
			}
		} else if (isNotCollected(collectionStatus)) {
			if (!getVisit().isCompleted() && !getVisit().isNotCollected()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_OR_NC_VISIT_REQ);
			} else if (getParentSpecimen() != null && !getParentSpecimen().isCollected() && !getParentSpecimen().isNotCollected()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_OR_NC_PARENT_REQ);
			} else {
				updateHierarchyStatus(collectionStatus);
			}
		} else if (isPending(collectionStatus)) {
			if (!getVisit().isCompleted() && !getVisit().isPending()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_OR_PENDING_VISIT_REQ);
			} else if (getParentSpecimen() != null && !getParentSpecimen().isCollected() && !getParentSpecimen().isPending()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_OR_PENDING_PARENT_REQ);
			} else {
				updateHierarchyStatus(collectionStatus);
			}
		} else if (isCollected(collectionStatus)) {
			if (!getVisit().isCompleted()) {
				throw OpenSpecimenException.userError(VisitErrorCode.COMPL_VISIT_REQ);
			} else {
				if (getParentSpecimen() != null && !getParentSpecimen().isCollected()) {
					autoCollectParentSpecimens(this);
				}

				setCollectionStatus(collectionStatus);
				decAliquotedQtyFromParent();
				addOrUpdateCollRecvEvents();
			}
		}

		setStatusChanged(true);
	}
		
	public void distribute(DistributionOrderItem item) {
		if (!isCollected() || isClosed()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_AVAILABLE_FOR_DIST, getLabel());
		}

		if (isStoredInAutoFreezer()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_DIST_NA, getLabel(), getPosition().toString());
		}
		
		//
		// Deduct distributed quantity from available quantity
		//
		if (getAvailableQuantity() != null) {
			if (NumUtil.greaterThanEquals(getAvailableQuantity(), item.getQuantity())) {
				setAvailableQuantity(getAvailableQuantity().subtract(item.getQuantity()));
			} else {
				setAvailableQuantity(BigDecimal.ZERO);
			}
		}

		//
		// add distributed event
		//
		SpecimenDistributionEvent.createForDistributionOrderItem(item).saveRecordEntry();

		//
		// cancel the reservation so that it can be distributed subsequently if available
		//
		cancelReservation();

		//
		// close specimen if explicitly closed or no quantity available
		//
		DistributionOrder order = item.getOrder();
		String dpShortTitle = order.getDistributionProtocol().getShortTitle();
		if (Boolean.TRUE.equals(order.getCheckoutSpecimens())) {
			transferTo(null, order.getDistributor(), order.getExecutionDate(), "Distributed to " + dpShortTitle, true);
			item.setStatus(DistributionOrderItem.Status.DISTRIBUTED);
			setAvailabilityStatus(DISTRIBUTED);
		} else if (item.isDistributedAndClosed()) {
			close(order.getDistributor(), order.getExecutionDate(), "Distributed", "Distributed to " + dpShortTitle);
			setAvailabilityStatus(DISTRIBUTED);
		}
	}

	public void returnSpecimen(DistributionOrderItem item) {
		if (isClosed()) {
			setAvailableQuantity(item.getReturnedQuantity());
			activate();
		} else {
			if (getAvailableQuantity() == null) {
				setAvailableQuantity(item.getReturnedQuantity());
			} else if (item.getReturnedQuantity() != null) {
				setAvailableQuantity(getAvailableQuantity().add(item.getReturnedQuantity()));
			}

			updateAvailableStatus();
		}

		StorageContainer container = item.getReturningContainer();
		if (container != null) {
			StorageContainerPosition position = container.createPosition(item.getReturningColumn(), item.getReturningRow());
			transferTo(position, item.getReturnedBy(), item.getReturnDate(), "Specimen returned", false);
		}

		SpecimenReturnEvent.createForDistributionOrderItem(item).saveRecordEntry();
	}

	public void undoDistribution(DistributionOrderItem item) {
		if (isClosed()) {
			activate();
		}

		if (getAvailableQuantity() == null) {
			setAvailableQuantity(item.getQuantity());
		} else if (item.getQuantity() != null) {
			setAvailableQuantity(getAvailableQuantity().add(item.getQuantity()));
		}

		if (NumUtil.greaterThan(getAvailableQuantity(), getInitialQuantity())) {
			setAvailableQuantity(getInitialQuantity());
		}

		SpecimenDistributionEvent.createForDistributionOrderItem(item).delete();
		if (item.getStatus() == DistributionOrderItem.Status.RETURNED) {
			SpecimenReturnEvent.createForDistributionOrderItem(item).delete();
		}

		if (getPosition() != null && getPosition().isHoldingLocation()) {
			virtualize(null, "Undo distribution");
		}
	}

	public void cancelReservation() {
		if (getReservedEvent() != null) {
			setReservedEvent(null);
			updateAvailableStatus();
		}
	}

	public void virtualise(String comments) {
		virtualize(Calendar.getInstance().getTime(), comments);
	}

	private void updateCreatedOn(Date createdOn) {
		this.createdOn = createdOn;
		if (getParentEvent() != null && createdOn != null && !createdOn.equals(getParentEvent().getTime())) {
			getParentEvent().setTime(createdOn);
			getParentEvent().getChildren().forEach(spmn -> spmn.setCreatedOn(createdOn));
		}

		if (createdOn == null) {
			for (Specimen childSpecimen : getChildCollection()) {
				childSpecimen.updateCreatedOn(createdOn);
			}
		}

		// OPSMN-4871: No checks on created on date.
		// if (createdOn.after(Calendar.getInstance().getTime())) {
		//	throw OpenSpecimenException.userError(SpecimenErrorCode.CREATED_ON_GT_CURRENT);
		// }

		// The below code is commented for now, so that there will not be any issue for the legacy data.
		// In legacy data created on was simple date field, but its been changed to timestamp in v20.
		// While migrating time part of the date is set as 00:00:00,
		// but the created on of primary specimen(fetched from received event time stamp) will have time part within.
		// So there is large possibility of below 2 exceptions.
		/*if (!isPrimary() && createdOn.before(getParentSpecimen().getCreatedOn())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.CHILD_CREATED_ON_LT_PARENT);
		}

		for (Specimen childSpecimen : getChildCollection()) {
			if (childSpecimen.getCreatedOn() != null && createdOn.after(childSpecimen.getCreatedOn())) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.PARENT_CREATED_ON_GT_CHILDREN);
			}
		}*/
	}

	private void updateCreatedBy(User user) {
		setCreatedBy(user);
		if (isPrimary() || user == null || getParentEvent() == null) {
			return;
		}

		getParentEvent().setUser(user);
	}

	private void addDisposalEvent(User user, Date time, String reason, String comments) {
		SpecimenDisposalEvent event = new SpecimenDisposalEvent(this);
		if (StringUtils.isBlank(reason)) {
			reason = Specimen.NOT_SPECIFIED;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getByValue(PvAttributes.SPECIMEN_DISPOSE_REASON, reason);
		if (pv == null) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.INV_DISPOSE_REASON, reason);
		}

		event.setReason(pv);
		event.setUser(user);
		event.setTime(time);
		event.setComments(comments);
		event.saveOrUpdate();
		zeroOutAvailableQty();
	}

	private void zeroOutAvailableQty() {
		if (getAvailableQuantity() != null && getCollectionProtocol().isZeroOutQtyEnabled()) {
			setAvailableQuantity(BigDecimal.ZERO);
		}
	}

	private void virtualize(Date time, String comments) {
		transferTo(null, null, time, comments, Boolean.TRUE.equals(checkout));
	}

	private void transferTo(StorageContainerPosition newPosition, User user, Date time, String comments, boolean checkout) {
		StorageContainerPosition oldPosition = getPosition();
		setOldPosition(oldPosition);

		if (isStoredInAutoFreezer()) {
			if (newPosition != null && !StorageContainerPosition.areSame(oldPosition, newPosition)) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_TRANSFER_NA, getLabel(), getPosition().toString());
			}

			if (checkout) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_CHECK_OUT_NA, getLabel(), getPosition().toString());
			}
		}

		setTransferComments(comments);
		if (StorageContainerPosition.areSame(oldPosition, newPosition)) {
			// for closed and checked out specimens, both old and new position will be the same
			if ((isDeleted() || isClosed()) && getCheckoutPosition() != null) {
				getCheckoutPosition().vacate();
				setCheckoutPosition(null);
			}

			return;
		}

		if (isClosed() && (newPosition != null && !newPosition.getContainer().isDistributionContainer())) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORE_NOT_ALLOWED, getLabel());
		}

		if (oldPosition != null && !oldPosition.isSupressAccessChecks()) {
			AccessCtrlMgr.getInstance().ensureSpecimenStoreRights(oldPosition.getContainer());
		}

		if (newPosition != null && !newPosition.isSupressAccessChecks()) {
			AccessCtrlMgr.getInstance().ensureSpecimenStoreRights(newPosition.getContainer());
		}

		SpecimenTransferEvent transferEvent = new SpecimenTransferEvent(this);
		transferEvent.setUser(user == null ? AuthUtil.getCurrentUser() : user);
		transferEvent.setTime(time == null ? Calendar.getInstance().getTime() : time);
		transferEvent.setComments(comments);
		
		if (oldPosition != null && newPosition != null) {
			oldPosition.getContainer().retrieveSpecimen(this);
			newPosition.getContainer().storeSpecimen(this);

			transferEvent.setFromLocation(oldPosition);
			transferEvent.setToLocation(newPosition);

			oldPosition.update(newPosition);			
		} else if (oldPosition != null) {
			oldPosition.getContainer().retrieveSpecimen(this);
			if (isStoredInAutoFreezer() && !Boolean.TRUE.equals(retrievedNotif)) {
				return;
			}

			transferEvent.setFromLocation(oldPosition);
			if (checkout) {
				StorageContainerPosition checkoutPos = new StorageContainerPosition();
				checkoutPos.setPosOneOrdinal(oldPosition.getPosOneOrdinal());
				checkoutPos.setPosOne(oldPosition.getPosOne());
				checkoutPos.setPosTwoOrdinal(oldPosition.getPosTwoOrdinal());
				checkoutPos.setPosTwo(oldPosition.getPosTwo());
				checkoutPos.setContainer(oldPosition.getContainer());
				checkoutPos.setBlocked(true);
				checkoutPos.setCheckoutSpecimen(this);
				checkoutPos.setCheckoutBy(transferEvent.getUser());
				checkoutPos.setCheckoutTime(transferEvent.getTime());
				checkoutPos.setCheckoutComments(transferEvent.getComments());
				setCheckoutPosition(checkoutPos);

				transferEvent.setStatus("CHECK-OUT");
			}

			oldPosition.vacate();
			setPosition(null);
		} else if (newPosition != null) {
			newPosition.getContainer().storeSpecimen(this);
			transferEvent.setToLocation(newPosition);
			
			newPosition.setOccupyingSpecimen(this);
			newPosition.occupy();
			setPosition(newPosition);
		}

		if (!checkout && getCheckoutPosition() != null) {
			getCheckoutPosition().vacate();
			setCheckoutPosition(null);
			transferEvent.setStatus("CHECK-IN");
			transferEvent.setComments(comments);
		}
		
		transferEvent.saveOrUpdate();		
	}

	public void addChildSpecimen(Specimen specimen) {
		int childrenLimit = ConfigUtil.getInstance().getIntSetting(ConfigParams.MODULE, ConfigParams.MAX_CHILDREN_LIMIT, 100);
		if (getChildCollection().size() >= childrenLimit) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.CHILDREN_LIMIT_MAXED, getLabel(), childrenLimit);
		}

		specimen.setParentSpecimen(this);
		if (!isCollected() && specimen.isCollected()) {
			autoCollectParentSpecimens(specimen);
		}

		if (specimen.isAliquot()) {
			specimen.decAliquotedQtyFromParent();
		}

		specimen.occupyPosition();
		getChildCollection().add(specimen);
		addToChildrenEvent(specimen);
	}

	public void addPoolSpecimen(Specimen spmn, boolean close) {
		if (!isCollected()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_COLLECTED, getLabel());
		}

		if (!spmn.getCollectionProtocol().equals(getCollectionProtocol())) {
			throw OpenSpecimenException.userError(
				SpecimenErrorCode.POOL_SAME_CP_REQ,
				getCollectionProtocol().getShortTitle(),
				spmn.getCollectionProtocol().getShortTitle());
		}

		if (!spmn.isCollected()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_COLLECTED, spmn.getLabel());
		}

		if (!spmn.isEditAllowed()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PROC_NOT_ALLOWED, spmn.getLabel());
		}

		if (spmn.isStorageSiteBasedAccessRightsEnabled() && spmn.isStored()) {
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateSpecimenRights(spmn);
		}

		if (pooledEvent == null) {
			pooledEvent = new SpecimenPooledEvent();
			pooledEvent.setPooledSpecimen(this);
			pooledEvent.setUser(AuthUtil.getCurrentUser());
			pooledEvent.setTime(Calendar.getInstance().getTime());
			pooledEvent.setComments(getOpComments());
			daoFactory.getSpecimenDao().savedPooledEvent(pooledEvent);
		}

		String comments = "Consumed for creating pooled specimen: " + getLabel();
		if (StringUtils.isNotBlank(pooledEvent.getComments())) {
			comments += "\n\n" + pooledEvent.getComments();
		}

		pooledEvent.addPoolItem(spmn);
		spmn.setPoolItem(true);
		if (close) {
			spmn.close(pooledEvent.getUser(), pooledEvent.getTime(), "Pooled", comments);
		}
	}

	public void addPooledEvent() {
		SpecimenEvent event = new SpecimenEvent(this) {
			{
				setUser(pooledEvent.getUser());
				setTime(pooledEvent.getTime());
				setComments(pooledEvent.getComments());
			};

			@Override
			public String getFormName() {
				return "SpecimenPooledEvent";
			}

			@Override
			protected Map<String, Object> getEventAttrs() {
				return null;
			}

			@Override
			protected void setEventAttrs(Map<String, Object> attrValues) {

			}
		};

		// Note: The ID cannot be set in the initialiser like other attributes.
		// Otherwise it will result in NPE when setting user etc
		event.setId(pooledEvent.getId());
		event.saveRecordEntry();
	}
	
	public void setPending() {
		updateCollectionStatus(PENDING);
	}

	public void decAliquotedQtyFromParent() {
		if (isCollected() && isAliquot()) {
			adjustParentSpecimenQty(initialQuantity);
		}		
	}
	
	public void occupyPosition() {
		if (position == null) {
			return;
		}
		
		if (!isCollected()) { 
			// Un-collected (pending/missed collection) specimens can't occupy space
			position = null;
			return;
		}

		position.getContainer().storeSpecimen(this);
		position.occupy();
	}
	
	public void addOrUpdateCollRecvEvents() {
		if (!isCollected() || isAliquot() || isDerivative()) {
			return;
		}

		getCollectionEvent().saveOrUpdate();

		SpecimenReceivedEvent receivedEvent = getReceivedEvent();
		if (receivedEvent.isReceived()) {
			if (receivedEvent.getUser() == null) {
				receivedEvent.setUser(AuthUtil.getCurrentUser());
			}

			if (receivedEvent.getTime() == null) {
				receivedEvent.setTime(Calendar.getInstance().getTime());
			}
		} else {
			receivedEvent.setUser(null);
			receivedEvent.setTime(null);
		}

		receivedEvent.saveOrUpdate();
	}

	public void setKitLabelsIfEmpty() {
		boolean skip = !getCollectionProtocol().isKitLabelsEnabled() ||
			isMissedOrNotCollected() ||
			StringUtils.isNotBlank(label) ||
			getSpecimenRequirement() == null;
		if (skip) {
			return;
		}

		Map<String, String> kitLabels = getKitLabel(getVisit(), getSpecimenRequirement());
		if (kitLabels == null || kitLabels.isEmpty()) {
			return;
		}

		if (StringUtils.isBlank(label)) {
			setLabel(kitLabels.get("label"));
		}

		if (getCollectionProtocol().isBarcodingEnabledToUse() && StringUtils.isBlank(barcode)) {
			setBarcode(kitLabels.get("barcode"));
		}
	}
	
	public void setLabelIfEmpty() {
		if (StringUtils.isNotBlank(label) || isMissedOrNotCollected()) {
			return;
		}

		String labelTmpl = getLabelTmpl();
		String label = null;
		if (StringUtils.isNotBlank(labelTmpl)) {
			label = labelGenerator.generateLabel(labelTmpl, this);
		} else if (isAliquot() || isDerivative()) {
			Specimen parentSpecimen = getParentSpecimen();
			int count = parentSpecimen.getChildCollection().size();
			label = parentSpecimen.getLabel() + "_" + (count + 1);
		}
		
		if (StringUtils.isBlank(label)) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABEL_REQUIRED);
		}
		
		setLabel(label);
	}

	public void setAdditionalLabelIfEmpty() {
		if (StringUtils.isNotBlank(additionalLabel) || isMissedOrNotCollected()) {
			return;
		}

		String labelTmpl = getCollectionProtocol().getAdditionalLabelFormatToUse();
		if (StringUtils.isNotBlank(labelTmpl)) {
			setAdditionalLabel(addlLabelGenerator.generateLabel(labelTmpl, this));
		}
	}

	public void setBarcodeIfEmpty() {
		if (StringUtils.isNotBlank(barcode) ||
			isMissedOrNotCollected() ||
			!getCollectionProtocol().isBarcodingEnabledToUse() ||
			isPreBarcodedTube()) {
			return;
		}

		String barcodeTmpl = getCollectionProtocol().getSpecimenBarcodeFormatToUse();
		if (StringUtils.isNotBlank(barcodeTmpl)) {
			setBarcode(barcodeGenerator.generateLabel(barcodeTmpl, this));
		}
	}

	public boolean isPreBarcodedTube() {
		return getSpecimenRequirement() != null && getSpecimenRequirement().isPreBarcodedTube();
	}

	public String getLabelTmpl() {
		return getLabelTmpl(true);
	}

	public String getLabelTmpl(boolean useWfSettings) {
		String labelTmpl = null;
		
		SpecimenRequirement sr = getSpecimenRequirement();
		if (sr != null) { // anticipated specimen
			labelTmpl = sr.getLabelFormat();
		}
				
		if (StringUtils.isNotBlank(labelTmpl)) {
			return labelTmpl;
		}
		
		CollectionProtocol cp = getVisit().getCollectionProtocol();
		if (isAliquot()) {
			labelTmpl = cp.getAliquotLabelFormatToUse();
		} else if (isDerivative()) {
			labelTmpl = cp.getDerivativeLabelFormat();
		} else {
			labelTmpl = cp.getSpecimenLabelFormat();
		}

		if (StringUtils.isBlank(labelTmpl) && useWfSettings) {
			labelTmpl = LabelSettingsUtil.getLabelFormat(this);
		}
		
		return labelTmpl;		
	}

	public void updatePosition(StorageContainerPosition newPosition) {
		updatePosition(newPosition, null);
	}
	
	public void updatePosition(StorageContainerPosition newPosition, Date time) {
		updatePosition(newPosition, null, time, null);
	}

	public void updatePosition(StorageContainerPosition newPosition, User user, Date time, String comments) {
		if (!isCollected()) {
			return;
		}

		if (isDeleted()) {
			//
			// deleted specimens will not have any locations assigned to them
			//
			newPosition = null;
		}

		if (newPosition != null) {
			StorageContainer container = newPosition.getContainer();
			if (container == null || (!container.isDimensionless() && !newPosition.isSpecified())) {
				newPosition = null;
			}
		}

		transferTo(newPosition, user, time, comments, Boolean.TRUE.equals(checkout));
	}

	public String getLabelOrDesc() {
		if (StringUtils.isNotBlank(label)) {
			return label;
		}
		
		return getDesc(specimenClass, specimenType);
	}

	public void incrementFreezeThaw(Integer incrementFreezeThaw) {
		if (freezeThawIncremented) {
			return;
		}

		if (incrementFreezeThaw == null || incrementFreezeThaw <= 0) {
			return;
		}

		if (getFreezeThawCycles() == null) {
			setFreezeThawCycles(incrementFreezeThaw);
		} else {
			setFreezeThawCycles(getFreezeThawCycles() + incrementFreezeThaw);
		}

		freezeThawIncremented = true;
	}

	public StorageContainerPosition getOldPosition() {
		return oldPosition;
	}

	public void setOldPosition(StorageContainerPosition oldPosition) {
		this.oldPosition = oldPosition;
	}

	public boolean isStoredInDistributionContainer() {
		return getPosition() != null && getPosition().getContainer().isDistributionContainer();
	}

	public boolean isStored() {
		return getPosition() != null && getPosition().getContainer() != null;
	}

	public void updateHierarchyStatus() {
		updateHierarchyStatus(getCollectionStatus());
	}

	public void updateHierarchyStatus(String collectionStatus) {
		updateHierarchyStatus0(collectionStatus);

		List<Specimen> createdSpmns = null;
		if (isMissed(collectionStatus)) {
			createdSpmns = createMissedChildSpecimens();
		} else if (isNotCollected(collectionStatus)) {
			createdSpmns = createNotCollectedSpecimens();
		}

		if (CollectionUtils.isEmpty(createdSpmns)) {
			return;
		}

		if (preCreatedSpmnsMap == null) {
			preCreatedSpmnsMap = new HashMap<>();
		}

		preCreatedSpmnsMap.putAll(createdSpmns.stream().collect(Collectors.toMap(Specimen::getReqId, s -> s)));
	}

	public Long getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(Long ancestorId) {
		this.ancestorId = ancestorId;
	}

	public void updateAvailableStatus() {
		if (isActive()) {
			if (isCollected()) {
				setAvailabilityStatus(isReserved() ? Specimen.RESERVED : Specimen.AVAILABLE);
			} else {
				setAvailabilityStatus(getCollectionStatus());
			}
		} else if (isDeleted()) {
			setAvailabilityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		} else if (!DISTRIBUTED.equals(getAvailabilityStatus()) && isClosed()) {
			setAvailabilityStatus(Specimen.DISPOSED);
		}
	}

	//
	// HSEARCH-1350: https://hibernate.atlassian.net/browse/HSEARCH-1350
	//
	public void initCollections() {
		getBiohazards().size();
		getExternalIds().size();
	}

	public static LabelPrinter<Specimen> getLabelPrinter() {
		String labelPrinterBean = ConfigUtil.getInstance().getStrSetting(
			ConfigParams.MODULE,
			ConfigParams.SPECIMEN_LABEL_PRINTER,
			"defaultSpecimenLabelPrinter");

		return (LabelPrinter<Specimen>)OpenSpecimenAppCtxProvider.getAppCtx().getBean(labelPrinterBean);
	}

	public static String getDesc(PermissibleValue specimenClass, PermissibleValue type) {
		String spmnClass = specimenClass != null && StringUtils.isNotBlank(specimenClass.getValue()) ? specimenClass.getValue() : "Unknown";
		String spmnType  = type != null && StringUtils.isNotBlank(type.getValue()) ? type.getValue() : "Unknown";
		return getDesc(spmnClass, spmnType);
	}

	public static String getDesc(String specimenClass, String type) {
		StringBuilder desc = new StringBuilder();
		if (StringUtils.isNotBlank(specimenClass)) {
			desc.append(specimenClass.trim());
		}
		
		if (StringUtils.isNotBlank(type)) {
			if (desc.length() > 0) {
				desc.append("-");
			}
			
			desc.append(type.trim());
		}

		if (desc.length() == 0) {
			desc.append("Unknown");
		}
			
		return desc.toString();		
	}
	
	//
	// Useful for sorting specimens at same level
	//
	public static List<Specimen> sort(Collection<Specimen> specimens) {
		List<Specimen> result = new ArrayList<>(specimens);
		Collections.sort(result, new Comparator<Specimen>() {
			@Override
			public int compare(Specimen s1, Specimen s2) {
				Integer s1SortOrder = sortOrder(s1);
				Integer s2SortOrder = sortOrder(s2);

				Long s1ReqId = reqId(s1);
				Long s2ReqId = reqId(s2);

				if (s1SortOrder != null && s2SortOrder != null) {
					return s1SortOrder.compareTo(s2SortOrder);
				} else if (s1SortOrder != null) {
					return -1;
				} else if (s2SortOrder != null) {
					return 1;
				} else if (s1ReqId != null && s2ReqId != null) {
					if (!s1ReqId.equals(s2ReqId)) {
						return s1ReqId.compareTo(s2ReqId);
					} else {
						return compareById(s1, s2);
					}
				} else if (s1ReqId != null) {
					return -1;
				} else if (s2ReqId != null) {
					return 1;
				} else {
					return compareById(s1, s2);
				}
			}

			private int compareById(Specimen s1, Specimen s2) {
				if (s1.getId() != null && s2.getId() != null) {
					return s1.getId().compareTo(s2.getId());
				} else if (s1.getId() != null) {
					return -1;
				} else if (s2.getId() != null) {
					return 1;
				} else {
					return 0;
				}
			}

			private Integer sortOrder(Specimen s) {
				if (s.getSpecimenRequirement() != null) {
					return s.getSpecimenRequirement().getSortOrder();
				}

				return null;
			}

			private Long reqId(Specimen s) {
				if (s.getSpecimenRequirement() != null) {
					return s.getSpecimenRequirement().getId();
				}

				return null;
			}
		});

		return result;
	}

	public static List<Specimen> sortByLabels(Collection<Specimen> specimens, final List<String> labels) {
		return specimens.stream()
			.sorted(Comparator.comparingInt((s) -> labels.indexOf(s.getLabel())))
			.collect(Collectors.toList());
	}

	public static List<Specimen> sortByIds(Collection<Specimen> specimens, final List<Long> ids) {
		return specimens.stream()
			.sorted(Comparator.comparingInt((s) -> ids.indexOf(s.getId())))
			.collect(Collectors.toList());
	}

	public static List<Specimen> sortByBarcodes(Collection<Specimen> specimens, final List<String> barcodes) {
		return specimens.stream()
			.sorted(Comparator.comparingInt((s) -> barcodes.indexOf(s.getBarcode())))
			.collect(Collectors.toList());
	}

	public static List<Specimen> sortByAdditionalLabels(Collection<Specimen> specimens, final List<String> additionalLabels) {
		return specimens.stream()
			.sorted(Comparator.comparingInt((s) -> additionalLabels.indexOf(s.getAdditionalLabel())))
			.collect(Collectors.toList());
	}

	public static boolean isValidLineage(String lineage) {
		if (StringUtils.isBlank(lineage)) {
			return false;
		}

		return lineage.equals(NEW) || lineage.equals(DERIVED) || lineage.equals(ALIQUOT);
	}

	private Map<String, String>  getKitLabel(Visit visit, SpecimenRequirement sr) {
		List<Map<String, String>> labels = daoFactory.getSpecimenDao().getKitLabels(visit, sr);
		if (labels.isEmpty()) {
			String validation = CpWorkflowTxnCache.getInstance().getValue(visit.getCollectionProtocol().getId(), "supplySettings", "barcodeValidation");
			if (StringUtils.equalsIgnoreCase(validation, "relaxed")) {
				return null;
			}

			throw OpenSpecimenException.userError(SpecimenErrorCode.NO_KIT_LABEL, visit.getDescription(), sr.getDescription());
		} else if (labels.size() > 1) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.MULTI_KIT_LABELS, visit.getDescription(), sr.getDescription());
		}

		return labels.iterator().next();
	}

	private void addToChildrenEvent(Specimen childSpmn) {
		if (!childSpmn.isCollected() || childSpmn.getParentSpecimen() == null) {
			return;
		}

		if (!isEditAllowed()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.PROC_NOT_ALLOWED, getLabel());
		}

		SpecimenChildrenEvent currentEvent = null;
		if (childSpmn.isAliquot()) {
			currentEvent = aliquotEvent;
		}

		if (currentEvent == null) {
			currentEvent = new SpecimenChildrenEvent();
			currentEvent.setSpecimen(this);
			currentEvent.setLineage(childSpmn.getLineage());
			currentEvent.setUser(childSpmn.getCreatedBy() != null ? childSpmn.getCreatedBy() : AuthUtil.getCurrentUser());
			currentEvent.setTime(childSpmn.getCreatedOn());
			getChildrenEvents().add(currentEvent);
		}

		currentEvent.addChild(childSpmn);

		if (childSpmn.isAliquot()) {
			aliquotEvent = currentEvent;
		}
	}

	private void ensureNoActiveChildSpecimens() {
		for (Specimen specimen : getChildCollection()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.REF_ENTITY_FOUND);
			}
		}
	}
	
	private int getActiveChildSpecimens() {
		int count = 0;
		for (Specimen specimen : getChildCollection()) {
			if (specimen.isActiveOrClosed() && specimen.isCollected()) {
				++count;
			}
		}

		return count;
	}
			
	private void deleteEvents() {
		if (!isAliquot() && !isDerivative()) {
			getCollectionEvent().delete();
			getReceivedEvent().delete();
		}
		
		for (SpecimenTransferEvent te : getTransferEvents()) {
			te.delete();
		}
	}

	private void adjustParentSpecimenQty(BigDecimal qty) {
		BigDecimal parentQty = parentSpecimen.getAvailableQuantity();
		if (parentQty == null || NumUtil.isZero(parentQty) || qty == null) {
			return;
		}

		parentQty = parentQty.subtract(qty);
		if (NumUtil.lessThanEqualsZero(parentQty)) {
			parentQty = BigDecimal.ZERO;
		}

		parentSpecimen.setAvailableQuantity(parentQty);
	}
	
	private void updateEvent(SpecimenEvent thisEvent, SpecimenEvent otherEvent) {
		if (isAliquot() || isDerivative()) {
			return;
		}
		
		thisEvent.update(otherEvent);
	}
	
	private void updateHierarchyStatus0(String status) {
		setCollectionStatus(status);
		setStatusChanged(true);
		updateAvailableStatus();

		if (getId() != null && !isCollected(status)) {
			setAvailableQuantity(BigDecimal.ZERO);

			if (getPosition() != null) {
				getPosition().vacate();
			}
			setPosition(null);

			if (getParentEvent() != null) {
				getParentEvent().removeChild(this);
			}
				
			deleteEvents();
		}

		getChildCollection().forEach(child -> child.updateHierarchyStatus0(status));
	}

	private List<Specimen> createMissedChildSpecimens() {
		return createChildSpecimens(Specimen.MISSED_COLLECTION);
	}

	private List<Specimen> createNotCollectedSpecimens() {
		return createChildSpecimens(Specimen.NOT_COLLECTED);
	}

	private List<Specimen> createChildSpecimens(String status) {
		if (getSpecimenRequirement() == null) {
			return Collections.emptyList();
		}

		List<Specimen> result = new ArrayList<>();
		Set<SpecimenRequirement> anticipated = new LinkedHashSet<>(getSpecimenRequirement().getOrderedChildRequirements());
		for (Specimen childSpmn : getChildCollection()) {
			if (childSpmn.getSpecimenRequirement() != null) {
				anticipated.remove(childSpmn.getSpecimenRequirement());
				result.addAll(childSpmn.createChildSpecimens(status));
			}
		}

		for (SpecimenRequirement sr : anticipated) {
			Specimen specimen = sr.getSpecimen();
			specimen.setVisit(getVisit());
			specimen.setParentSpecimen(this);
			specimen.setCollectionStatus(status);
			specimen.updateAvailableStatus();
			daoFactory.getSpecimenDao().saveOrUpdate(specimen);
			specimen.addOrUpdateExtension();
			getChildCollection().add(specimen);

			result.add(specimen);
			result.addAll(specimen.createChildSpecimens(status));
		}

		return result;
	}

	private void autoCollectParentSpecimens(Specimen childSpmn) {
		Specimen parentSpmn = childSpmn.getParentSpecimen();
		while (parentSpmn != null && parentSpmn.isPending()) {
			parentSpmn.setCollectionStatus(COLLECTED);
			parentSpmn.updateAvailableStatus();
			parentSpmn.setStatusChanged(true);

			Date createdOn = childSpmn.getCreatedOn();
			if (parentSpmn.isPrimary()) {
				if (!parentSpmn.getReceivedEvent().isReceived()) {
					parentSpmn.getReceivedEvent().setQuality(getAcceptableReceiveQuality());
				}

				parentSpmn.addOrUpdateCollRecvEvents();

				if (createdOn == null) {
					createdOn = parentSpmn.getReceivedEvent().getTime();
				}

				if (createdOn == null) {
					createdOn = Calendar.getInstance().getTime();
				}

				parentSpmn.setCreatedOn(createdOn);
			} else {
				parentSpmn.setCreatedOn(createdOn != null ? createdOn : Calendar.getInstance().getTime());
			}

			parentSpmn.addToChildrenEvent(childSpmn);

			childSpmn = parentSpmn;
			parentSpmn = parentSpmn.getParentSpecimen();
		}

		if (parentSpmn != null) {
			//
			// this means the parent specimen was pre-collected.
			// therefore need to add a processing event for its
			// recently collected child specimen
			//
			parentSpmn.addToChildrenEvent(childSpmn);
		}
	}

	private void updateVisit(Visit visit, SpecimenRequirement sr) {
		setVisit(visit);
		setCollectionProtocol(visit.getCollectionProtocol());
		setSpecimenRequirement(sr);
		getChildCollection().forEach(child -> child.updateVisit(visit, null));
	}

	private void updateRequirement(SpecimenRequirement sr) {
		if (Objects.equals(getSpecimenRequirement(), sr)) {
			return;
		}

		boolean hasPrevReq = getSpecimenRequirement() != null;
		setSpecimenRequirement(sr);
		if (hasPrevReq) {
			getChildCollection().forEach(child -> child.updateRequirement(null));
		}

		if (sr != null && !isPrimary()) {
			if (!Objects.equals(getParentSpecimen().getSpecimenRequirement(), sr.getParentSpecimenRequirement())) {
				throw OpenSpecimenException.userError(SpecimenErrorCode.REQ_PARENT_MISMATCH);
			}
		}
	}

	private void updateExternalIds(Collection<SpecimenExternalIdentifier> otherExternalIds) {
		for (SpecimenExternalIdentifier externalId : otherExternalIds) {
			SpecimenExternalIdentifier existing = getExternalIdByName(getExternalIds(), externalId.getName());
			if (existing == null) {
				SpecimenExternalIdentifier newId = new SpecimenExternalIdentifier();
				newId.setSpecimen(this);
				newId.setName(externalId.getName());
				newId.setValue(externalId.getValue());
				getExternalIds().add(newId);
			} else {
				existing.setValue(externalId.getValue());
			}
		}

		getExternalIds().removeIf(externalId -> getExternalIdByName(otherExternalIds, externalId.getName()) == null);
	}

	private SpecimenExternalIdentifier getExternalIdByName(Collection<SpecimenExternalIdentifier> externalIds, String name) {
		return externalIds.stream().filter(externalId -> StringUtils.equals(externalId.getName(), name)).findFirst().orElse(null);
	}

	private List<Specimen> createPendingSpecimens(SpecimenRequirement sr, Specimen parent) {
		List<Specimen> result = new ArrayList<>();
		if (sr == null || sr.isClosed()) {
			return result;
		}

		for (SpecimenRequirement childSr : sr.getOrderedChildRequirements()) {
			if (childSr.isClosed()) {
				continue;
			}

			Specimen specimen = childSr.getSpecimen();
			specimen.setParentSpecimen(parent);
			specimen.setVisit(parent.getVisit());
			specimen.setCollectionStatus(Specimen.PENDING);
			specimen.updateAvailableStatus();
			specimen.setKitLabelsIfEmpty();
			specimen.setLabelIfEmpty();

			parent.addChildSpecimen(specimen);
			daoFactory.getSpecimenDao().saveOrUpdate(specimen);
			specimen.addOrUpdateExtension();
			EventPublisher.getInstance().publish(new SpecimenSavedEvent(specimen));

			result.add(specimen);
			result.addAll(createPendingSpecimens(childSr, specimen));
		}

		return result;
	}

	private List<String> getReqAttrs(Function<SpecimenRequirement, String> mapper) {
		List<String> result = new ArrayList<>();
		Specimen specimen = this;
		while (specimen != null) {
			if (specimen.getSpecimenRequirement() != null) {
				String attr = mapper.apply(specimen.getSpecimenRequirement());
				if (StringUtils.isNotBlank(attr)) {
					result.add(0, attr);
				}
			}

			specimen = specimen.getParentSpecimen();
		}

		return result;
	}

	private PermissibleValue getAcceptableReceiveQuality() {
		return daoFactory.getPermissibleValueDao().getPv("receive_quality", "Acceptable", true);
	}
}
