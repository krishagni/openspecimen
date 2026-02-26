package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import static com.krishagni.catissueplus.core.common.PvAttributes.CLINICAL_DIAG;
import static com.krishagni.catissueplus.core.common.PvAttributes.CLINICAL_STATUS;
import static com.krishagni.catissueplus.core.common.service.PvValidator.isValid;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol.VisitNamePrintMode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;

public class CpeFactoryImpl implements CpeFactory {
	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public CollectionProtocolEvent createCpe(CollectionProtocolEventDetail detail) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		CollectionProtocolEvent cpe = new CollectionProtocolEvent();
				
		cpe.setId(detail.getId());
		setEventLabel(detail, cpe, ose);
		setEventPoint(detail, cpe, ose);
		setEventPointUnit(detail, cpe, ose);
		setCode(detail, cpe, ose);
		setCp(detail, cpe, ose);
		setDefaultSite(detail, cpe, ose);
		setClinicalDiagnosis(detail, cpe, ose);
		setClinicalStatus(detail, cpe, ose);
		setVisitNamePrintMode(detail, cpe, ose);
		setVisitNamePrintCopies(detail, cpe, ose);
		setActivityStatus(detail, cpe, ose);
		
		ose.checkAndThrow();
		return cpe;
	}

	@Override
	public CollectionProtocolEvent createCpeCopy(CollectionProtocolEventDetail detail, CollectionProtocolEvent existing) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		CollectionProtocolEvent cpe = new CollectionProtocolEvent();

		setEventLabel(detail, cpe, ose);
		setCode(detail, cpe, ose);
		
		if (detail.getEventPoint() != null) {
			setEventPoint(detail, cpe, ose);
		} else {
			cpe.setEventPoint(existing.getEventPoint());
		}

		if (StringUtils.isNotBlank(detail.getEventPointUnit())) {
			setEventPointUnit(detail, cpe, ose);
		} else {
			cpe.setEventPointUnit(existing.getEventPointUnit());
		}

		if (cpe.getEventPoint() != null && cpe.getEventPointUnit() == null) {
			// scenario: the source has no event point and unit
			// this check ensures the copy has both event point and unit
			ose.addError(CpeErrorCode.POINT_UNIT_REQUIRED);
		}
		
		cpe.setCollectionProtocol(existing.getCollectionProtocol());
		
		if (StringUtils.isNotBlank(detail.getDefaultSite())) {
			setDefaultSite(detail, cpe, ose);
		} else {
			cpe.setDefaultSite(existing.getDefaultSite());
		}
		
		if (StringUtils.isNotBlank(detail.getClinicalDiagnosis())) {
			setClinicalDiagnosis(detail, cpe, ose);
		} else {
			cpe.setClinicalDiagnosis(existing.getClinicalDiagnosis());
		}
		
		if (StringUtils.isNotBlank(detail.getClinicalStatus())) {
			setClinicalStatus(detail, cpe, ose);
		} else {
			cpe.setClinicalStatus(existing.getClinicalStatus());
		}

		if (StringUtils.isNotBlank(detail.getVisitNamePrintMode())) {
			setVisitNamePrintMode(detail, cpe, ose);
		} else {
			cpe.setVisitNamePrintMode(existing.getVisitNamePrintMode());
		}

		if (detail.getVisitNamePrintCopies() != null) {
			setVisitNamePrintCopies(detail, cpe, ose);
		} else {
			cpe.setVisitNamePrintCopies(existing.getVisitNamePrintCopies());
		}

		if (StringUtils.isNotBlank(detail.getActivityStatus())) {
			setActivityStatus(detail, cpe, ose);
		} else {
			cpe.setActivityStatus(existing.getActivityStatus());
		}

		ose.checkAndThrow();		
		return cpe;
	}
	
	public void setEventLabel(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getEventLabel())) {
			ose.addError(CpeErrorCode.LABEL_REQUIRED);
			return;
		}
		
		cpe.setEventLabel(detail.getEventLabel());
	}
	
	public void setEventPoint(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		cpe.setEventPoint(detail.getEventPoint());
	}

	private void setEventPointUnit(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		if (cpe.getEventPoint() == null) {
			return;
		}

		if (StringUtils.isBlank(detail.getEventPointUnit())) {
			ose.addError(CpeErrorCode.POINT_UNIT_REQUIRED);
			return;
		}

		IntervalUnit eventPointUnit = null;
		try {
			eventPointUnit = IntervalUnit.valueOf(detail.getEventPointUnit());
			cpe.setEventPointUnit(eventPointUnit);
		} catch (IllegalArgumentException iae) {
			ose.addError(CpeErrorCode.INVALID_POINT_UNIT, detail.getEventPointUnit());
		}
	}


	public void setCode(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		if (StringUtils.isNotBlank(detail.getCode())) {
			cpe.setCode(detail.getCode().trim());
		}
	}
	
	public void setCp(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		CollectionProtocol cp = null;
		if (StringUtils.isNotBlank(detail.getCollectionProtocol())) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(detail.getCollectionProtocol());
		} else if (StringUtils.isNotBlank(detail.getCpShortTitle())) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
		} else {
			ose.addError(CpeErrorCode.CP_REQUIRED);
			return;
		}
		
		if (cp == null) {
			ose.addError(CpErrorCode.NOT_FOUND);
			return;
		}
		
		cpe.setCollectionProtocol(cp);		
	}
	
	public void setDefaultSite(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getDefaultSite())) {
			return;
		}
		
		Site site = daoFactory.getSiteDao().getSiteByName(detail.getDefaultSite());
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);
			return;
		}
		
		cpe.setDefaultSite(site);
	}
	
	public void setActivityStatus(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			cpe.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(activityStatus)) {
			cpe.setActivityStatus(activityStatus);
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID);
		}
	}
	
	public void setClinicalDiagnosis(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		String clinicalDiagnosis = detail.getClinicalDiagnosis();
		if (StringUtils.isBlank(clinicalDiagnosis)) {
			return;
		}

		PermissibleValue cdPv = daoFactory.getPermissibleValueDao().getPv(CLINICAL_DIAG, clinicalDiagnosis);
		if (cdPv == null) {
			ose.addError(CpeErrorCode.INVALID_CLINICAL_DIAGNOSIS, clinicalDiagnosis);
			return;
		}
		
		cpe.setClinicalDiagnosis(cdPv);
	}
	
	public void setClinicalStatus(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		String clinicalStatus = detail.getClinicalStatus();
		if (StringUtils.isBlank(clinicalStatus)) {
			return;
		}

		PermissibleValue statusPv = daoFactory.getPermissibleValueDao().getPv(CLINICAL_STATUS, clinicalStatus);
		if (statusPv == null) {
			ose.addError(CpeErrorCode.INVALID_CLINICAL_STATUS, clinicalStatus);
			return;
		}
		
		cpe.setClinicalStatus(statusPv);
	}

	private void setVisitNamePrintMode(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		if (detail.getVisitNamePrintMode() == null) {
			return;
		}

		VisitNamePrintMode printMode;
		try {
			printMode = VisitNamePrintMode.valueOf(detail.getVisitNamePrintMode());
		} catch (IllegalArgumentException iae) {
			String mode = String.join("_", detail.getVisitNamePrintMode().split("\\s+")).toUpperCase();
			try {
				printMode = VisitNamePrintMode.valueOf(mode);
			} catch (IllegalArgumentException iae1) {
				ose.addError(CpErrorCode.INVALID_VISIT_NAME_PRINT_MODE, detail.getVisitNamePrintMode());
				return;
			}
		}

		cpe.setVisitNamePrintMode(printMode);
	}

	private void setVisitNamePrintCopies(CollectionProtocolEventDetail detail, CollectionProtocolEvent cpe, OpenSpecimenException ose) {
		cpe.setVisitNamePrintCopies(detail.getVisitNamePrintCopies());
	}

}