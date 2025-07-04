package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.events.SrServiceDetail;

public class AliquotSpecimensRequirement {
	private String cpShortTitle;
	
	private String eventLabel;
	
	private Long parentSrId;
	
	private String parentSrCode;
	
	private Integer noOfAliquots;
	
	private BigDecimal qtyPerAliquot;
	
	private String code;
	
	private String storageType;
	
	private String labelFmt;
	
	private String labelAutoPrintMode;
	
	private Integer labelPrintCopies;

	private Map<String, Object> defaultCustomFieldValues;

	private boolean isPreBarcodedTube;

	private List<SrServiceDetail> services;
	
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

	public Integer getNoOfAliquots() {
		return noOfAliquots;
	}

	public void setNoOfAliquots(Integer noOfAliquots) {
		this.noOfAliquots = noOfAliquots;
	}
	
	public BigDecimal getQtyPerAliquot() {
		return qtyPerAliquot;
	}

	public void setQtyPerAliquot(BigDecimal qtyPerAliquot) {
		this.qtyPerAliquot = qtyPerAliquot;
	}
	
	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
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

	public Map<String, Object> getDefaultCustomFieldValues() {
		return defaultCustomFieldValues;
	}

	public void setDefaultCustomFieldValues(Map<String, Object> defaultCustomFieldValues) {
		this.defaultCustomFieldValues = defaultCustomFieldValues;
	}

	public boolean isPreBarcodedTube() {
		return isPreBarcodedTube;
	}

	public void setPreBarcodedTube(boolean preBarcodedTube) {
		isPreBarcodedTube = preBarcodedTube;
	}

	public List<SrServiceDetail> getServices() {
		return services;
	}

	public void setServices(List<SrServiceDetail> services) {
		this.services = services;
	}
}
