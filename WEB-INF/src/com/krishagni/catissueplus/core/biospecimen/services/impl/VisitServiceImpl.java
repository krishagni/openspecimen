
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.InitializingBean;

import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SprSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.VisitPreSaveEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.VisitSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedVisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PrintVisitNameDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SprDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SprLockDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSearchDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.matching.VisitsLookup;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.DocumentDeIdentifier;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.SprPdfGenerator;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class VisitServiceImpl implements VisitService, ObjectAccessor, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(VisitServiceImpl.class);

	private static String defaultVisitSprDir;

	private DaoFactory daoFactory;

	private VisitFactory visitFactory;
	
	private SpecimenService specimenSvc;
	
	private ConfigurationService cfgSvc;
	
	private LabelGenerator visitNameGenerator;
	
	private SprPdfGenerator sprText2PdfGenerator;

	private VisitsLookup defaultVisitsLookup;

	private VisitsLookup visitsLookup;

	private ExportService exportSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setVisitFactory(VisitFactory visitFactory) {
		this.visitFactory = visitFactory;
	}
	
	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}
	
	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}
	
	public void setVisitNameGenerator(LabelGenerator visitNameGenerator) {
		this.visitNameGenerator = visitNameGenerator;
	}
	
	public void setSprText2PdfGenerator(SprPdfGenerator sprText2PdfGenerator) {
		this.sprText2PdfGenerator = sprText2PdfGenerator;
	}

	public void setDefaultVisitsLookup(VisitsLookup defaultVisitsLookup) {
		this.defaultVisitsLookup = defaultVisitsLookup;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> getVisit(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();			
			Visit visit = getVisit(crit.getId(), crit.getName());
			boolean allowPhi = AccessCtrlMgr.getInstance().ensureReadVisitRights(visit);
			return ResponseEvent.response(VisitDetail.from(visit, false, !allowPhi));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@PlusTransactional
	@Override
	public ResponseEvent<List<VisitDetail>> getVisits(RequestEvent<VisitsListCriteria> criteria) {
		VisitsListCriteria crit = criteria.getPayload();
		if (StringUtils.isBlank(crit.name()) && StringUtils.isBlank(crit.sprNumber())) {
			return ResponseEvent.response(getVisits0(crit));
		}

		List<Visit> visits = new ArrayList<>();
		boolean hasPhiFields = false;
		if (StringUtils.isNotEmpty(crit.name())) {
			visits.add(getVisit(null, crit.name()));
		} else if (StringUtils.isNotEmpty(crit.sprNumber())) {
			visits.addAll(daoFactory.getVisitsDao().getBySpr(crit.sprNumber()));
			hasPhiFields = true;
		}

		Iterator<Visit> iterator = visits.iterator();
		while (iterator.hasNext()) {
			Visit visit = iterator.next();
			try {
				boolean phiAccess = AccessCtrlMgr.getInstance().ensureReadVisitRights(visit, hasPhiFields);
				if (hasPhiFields && !phiAccess) {
					iterator.remove();
				}
			} catch (OpenSpecimenException ose) {
				if (ose.getErrorType().equals(ErrorType.USER_ERROR)) {
					iterator.remove();
				}
			}
		}

		return ResponseEvent.response(VisitDetail.from(visits));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> addVisit(RequestEvent<VisitDetail> req) {
		try {
			Visit visit = saveOrUpdateVisit(req.getPayload(), false, false);
			return ResponseEvent.response(VisitDetail.from(visit, false, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> updateVisit(RequestEvent<VisitDetail> req) {
		try {
			Visit visit = saveOrUpdateVisit(req.getPayload(), true, false);
			return ResponseEvent.response(VisitDetail.from(visit, false, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> patchVisit(RequestEvent<VisitDetail> req) {
		try {
			Visit visit = saveOrUpdateVisit(req.getPayload(), true, true);
			return ResponseEvent.response(VisitDetail.from(visit, false, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			Visit visit = getVisit(crit.getId(), crit.getName());
			AccessCtrlMgr.getInstance().ensureReadVisitRights(visit, false);
			return ResponseEvent.response(visit.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> deleteVisit(RequestEvent<CpEntityDeleteCriteria> req) {
		try {
			CpEntityDeleteCriteria crit = req.getPayload();
			Visit visit = getVisit(crit.getId(), crit.getName());
			raiseErrorIfSpecimenCentric(visit);
			AccessCtrlMgr.getInstance().ensureDeleteVisitRights(visit);

			visit.delete(!crit.isForceDelete());
			visit.setOpComments(crit.getReason());

			DeleteLogUtil.getInstance().log(visit);
			EventPublisher.getInstance().publish(new VisitSavedEvent(visit));
			return ResponseEvent.response(VisitDetail.from(visit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<VisitSpecimenDetail> collectVisitAndSpecimens(RequestEvent<VisitSpecimenDetail> req) {		
		try {
			//
			// Step 1: Create visit
			//
			VisitDetail inputVisit = req.getPayload().getVisit();
			VisitDetail savedVisit = VisitDetail.from(saveOrUpdateVisit(inputVisit, inputVisit.getId() != null, false));
			
			List<SpecimenDetail> specimens = req.getPayload().getSpecimens();
			setVisitId(savedVisit.getId(), specimens);
			
			// 
			// Step 2: Set IDs of specimens that are pre-created for the visit
			// 
			Visit visit = daoFactory.getVisitsDao().getById(savedVisit.getId());
			Map<Long, Specimen> reqSpecimenMap = visit.getSpecimens().stream()
				.filter(s -> s.getSpecimenRequirement() != null) // OPSMN-4227: Complete -> Missed -> Pending -> Complete
				.collect(Collectors.toMap(s -> s.getSpecimenRequirement().getId(), s -> s));
			setSpecimenIds(specimens, reqSpecimenMap);
			
			// 
			// Step 3: Collect specimens
			//
			RequestEvent<List<SpecimenDetail>> collectSpecimensReq = new RequestEvent<>(specimens);
			ResponseEvent<List<SpecimenDetail>> collectSpecimensResp = specimenSvc.collectSpecimens(collectSpecimensReq);
			collectSpecimensResp.throwErrorIfUnsuccessful();
			
			VisitSpecimenDetail resp = new VisitSpecimenDetail();
			resp.setVisit(savedVisit);
			resp.setSpecimens(collectSpecimensResp.getPayload());
			return ResponseEvent.response(resp);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabelPrintJobSummary> printVisitNames(RequestEvent<PrintVisitNameDetail> req) {
		LabelPrinter<Visit> printer = getLabelPrinter();
		if (printer == null) {
			return ResponseEvent.serverError(VisitErrorCode.NO_PRINTER_CONFIGURED);
		}

		PrintVisitNameDetail printDetail = req.getPayload();
		LabelPrintJob job = printer.print(PrintItem.make(getVisitsToPrint(printDetail), printDetail.getCopies()));
		if (job == null) {
			return ResponseEvent.userError(VisitErrorCode.PRINT_ERROR);
		}

		return ResponseEvent.response(LabelPrintJobSummary.from(job));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<FileDetail> getSpr(RequestEvent<FileDetail> req) {
		try {
			FileDetail input = req.getPayload();
			Visit visit = getVisit(input.getId(), input.getName());
			
			AccessCtrlMgr.getInstance().ensureReadSprRights(visit);
			
			if (StringUtils.isBlank(visit.getSprName())) {
				throw OpenSpecimenException.userError(VisitErrorCode.NO_SPR_UPLOADED);
			}
			
			File file = getSprFile(visit.getId());
			if (file == null) {
				throw OpenSpecimenException.serverError(VisitErrorCode.UNABLE_TO_LOCATE_SPR);
			}
			
			String fileExtension = file.getName().substring(file.getName().lastIndexOf('.'));
			if (isPdfType(input.getContentType()) && isTextFile(file)) {
				Map<String, Object> props = Collections.singletonMap("visit", visit);
				file = sprText2PdfGenerator.generate(file, props);
				fileExtension = ".pdf";
			}

			FileDetail fileDetail = new FileDetail();
			fileDetail.setFileOut(file);
			fileDetail.setFilename(Utility.sanitizeFilename(visit.getName()) + fileExtension);
			return ResponseEvent.response(fileDetail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> uploadSprFile(RequestEvent<SprDetail> req) {
		try {
			SprDetail detail = req.getPayload();
			Visit visit = getVisit(detail.getId(), null);
			ensureSprCanBeUploaded(visit);

			String filename = detail.getFilename();
			if (detail.isTextContent() || (detail.isPdfContent() && isExtractSprTextEnabled(visit))) {
				String sprText = getTextFromReq(detail);

				File sprFile = new File(getSprDirPath(visit.getId()) + File.separator + "spr.txt");
				FileUtils.writeStringToFile(sprFile, sprText, (String) null, false);
				
				filename = filename.substring(0, filename.lastIndexOf(".")) + ".txt";
				visit.updateSprName(filename);
			} else {
				String extension = filename.substring(filename.lastIndexOf('.'));
				File sprFile = new File(getSprDirPath(visit.getId()) + File.separator + "spr" + extension);
				FileUtils.copyInputStreamToFile(detail.getFileIn(), sprFile);
				visit.updateSprName(filename);
			}

			EventPublisher.getInstance().publish(new SprSavedEvent(visit));
			return new ResponseEvent<>(filename);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<String> updateSprText(RequestEvent<SprDetail> req) {
		try {
			SprDetail detail = req.getPayload();
			Visit visit = getVisit(detail.getId(), null);
			ensureSprCanBeUploaded(visit);
			
			File file = getSprFile(detail.getId());
			if (file == null) {
				return ResponseEvent.serverError(VisitErrorCode.UNABLE_TO_LOCATE_SPR);
			}
			
			if (!isTextFile(file)) {
				return ResponseEvent.userError(VisitErrorCode.NON_TEXT_SPR);
			}
			
			FileUtils.writeStringToFile(file, detail.getSprText(), (String) null, false);
			EventPublisher.getInstance().publish(new SprSavedEvent(visit));
			return ResponseEvent.response(detail.getSprText());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteSprFile(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			Visit visit = getVisit(crit.getId(), crit.getName());

			raiseErrorIfSpecimenCentric(visit);
			AccessCtrlMgr.getInstance().ensureDeleteSprRights(visit);
			
			if (visit.isSprLocked()) {
				return ResponseEvent.userError(VisitErrorCode.LOCKED_SPR);
			}
			
			if (StringUtils.isBlank(visit.getSprName())) {
				return ResponseEvent.userError(VisitErrorCode.NO_SPR_UPLOADED);
			}
			
			File file = getSprFile(visit.getId());
			if (file == null) {
				return ResponseEvent.serverError(VisitErrorCode.UNABLE_TO_LOCATE_SPR);
			}
		
			boolean isFileDeleted = file.delete();
			if (isFileDeleted) {
				visit.setSprName(null);
			}

			EventPublisher.getInstance().publish(new SprSavedEvent(visit));
			return ResponseEvent.response(isFileDeleted);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@PlusTransactional
	@Override
	public ResponseEvent<SprLockDetail> updateSprLockStatus(RequestEvent<SprLockDetail> req) {
		SprLockDetail detail = req.getPayload();
		Visit visit = getVisit(detail.getVisitId(), detail.getVisitName());
		raiseErrorIfSpecimenCentric(visit);
		
		if (detail.isLocked()) {
			AccessCtrlMgr.getInstance().ensureLockSprRights(visit);
		} else {
			AccessCtrlMgr.getInstance().ensureUnlockSprRights(visit);
		}
		
		File sprFile = getSprFile(visit.getId());
		if (sprFile == null) {
			return ResponseEvent.userError(VisitErrorCode.NO_SPR_UPLOADED);
		}
		
		if (!isTextFile(sprFile)) {
			return ResponseEvent.userError(VisitErrorCode.NON_TEXT_SPR);
		}
		
		visit.setSprLocked(detail.isLocked());
		EventPublisher.getInstance().publish(new SprSavedEvent(visit));
		return ResponseEvent.response(detail);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<MatchedVisitDetail>> getMatchingVisits(RequestEvent<VisitSearchDetail> req) {
		try {
			return ResponseEvent.response(getVisitsLookup().getVisits(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public List<LabelTokenDetail> getPrintLabelTokens() {
		return LabelTokenDetail.from("print_", getLabelPrinter().getTokens());
	}

	@SuppressWarnings("unchecked")
	@Override
	public LabelPrinter<Visit> getLabelPrinter() {
		return Visit.getLabelPrinter();
	}

	@PlusTransactional
	@Override
	public List<Visit> getVisitsByName(List<String> visitNames) {
		List<Visit> visits = daoFactory.getVisitsDao().getByName(visitNames);
		for (Visit visit : visits) {
			AccessCtrlMgr.getInstance().ensureReadVisitRights(visit);
		}

		return visits;
	}

	@PlusTransactional
	@Override
	public List<Visit> getSpecimenVisits(List<String> specimenLabels) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return Collections.emptyList();
		}

		SpecimenListCriteria crit = new SpecimenListCriteria()
			.labels(specimenLabels)
			.siteCps(siteCps)
			.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());
		return daoFactory.getSpecimenDao().getSpecimenVisits(crit);
	}

	public Visit addVisit(VisitDetail input, boolean checkPermission) {
		if (checkPermission) {
			return saveOrUpdateVisit(input, false, false);
		} else {
			return saveOrUpdateVisit(visitFactory.createVisit(input), null);
		}
	}

	@Override
	public String getObjectName() {
		return Visit.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		Map<String, Object> ids = daoFactory.getVisitsDao().getCprVisitIds(key, value);
		if (ids == null || ids.isEmpty()) {
			throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, value);
		}

		return ids;
	}

	@Override
	public String getAuditTable() {
		return "CAT_SPECIMEN_COLL_GROUP_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		AccessCtrlMgr.getInstance().ensureReadVisitRights(id);
	}

	@Override
	public void afterPropertiesSet() {
		cfgSvc.registerChangeListener(ConfigParams.MODULE, (name, value) -> { visitsLookup = null; });
		exportSvc.registerObjectsGenerator("visit", this::getVisitsGenerator);
	}

	private List<VisitDetail> getVisits0(VisitsListCriteria crit) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadVisitSiteCps(crit.cpId());
		if (siteCps != null) {
			if (siteCps.isEmpty()) {
				return Collections.emptyList();
			}

			crit.siteCps(new ArrayList<>(siteCps));
		}

		List<Visit> visits = daoFactory.getVisitsDao().getVisitsList(crit);
		Map<Long, Boolean> hasPhiAccess = new HashMap<>(); // {cprId -> phiAccess}
		List<VisitDetail> result = new ArrayList<>();
		for (Visit visit : visits) {
			Long cprId = visit.getRegistration().getId();
			if (!hasPhiAccess.containsKey(cprId)) {
				try {
					boolean phiAccess = AccessCtrlMgr.getInstance().ensureReadVisitRights(visit, true);
					hasPhiAccess.put(cprId, phiAccess);
				} catch (OpenSpecimenException e) {
					if (e.containsError(RbacErrorCode.ACCESS_DENIED)) {
						continue;
					}

					throw e;
				}
			}

			boolean excludePhi = !hasPhiAccess.get(cprId);
			result.add(VisitDetail.from(visit, crit.includeExtensions(), excludePhi));
		}

		return result;
	}

	private Visit saveOrUpdateVisit(VisitDetail input, boolean update, boolean partial) {
		Visit existing = null;
		if (update) {
			existing = getVisit(input.getId(), input.getName());
			AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(existing);
		}

		Visit visit;
		if (partial) {
			visit = visitFactory.createVisit(existing, input);
		} else {
			visit = visitFactory.createVisit(input);
		}

		raiseErrorIfSpecimenCentric(visit);
		AccessCtrlMgr.getInstance().ensureCreateOrUpdateVisitRights(visit);
		return saveOrUpdateVisit(visit, existing);
	}

	private Visit saveOrUpdateVisit(Visit visit, Visit existing) {
		raiseErrorIfSpecimenCentric(visit);
		EventPublisher.getInstance().publish(new VisitPreSaveEvent(existing, visit));

		String prevVisitStatus = existing != null ? existing.getStatus() : null;
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidAndUniqueVisitName(existing, visit, ose);
		ose.checkAndThrow();

		if (existing == null) {
			if (visit.isDeleted()) {
				throw OpenSpecimenException.userError(VisitErrorCode.CREATE_DISABLED_NA);
			}

			if (visit.isEventClosed()) {
				throw OpenSpecimenException.userError(CpeErrorCode.CLOSED, visit.getCpEvent().getEventLabel());
			}

			if (visit.isMissed()) {
				visit.createMissedSpecimens();
			} else if (visit.isNotCollected()) {
				visit.createNotCollectedSpecimens();
			}
			
			existing = visit;
		} else {
			if (visit.isDeleted()) {
				AccessCtrlMgr.getInstance().ensureDeleteVisitRights(existing);
			}

			existing.update(visit);
		}
		
		existing.setNameIfEmpty();
		daoFactory.getVisitsDao().saveOrUpdate(existing);
		existing.addOrUpdateExtension();
		if (existing.isDeleted()) {
			DeleteLogUtil.getInstance().log(existing);
		}

		EventPublisher.getInstance().publish(new VisitSavedEvent(existing));

		//
		// this might result in creation of pending specimens
		// and associated supplies consumption
		// therefore has to be invoked after emitting the visit saved event
		//
		existing.printLabels(prevVisitStatus);
		return existing;
	}

	private Visit getVisit(Long visitId, String visitName) {
		Visit visit = null;
		Object key = null;
		
		if (visitId != null) {
			visit = daoFactory.getVisitsDao().getById(visitId);
			key = visitId;
		} else if (StringUtils.isNotBlank(visitName)) {
			visit = daoFactory.getVisitsDao().getByName(visitName);
			key = visitName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.NAME_REQUIRED);
		} else if (visit == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, key);
		}
		
		return visit;
	}
	
	private void ensureValidAndUniqueVisitName(Visit existing, Visit visit, OpenSpecimenException ose) {
		if (existing != null && 
			StringUtils.isNotBlank(existing.getName()) && 
			existing.getName().equals(visit.getName())) {
			return;
		}
		
		CollectionProtocol cp = visit.getCollectionProtocol();
		String name = visit.getName();
		
		if (StringUtils.isBlank(name)) {
			if (cp.isManualVisitNameEnabled() && visit.isCompleted()) {
				ose.addError(VisitErrorCode.NAME_REQUIRED);
			}
			
			return;
		}
		
		if (StringUtils.isNotBlank(cp.getVisitNameFormat())) {
			//
			// Visit name format is specified
			//
			
			if (!cp.isManualVisitNameEnabled()) {
				ose.addError(VisitErrorCode.MANUAL_NAME_NOT_ALLOWED);
				return;
			}


			if (!visitNameGenerator.validate(cp.getVisitNameFormat(), visit, name)) {
				ose.addError(VisitErrorCode.INVALID_NAME, name);
				return;
			}
		}
		
		if (daoFactory.getVisitsDao().getByName(name) != null) {
			ose.addError(VisitErrorCode.DUP_NAME, name);
		}		
	}
	
	private void setVisitId(Long visitId, List<SpecimenDetail> specimens) {
		if (CollectionUtils.isEmpty(specimens)) {
			return;
		}
		
		for (SpecimenDetail specimen : specimens) {
			specimen.setVisitId(visitId);
			setVisitId(visitId, specimen.getChildren());
		}
	}

	private File getSprFile(Long visitId) {
		String path = getSprDirPath(visitId);
		File dir = new File(path);
		if (!dir.exists()) {
			return null;
		}
		
		File[] files = dir.listFiles();
		if (files.length == 0) {
			return null;
		}
		
		return files[0];
	}
	
	private String getSprDirPath(Long visitId) {
		String path = cfgSvc.getStrSetting(ConfigParams.MODULE, ConfigParams.SPR_DIR, getDefaultVisitSprDir());
		return path + File.separator + visitId;
	}
	
	private String getTextFromReq(SprDetail detail) {
		String text = Utility.getString(detail.getFileIn(), detail.getContentType());
		
		DocumentDeIdentifier deIdentifier = getSprDeIdentifier();
		if (deIdentifier != null) {
			Map<String, Object> props = Collections.singletonMap("visitId", detail.getId());
			text = deIdentifier.deIdentify(text, props);
		}
		
		return text;
	}

	private DocumentDeIdentifier getSprDeIdentifier() {
		String sprDeidentifierBean = cfgSvc.getStrSetting(ConfigParams.MODULE, ConfigParams.SPR_DEIDENTIFIER);
		if (StringUtils.isBlank(sprDeidentifierBean)) {
			return null;
		}
		
		return (DocumentDeIdentifier) OpenSpecimenAppCtxProvider
				.getAppCtx()
				.getBean(sprDeidentifierBean);
	}
	
	private static String getDefaultVisitSprDir() {
		if (StringUtils.isBlank(defaultVisitSprDir)) {
			defaultVisitSprDir = ConfigUtil.getInstance().getDataDir() + File.separator + "visit-sprs";
		}
		return defaultVisitSprDir;
	}
	
	private void ensureUpdateSprRights(Visit visit) {
		if (visit.isSprLocked()) {
			throw OpenSpecimenException.userError(VisitErrorCode.LOCKED_SPR);
		}
		
		AccessCtrlMgr.getInstance().ensureCreateOrUpdateSprRights(visit);
	}
	
	private boolean isTextFile(File file) {
		String contentType = Utility.getContentType(file);
		return contentType.startsWith("text/") || contentType.equals("application/pdf");
	}
	
	private boolean isPdfType(String type) {
		return StringUtils.isNotBlank(type) && type.equals("pdf");
	}
	
	private void setSpecimenIds(List<SpecimenDetail> inputSpecimens, Map<Long, Specimen> reqSpecimenMap) {
		if (reqSpecimenMap.isEmpty()) {
			return;
		}
		
		for (SpecimenDetail specimenDetail : inputSpecimens) {
			if (specimenDetail.getReqId() != null) {
				Specimen specimen = reqSpecimenMap.get(specimenDetail.getReqId());
				if (specimen == null) {
					//
					// Anticipated specimen not yet created; therefore none of its children either
					//
					continue;
				}

				specimenDetail.setId(specimen.getId());
				if (StringUtils.isBlank(specimenDetail.getLabel())) {
					specimenDetail.setLabel(specimen.getLabel());
				}

				if (StringUtils.isBlank(specimenDetail.getAdditionalLabel())) {
					specimenDetail.setAdditionalLabel(specimen.getAdditionalLabel());
				}

				if (StringUtils.isBlank(specimenDetail.getBarcode())) {
					specimenDetail.setBarcode(specimen.getBarcode());
				}
			}

			if (CollectionUtils.isNotEmpty(specimenDetail.getChildren())) {
				setSpecimenIds(specimenDetail.getChildren(), reqSpecimenMap);
			}
		}
	}

	private List<Visit> getVisitsToPrint(PrintVisitNameDetail printDetail) {
		List<Long> ids = printDetail.getVisitIds();
		List<String> names = printDetail.getVisitNames();

		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadVisitSiteCps(null);
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		VisitsListCriteria crit = new VisitsListCriteria().siteCps(siteCps != null ? new ArrayList<>(siteCps) : null);

		List<Visit> visits = null;
		if (CollectionUtils.isNotEmpty(ids)) {
			visits = daoFactory.getVisitsDao().getVisitsList(crit.ids(ids)).stream()
				.sorted(Comparator.comparingInt((v) -> ids.indexOf(v.getId())))
				.collect(Collectors.toList());
		} else if (CollectionUtils.isNotEmpty(names)) {
			visits = daoFactory.getVisitsDao().getVisitsList(crit.names(names)).stream()
				.sorted(Comparator.comparingInt((v) -> names.indexOf(v.getName())))
				.collect(Collectors.toList());
		}

		if (CollectionUtils.isEmpty(visits)) {
			throw OpenSpecimenException.userError(VisitErrorCode.NO_VISITS_TO_PRINT);
		}

		return visits;
	}

	private boolean isExtractSprTextEnabled(Visit visit) {
		Boolean extractSprText = visit.getCollectionProtocol().getExtractSprText();
		if (extractSprText == null) {
			extractSprText = ConfigUtil.getInstance().getBoolSetting(
				ConfigParams.MODULE, ConfigParams.EXTRACT_SPR_TEXT, false);
		}

		return extractSprText;
	}

	private void ensureSprCanBeUploaded(Visit visit) {
		ensureCompletedVisit(visit);
		raiseErrorIfSpecimenCentric(visit);
		ensureUpdateSprRights(visit);
	}

	private void ensureCompletedVisit(Visit visit) {
		if (!visit.isCompleted()) {
			throw OpenSpecimenException.userError(VisitErrorCode.COMPL_VISIT_REQ);
		}
	}

	private void raiseErrorIfSpecimenCentric(Visit visit) {
		if (visit.getCollectionProtocol().isSpecimenCentric()) {
			throw OpenSpecimenException.userError(CpErrorCode.OP_NOT_ALLOWED_SC, visit.getCollectionProtocol().getShortTitle());
		}
	}

	private VisitsLookup getVisitsLookup() {
		if (visitsLookup == null) {
			initVisitsLookup(ConfigUtil.getInstance().getStrSetting(ConfigParams.MODULE, ConfigParams.VISITS_LOOKUP_FLOW, null));
		}

		return visitsLookup;
	}

	private void initVisitsLookup(String lookupFlow) {
		if (StringUtils.isBlank(lookupFlow)) {
			visitsLookup = defaultVisitsLookup;
			return;
		}

		VisitsLookup result = null;
		try {
			lookupFlow = lookupFlow.trim();
			if (lookupFlow.startsWith("bean:")) {
				result = OpenSpecimenAppCtxProvider.getBean(lookupFlow.substring("bean:".length()).trim());
			} else {
				String className = lookupFlow;
				if (lookupFlow.startsWith("class:")) {
					className = lookupFlow.substring("class:".length()).trim();
				}

				result = (VisitsLookup) BeanUtils.instantiateClass(Class.forName(className));
			}
		} catch (Exception e) {
			logger.error("Invalid visits lookup flow configuration setting: " + lookupFlow, e);
		}

		if (result == null) {
			throw OpenSpecimenException.userError(VisitErrorCode.INVALID_LOOKUP_FLOW, lookupFlow);
		}

		visitsLookup = result;
	}

	private Function<ExportJob, List<? extends Object>> getVisitsGenerator() {
		return new Function<ExportJob, List<? extends Object>>() {
			private boolean endOfVisits;

			private boolean paramsInited;

			private boolean pdfReports;

			private VisitsListCriteria crit;

			private Long lastId;

			@Override
			public List<? extends Object> apply(ExportJob exportJob) {
				initParams(exportJob);

				if (endOfVisits) {
					return Collections.emptyList();
				}

				List<Visit> visits = daoFactory.getVisitsDao().getVisitsList(crit.lastId(lastId));
				if (CollectionUtils.isNotEmpty(crit.names()) || visits.size() < 100) {
					endOfVisits = true;
				}

				List<VisitDetail> records = new ArrayList<>();
				for (Visit visit : visits) {
					lastId = visit.getId();

					try {
						boolean hasPhi = AccessCtrlMgr.getInstance().ensureReadVisitRights(visit, true);
						VisitDetail detail = VisitDetail.from(visit, false, !hasPhi);
						records.add(detail);
						if (!hasPhi) {
							continue;
						}

						File file = getSprFile(visit.getId());
						if (file == null) {
							continue;
						}

						String fileExtension = file.getName().substring(file.getName().lastIndexOf('.'));
						if (pdfReports && isTextFile(file)) {
							file = sprText2PdfGenerator.generate(file, Collections.singletonMap("visit", visit));
							fileExtension = ".pdf";
						}

						detail.setSprFile(file);
						detail.setSprName(visit.getName() + fileExtension);
					} catch (OpenSpecimenException ose) {
						if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
							logger.error("Encountered error exporting visit record", ose);
						}
					}
				}

				return records;
			}

			private void initParams(ExportJob job) {
				if (paramsInited) {
					return;
				}

				Map<String, String> params = job.getParams();
				if (params == null) {
					params = Collections.emptyMap();
				}

				Long cpId = getCpId(params);
				List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps(cpId, false);
				if (siteCps != null && siteCps.isEmpty()) {
					endOfVisits = true;
				} else if (!AccessCtrlMgr.getInstance().hasVisitEximRights(cpId)) {
					endOfVisits = true;
				} else {
					crit = new VisitsListCriteria()
						.cpId(cpId)
						.names(Utility.csvToStringList(params.get("visitNames")))
						.siteCps(siteCps)
						.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

					if (!crit.names().isEmpty()) {
						crit.limitItems(false);
					} else {
						crit.limitItems(true).maxResults(100);
					}
				}

				pdfReports = job.getParams() != null && StringUtils.equals(job.getParams().get("sprFileType"), "pdf");
				paramsInited = true;
			}

			private Long getCpId(Map<String, String> params) {
				Long cpId = null;

				String cpIdStr = params.get("cpId");
				if (StringUtils.isNotBlank(cpIdStr)) {
					try {
						cpId = Long.parseLong(cpIdStr);
						if (cpId == -1L) {
							cpId = null;
						}
					} catch (Exception e) {
						logger.error("Invalid CP ID: " + cpIdStr, e);
					}
				}

				return cpId;
			}
		};
	}
}
