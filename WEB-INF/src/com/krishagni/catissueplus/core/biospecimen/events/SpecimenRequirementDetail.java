package com.krishagni.catissueplus.core.biospecimen.events;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@JsonFilter("withoutId")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SpecimenRequirementDetail implements Comparable<SpecimenRequirementDetail> {
	private Long id;
	
	private String name;
	
	private String code;
	
	private String lineage;
	
	private String specimenClass;
	
	private String type;
	
	private String anatomicSite;
	
	private String laterality;
	
	private String pathology;
	
	private String storageType;
	
	private BigDecimal initialQty;
	
	private BigDecimal concentration;

	private String quantityUnit;

	private String concentrationUnit;
	
	private UserSummary collector;
	
	private String collectionProcedure;
	
	private String collectionContainer;
	
	private UserSummary receiver;
	
	private String labelFmt;
	
	private String labelAutoPrintMode;

	private Integer labelPrintCopies;
	
	private Integer sortOrder;

	private Map<String, Object> defaultCustomFieldValues;

	private boolean preBarcodedTube;
	
	private Long eventId;

	private Long cpId;
	
	private List<SpecimenRequirementDetail> children;

	private String cpShortTitle;
	
	private String eventLabel;
	
	private String parentSrCode;

	private String activityStatus;

	private ExtensionDetail extensionDetail;

	private List<SrServiceDetail> services;

	private String uid;

	private String parentUid;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
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

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public BigDecimal getInitialQty() {
		return initialQty;
	}

	public void setInitialQty(BigDecimal initialQty) {
		this.initialQty = initialQty;
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

	public UserSummary getCollector() {
		return collector;
	}

	public void setCollector(UserSummary collector) {
		this.collector = collector;
	}

	public String getCollectionProcedure() {
		return collectionProcedure;
	}

	public void setCollectionProcedure(String collectionProcedure) {
		this.collectionProcedure = collectionProcedure;
	}

	public String getCollectionContainer() {
		return collectionContainer;
	}

	public void setCollectionContainer(String collectionContainer) {
		this.collectionContainer = collectionContainer;
	}

	public UserSummary getReceiver() {
		return receiver;
	}

	public void setReceiver(UserSummary receiver) {
		this.receiver = receiver;
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
	@JsonIgnore
	public String getDefCustomFieldValuesJson() {
		if (defaultCustomFieldValues == null) {
			return null;
		}

		return Utility.mapToJson(defaultCustomFieldValues);
	}

	@JsonProperty
	public void setDefCustomFieldValuesJson(String json) {
		if (StringUtils.isBlank(json)) {
			this.defaultCustomFieldValues = null;
		} else {
			this.defaultCustomFieldValues = Utility.jsonToMap(json);
		}
	}

	public boolean isPreBarcodedTube() {
		return preBarcodedTube;
	}

	public void setPreBarcodedTube(boolean preBarcodedTube) {
		this.preBarcodedTube = preBarcodedTube;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public List<SpecimenRequirementDetail> getChildren() {
		return children;
	}

	public void setChildren(List<SpecimenRequirementDetail> children) {
		this.children = children;
	}

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

	public String getParentSrCode() {
		return parentSrCode;
	}

	public void setParentSrCode(String parentSrCode) {
		this.parentSrCode = parentSrCode;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public List<SrServiceDetail> getServices() {
		return services;
	}

	public void setServices(List<SrServiceDetail> services) {
		this.services = services;
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

	@Override
	public int compareTo(SpecimenRequirementDetail other) {
		int cmp = NumUtil.compareTo(sortOrder, other.sortOrder);
		if (cmp != 0) {
			return cmp;
		}

		return NumUtil.compareTo(id, other.id);
	}
	
	public AliquotSpecimensRequirement toAliquotRequirement(Long parentSrId, int noOfAliquots) {
		AliquotSpecimensRequirement req = new AliquotSpecimensRequirement();
		req.setNoOfAliquots(noOfAliquots);
		req.setLabelFmt(getLabelFmt());
		req.setLabelAutoPrintMode(getLabelAutoPrintMode());
		req.setLabelPrintCopies(getLabelPrintCopies());
		req.setParentSrId(parentSrId);
		req.setQtyPerAliquot(getInitialQty());
		req.setStorageType(getStorageType());
		req.setDefaultCustomFieldValues(getDefaultCustomFieldValues());
		req.setPreBarcodedTube(isPreBarcodedTube());
		return req;		
	}
	
	public DerivedSpecimenRequirement toDerivedRequirement(Long parentSrId) {
		DerivedSpecimenRequirement req = new DerivedSpecimenRequirement();
		req.setConcentration(getConcentration());
		req.setLabelFmt(getLabelFmt());
		req.setLabelAutoPrintMode(getLabelAutoPrintMode());
		req.setLabelPrintCopies(getLabelPrintCopies());
		req.setName(getName());
		req.setParentSrId(parentSrId);
		req.setQuantity(getInitialQty());
		req.setSpecimenClass(getSpecimenClass());
		req.setStorageType(getStorageType());
		req.setType(getType());
		req.setCode(getCode());
		req.setPathology(getPathology());
		req.setDefaultCustomFieldValues(getDefaultCustomFieldValues());
		req.setPreBarcodedTube(isPreBarcodedTube());
		return req;
	}
	
	public static SpecimenRequirementDetail from(SpecimenRequirement sr) {
		return from(sr, true);
	}
	
	public static SpecimenRequirementDetail from(SpecimenRequirement sr, boolean incChildren) {
		SpecimenRequirementDetail detail = new SpecimenRequirementDetail();
		detail.setId(sr.getId());
		detail.setName(sr.getName());
		detail.setCode(sr.getCode());
		detail.setLineage(sr.getLineage());
		detail.setSpecimenClass(PermissibleValue.getValue(sr.getSpecimenClass()));
		detail.setType(PermissibleValue.getValue(sr.getSpecimenType()));
		detail.setAnatomicSite(PermissibleValue.getValue(sr.getAnatomicSite()));
		detail.setLaterality(PermissibleValue.getValue(sr.getLaterality()));
		detail.setPathology(PermissibleValue.getValue(sr.getPathologyStatus()));
		detail.setStorageType(sr.getStorageType());
		detail.setInitialQty(sr.getInitialQuantity());
		detail.setConcentration(sr.getConcentration());
		detail.setCollector(sr.getCollector() == null ? null : UserSummary.from(sr.getCollector()));
		detail.setCollectionProcedure(PermissibleValue.getValue(sr.getCollectionProcedure()));
		detail.setCollectionContainer(PermissibleValue.getValue(sr.getCollectionContainer()));
		detail.setReceiver(sr.getReceiver() == null ? null : UserSummary.from(sr.getReceiver()));
		detail.setLabelFmt(sr.getLabelFormat());
		detail.setLabelAutoPrintMode(sr.getLabelAutoPrintMode() == null ? null : sr.getLabelAutoPrintMode().name());
		detail.setLabelPrintCopies(sr.getLabelPrintCopies());
		detail.setSortOrder(sr.getSortOrder());
		detail.setDefaultCustomFieldValues(sr.getDefaultCustomFieldValues());
		detail.setPreBarcodedTube(sr.isPreBarcodedTube());
		detail.setEventId(sr.getCollectionProtocolEvent().getId());
		detail.setCpId(sr.getCollectionProtocol().getId());
		detail.setCpShortTitle(sr.getCollectionProtocol().getShortTitle());
		detail.setServices(SrServiceDetail.from(sr.getServices()));
		detail.setActivityStatus(sr.getActivityStatus());
		if (sr.getId() != null) {
			detail.setUid("sr_" + sr.getId());
			if (sr.getParentSpecimenRequirement() != null) {
				detail.setParentUid("sr_" + sr.getParentSpecimenRequirement().getId());
			}
		}

		if (incChildren) {
			detail.setChildren(from(sr.getChildSpecimenRequirements()));
		}
		
		return detail;
	}
	
	public static List<SpecimenRequirementDetail> from(Collection<SpecimenRequirement> srs) {
		return from(srs, true);
	}
	
	public static List<SpecimenRequirementDetail> from(Collection<SpecimenRequirement> srs, boolean incChildren) {
		return Utility.nullSafeStream(srs).map(sr -> from(sr, incChildren)).sorted().collect(Collectors.toList());
	}
}
