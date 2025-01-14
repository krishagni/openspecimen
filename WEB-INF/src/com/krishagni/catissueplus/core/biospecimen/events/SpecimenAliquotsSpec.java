package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

public class SpecimenAliquotsSpec {
	private Long id; // workflow support

	private Long parentId;
	
	private String parentLabel;

	private String derivedReqCode;

	private String cpShortTitle;
	
	private Integer noOfAliquots;

	private String labels;

	private String barcodes;
	
	private BigDecimal qtyPerAliquot;

	private String specimenClass;

	private String type;

	private BigDecimal concentration;
	
	private Date createdOn;

	private UserSummary createdBy;

	private String parentContainerName;

	private String containerType;

	private String containerName;

	private String positionX;

	private String positionY;

	private Integer position;

	private List<StorageLocationSummary> locations;

	private Integer freezeThawCycles;

	private Integer incrParentFreezeThaw;

	private Boolean closeParent;

	private Boolean createDerived;

	private Boolean keepDerivedOpen;

	private Boolean useExistingDerived;

	private Boolean printLabel;

	private String comments;

	private ExtensionDetail extensionDetail;

	private boolean linkToReqs;

	private String status;

	private List<Long> reqIds;

	private List<Long> ids;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public String getDerivedReqCode() {
		return derivedReqCode;
	}

	public void setDerivedReqCode(String derivedReqCode) {
		this.derivedReqCode = derivedReqCode;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Integer getNoOfAliquots() {
		return noOfAliquots;
	}

	public void setNoOfAliquots(Integer noOfAliquots) {
		this.noOfAliquots = noOfAliquots;
	}

	public String getLabels() {
		return labels;
	}

	public void setLabels(String labels) {
		this.labels = labels;
	}

	public String getBarcodes() {
		return barcodes;
	}

	public void setBarcodes(String barcodes) {
		this.barcodes = barcodes;
	}

	public BigDecimal getQtyPerAliquot() {
		return qtyPerAliquot;
	}

	public void setQtyPerAliquot(BigDecimal qtyPerAliquot) {
		this.qtyPerAliquot = qtyPerAliquot;
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

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		this.concentration = concentration;
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

	public String getParentContainerName() {
		return parentContainerName;
	}

	public void setParentContainerName(String parentContainerName) {
		this.parentContainerName = parentContainerName;
	}

	public String getContainerType() {
		return containerType;
	}

	public void setContainerType(String containerType) {
		this.containerType = containerType;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
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

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public List<StorageLocationSummary> getLocations() {
		return locations;
	}

	public void setLocations(List<StorageLocationSummary> locations) {
		this.locations = locations;
	}

	public Integer getFreezeThawCycles() {
		return freezeThawCycles;
	}

	public void setFreezeThawCycles(Integer freezeThawCycles) {
		this.freezeThawCycles = freezeThawCycles;
	}

	public Integer getIncrParentFreezeThaw() {
		return incrParentFreezeThaw;
	}

	public void setIncrParentFreezeThaw(Integer incrParentFreezeThaw) {
		this.incrParentFreezeThaw = incrParentFreezeThaw;
	}

	public Boolean getCloseParent() {
		return closeParent;
	}

	public void setCloseParent(Boolean closeParent) {
		this.closeParent = closeParent;
	}

	public boolean closeParent() {
		return closeParent != null && closeParent;
	}

	public Boolean getCreateDerived() {
		return createDerived;
	}

	public void setCreateDerived(Boolean createDerived) {
		this.createDerived = createDerived;
	}

	public boolean createDerived() { return createDerived != null && createDerived; }

	public Boolean getKeepDerivedOpen() {
		return keepDerivedOpen;
	}

	public void setKeepDerivedOpen(Boolean keepDerivedOpen) {
		this.keepDerivedOpen = keepDerivedOpen;
	}

	public boolean keepDerivedOpen() {
		if (Specimen.PENDING.equals(getStatus())) {
			return true;
		}

		return keepDerivedOpen != null && keepDerivedOpen;
	}

	public Boolean getUseExistingDerived() {
		return useExistingDerived;
	}

	public void setUseExistingDerived(Boolean useExistingDerived) {
		this.useExistingDerived = useExistingDerived;
	}

	public boolean useExistingDerived() {
		return useExistingDerived != null && useExistingDerived;
	}

	public Boolean getPrintLabel() {
		return printLabel;
	}

	public void setPrintLabel(Boolean printLabel) {
		this.printLabel = printLabel;
	}

	public boolean printLabel() { return printLabel != null && printLabel; }

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public boolean isLinkToReqs() {
		return linkToReqs;
	}

	public void setLinkToReqs(boolean linkToReqs) {
		this.linkToReqs = linkToReqs;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public List<Long> getReqIds() {
		return reqIds;
	}

	public void setReqIds(List<Long> reqIds) {
		this.reqIds = reqIds;
	}

	public List<Long> getIds() {
		return ids;
	}

	public void setIds(List<Long> ids) {
		this.ids = ids;
	}
}