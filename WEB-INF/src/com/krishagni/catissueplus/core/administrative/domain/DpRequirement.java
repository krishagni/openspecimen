package com.krishagni.catissueplus.core.administrative.domain;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.util.Status;

@Audited
public class DpRequirement extends BaseExtensionEntity {
	public static final String EXTN = "DpRequirementExtension";

	private DistributionProtocol distributionProtocol;
	
	private PermissibleValue specimenType;
	
	private PermissibleValue anatomicSite;
	
	private Set<PermissibleValue> pathologyStatuses = new HashSet<>();

	private PermissibleValue clinicalDiagnosis;

	private BigDecimal cost;
	
	private Long specimenCount;
	
	private BigDecimal quantity;
	
	private String comments;
	
	private String activityStatus;

	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}

	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
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
	public Set<PermissibleValue> getPathologyStatuses() {
		return pathologyStatuses;
	}
	
	public void setPathologyStatuses(Set<PermissibleValue> pathologyStatuses) {
		this.pathologyStatuses = pathologyStatuses;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getClinicalDiagnosis() {
		return clinicalDiagnosis;
	}

	public void setClinicalDiagnosis(PermissibleValue clinicalDiagnosis) {
		this.clinicalDiagnosis = clinicalDiagnosis;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public Long getSpecimenCount() {
		return specimenCount;
	}
	
	public void setSpecimenCount(Long specimenCount) {
		this.specimenCount = specimenCount;
	}
	
	public BigDecimal getQuantity() {
		return quantity;
	}
	
	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}
	
	public String getComments() {
		return comments;
	}
	
	public void setComments(String comments) {
		this.comments = comments;
	}
	
	public String getActivityStatus() {
		return activityStatus;
	}
	
	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	@Override
	public String getEntityType() {
		return EXTN;
	}

	public void update(DpRequirement dpr) {
		setDistributionProtocol(dpr.getDistributionProtocol());
		setSpecimenType(dpr.getSpecimenType());
		setAnatomicSite(dpr.getAnatomicSite());
		CollectionUpdater.update(getPathologyStatuses(), dpr.getPathologyStatuses());
		setClinicalDiagnosis(dpr.getClinicalDiagnosis());
		setCost(dpr.getCost());
		setSpecimenCount(dpr.getSpecimenCount());
		setQuantity(dpr.getQuantity());
		setComments(dpr.getComments());
		setActivityStatus(dpr.getActivityStatus());
		setExtension(dpr.getExtension());
	}
	
	public boolean equalsSpecimenGroup(DpRequirement dpr) {
		return equalsSpecimenGroup(dpr.getSpecimenType(), dpr.getAnatomicSite(), dpr.getPathologyStatuses(), dpr.getClinicalDiagnosis());
	}

	public boolean equalsSpecimenGroup(PermissibleValue specimenType, PermissibleValue anatomicSite, Set<PermissibleValue> pathologyStatuses, PermissibleValue clinicalDiagnosis) {
		return Objects.equals(getSpecimenType(), specimenType) &&
			Objects.equals(getAnatomicSite(), anatomicSite) &&
			arePathologyStatusesEqual(pathologyStatuses) &&
			Objects.equals(getClinicalDiagnosis(), clinicalDiagnosis);
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public int getMatchPoints(Specimen specimen) {
		int points = 0;
		if (getSpecimenType() != null) {
			if (!getSpecimenType().equals(specimen.getSpecimenType())) {
				return 0;
			}

			points += 40;
		}

		if (!getPathologyStatuses().isEmpty()) {
			if (!getPathologyStatuses().contains(specimen.getPathologicalStatus())) {
				return 0;
			}

			points += 30;
		}

		if (getAnatomicSite() != null) {
			if (!getAnatomicSite().equals(specimen.getTissueSite())) {
				return 0;
			}

			points += 20;
		}

		if (getClinicalDiagnosis() != null) {
			if (!specimen.getVisit().getClinicalDiagnoses().contains(getClinicalDiagnosis())) {
				return 0;
			}

			points += 10;
		}

		return points;
	}

	private boolean arePathologyStatusesEqual(Set<PermissibleValue> pathologyStatuses) {
		boolean isEmptyOldPaths = CollectionUtils.isEmpty(getPathologyStatuses());
		boolean isEmptyNewPaths = CollectionUtils.isEmpty(pathologyStatuses);

		if (isEmptyOldPaths && isEmptyNewPaths) {
			return true;
		} else if (isEmptyOldPaths || isEmptyNewPaths) {
			return false;
		}

		return CollectionUtils.isSubCollection(pathologyStatuses, getPathologyStatuses()) ||
				CollectionUtils.isSubCollection(getPathologyStatuses(), pathologyStatuses);
	}
}
