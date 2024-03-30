package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.SpecimenUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.AliquotSpecimensRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol.SpecimenLabelAutoPrintMode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.DerivedSpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenTypeUnit;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenRequirementFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenRequirementDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;

import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.ANATOMIC_SITE_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.COLLECTOR_NOT_FOUND;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.COLL_CONT_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.COLL_PROC_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.CONCENTRATION_MUST_BE_POSITIVE;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.CPE_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_ANATOMIC_SITE;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_COLL_CONT;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_COLL_PROC;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_LABEL_FMT;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_LATERALITY;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_PATHOLOGY_STATUS;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_QTY;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_SPECIMEN_CLASS;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.INVALID_SPECIMEN_TYPE;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.LATERALITY_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.PARENT_REQ_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.PATHOLOGY_STATUS_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.RECEIVER_NOT_FOUND;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.SPECIMEN_CLASS_REQUIRED;
import static com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode.SPECIMEN_TYPE_REQUIRED;
import static com.krishagni.catissueplus.core.common.PvAttributes.COLL_PROC;
import static com.krishagni.catissueplus.core.common.PvAttributes.CONTAINER;
import static com.krishagni.catissueplus.core.common.PvAttributes.PATH_STATUS;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_ANATOMIC_SITE;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_CLASS;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_LATERALITY;
import static com.krishagni.catissueplus.core.common.service.PvValidator.isValid;

public class SpecimenRequirementFactoryImpl implements SpecimenRequirementFactory {

	private DaoFactory daoFactory;
	
	private LabelGenerator specimenLabelGenerator;

	private CpeFactory cpeFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public LabelGenerator getSpecimenLabelGenerator() {
		return specimenLabelGenerator;
	}

	public void setSpecimenLabelGenerator(LabelGenerator specimenLabelGenerator) {
		this.specimenLabelGenerator = specimenLabelGenerator;
	}

	public void setCpeFactory(CpeFactory cpeFactory) {
		this.cpeFactory = cpeFactory;
	}

	@Override
	public SpecimenRequirement createSpecimenRequirement(SpecimenRequirementDetail detail) {
		SpecimenRequirement requirement = new SpecimenRequirement();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		requirement.setId(detail.getId());
		requirement.setName(detail.getName());
		requirement.setLineage(Specimen.NEW);
		requirement.setLabelPrintCopies(detail.getLabelPrintCopies());
		requirement.setSortOrder(detail.getSortOrder());

		setCpe(detail, requirement, ose);
		setCode(detail, requirement, ose);
		setLabelFormat(detail, requirement, ose);
		setLabelAutoPrintMode(detail, requirement, ose);
		setSpecimenClass(detail, requirement, ose);
		setSpecimenType(detail, requirement, ose);
		setAnatomicSite(detail, requirement, ose);
		setLaterality(detail, requirement, ose);
		setPathologyStatus(detail, requirement, ose);
		setStorageType(detail, requirement, ose);
		setInitialQty(detail, requirement, ose);
		setConcentration(detail, requirement, ose);
		setCollector(detail, requirement, ose);
		setCollectionProcedure(detail, requirement, ose);
		setCollectionContainer(detail, requirement, ose);
		setReceiver(detail, requirement, ose);
		setActivityStatus(detail, requirement, ose);

		ose.checkAndThrow();
		return requirement;
	}

	@Override
	public SpecimenRequirement createDerived(DerivedSpecimenRequirement req) {
		String cpShortTitle = req.getCpShortTitle();
		String eventLabel = req.getEventLabel();
		String srCode = req.getParentSrCode();

		Object key = null;
		SpecimenRequirement parent = null;
		if (req.getParentSrId() != null) {
			key = req.getParentSrId();
			parent = daoFactory.getSpecimenRequirementDao().getById(req.getParentSrId());
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(eventLabel) && StringUtils.isNotBlank(srCode)){
			key = srCode;
			parent = daoFactory.getSpecimenRequirementDao().getByCpEventLabelAndSrCode(cpShortTitle, eventLabel, srCode);
		}

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		if (key == null) {
			ose.addError(PARENT_REQ_REQUIRED);
			throw ose;
		} else if (parent == null) {
			ose.addError(SrErrorCode.PARENT_NOT_FOUND, key);
			throw ose;
		}

		SpecimenRequirement derived = parent.copy();
		derived.setLabelFormat(null);
		derived.setLineage(Specimen.DERIVED);
		derived.setName(req.getName());
		derived.setLabelPrintCopies(req.getLabelPrintCopies());
		derived.setSortOrder(req.getSortOrder());

		setSpecimenClass(req.getSpecimenClass(), derived, ose);
		setSpecimenType(req.getSpecimenClass(), req.getType(), derived, ose);
		setInitialQty(req.getQuantity(), derived, ose);
		setStorageType(req.getStorageType(), derived, ose);
		setConcentration(req.getConcentration(), derived, ose);
		setAnatomicSite(req.getAnatomicSite(), derived, ose);
		setLaterality(req.getLaterality(), derived, ose);
		setPathologyStatus(req.getPathology(), derived, ose);
		setLabelFormat(req.getLabelFmt(), derived, ose);
		setLabelAutoPrintMode(req.getLabelAutoPrintMode(), derived, ose);
		setCode(req.getCode(), derived, ose);
		setActivityStatus(StringUtils.EMPTY, derived, ose);

		ose.checkAndThrow();
		derived.setParentSpecimenRequirement(parent);
		return derived;
	}
	
	@Override
	public SpecimenRequirement createForUpdate(SpecimenRequirement existingSr, SpecimenRequirementDetail req) {
		SpecimenRequirement sr = new SpecimenRequirement();
		sr.setName(req.getName());
		sr.setSortOrder(req.getSortOrder());
		sr.setLabelPrintCopies(req.getLabelPrintCopies());
		sr.setLineage(existingSr.getLineage());
		sr.setCollectionProtocolEvent(existingSr.getCollectionProtocolEvent());
		
		//
		// Specimen class and type are set here so that properties dependent on these can
		// be calculated and set appropriately. 
		//
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		setSpecimenClass(req, sr, ose);
		setSpecimenType(req, sr, ose);		
		setInitialQty(req, sr, ose);
		setStorageType(req, sr, ose);
		setLabelFormat(req, sr, ose);
		setLabelAutoPrintMode(req, sr, ose);
		setCode(req, sr, ose);
		setConcentration(req, sr, ose);
		setActivityStatus(req, sr, ose);

		if (existingSr.isPrimary() || existingSr.isDerivative()) {
			setPathologyStatus(req, sr, ose);
			setAnatomicSite(req, sr, ose);
			setLaterality(req, sr, ose);
		}

		if (existingSr.isPrimary()) {
			setCollector(req, sr, ose);
			setCollectionProcedure(req, sr, ose);
			setCollectionContainer(req, sr, ose);
			setReceiver(req, sr, ose);
		}

		ose.checkAndThrow();
		return sr;
	}
	
	@Override
	public List<SpecimenRequirement> createAliquots(AliquotSpecimensRequirement req) {
		String cpShortTitle = req.getCpShortTitle();
		String eventLabel = req.getEventLabel();
		String srCode = req.getParentSrCode();

		Object key = null;
		SpecimenRequirement parent = null;
		if (req.getParentSrId() != null) {
			key = req.getParentSrId();
			parent = daoFactory.getSpecimenRequirementDao().getById(req.getParentSrId());
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(eventLabel) && StringUtils.isNotBlank(srCode)){
			key = srCode;
			parent = daoFactory.getSpecimenRequirementDao().getByCpEventLabelAndSrCode(cpShortTitle, eventLabel, srCode);
		}

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		if (key == null) {
			ose.addError(PARENT_REQ_REQUIRED);
		} else if (parent == null) {
			ose.addError(SrErrorCode.PARENT_NOT_FOUND);
		} else if (req.getNoOfAliquots() == null || req.getNoOfAliquots() < 1L) {
			ose.addError(SrErrorCode.INVALID_ALIQUOT_CNT);
		} else if (NumUtil.lessThanEqualsZero(req.getQtyPerAliquot())) {
			ose.addError(SrErrorCode.INVALID_QTY);
		} else if (req.getQtyPerAliquot() == null) {
			if (ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.ALIQUOT_QTY_REQ, true)) {
				ose.addError(SrErrorCode.INVALID_QTY);
			}
		} else { /* req.getQtyPerAliquot() != null */
			BigDecimal total = NumUtil.multiply(req.getQtyPerAliquot(), req.getNoOfAliquots());
			if (NumUtil.greaterThan(total, parent.getQtyAfterAliquotsUse())) {
				ose.addError(SrErrorCode.INSUFFICIENT_QTY);
			}
		}

		ose.checkAndThrow();

		List<SpecimenRequirement> aliquots = new ArrayList<>();
		for (int i = 0; i < req.getNoOfAliquots(); ++i) {
			SpecimenRequirement aliquot = parent.copy();
			aliquot.setLabelFormat(null);
			aliquot.setLineage(Specimen.ALIQUOT);
			setStorageType(req.getStorageType(), aliquot, ose);
			setLabelFormat(req.getLabelFmt(), aliquot, ose);
			setLabelAutoPrintMode(req.getLabelAutoPrintMode(), aliquot, ose);
			aliquot.setLabelPrintCopies(req.getLabelPrintCopies());
			aliquot.setInitialQuantity(req.getQtyPerAliquot());
			aliquot.setParentSpecimenRequirement(parent);

			ose.checkAndThrow();
			aliquots.add(aliquot);
		}

		return aliquots;
	}
	
	private void setCode(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setCode(detail.getCode(), sr, ose);
	}
	
	private void setCode(String code, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isNotBlank(code)) {
			sr.setCode(code.trim());
		} else {
			sr.setCode(null);
		}
	}
	
	private void setLabelFormat(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setLabelFormat(detail.getLabelFmt(), sr, ose);
	}
	
	private void setLabelFormat(String labelFmt, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(labelFmt)) {
			return;
		}
		
		if (!specimenLabelGenerator.isValidLabelTmpl(labelFmt)) {
			ose.addError(INVALID_LABEL_FMT);
		}
		
		sr.setLabelFormat(labelFmt);
	}
	

	private void setLabelAutoPrintMode(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setLabelAutoPrintMode(detail.getLabelAutoPrintMode(), sr, ose);
	}
	
	private void setLabelAutoPrintMode(String input, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input)) {
			return;
		}
		
		SpecimenLabelAutoPrintMode labelAutoPrintMode = null;
		try {
			labelAutoPrintMode = SpecimenLabelAutoPrintMode.valueOf(input);
		} catch (IllegalArgumentException iae) {
			ose.addError(CpErrorCode.INVALID_SPMN_LABEL_PRINT_MODE, input);
			return;
		}
		
		sr.setLabelAutoPrintMode(labelAutoPrintMode);
	}

	private void setSpecimenClass(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setSpecimenClass(detail.getSpecimenClass(), sr, ose);
	}
	
	private void setSpecimenClass(String specimenClass, SpecimenRequirement sr, OpenSpecimenException ose) {
		PermissibleValue classPv = getPv(
			SPECIMEN_CLASS, specimenClass, false,
			SPECIMEN_CLASS_REQUIRED, INVALID_SPECIMEN_CLASS, ose);
		sr.setSpecimenClass(classPv);
	}
	
	private void setSpecimenType(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setSpecimenType(detail.getSpecimenClass(), detail.getType(), sr, ose);
	}
	
	private void setSpecimenType(String specimenClass, String type, SpecimenRequirement sr, OpenSpecimenException ose) {
		PermissibleValue typePv = getPv(
			SPECIMEN_CLASS, specimenClass, type,
			SPECIMEN_TYPE_REQUIRED, INVALID_SPECIMEN_TYPE, ose);
		sr.setSpecimenType(typePv);
	}
	
	private void setAnatomicSite(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setAnatomicSite(detail.getAnatomicSite(), sr, ose);
	}

	private void setAnatomicSite(String anatomicSite, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(anatomicSite) && sr.isDerivative()) {
			//
			// If anatomic site is not specified for derivative requirement
			// then its value is picked from parent requirement
			//
			return;
		}

		PermissibleValue site = getPv(
			SPECIMEN_ANATOMIC_SITE, anatomicSite, true,
			ANATOMIC_SITE_REQUIRED, INVALID_ANATOMIC_SITE, ose);
		sr.setAnatomicSite(site);
	}
	
	private void setLaterality(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setLaterality(detail.getLaterality(), sr, ose);
	}

	private void setLaterality(String laterality, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(laterality) && sr.isDerivative()) {
			//
			// If laterality is not specified for derivative requirement
			// then its value is picked from parent requirement
			//
			return;
		}

		sr.setLaterality(getPv(SPECIMEN_LATERALITY, laterality, false, LATERALITY_REQUIRED, INVALID_LATERALITY, ose));
	}

	private void setPathologyStatus(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setPathologyStatus(detail.getPathology(), sr, ose);
	}
	
	private void setPathologyStatus(String pathology, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(pathology) && sr.isDerivative()) {
			//
			// If pathology status is not specified for derivative requirement
			// then its value is picked from parent requirement
			//
			return;
		}

		sr.setPathologyStatus(getPv(PATH_STATUS, pathology, false, PATHOLOGY_STATUS_REQUIRED, INVALID_PATHOLOGY_STATUS, ose));
	}
	
	private void setStorageType(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setStorageType(detail.getStorageType(), sr, ose);
	}

	private void setStorageType(String storageType, SpecimenRequirement sr, OpenSpecimenException ose) {
		storageType = ensureNotEmpty(storageType, SrErrorCode.STORAGE_TYPE_REQUIRED, ose);
		sr.setStorageType(storageType);
		// TODO: check for valid storage type
	}
	
	private void setInitialQty(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setInitialQty(detail.getInitialQty(), sr, ose);
	}
		
	private void setInitialQty(BigDecimal initialQty, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (NumUtil.lessThanZero(initialQty)) {
			ose.addError(INVALID_QTY);
			return;
		}

		if (sr.isAliquot() && (NumUtil.lessThanEqualsZero(initialQty) || (isAliquotQtyReq() && initialQty == null))) {
			ose.addError(INVALID_QTY);
			return;
		}

		sr.setInitialQuantity(initialQty);
	}

	private void setConcentration(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setConcentration(detail.getConcentration(), sr, ose);
	}

	private void setConcentration(BigDecimal concentration, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (concentration != null && NumUtil.lessThanZero(concentration)) {
			ose.addError(CONCENTRATION_MUST_BE_POSITIVE);
			return;
		}
		
		sr.setConcentration(concentration);
	}
	
	private void setCollector(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		sr.setCollector(ensureValidUser(detail.getCollector(), COLLECTOR_NOT_FOUND, ose));
	}

	private void setCollectionProcedure(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		PermissibleValue procedure = getPv(
			COLL_PROC, detail.getCollectionProcedure(), false,
			COLL_PROC_REQUIRED, INVALID_COLL_PROC, ose);
		sr.setCollectionProcedure(procedure);
	}

	private void setCollectionContainer(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		PermissibleValue container = getPv(
			CONTAINER, detail.getCollectionContainer(), false,
			COLL_CONT_REQUIRED, INVALID_COLL_CONT, ose);
		sr.setCollectionContainer(container);
	}

	private void setReceiver(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		sr.setReceiver(ensureValidUser(detail.getReceiver(), RECEIVER_NOT_FOUND, ose));
	}

	private void setCpe(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		Long eventId = detail.getEventId();
		String cpShortTitle = detail.getCpShortTitle();
		String eventLabel = detail.getEventLabel();
		
		CollectionProtocolEvent cpe = null;
		Object key = null;
		if (eventId != null && eventId > 0) {
			cpe = daoFactory.getCollectionProtocolDao().getCpe(eventId);
			key = eventId;
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(eventLabel)) {
			cpe = daoFactory.getCollectionProtocolDao().getCpeByShortTitleAndEventLabel(cpShortTitle, eventLabel);
			key = eventLabel;
		} else {
			if (detail.getCpId() != null) {
				CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
				if (cp != null && cp.isSpecimenCentric()) {
					cpe = getEventFor(cp);
				}
			} else if (StringUtils.isNotBlank(detail.getCpShortTitle())) {
				CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
				if (cp != null && cp.isSpecimenCentric()) {
					cpe = getEventFor(cp);
				}
			}
		}

		if (cpe == null) {
			if (key == null) {
				ose.addError(CPE_REQUIRED);
			} else {
				ose.addError(CpeErrorCode.NOT_FOUND, key, 1);
			}
		}

		sr.setCollectionProtocolEvent(cpe);
	}

	private CollectionProtocolEvent getEventFor(CollectionProtocol cp) {
		String eventName = cp.getEventName();
		CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpeByEventLabel(cp.getId(), eventName);
		if (cpe != null) {
			return cpe;
		}

		CollectionProtocolEventDetail eventInput = new CollectionProtocolEventDetail();
		eventInput.setEventLabel(eventName);
		eventInput.setCode(eventName);
		eventInput.setCpId(cp.getId());
		eventInput.setCpShortTitle(cp.getShortTitle());
		cpe = cpeFactory.createCpe(eventInput);
		cp.addCpe(cpe);
		daoFactory.getCollectionProtocolDao().saveOrUpdate(cp, true);

		Visit visit = daoFactory.getVisitsDao().getByName(cp.getVisitName());
		if (visit != null) {
			visit.setCpEvent(cpe);
		}

		return cpe;
	}

	private void setActivityStatus(SpecimenRequirementDetail detail, SpecimenRequirement sr, OpenSpecimenException ose) {
		setActivityStatus(detail.getActivityStatus(), sr, ose);
	}

	private void setActivityStatus(String activityStatus, SpecimenRequirement sr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(activityStatus)) {
			sr.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(activityStatus)) {
			sr.setActivityStatus(activityStatus);
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID, activityStatus);
		}
	}

	private PermissibleValue getPv(String attr, String value, boolean leafNode, ErrorCode req, ErrorCode invalid, OpenSpecimenException ose) {
		if (StringUtils.isBlank(value)) {
			ose.addError(req);
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, value, leafNode);
		if (pv == null) {
			ose.addError(invalid, value);
		}

		return pv;
	}

	private PermissibleValue getPv(String attr, String parentValue, String value, ErrorCode req, ErrorCode invalid, OpenSpecimenException ose) {
		if (StringUtils.isBlank(value)) {
			ose.addError(req);
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, parentValue, value);
		if (pv == null) {
			ose.addError(invalid, value);
		}

		return pv;
	}

	private String ensureNotEmptyAndValid(String attr, String value, ErrorCode req, ErrorCode invalid, OpenSpecimenException ose) {
		return ensureNotEmptyAndValid(attr, value, false, req, invalid, ose);
	}

	private String ensureNotEmptyAndValid(String attr, String value, boolean leafCheck, ErrorCode req, ErrorCode invalid, OpenSpecimenException ose) {
		value = ensureNotEmpty(value, req, ose);
		if (value != null) {
			value = ensureValid(attr, value, leafCheck, invalid, ose);
		}
		
		return value;
	}
	
	private String ensureValid(String attr, String value, boolean leafCheck, ErrorCode invalid, OpenSpecimenException ose) {
		if (!isValid(attr, value, leafCheck)) {
			ose.addError(invalid, value);
			return null;
		}
		
		return value;
	}
	
	private String ensureNotEmpty(String value, ErrorCode required, OpenSpecimenException ose) {
		if (StringUtils.isBlank(value)) {
			ose.addError(required);
			return null;
		}
		
		return value;
	}
	
	private User ensureValidUser(UserSummary userSummary, ErrorCode notFound, OpenSpecimenException ose) {
		if (userSummary == null) {
			return null;
		}
		
		User user = null;
		if (userSummary.getId() != null) {
			user = daoFactory.getUserDao().getById(userSummary.getId());
		} else if (StringUtils.isNotBlank(userSummary.getLoginName()) && StringUtils.isNotBlank(userSummary.getDomain())) {
			user = daoFactory.getUserDao().getUser(userSummary.getLoginName(), userSummary.getDomain());
		} else if (StringUtils.isNotBlank(userSummary.getEmailAddress())) {
			user = daoFactory.getUserDao().getUserByEmailAddress(userSummary.getEmailAddress());
		}
		
		if (user == null) {
			ose.addError(notFound);
		}
		
		return user;		
	}

	private boolean isAliquotQtyReq() {
		return ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.ALIQUOT_QTY_REQ, true);
	}
}
