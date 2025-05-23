package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol.VisitNamePrintMode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class CollectionProtocolEvent extends BaseEntity implements Comparable<CollectionProtocolEvent> {

	private static final String ENTITY_NAME = "collection_protocol_event";
	
	private String eventLabel;

	private Integer eventPoint;

	private IntervalUnit eventPointUnit;

	private CollectionProtocol collectionProtocol;
	
	private String code;
	
	private Site defaultSite;

	private PermissibleValue clinicalDiagnosis;
	
	private PermissibleValue clinicalStatus;

	private VisitNamePrintMode visitNamePrintMode;

	private Integer visitNamePrintCopies;
	
	private String activityStatus;

	private Set<SpecimenRequirement> specimenRequirements = new LinkedHashSet<>();

	private Set<Visit> specimenCollectionGroups = new HashSet<>();

	private transient int offset = 0;

	private transient IntervalUnit offsetUnit = IntervalUnit.DAYS;

	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public Integer getEventPoint() {
		return eventPoint;
	}

	public void setEventPoint(Integer eventPoint) {
		this.eventPoint = eventPoint;
	}

	public IntervalUnit getEventPointUnit() {
		return eventPointUnit;
	}

	public void setEventPointUnit(IntervalUnit eventPointUnit) {
		this.eventPointUnit = eventPointUnit;
	}

	@NotAudited
	public CollectionProtocol getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocol collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public Site getDefaultSite() {
		return defaultSite;
	}

	public void setDefaultSite(Site defaultSite) {
		this.defaultSite = defaultSite;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getClinicalDiagnosis() {
		return clinicalDiagnosis;
	}

	public void setClinicalDiagnosis(PermissibleValue clinicalDiagnosis) {
		this.clinicalDiagnosis = clinicalDiagnosis;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getClinicalStatus() {
		return clinicalStatus;
	}

	public void setClinicalStatus(PermissibleValue clinicalStatus) {
		this.clinicalStatus = clinicalStatus;
	}

	public VisitNamePrintMode getVisitNamePrintMode() {
		return visitNamePrintMode;
	}

	public VisitNamePrintMode getVisitNamePrintModeToUse() {
		return visitNamePrintMode != null ? visitNamePrintMode : getCollectionProtocol().getVisitNamePrintMode();
	}

	public void setVisitNamePrintMode(VisitNamePrintMode visitNamePrintMode) {
		this.visitNamePrintMode = visitNamePrintMode;
	}

	public Integer getVisitNamePrintCopies() {
		return visitNamePrintCopies;
	}

	public Integer getVisitNamePrintCopiesToUse() {
		return visitNamePrintCopies != null ? visitNamePrintCopies : getCollectionProtocol().getVisitNamePrintCopies();
	}

	public void setVisitNamePrintCopies(Integer visitNamePrintCopies) {
		this.visitNamePrintCopies = visitNamePrintCopies;
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
	public Set<SpecimenRequirement> getSpecimenRequirements() {
		return specimenRequirements;
	}
	
	public void setSpecimenRequirements(Set<SpecimenRequirement> specimenRequirements) {
		this.specimenRequirements = specimenRequirements;
	}
	
	public Set<SpecimenRequirement> getTopLevelAnticipatedSpecimens() {
		return Utility.nullSafeStream(getSpecimenRequirements())
			.filter(SpecimenRequirement::isPrimary)
			.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	public List<SpecimenRequirement> getOrderedTopLevelAnticipatedSpecimens() {
		return getTopLevelAnticipatedSpecimens().stream().sorted().collect(Collectors.toList());
	}

	@NotAudited
	public Set<Visit> getSpecimenCollectionGroups() {
		return specimenCollectionGroups;
	}

	public void setSpecimenCollectionGroups(Set<Visit> specimenCollectionGroups) {
		this.specimenCollectionGroups = specimenCollectionGroups;
	}

	public String getDescription() {
		String result = "";
		if (eventPoint != null) {
			result += ((eventPoint < 0) ? "-" : "") + "T" + Math.abs(eventPoint);
			result += eventPointUnit.name().charAt(0) + ": ";
		}

		result += eventLabel;
		if (code != null) {
			result += " (" + code + ")";
		}

		return result;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public IntervalUnit getOffsetUnit() {
		return offsetUnit;
	}

	public void setOffsetUnit(IntervalUnit offsetUnit) {
		this.offsetUnit = offsetUnit;
	}

	// updates all but specimen requirements
	public void update(CollectionProtocolEvent other) {
		if (isClosed() && other.isClosed()) {
			throw OpenSpecimenException.userError(CpeErrorCode.CLOSED, getEventLabel());
		}

		setEventPoint(other.getEventPoint());
		setEventPointUnit(other.getEventPointUnit());
		setEventLabel(other.getEventLabel());
		setCollectionProtocol(other.getCollectionProtocol());
		setCode(other.getCode());
		setDefaultSite(other.getDefaultSite());
		setClinicalDiagnosis(other.getClinicalDiagnosis());
		setClinicalStatus(other.getClinicalStatus());
		setVisitNamePrintMode(other.getVisitNamePrintMode());
		setVisitNamePrintCopies(other.getVisitNamePrintCopies());

		setActivityStatus(other.getActivityStatus());
		if (isClosed()) {
			getTopLevelAnticipatedSpecimens().forEach(SpecimenRequirement::close);
		}
	}
	
	public void addSpecimenRequirement(SpecimenRequirement sr) {
		if (isClosed()) {
			throw OpenSpecimenException.userError(CpeErrorCode.CLOSED, getEventLabel());
		}

		ensureUniqueSrCode(sr);
		getSpecimenRequirements().add(sr);
		sr.setCollectionProtocolEvent(this);
	}
	
	public CollectionProtocolEvent copy() {
		if (isClosed()) {
			throw OpenSpecimenException.userError(CpeErrorCode.CLOSED, getEventLabel());
		}

		CollectionProtocolEvent copy = new CollectionProtocolEvent();
		BeanUtils.copyProperties(this, copy, EXCLUDE_COPY_PROPS);
		return copy;
	}
	
	public CollectionProtocolEvent deepCopy() {
		CollectionProtocolEvent result = copy();
		copySpecimenRequirementsTo(result);
		return result;
	}
	
	public void copySpecimenRequirementsTo(CollectionProtocolEvent cpe) {
		List<SpecimenRequirement> topLevelSrs = new ArrayList<>(getTopLevelAnticipatedSpecimens());
		Collections.sort(topLevelSrs);

		int order = 1;
		for (SpecimenRequirement sr : topLevelSrs) {
			if (sr.isClosed()) {
				continue;
			}

			SpecimenRequirement copiedSr = sr.deepCopy(cpe);
			copiedSr.setSortOrder(order++);
			cpe.addSpecimenRequirement(copiedSr);
		}
	}
	
	public SpecimenRequirement getSrByCode(String code) {
		for (SpecimenRequirement sr : getSpecimenRequirements()) {
			if (code.equals(sr.getCode())) {
				return sr;
			}
		}
		
		return null;
	}
	
	public void delete() {
		for (SpecimenRequirement sr : getSpecimenRequirements()) {
			if (sr.isPrimary()) {
				sr.delete();
			}
		}

		setEventLabel(Utility.getDisabledValue(getEventLabel(), 255));
		setCode(Utility.getDisabledValue(getCode(), 32));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public void ensureUniqueSrCode(SpecimenRequirement sr) {
		if (StringUtils.isNotBlank(sr.getCode()) && getSrByCode(sr.getCode()) != null) {
			throw OpenSpecimenException.userError(SrErrorCode.DUP_CODE, sr.getCode());
		}
	}

	public void ensureUniqueSrCodes(Collection<SpecimenRequirement> srs) {
		Set<String> codes = new HashSet<String>();
		for (SpecimenRequirement sr : srs) {
			if (StringUtils.isBlank(sr.getCode())) {
				continue;
			}

			if (codes.contains(sr.getCode()) || getSrByCode(sr.getCode()) != null) {
				throw OpenSpecimenException.userError(SrErrorCode.DUP_CODE, sr.getCode());
			}

			codes.add(sr.getCode());
		}
	}

	@Override
	public int compareTo(CollectionProtocolEvent other) {
		Integer thisEventPoint  = Utility.getNoOfDays(getEventPoint(), getEventPointUnit());
		Integer otherEventPoint = Utility.getNoOfDays(other.getEventPoint(), other.getEventPointUnit());
		int result = ObjectUtils.compare(thisEventPoint, otherEventPoint, true);
		if (result == 0) {
			result = ObjectUtils.compare(getId(), other.getId());
		}

		return result;
	}

	private static final String[] EXCLUDE_COPY_PROPS = {
			"id",
			"collectionProtocol",
			"specimenRequirements",
			"specimenCollectionGroups"
		};
}