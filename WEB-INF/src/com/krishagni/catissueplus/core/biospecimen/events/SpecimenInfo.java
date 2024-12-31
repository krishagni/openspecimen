
package com.krishagni.catissueplus.core.biospecimen.events;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CpSpecimenLabelPrintSetting;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenChildrenEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionReceiveDetail;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.NameValuePair;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.NumUtil;

@ListenAttributeChanges
public class SpecimenInfo extends AttributeModifiedSupport implements Comparable<SpecimenInfo>, Serializable {
	
	private static final long serialVersionUID = -2766658206691562011L;

	private Long id;

	private Long cpId;

	private Long cprId;

	private String ppid;
	
	private Long eventId;

	private String eventCode;

	private String eventLabel;
	
	private Long visitId;
	
	private String visitName;

	private String visitStatus;

	private String sprNo;

	private Date visitDate;
	
	private String cpShortTitle;
	
	private Long reqId;
	
	private Integer sortOrder;
	
	private String label;

	private String additionalLabel;
	
	private String barcode;

	private String type;
	
	private String specimenClass;
		
	private String lineage;

	private String anatomicSite;

	private String laterality;
	
	private String status;
	
	private String reqLabel;
	
	private String pathology;
	
	private BigDecimal initialQty;
	
	private BigDecimal availableQty;
	
	private BigDecimal concentration;

	private String quantityUnit;

	private String concentrationUnit;
	
	private Long parentId;
	
	private String parentLabel;
	
	private StorageLocationSummary storageLocation;
	
	private String storageType;

	private UserSummary collector;
	
	private String collectionContainer;

	private Date collectionDate;

	private UserSummary receiver;

	private String storageSite;
	
	private String activityStatus;
	
	private Date createdOn;

	private UserSummary createdBy;
	
	private String code;

	private String availabilityStatus;
	
	private Integer freezeThawCycles;

	private String imageId;

	private List<NameValuePair> externalIds;

	private String labelAutoPrintMode;

	private Boolean closeAfterPooledCreation;

	private boolean printLabel;

	private boolean preBarcodedTube;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public String getEventCode() {
		return eventCode;
	}

	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
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

	public String getVisitStatus() {
		return visitStatus;
	}

	public void setVisitStatus(String visitStatus) {
		this.visitStatus = visitStatus;
	}

	public String getSprNo() {
		return sprNo;
	}

	public void setSprNo(String sprNo) {
		this.sprNo = sprNo;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
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

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
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

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getReqLabel() {
		return reqLabel;
	}

	public void setReqLabel(String reqLabel) {
		this.reqLabel = reqLabel;
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

	public String getQuantityUnit() {
		return quantityUnit;
	}

	public void setQuantityUnit(String quantityUnit) {
		this.quantityUnit = quantityUnit;
	}

	public String getConcentrationUnit() {
		return concentrationUnit;
	}

	public void setConcentrationUnit(String concentrationUnit) {
		this.concentrationUnit = concentrationUnit;
	}

	public Long getParentId() {
		return parentId;
	}

	public void setParentId(Long parentId) {
		this.parentId = parentId;
	}

	public String getParentLabel() {
		return parentLabel;
	}

	public void setParentLabel(String parentLabel) {
		this.parentLabel = parentLabel;
	}

	public StorageLocationSummary getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(StorageLocationSummary storageLocation) {
		this.storageLocation = storageLocation;
	}
	
	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public UserSummary getCollector() {
		return collector;
	}

	public void setCollector(UserSummary collector) {
		this.collector = collector;
	}

	public String getCollectionContainer() {
		return collectionContainer;
	}

	public void setCollectionContainer(String collectionContainer) {
		this.collectionContainer = collectionContainer;
	}

	public Date getCollectionDate() {
		return collectionDate;
	}

	public void setCollectionDate(Date collectionDate) {
		this.collectionDate = collectionDate;
	}

	public UserSummary getReceiver() {
		return receiver;
	}

	public void setReceiver(UserSummary receiver) {
		this.receiver = receiver;
	}

	public String getStorageSite() {
		return storageSite;
	}

	public void setStorageSite(String storageSite) {
		this.storageSite = storageSite;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
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

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getAvailabilityStatus() {
		return availabilityStatus;
	}

	public void setAvailabilityStatus(String availabilityStatus) {
		this.availabilityStatus = availabilityStatus;
	}

	public Integer getFreezeThawCycles() {
		return freezeThawCycles;
	}

	public void setFreezeThawCycles(Integer freezeThawCycles) {
		this.freezeThawCycles = freezeThawCycles;
	}

	public String getImageId() {
		return imageId;
	}

	public void setImageId(String imageId) {
		this.imageId = imageId;
	}

	public List<NameValuePair> getExternalIds() {
		return externalIds;
	}

	public void setExternalIds(List<NameValuePair> externalIds) {
		this.externalIds = externalIds;
	}

	public String getLabelAutoPrintMode() {
		return labelAutoPrintMode;
	}

	public void setLabelAutoPrintMode(String labelAutoPrintMode) {
		this.labelAutoPrintMode = labelAutoPrintMode;
	}

	@JsonIgnore
	public Boolean getCloseAfterPooledCreation() {
		return closeAfterPooledCreation;
	}

	@JsonProperty
	public void setCloseAfterPooledCreation(Boolean closeAfterPooledCreation) {
		this.closeAfterPooledCreation = closeAfterPooledCreation;
	}

	//
	// Do not serialise printLabel from interaction object to response JSON. Therefore @JsonIgnore
	// However, deserialise, if present, from input request JSON to interaction object. Hence @JsonProperty
	//
	@JsonIgnore
	public boolean isPrintLabel() {
		return printLabel;
	}

	@JsonProperty
	public void setPrintLabel(boolean printLabel) {
		this.printLabel = printLabel;
	}

	public boolean isPreBarcodedTube() {
		return preBarcodedTube;
	}

	public void setPreBarcodedTube(boolean preBarcodedTube) {
		this.preBarcodedTube = preBarcodedTube;
	}

	public static SpecimenInfo from(Specimen specimen) {
		return fromTo(specimen, new SpecimenInfo());
	}
	
	public static List<SpecimenInfo> from(List<Specimen> specimens) {
		return specimens.stream().map(SpecimenInfo::from).collect(Collectors.toList());
	}

	public static SpecimenInfo fromTo(Specimen specimen, SpecimenInfo result) {
		result.setId(specimen.getId());
		
		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		if (sr != null) {
			result.setReqId(sr.getId());
			result.setReqLabel(sr.getName());
			result.setSortOrder(sr.getSortOrder());
			result.setPreBarcodedTube(specimen.isPreBarcodedTube());
		}

		CollectionProtocolEvent cpe = specimen.getVisit().getCpEvent();
		if (cpe != null) {
			result.setEventId(cpe.getId());
			result.setEventCode(cpe.getCode());
			result.setEventLabel(cpe.getEventLabel());
		}

		result.setLabel(specimen.getLabel());
		result.setAdditionalLabel(specimen.getAdditionalLabel());
		result.setBarcode(specimen.getBarcode());
		result.setType(PermissibleValue.getValue(specimen.getSpecimenType()));
		result.setSpecimenClass(PermissibleValue.getValue(specimen.getSpecimenClass()));
		result.setLineage(specimen.getLineage());
		result.setAnatomicSite(PermissibleValue.getValue(specimen.getTissueSite()));
		result.setLaterality(PermissibleValue.getValue(specimen.getTissueSide()));
		result.setStatus(specimen.getCollectionStatus());
		result.setPathology(PermissibleValue.getValue(specimen.getPathologicalStatus()));
		result.setInitialQty(specimen.getInitialQuantity());
		result.setAvailableQty(specimen.getAvailableQuantity());
		result.setConcentration(specimen.getConcentration());
		if (specimen.getParentSpecimen() != null) {
			result.setParentId(specimen.getParentSpecimen().getId());
			result.setParentLabel(specimen.getParentSpecimen().getLabel());
		}
	
		StorageLocationSummary location = null;
		StorageContainerPosition position = specimen.getPosition();
		if (position == null) {
			location = new StorageLocationSummary();
			location.setId(-1L);
		} else {
			location = StorageLocationSummary.from(position);
			result.setStorageSite(position.getContainer().getSite().getName());
		}
		result.setStorageLocation(location);

		result.setActivityStatus(specimen.getActivityStatus());
		result.setAvailabilityStatus(specimen.getAvailabilityStatus());
		result.setCreatedOn(specimen.getCreatedOn());
		result.setStorageType(sr != null ? sr.getStorageType() : null);
		result.setVisitId(specimen.getVisit().getId());
		result.setVisitName(specimen.getVisit().getName());
		result.setVisitStatus(specimen.getVisit().getStatus());
		result.setSprNo(specimen.getVisit().getSurgicalPathologyNumber());
		result.setVisitDate(specimen.getVisit().getVisitDate());
		result.setCprId(specimen.getRegistration().getId());
		result.setPpid(specimen.getRegistration().getPpid());
		result.setCpId(specimen.getCollectionProtocol().getId());
		result.setCpShortTitle(specimen.getCollectionProtocol().getShortTitle());
		result.setFreezeThawCycles(specimen.getFreezeThawCycles());
		result.setImageId(specimen.getImageId());
		result.setExternalIds(specimen.getExternalIds().stream()
			.map(externalId -> NameValuePair.create(externalId.getName(), externalId.getValue()))
			.collect(Collectors.toList()));

		if (sr != null && sr.getLabelAutoPrintModeToUse() != null) {
			result.setLabelAutoPrintMode(sr.getLabelAutoPrintModeToUse().name());
		} else {
			CpSpecimenLabelPrintSetting setting = specimen.getCollectionProtocol()
				.getSpmnLabelPrintSetting(specimen.getLineage());
			if (setting != null && setting.getPrintMode() != null) {
				result.setLabelAutoPrintMode(setting.getPrintMode().name());
			}
		}

		SpecimenCollectionReceiveDetail collRecvDetail = specimen.getCollRecvDetails();
		if (collRecvDetail != null) {
			result.setCollector(UserSummary.from(collRecvDetail.getCollector()));
			result.setCollectionContainer(collRecvDetail.getCollContainer());
			result.setCollectionDate(collRecvDetail.getCollTime());
		} else if (specimen.isPrimary() && specimen.getSpecimenRequirement() != null) {
			result.setCollector(UserSummary.from(specimen.getSpecimenRequirement().getCollector()));
			result.setCollectionContainer(PermissibleValue.getValue(specimen.getSpecimenRequirement().getCollectionContainer()));
		}

		if (specimen.getCollRecvDetails() != null && !specimen.getCollRecvDetails().getRecvQuality().equals(Specimen.TO_BE_RECEIVED)) {
			result.setReceiver(UserSummary.from(specimen.getCollRecvDetails().getReceiver()));
		} else if (specimen.isPrimary() && specimen.getSpecimenRequirement() != null) {
			result.setReceiver(UserSummary.from(specimen.getSpecimenRequirement().getReceiver()));
		}

		SpecimenChildrenEvent parentEvent = specimen.getParentEvent();
		if (parentEvent != null) {
			result.setCreatedBy(UserSummary.from(parentEvent.getUser()));
			if (result.getCreatedOn() == null) {
				result.setCreatedOn(parentEvent.getTime());
			}
		}

		return result;
	}	
	
	public static SpecimenInfo fromTo(SpecimenRequirement anticipated, SpecimenInfo result) {
		CollectionProtocolEvent cpe = anticipated.getCollectionProtocolEvent();
		result.setId(null);
		result.setCpId(cpe != null ? cpe.getCollectionProtocol().getId() : null);
		result.setCpShortTitle(cpe != null ? cpe.getCollectionProtocol().getShortTitle() : null);
		result.setEventId(cpe != null ? cpe.getId() : null);
		result.setEventCode(cpe != null ? cpe.getCode() : null);
		result.setEventLabel(cpe != null ? cpe.getEventLabel() : null);
		result.setReqId(anticipated.getId());
		result.setReqLabel(anticipated.getName());
		result.setPreBarcodedTube(anticipated.isPreBarcodedTube());
		result.setSortOrder(anticipated.getSortOrder());
		result.setBarcode(null);
		result.setType(PermissibleValue.getValue(anticipated.getSpecimenType()));
		result.setSpecimenClass(PermissibleValue.getValue(anticipated.getSpecimenClass()));
		result.setLineage(anticipated.getLineage());
		result.setAnatomicSite(PermissibleValue.getValue(anticipated.getAnatomicSite()));
		result.setLaterality(PermissibleValue.getValue(anticipated.getLaterality()));
		result.setPathology(PermissibleValue.getValue(anticipated.getPathologyStatus()));
		result.setInitialQty(anticipated.getInitialQuantity());
		result.setConcentration(anticipated.getConcentration());
		result.setParentId(null);
		result.setCollector(UserSummary.from(anticipated.getCollector()));
		result.setCollectionContainer(PermissibleValue.getValue(anticipated.getCollectionContainer()));
		result.setReceiver(UserSummary.from(anticipated.getReceiver()));
		result.setAvailabilityStatus(Specimen.PENDING);

		StorageLocationSummary location = new StorageLocationSummary();
		result.setStorageLocation(location);
		result.setStorageType(anticipated.getStorageType());
		return result;
	}	
	
	@Override
	public int compareTo(SpecimenInfo other) {
		int cmp = NumUtil.compareTo(sortOrder, other.sortOrder);
		if (cmp != 0) {
			return cmp;
		}

		cmp = NumUtil.compareTo(reqId, other.reqId);
		if (cmp != 0) {
			return cmp;
		}

		return NumUtil.compareTo(id, other.id);
	}
}
