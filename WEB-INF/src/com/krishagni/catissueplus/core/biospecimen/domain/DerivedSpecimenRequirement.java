package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.Map;

public class DerivedSpecimenRequirement {
	private String cpShortTitle;
	
	private String eventLabel;
	
	private Long parentSrId;
	
	private String parentSrCode;
	
	private String specimenClass;
	
	private String type;

	private String anatomicSite;

	private String laterality;
	
	private String pathology;
	
	private BigDecimal quantity;
	
	private BigDecimal concentration;
	
	private String labelFmt;
	
	private String labelAutoPrintMode;
	
	private Integer labelPrintCopies;
	
	private String name;
	
	private String storageType;
	
	private String code;

	private Integer sortOrder;

	private Map<String, Object> defaultCustomFieldValues;
	
	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}
	
	public Long getParentSrId() {
		return parentSrId;
	}

	public void setParentSrId(Long parentSrId) {
		this.parentSrId = parentSrId;
	}
	
	public String getParentSrCode() {
		return parentSrCode;
	}

	public void setParentSrCode(String parentSrCode) {
		this.parentSrCode = parentSrCode;
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

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		this.concentration = concentration;
	}

	public String getLabelFmt() {
		return labelFmt;
	}

	public void setLabelFmt(String labelFmt) {
		this.labelFmt = labelFmt;
	}
	
	public String getLabelAutoPrintMode() {
		return labelAutoPrintMode;
	}

	public void setLabelAutoPrintMode(String labelAutoPrintMode) {
		this.labelAutoPrintMode = labelAutoPrintMode;
	}
	
	public Integer getLabelPrintCopies() {
		return labelPrintCopies;
	}

	public void setLabelPrintCopies(Integer labelPrintCopies) {
		this.labelPrintCopies = labelPrintCopies;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Map<String, Object> getDefaultCustomFieldValues() {
		return defaultCustomFieldValues;
	}

	public void setDefaultCustomFieldValues(Map<String, Object> defaultCustomFieldValues) {
		this.defaultCustomFieldValues = defaultCustomFieldValues;
	}
}
