package com.krishagni.catissueplus.core.biospecimen.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol.SpecimenLabelAutoPrintMode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;

@Audited
public class SpecimenRequirement extends BaseEntity implements Comparable<SpecimenRequirement> {
	private String name;
	
	private String code;
	
	private String lineage;
		
	private PermissibleValue specimenClass;

	private PermissibleValue specimenType;

	private PermissibleValue anatomicSite;

	private PermissibleValue laterality;
			
	private PermissibleValue pathologyStatus;
	
	private String storageType;
	
	private BigDecimal initialQuantity;
	
	private BigDecimal concentration;

	private User collector;

	private PermissibleValue collectionProcedure;

	private PermissibleValue collectionContainer;

	private User receiver;

	private CollectionProtocolEvent collectionProtocolEvent;

	private String labelFormat;
	
	private SpecimenLabelAutoPrintMode labelAutoPrintMode;
	
	private Integer labelPrintCopies;
	
	private Integer sortOrder;

	private Boolean preBarcodedTube;

	private Map<String, Object> defaultCustomFieldValues;

	private String activityStatus;
			
	private SpecimenRequirement parentSpecimenRequirement;
	
	private Set<SpecimenRequirement> childSpecimenRequirements = new LinkedHashSet<>();

	private Set<SpecimenRequirementService> services = new LinkedHashSet<>();

	private Set<Specimen> specimens = new HashSet<>();

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

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getSpecimenClass() {
		return specimenClass;
	}

	public void setSpecimenClass(PermissibleValue specimenClass) {
		this.specimenClass = specimenClass;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getSpecimenType() {
		return specimenType;
	}

	public void setSpecimenType(PermissibleValue specimenType) {
		this.specimenType = specimenType;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getAnatomicSite() {
		return anatomicSite;
	}

	public void setAnatomicSite(PermissibleValue anatomicSite) {
		this.anatomicSite = anatomicSite;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getLaterality() {
		return laterality;
	}

	public void setLaterality(PermissibleValue laterality) {
		this.laterality = laterality;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getPathologyStatus() {
		return pathologyStatus;
	}

	public void setPathologyStatus(PermissibleValue pathologyStatus) {
		this.pathologyStatus = pathologyStatus;
	}

	public String getStorageType() {
		return storageType;
	}

	public void setStorageType(String storageType) {
		this.storageType = storageType;
	}

	public BigDecimal getInitialQuantity() {
		return initialQuantity;
	}

	public void setInitialQuantity(BigDecimal initialQuantity) {
		this.initialQuantity = initialQuantity;
	}

	public BigDecimal getConcentration() {
		return concentration;
	}

	public void setConcentration(BigDecimal concentration) {
		this.concentration = concentration;
	}

	public User getCollector() {
		return collector;
	}

	public void setCollector(User collector) {
		this.collector = collector;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getCollectionProcedure() {
		return collectionProcedure;
	}

	public void setCollectionProcedure(PermissibleValue collectionProcedure) {
		this.collectionProcedure = collectionProcedure;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getCollectionContainer() {
		return collectionContainer;
	}

	public void setCollectionContainer(PermissibleValue collectionContainer) {
		this.collectionContainer = collectionContainer;
	}

	public User getReceiver() {
		return receiver;
	}

	public void setReceiver(User receiver) {
		this.receiver = receiver;
	}

	@NotAudited
	public CollectionProtocolEvent getCollectionProtocolEvent() {
		return collectionProtocolEvent;
	}

	public void setCollectionProtocolEvent(CollectionProtocolEvent collectionProtocolEvent) {
		this.collectionProtocolEvent = collectionProtocolEvent;
	}
	
	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocolEvent != null ? collectionProtocolEvent.getCollectionProtocol() : null;
	}

	public String getLabelFormat() {
		return labelFormat;
	}

	public void setLabelFormat(String labelFormat) {
		this.labelFormat = labelFormat;
	}
	
	public SpecimenLabelAutoPrintMode getLabelAutoPrintMode() {
		return labelAutoPrintMode;
	}
	
	public SpecimenLabelAutoPrintMode getLabelAutoPrintModeToUse() {
		if (labelAutoPrintMode != null) {
			return labelAutoPrintMode;
		}

		CpSpecimenLabelPrintSetting setting = getCollectionProtocol().getSpmnLabelPrintSetting(getLineage());
		return setting != null ? setting.getPrintMode() : null;
	}

	public void setLabelAutoPrintMode(SpecimenLabelAutoPrintMode labelAutoPrintMode) {
		this.labelAutoPrintMode = labelAutoPrintMode;
	}

	public Integer getLabelPrintCopies() {
		return labelPrintCopies;
	}

	public void setLabelPrintCopies(Integer labelPrintCopies) {
		this.labelPrintCopies = labelPrintCopies;
	}
	
	public Integer getLabelPrintCopiesToUse() {
		if (labelPrintCopies != null) {
			return labelPrintCopies;
		}
		
		CpSpecimenLabelPrintSetting setting = getCollectionProtocol().getSpmnLabelPrintSetting(getLineage());
		return setting != null ? setting.getCopies() : null;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public Boolean isPreBarcodedTube() {
		return Boolean.TRUE.equals(preBarcodedTube);
	}

	public void setPreBarcodedTube(Boolean preBarcodedTube) {
		this.preBarcodedTube = preBarcodedTube;
	}

	public Map<String, Object> getDefaultCustomFieldValues() {
		return defaultCustomFieldValues;
	}
	public void setDefaultCustomFieldValues(Map<String, Object> defaultCustomFieldValues) {
		this.defaultCustomFieldValues = defaultCustomFieldValues;
	}

	public String getDefCustomFieldValuesJson() {
		if (defaultCustomFieldValues == null) {
			return null;
		}

		return Utility.mapToJson(defaultCustomFieldValues);
	}

	public void setDefCustomFieldValuesJson(String json) {
		if (StringUtils.isBlank(json)) {
			this.defaultCustomFieldValues = null;
		} else {
			this.defaultCustomFieldValues = Utility.jsonToMap(json);
		}
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

	@NotAudited
	public SpecimenRequirement getParentSpecimenRequirement() {
		return parentSpecimenRequirement;
	}

	public void setParentSpecimenRequirement(SpecimenRequirement parentSpecimenRequirement) {
		this.parentSpecimenRequirement = parentSpecimenRequirement;
	}

	@NotAudited
	public Set<SpecimenRequirement> getChildSpecimenRequirements() {
		return childSpecimenRequirements;
	}

	public void setChildSpecimenRequirements(Set<SpecimenRequirement> childSpecimenRequirements) {
		this.childSpecimenRequirements = childSpecimenRequirements;
	}

	@NotAudited
	public Set<SpecimenRequirementService> getServices() {
		return services;
	}

	public void setServices(Set<SpecimenRequirementService> services) {
		this.services = services;
	}

	public void addService(SpecimenRequirementService service) {
		addService(service.getService(), service.getUnits());
	}

	public void addService(Service service, int units) {
		SpecimenRequirementService srSvc = new SpecimenRequirementService();
		srSvc.setRequirement(this);
		srSvc.setService(service);
		srSvc.setUnits(units);
		getServices().add(srSvc);
	}

	public List<SpecimenRequirement> getOrderedChildRequirements() {
		List<SpecimenRequirement> childReqs = new ArrayList<>(getChildSpecimenRequirements());
		Collections.sort(childReqs);
		return childReqs;
	}

	@NotAudited
	public Set<Specimen> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(Set<Specimen> specimens) {
		this.specimens = specimens;
	}
	
	public boolean isPrimary() {
		return getLineage().equals(Specimen.NEW);
	}
	
	public boolean isAliquot() {
		return getLineage().equals(Specimen.ALIQUOT);
	}
	
	public boolean isDerivative() {
		return getLineage().equals(Specimen.DERIVED);
	}
	
	public void update(SpecimenRequirement sr) {
		if (getActivityStatus().equals(sr.getActivityStatus())) {
			ensureReqIsNotClosed();
		}

		if (isPrimary()) {
			updateRequirementAttrs(sr);
		}

		if (StringUtils.isNotBlank(sr.getCode()) && !sr.getCode().equals(getCode())) {
			if (getCollectionProtocolEvent().getSrByCode(sr.getCode()) != null) {
				throw OpenSpecimenException.userError(SrErrorCode.DUP_CODE, sr.getCode());
			}
		}
		
		if (!StringUtils.equals(getName(), sr.getName())) {
			updateName(sr.getName());
		}

		setCode(sr.getCode());
		setSortOrder(sr.getSortOrder());
		setInitialQuantity(sr.getInitialQuantity());
		setStorageType(sr.getStorageType());
		setLabelFormat(sr.getLabelFormat());
		setLabelAutoPrintMode(sr.getLabelAutoPrintMode());
		setLabelPrintCopies(sr.getLabelPrintCopies());
		setDefaultCustomFieldValues(sr.getDefaultCustomFieldValues());
		setPreBarcodedTube(sr.isPreBarcodedTube());

		Map<Service, SpecimenRequirementService> existingSvcs = getServices().stream()
			.collect(Collectors.toMap(SpecimenRequirementService::getService, svc -> svc));

		for (SpecimenRequirementService svc : sr.getServices()) {
			SpecimenRequirementService existing = existingSvcs.remove(svc.getService());
			if (existing != null) {
				existing.update(svc);
			} else {
				addService(svc);
			}
		}

		existingSvcs.values().forEach(svc -> getServices().remove(svc));

		if (!isAliquot()) {
			update(sr.getAnatomicSite(), sr.getLaterality(), sr.getConcentration(),
				sr.getSpecimenClass(), sr.getSpecimenType(), sr.getPathologyStatus());
		}

		if (NumUtil.lessThanZero(getQtyAfterAliquotsUse())) {
			throw OpenSpecimenException.userError(SrErrorCode.INSUFFICIENT_QTY);
		}
		
		if (isAliquot() && NumUtil.lessThanZero(getParentSpecimenRequirement().getQtyAfterAliquotsUse())) {
			throw OpenSpecimenException.userError(SrErrorCode.INSUFFICIENT_QTY);
		}

		boolean wasClosed = isClosed();
		setActivityStatus(sr.getActivityStatus());
		if (!wasClosed && isClosed()) {
			close();
		}
	}
				
	public SpecimenRequirement copy() {
		SpecimenRequirement copy = new SpecimenRequirement();
		BeanUtils.copyProperties(this, copy, EXCLUDE_COPY_PROPS);
		getServices().forEach(copy::addService);
		return copy;
	}

	public SpecimenRequirement deepCopy(CollectionProtocolEvent cpe) {
		ensureReqIsNotClosed();

		if (cpe == null) {
			cpe = getCollectionProtocolEvent();
		}
		
		if (isAliquot()) {
			if (NumUtil.greaterThan(getInitialQuantity(), getParentSpecimenRequirement().getQtyAfterAliquotsUse())) {
				throw OpenSpecimenException.userError(SrErrorCode.INSUFFICIENT_QTY);
			}
		}
		
		return deepCopy(cpe, getParentSpecimenRequirement());
	}

	public void close() {
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
		getChildSpecimenRequirements().forEach(SpecimenRequirement::close);
	}
		
	public void addChildRequirement(SpecimenRequirement childReq) {
		ensureReqIsNotClosed();
		childReq.setParentSpecimenRequirement(this);
		getChildSpecimenRequirements().add(childReq);
	}
	
	public void addChildRequirements(Collection<SpecimenRequirement> children) {
		ensureReqIsNotClosed();
		for (SpecimenRequirement childReq : children) {
			addChildRequirement(childReq);
		}
	}

	public BigDecimal getQtyAfterAliquotsUse() {
		BigDecimal available = getInitialQuantity();
		if (available == null) {
			return null;
		}

		for (SpecimenRequirement childReq : getChildSpecimenRequirements()) {
			if (childReq.isAliquot() && childReq.getInitialQuantity() != null) {
				available =  available.subtract(childReq.getInitialQuantity());
			}
		}
		
		return available;
	}
	
	public Specimen getSpecimen() {
		Specimen specimen = new Specimen();
		specimen.setCollectionProtocol(getCollectionProtocol());
		specimen.setLineage(getLineage());
		specimen.setSpecimenClass(getSpecimenClass());
		specimen.setSpecimenType(getSpecimenType());
		specimen.setTissueSite(getAnatomicSite());
		specimen.setTissueSide(getLaterality());
		specimen.setPathologicalStatus(getPathologyStatus());
		specimen.setInitialQuantity(getInitialQuantity());
		specimen.setAvailableQuantity(getInitialQuantity());
		specimen.setConcentration(getConcentration());
		specimen.setSpecimenRequirement(this);
		specimen.setExtension(DeObject.createExtension(getDefaultCustomFieldValues(), specimen));
		return specimen;
	}
	
	public String getLabelTmpl() {
		if (StringUtils.isNotBlank(labelFormat)) {
			return labelFormat;
		}
		
		CollectionProtocol cp = getCollectionProtocolEvent().getCollectionProtocol();
		if (isAliquot()) {
			return cp.getAliquotLabelFormatToUse();
		} else if (isDerivative()) {
			return cp.getDerivativeLabelFormat();
		} else {
			return cp.getSpecimenLabelFormat();
		}
	}
	
	public void delete() {
		for (SpecimenRequirement childReq : getChildSpecimenRequirements()) {
			childReq.delete();
		}

		setCode(Utility.getDisabledValue(getCode(), 32));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public String getDescription() {
		String desc = getLineage() + ", " + getSpecimenType().getValue();
		if (StringUtils.isNotBlank(getCode())) {
			desc += ", " + getCode();
		} else if (StringUtils.isNotBlank(getName())) {
			desc += ", " + getName();
		} else {
			desc += ", " + getId();
		}

		return desc;
	}
		
	@Override
	public int compareTo(SpecimenRequirement other) {
		if (getSortOrder() != null && other.getSortOrder() != null) {
			return getSortOrder().compareTo(other.getSortOrder());
		} else if (getSortOrder() != null) {
			return -1;
		} else if (other.getSortOrder() != null) {
			return 1;
		} else if (getId() != null && other.getId() != null) {
			return getId().compareTo(other.getId());
		} else if (getId() != null) {
			return -1;
		} else if(other.getId() != null) {
			return 1;
		} else {
			return 0;
		}
	}

	private SpecimenRequirement deepCopy(CollectionProtocolEvent cpe, SpecimenRequirement parent) {
		SpecimenRequirement result = copy();
		result.setCollectionProtocolEvent(cpe);
		result.setParentSpecimenRequirement(parent);
		if (isSafeToCopyCode(cpe)) {
			result.setCode(getCode());
		}

		Set<SpecimenRequirement> childSrs = new LinkedHashSet<>();
		int order = 1;
		for (SpecimenRequirement childSr : getOrderedChildRequirements()) {
			if (childSr.isClosed()) {
				continue;
			}

			SpecimenRequirement copiedSr = childSr.deepCopy(cpe, result);
			copiedSr.setSortOrder(order++);
			childSrs.add(copiedSr);
		}
		
		result.setChildSpecimenRequirements(childSrs);

		if (!Specimen.NEW.equals(getLineage())) {
			return result;
		}

		return result;
	}
	
	private boolean isSafeToCopyCode(CollectionProtocolEvent cpe) {
		if (StringUtils.isBlank(getCode())) {
			return false;
		}

		if (cpe.equals(getCollectionProtocolEvent())) {
			return false;
		}

		return cpe.getSrByCode(getCode()) == null;
	}

	private void updateName(String name) {
		setName(name);

		getChildSpecimenRequirements().stream()
			.filter(SpecimenRequirement::isAliquot)
			.forEach(child -> child.updateName(name));
	}

	private void updateRequirementAttrs(SpecimenRequirement sr) {
		setCollector(sr.getCollector());
		setCollectionContainer(sr.getCollectionContainer());
		setCollectionProcedure(sr.getCollectionProcedure());
		setReceiver(sr.getReceiver());
		
		for (SpecimenRequirement childSr : getChildSpecimenRequirements()) {
			childSr.updateRequirementAttrs(sr);
		}
	}
	
	private void update(PermissibleValue anatomicSite, PermissibleValue laterality, BigDecimal concentration,
		PermissibleValue specimenClass, PermissibleValue specimenType, PermissibleValue pathologyStatus) {

		setAnatomicSite(anatomicSite);
		setLaterality(laterality);
		setConcentration(concentration);
		setSpecimenClass(specimenClass);
		setSpecimenType(specimenType);
		setPathologyStatus(pathologyStatus);

		for (SpecimenRequirement childSr : getChildSpecimenRequirements()) {
			if (childSr.isAliquot()) {
				childSr.update(anatomicSite, laterality, concentration, specimenClass, specimenType, pathologyStatus);
			}
		}
	}

	private void ensureReqIsNotClosed() {
		if (!isClosed()) {
			return;
		}

		String key = getCode();
		if (StringUtils.isBlank(key)) {
			key = getName();
		}

		if (StringUtils.isBlank(key)) {
			key = getId().toString();
		}

		throw OpenSpecimenException.userError(SrErrorCode.CLOSED, key);
	}

	private static final String[] EXCLUDE_COPY_PROPS = {
		"id",
		"sortOrder",
		"code",
		"parentSpecimenRequirement",
		"childSpecimenRequirements",
		"specimenPoolReqs",
		"specimens",
		"services"
	};
}
