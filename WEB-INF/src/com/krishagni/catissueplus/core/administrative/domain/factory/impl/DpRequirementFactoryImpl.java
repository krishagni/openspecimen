package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import static com.krishagni.catissueplus.core.common.PvAttributes.*;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpRequirement;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DpRequirementFactory;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolSummary;
import com.krishagni.catissueplus.core.administrative.events.DpRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.DeObject;

public class DpRequirementFactoryImpl implements DpRequirementFactory {
	
	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public DpRequirement createRequirement(DpRequirementDetail detail) {
		return createRequirement(null, detail);
	}

	public DpRequirement createRequirement(DpRequirement existing, DpRequirementDetail detail) {
		DpRequirement dpr = new DpRequirement();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		dpr.setId(detail.getId());
		setDistributionProtocol(detail, existing, dpr, ose);
		setSpecimenType(detail, existing, dpr, ose);
		setAnatomicSite(detail, existing, dpr, ose);
		setPathologyStatuses(detail, existing, dpr, ose);
		setClinicalDiagnosis(detail, existing, dpr, ose);
		setCost(detail, existing, dpr, ose);
		setSpecimenCount(detail, existing, dpr, ose);
		setQuantity(detail, existing, dpr, ose);
		setComments(detail, existing, dpr, ose);
		setActivityStatus(detail, existing, dpr, ose);
		setExtension(detail, existing, dpr, ose);
		
		ose.checkAndThrow();
		return dpr;
	}

	private void setDistributionProtocol(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("dp")) {
			setDistributionProtocol(detail, dpr, ose);
		} else {
			dpr.setDistributionProtocol(existing.getDistributionProtocol());
		}
	}

	private void setDistributionProtocol(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		DistributionProtocolSummary dps = detail.getDp();
		Long dpId = dps != null ? dps.getId() : null;
		String dpShortTitle = dps != null ? dps.getShortTitle() : null;
		
		if (dpId == null && StringUtils.isBlank(dpShortTitle)) {
			ose.addError(DpRequirementErrorCode.DP_REQUIRED);
			return;
		}
		
		DistributionProtocol dp = null;
		Object key = null;
		if (dpId != null) {
			dp = daoFactory.getDistributionProtocolDao().getById(dpId);
			key = dpId;
		} else {
			dp = daoFactory.getDistributionProtocolDao().getByShortTitle(dpShortTitle);
			key = dpShortTitle;
		}
		
		if (dp == null) {
			ose.addError(DistributionProtocolErrorCode.NOT_FOUND, key, 1);
			return;
		}
		
		dpr.setDistributionProtocol(dp);
	}

	private void setSpecimenType(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("specimenType")) {
			setSpecimenType(detail, dpr, ose);
		} else {
			dpr.setSpecimenType(existing.getSpecimenType());
		}
	}
	
	private void setSpecimenType(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getSpecimenType())) {
			return;
		}

		PermissibleValue typePv = getPv(SPECIMEN_CLASS, detail.getSpecimenType(), false);
		if (typePv == null || typePv.getParent() == null) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, detail.getSpecimenType());
			return;
		}
		
		dpr.setSpecimenType(typePv);
	}

	private void setAnatomicSite(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("anatomicSite")) {
			setAnatomicSite(detail, dpr, ose);
		} else {
			dpr.setAnatomicSite(existing.getAnatomicSite());
		}
	}

	private void setAnatomicSite(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getAnatomicSite())) {
			return;
		}

		PermissibleValue anatomicSitePv = getPv(SPECIMEN_ANATOMIC_SITE, detail.getAnatomicSite(), true);
		if (anatomicSitePv == null) {
			ose.addError(SpecimenErrorCode.INVALID_ANATOMIC_SITE);
			return;
		}

		dpr.setAnatomicSite(anatomicSitePv);
	}

	private void setPathologyStatuses(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("pathologyStatuses")) {
			setPathologyStatuses(detail, dpr, ose);
		} else {
			dpr.setPathologyStatuses(existing.getPathologyStatuses());
		}
	}
	
	private void setPathologyStatuses(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		Set<String> pathologyStatuses = detail.getPathologyStatuses();
		if (CollectionUtils.isEmpty(pathologyStatuses)) {
			return;
		}

		List<PermissibleValue> pathPvs = getPvs(PATH_STATUS, pathologyStatuses);
		if (pathPvs.size() != pathologyStatuses.size()) {
			pathologyStatuses.removeAll(pathPvs.stream().map(pv -> pv.getValue()).collect(Collectors.toSet()));
			ose.addError(DpRequirementErrorCode.INVALID_PATHOLOGY_STATUSES, StringUtils.join(pathologyStatuses));
			return;
		}

		dpr.setPathologyStatuses(new HashSet<>(pathPvs));
	}

	private void setClinicalDiagnosis(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("clinicalDiagnosis")) {
			setClinicalDiagnosis(detail, dpr, ose);
		} else {
			dpr.setClinicalDiagnosis(existing.getClinicalDiagnosis());
		}
	}

	private void setClinicalDiagnosis(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getClinicalDiagnosis())) {
			return;
		}

		PermissibleValue cdPv = getPv(CLINICAL_DIAG, detail.getClinicalDiagnosis(), false);
		if (cdPv == null) {
			ose.addError(VisitErrorCode.INVALID_CLINICAL_DIAGNOSIS);
			return;
		}

		dpr.setClinicalDiagnosis(cdPv);
	}

	private void setCost(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("cost")) {
			setCost(detail, dpr, ose);
		} else {
			dpr.setCost(existing.getCost());
		}
	}

	private void setCost(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		BigDecimal cost = detail.getCost();
		if (NumUtil.lessThanZero(cost)) {
			ose.addError(DpRequirementErrorCode.INVALID_COST, cost);
			return;
		}

		dpr.setCost(cost);
	}

	private void setSpecimenCount(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("specimenCount")) {
			setSpecimenCount(detail, dpr, ose);
		} else {
			dpr.setSpecimenCount(existing.getSpecimenCount());
		}
	}
	
	private void setSpecimenCount(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		Long specimenCount = detail.getSpecimenCount();
		if (specimenCount == null) {
			return;
		}
		
		if (specimenCount < 0L) {
			ose.addError(DpRequirementErrorCode.INVALID_SPECIMEN_COUNT, specimenCount);
			return;
		}
		
		dpr.setSpecimenCount(specimenCount);
	}

	private void setQuantity(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("quantity")) {
			setQuantity(detail, dpr, ose);
		} else {
			dpr.setQuantity(existing.getQuantity());
		}
	}
	
	private void setQuantity(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		BigDecimal quantity = detail.getQuantity();
		if (quantity == null) {
			return;
		}
		
		if (NumUtil.lessThanZero(quantity)) {
			ose.addError(DpRequirementErrorCode.INVALID_QUANTITY, quantity);
			return;
		}
		
		dpr.setQuantity(quantity);
	}

	private void setComments(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("comments")) {
			setComments(detail, dpr, ose);
		} else {
			dpr.setComments(existing.getComments());
		}
	}
	
	private void setComments(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		dpr.setComments(detail.getComments());
	}

	private void setActivityStatus(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, dpr, ose);
		} else {
			dpr.setActivityStatus(existing.getActivityStatus());
		}
	}
	
	private void setActivityStatus(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		dpr.setActivityStatus(activityStatus);
	}

	private void setExtension(DpRequirementDetail detail, DpRequirement existing, DpRequirement dpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("extensionDetail")) {
			setExtension(detail, dpr, ose);
		} else {
			dpr.setExtension(existing.getExtension());
		}
	}

	private void setExtension(DpRequirementDetail detail, DpRequirement dpr, OpenSpecimenException ose) {
		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), dpr);
		dpr.setExtension(extension);
	}

	private PermissibleValue getPv(String attribute, String value, boolean leafNode) {
		return daoFactory.getPermissibleValueDao().getPv(attribute, value, leafNode);
	}

	private List<PermissibleValue> getPvs(String attribute, Collection<String> values) {
		return daoFactory.getPermissibleValueDao().getPvs(attribute, values);
	}
}
