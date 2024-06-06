
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.AnonymizeEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentResponses;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.ConsentTierResponse;
import com.krishagni.catissueplus.core.biospecimen.domain.CprSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolRegistrationFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentResponsesFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.BulkRegistrationsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpEntityDeleteCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.CprSummary;
import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipantsList;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantRegistrationsList;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.RegistrationQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSpecimensQueryCriteria;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.Anonymizer;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenKitService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.BulkEntityDetail;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.ConfigurationServiceImpl;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class CollectionProtocolRegistrationServiceImpl implements CollectionProtocolRegistrationService, ObjectAccessor, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(CollectionProtocolRegistrationServiceImpl.class);

	private DaoFactory daoFactory;

	private CollectionProtocolRegistrationFactory cprFactory;
	
	private ConsentResponsesFactory consentResponsesFactory;
	
	private ParticipantService participantService;

	private VisitService visitSvc;
	
	private ConfigurationServiceImpl cfgSvc;

	private LabelGenerator labelGenerator;

	private Anonymizer<CollectionProtocolRegistration> anonymizer;

	private SpecimenKitService specimenKitSvc;

	private ExportService exportSvc;

	private ThreadPoolTaskExecutor taskExecutor;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCprFactory(CollectionProtocolRegistrationFactory cprFactory) {
		this.cprFactory = cprFactory;
	}
	
	public void setConsentResponsesFactory(ConsentResponsesFactory consentResponsesFactory) {
		this.consentResponsesFactory = consentResponsesFactory;
	}

	public void setParticipantService(ParticipantService participantService) {
		this.participantService = participantService;
	}

	public void setVisitSvc(VisitService visitSvc) {
		this.visitSvc = visitSvc;
	}

	public void setCfgSvc(ConfigurationServiceImpl cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setLabelGenerator(LabelGenerator labelGenerator) {
		this.labelGenerator = labelGenerator;
	}

	public void setAnonymizer(Anonymizer<CollectionProtocolRegistration> anonymizer) {
		this.anonymizer = anonymizer;
	}

	public void setSpecimenKitSvc(SpecimenKitService specimenKitSvc) {
		this.specimenKitSvc = specimenKitSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setTaskExecutor(ThreadPoolTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> getRegistration(RequestEvent<RegistrationQueryCriteria> req) {				
		try {			
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getCpShortTitle(), crit.getPpid(), crit.getEmpi());
			boolean allowPhiAccess = AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);

			List<CollectionProtocolRegistration> otherCprs = getOtherCprs(cpr);
			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, !allowPhiAccess, otherCprs));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> createRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistration(req.getPayload(), null, true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> updateRegistration(RequestEvent<CollectionProtocolRegistrationDetail> req) {
		try {
			return ResponseEvent.response(updateRegistration(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolRegistrationDetail>> bulkUpdateRegistrations(RequestEvent<BulkEntityDetail<CollectionProtocolRegistrationDetail>> req) {
		try {
			BulkEntityDetail<CollectionProtocolRegistrationDetail> buDetail = req.getPayload();
			CollectionProtocolRegistrationDetail inputCpr = buDetail.getDetail();

			List<CollectionProtocolRegistrationDetail> savedCprs = new ArrayList<>();
			for (Long id : buDetail.getIds()) {
				inputCpr.setId(id);
				if (inputCpr.getParticipant() != null) {
					inputCpr.getParticipant().setId(null);
					inputCpr.getParticipant().setCpId(null);
				}

				savedCprs.add(updateRegistration(inputCpr));
			}

			return ResponseEvent.response(savedCprs);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolRegistrationDetail>> bulkRegistration(RequestEvent<BulkRegistrationsDetail> req) {
		try {
			BulkRegistrationsDetail detail = req.getPayload();
			String collectionSite = detail.getCollectionSite();
			if (StringUtils.isBlank(collectionSite)) {
				throw OpenSpecimenException.userError(CprErrorCode.COLLECTION_SITE_REQUIRED);
			}

			Date regDate = Calendar.getInstance().getTime();
			List<CollectionProtocolRegistration> cprs = new ArrayList<>();
			List<CollectionProtocolRegistrationDetail> result = new ArrayList<>();
			for (int i = 0; i < detail.getRegCount(); i++) {
				CollectionProtocolRegistration cpr = registerParticipant(detail, regDate, collectionSite, i == 0);
				cprs.add(cpr);
				result.add(CollectionProtocolRegistrationDetail.from(cpr, false));
			}

			if (detail.getKitDetail() != null && !cprs.isEmpty()) {
				Set<Long> eventIds = Utility.stream(detail.getEvents())
					.map(CollectionProtocolEventDetail::getId)
					.filter(Objects::nonNull)
					.collect(Collectors.toSet());

				Set<String> eventLabels = Utility.stream(detail.getEvents())
					.map(CollectionProtocolEventDetail::getEventLabel)
					.filter(Objects::nonNull)
					.map(String::toLowerCase)
					.collect(Collectors.toSet());

				List<Specimen> spmns = cprs.stream().flatMap(cpr -> cpr.getOrderedVisits().stream())
					.filter(visit ->
						visit.getCpEvent() != null &&
						eventIds.contains(visit.getCpEvent().getId()) ||
						eventLabels.contains(visit.getCpEvent().getEventLabel().toLowerCase())
					)
					.flatMap(visit -> visit.getSpecimens().stream())
					.filter(Specimen::isPrePrintEnabled)
					.collect(Collectors.toList());
				detail.getKitDetail().setCpId(result.get(0).getCpId());
				detail.getKitDetail().setReceivingSite(collectionSite);
				specimenKitSvc.createSpecimenKit(detail.getKitDetail(), spmns);
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception ex) {
			return ResponseEvent.serverError(ex);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> anonymize(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria detail = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(detail.getCprId(), detail.getCpId(), null, detail.getPpid(), detail.getEmpi());
			raiseErrorIfSpecimenCentric(cpr);
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
			anonymizer.anonymize(cpr);

			AnonymizeEvent event = new AnonymizeEvent();
			event.setCpr(cpr);
			event.setAnonymizedBy(AuthUtil.getCurrentUser());
			event.setAnonymizeTime(Calendar.getInstance().getTime());
			daoFactory.getAnonymizeEventDao().saveOrUpdate(event);

			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DependentEntityDetail>> getDependentEntities(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			AccessCtrlMgr.getInstance().ensureReadCprRights(cpr);
			return ResponseEvent.response(cpr.getDependentEntities());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolRegistrationDetail> deleteRegistration(RequestEvent<CpEntityDeleteCriteria> req) {
		try {
			CpEntityDeleteCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getId(), null, crit.getCpShortTitle(), crit.getName(), null);
			raiseErrorIfSpecimenCentric(cpr);

			AccessCtrlMgr.getInstance().ensureDeleteCprRights(cpr);
			deleteCpr(cpr, crit.getReason(), crit.isForceDelete(), crit.booleanParam("checkOnlyCollectedSpmns"));
			return ResponseEvent.response(CollectionProtocolRegistrationDetail.from(cpr, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<BulkDeleteEntityResp<CprSummary>> deleteRegistrations(RequestEvent<BulkDeleteEntityOp> req) {
		try {
			BulkDeleteEntityOp crit = req.getPayload();

			Set<Long> cprIds = crit.getIds();
			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getByIds(cprIds);
			if (crit.getIds().size() != cprs.size()) {
				cprs.forEach(cpr -> cprIds.remove(cpr.getId()));
				throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, cprIds, cprIds.size());
			}

			for (CollectionProtocolRegistration cpr : cprs) {
				AccessCtrlMgr.getInstance().ensureDeleteCprRights(cpr);
				raiseErrorIfSpecimenCentric(cpr);
			}

			boolean completed = deleteCprs(cprs, crit.getReason(), crit.isForceDelete());

			BulkDeleteEntityResp<CprSummary> resp = new BulkDeleteEntityResp<>();
			resp.setCompleted(completed);
			resp.setEntities(CprSummary.from(cprs, false));
			return ResponseEvent.response(resp);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<File> getConsentForm(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			Long cprId = req.getPayload().getCprId();
			CollectionProtocolRegistration existing = daoFactory.getCprDao().getById(cprId);
			if (existing == null) {
				return ResponseEvent.userError(CprErrorCode.M_NOT_FOUND, cprId, 1);
			}
			
			AccessCtrlMgr.getInstance().ensureReadConsentRights(existing);

			CollectionProtocol consentsCp = existing.getCollectionProtocol().getConsentsSource();
			if (consentsCp != null) {
				CollectionProtocolRegistration consentsCpr = daoFactory.getCprDao().getCprByParticipantId(consentsCp.getId(), existing.getParticipant().getId());
				if (consentsCpr == null) {
					return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
				}

				existing = consentsCpr;
			}

			String fileName = existing.getSignedConsentDocumentName();
			if (StringUtils.isBlank(fileName)) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			File file = new File(ConfigParams.getConsentsDirPath() + fileName);
			if (!file.exists()) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			return ResponseEvent.response(file);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<String> uploadConsentForm(RequestEvent<FileDetail> req) {
		OutputStream outputStream = null;
		try {
			FileDetail detail = req.getPayload();

			Long cprId = detail.getId();
			String cpShortTitle = null, ppid = null, empi = null;
			if (detail.getObjectProps() != null) {
				cpShortTitle = (String) detail.getObjectProps().get("cpShortTitle");
				ppid         = (String) detail.getObjectProps().get("ppid");
				empi         = (String) detail.getObjectProps().get("empi");
			}

			CollectionProtocolRegistration existing = getCpr(cprId, null, cpShortTitle, ppid, empi);
			CollectionProtocol consentsCp = existing.getCollectionProtocol().getConsentsSource();
			if (consentsCp != null) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_SOURCED, consentsCp.getShortTitle());
			}

			raiseErrorIfSpecimenCentric(existing);
			AccessCtrlMgr.getInstance().ensureUpdateConsentRights(existing);
			
			if (existing.getCollectionProtocol().isConsentsWaived()) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_WAIVED, existing.getCollectionProtocol().getShortTitle());
			}
			
			String newFileName = UUID.randomUUID() + "_" + detail.getFilename();
			File newFile = new File(ConfigParams.getConsentsDirPath() + newFileName);
			
			outputStream = new FileOutputStream(newFile);
			IOUtils.copy(detail.getFileIn(), outputStream);
			
			String oldFileName = existing.getSignedConsentDocumentName();
			if (StringUtils.isNotBlank(oldFileName)) {
				File oldFile = new File(ConfigParams.getConsentsDirPath() + oldFileName);
				if (oldFile.exists()) {
					oldFile.delete();
				}
 			}
			existing.setSignedConsentDocumentName(newFileName);
			return ResponseEvent.response(detail.getFilename());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			IOUtils.closeQuietly(outputStream);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> deleteConsentForm(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			Long cprId = req.getPayload().getCprId();
			CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(cprId);
			if (cpr == null) {
				return ResponseEvent.userError(CprErrorCode.M_NOT_FOUND, cprId, 1);
			}

			CollectionProtocol consentsCp = cpr.getCollectionProtocol().getConsentsSource();
			if (consentsCp != null) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_SOURCED, consentsCp.getShortTitle());
			}

			raiseErrorIfSpecimenCentric(cpr);
			AccessCtrlMgr.getInstance().ensureUpdateConsentRights(cpr);

			String fileName = cpr.getSignedConsentDocumentName();
			if (StringUtils.isBlank(fileName)) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			File file = new File(ConfigParams.getConsentsDirPath() + fileName);
			if (!file.exists()) {
				return ResponseEvent.userError(CprErrorCode.CONSENT_FORM_NOT_FOUND);
			}
			
			boolean isFileDeleted = file.delete();
			if (isFileDeleted) {
				cpr.setSignedConsentDocumentName(null);
			} 

			return ResponseEvent.response(isFileDeleted);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentDetail> getConsents(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			AccessCtrlMgr.getInstance().ensureReadConsentRights(cpr);

			CollectionProtocol cp = cpr.getCollectionProtocol();
			CollectionProtocol consentsCp = cp.getConsentsSource();
			if (consentsCp != null) {
				CollectionProtocolRegistration consentsCpr = daoFactory.getCprDao().getCprByParticipantId(consentsCp.getId(), cpr.getParticipant().getId());
				ConsentDetail output = new ConsentDetail();
				if (consentsCpr != null) {
					output = ConsentDetail.fromCpr(consentsCpr);
				}

				output.setCpShortTitle(cpr.getCpShortTitle());
				output.setPpid(cpr.getPpid());
				return ResponseEvent.response(output);
			}

			return ResponseEvent.response(ConsentDetail.fromCpr(cpr));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ConsentDetail> saveConsents(RequestEvent<ConsentDetail> req) {
		try {
			ConsentDetail consentDetail = req.getPayload();

			CollectionProtocolRegistration existing = getCpr(consentDetail.getCprId(),
				consentDetail.getCpId(), consentDetail.getCpShortTitle(), consentDetail.getPpid(), consentDetail.getEmpi());
			CollectionProtocol consentsCp = existing.getCollectionProtocol().getConsentsSource();
			if (consentsCp != null) {
				return ResponseEvent.userError(CpErrorCode.CONSENTS_SOURCED, consentsCp.getShortTitle());
			}

			raiseErrorIfSpecimenCentric(existing);

			AccessCtrlMgr.getInstance().ensureUpdateConsentRights(existing);
			
			ConsentResponses consentResponses = consentResponsesFactory.createConsentResponses(existing, consentDetail);
			existing.updateConsents(consentResponses);

			Map<String, String> responses = consentResponses.getConsentResponses().stream()
				.filter(r -> r.getResponse() != null)
				.collect(Collectors.toMap(
					ConsentTierResponse::getStatementCode,
					r -> r.getResponse().getValue()
				));
			EventPublisher.getInstance().publish(new ConsentSavedEvent(existing, responses));

			return ResponseEvent.response(ConsentDetail.fromCpr(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<VisitDetail>> getVisits(RequestEvent<VisitsListCriteria> req) {
		try {
			VisitsListCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(req.getPayload().cprId(), null, null);
			boolean hasPhiAccess = AccessCtrlMgr.getInstance().ensureReadVisitRights(cpr, true);

			//
			// Step 1: Fetch all created visit records along with their custom fields and stats
			//
			Collection<Visit> visits = cpr.getVisits();
			DeObject.createExtensions(true, Visit.EXTN, cpr.getCollectionProtocol().getId(), visits);
			Map<Long, VisitDetail> visitsMap = visits.stream()
				.collect(Collectors.toMap(Visit::getId, v -> VisitDetail.from(v, false, !hasPhiAccess)));
			if (crit.includeStat()) {
				daoFactory.getVisitsDao().loadCreatedVisitStats(visitsMap);
			}

			//
			// Step 2: Fetch anticipated visits and their details
			//
			Set<Long> occurredEvents = visits.stream()
				.map(Visit::getCpEvent).filter(Objects::nonNull).map(CollectionProtocolEvent::getId)
				.collect(Collectors.toSet());
			Set<CollectionProtocolEvent> unoccurredEvents = cpr.getCollectionProtocol().getCollectionProtocolEvents()
				.stream().filter(cpe -> !cpe.isClosed() && !occurredEvents.contains(cpe.getId()))
				.collect(Collectors.toSet());
			Map<Long, VisitDetail> anticipatedVisitsMap = unoccurredEvents.stream()
				.collect(Collectors.toMap(CollectionProtocolEvent::getId, VisitDetail::from));
			if (crit.includeStat()) {
				daoFactory.getVisitsDao().loadAnticipatedVisitStats(anticipatedVisitsMap);
			}

			//
			// Step 3: Merge, set anticipated visit dates, sort and return
			//
			List<VisitDetail> result = new ArrayList<>(visitsMap.values());
			result.addAll(anticipatedVisitsMap.values());
			VisitDetail.setAnticipatedVisitDates(cpr.getRegistrationDate(), result);

			result.sort(crit.sortByDates() ? VisitSummary::compareDates : VisitSummary::compareTo);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<VisitDetail> getLatestVisit(RequestEvent<RegistrationQueryCriteria> req) {
		try {
			RegistrationQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), crit.getCpId(), crit.getPpid());
			AccessCtrlMgr.getInstance().ensureReadVisitRights(cpr, false);

			Visit visit = daoFactory.getVisitsDao().getLatestVisit(cpr.getId());
			return ResponseEvent.response(visit != null ? VisitDetail.from(visit) : null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenDetail>> getSpecimens(RequestEvent<VisitSpecimensQueryCriteria> req) {
		try {
			VisitSpecimensQueryCriteria crit = req.getPayload();
			CollectionProtocolRegistration cpr = getCpr(crit.getCprId(), null, null);
			boolean phiAccess = false, excludeChildren = true;

			try {
				phiAccess = AccessCtrlMgr.getInstance().ensureReadSpecimenRights(cpr, true);
				excludeChildren = false;
			} catch (OpenSpecimenException ose) {
				if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
					throw ose;
				}

				phiAccess = AccessCtrlMgr.getInstance().ensureReadPrimarySpecimenRights(cpr, true);
			}

			List<SpecimenDetail> specimens = Collections.emptyList();			
			if (crit.getVisitId() != null || crit.getEventId() == null) {
				specimens = getSpecimensByVisit(cpr, crit.getVisitId(), !phiAccess, excludeChildren);
			} else if (crit.getEventId() != null) {
				specimens = getAnticipatedSpecimens(crit.getEventId(), excludeChildren);
			}

			return ResponseEvent.response(specimens);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantRegistrationsList> createRegistrations(RequestEvent<ParticipantRegistrationsList> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistrations(req.getPayload(), false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantRegistrationsList> updateRegistrations(RequestEvent<ParticipantRegistrationsList> req) {
		try {
			return ResponseEvent.response(saveOrUpdateRegistrations(req.getPayload(), true));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public String getObjectName() {
		return CollectionProtocolRegistration.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getCprDao().getCprIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_COLL_PROT_REG_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		AccessCtrlMgr.getInstance().ensureReadCprRights(id);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		exportSvc.registerObjectsGenerator("cpr", this::getCprsGenerator);
		exportSvc.registerObjectsGenerator("consent", this::getConsentsGenerator);
	}

	private CollectionProtocolRegistrationDetail updateRegistration(CollectionProtocolRegistrationDetail input) {
		String empi = input.getParticipant() != null ? input.getParticipant().getEmpi() : null;
		CollectionProtocolRegistration existing = getCpr(input.getId(), input.getCpId(), input.getCpShortTitle(), input.getPpid(), empi);
		return saveOrUpdateRegistration(input, existing, true);
	}

	private CollectionProtocolRegistrationDetail saveOrUpdateRegistration(
		CollectionProtocolRegistrationDetail input,
		CollectionProtocolRegistration existing,
		boolean saveParticipant) {

		CollectionProtocolRegistration cpr = saveOrUpdateRegistration(input, existing, null, saveParticipant);
		return CollectionProtocolRegistrationDetail.from(cpr, false);
	}

	private CollectionProtocolRegistration saveOrUpdateRegistration(
		CollectionProtocolRegistrationDetail input,
		CollectionProtocolRegistration existing,
		String collectionSite,
		boolean saveParticipant) {

		CollectionProtocolRegistration cpr = cprFactory.createCpr(existing, input);
		raiseErrorIfSpecimenCentric(cpr);

		if (existing == null) {
			AccessCtrlMgr.getInstance().ensureCreateCprRights(cpr);
		} else {
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(cpr);
		}

		return saveOrUpdateRegistration(cpr, existing, collectionSite, saveParticipant);
	}

	private CollectionProtocolRegistration saveOrUpdateRegistration(
		CollectionProtocolRegistration cpr,
		CollectionProtocolRegistration existing,
		String collectionSite,
		boolean saveParticipant) {

		raiseErrorIfSpecimenCentric(cpr);

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidAndUniquePpid(existing, cpr, ose);
		ensureUniqueBarcode(existing, cpr, ose);
		if (existing == null) {
			if (cpr.isDeleted()) {
				ose.addError(CprErrorCode.CREATE_DISABLED_NA);
			} else {
				ensureUniqueParticipantReg(cpr, ose);
			}
		}
		
		ose.checkAndThrow();
		
		if (saveParticipant && cpr.isActive()) {
			saveParticipant(existing, cpr);
		}

		if (existing != null) {
			if (cpr.isDeleted()) {
				AccessCtrlMgr.getInstance().ensureDeleteCprRights(existing);
			}

			existing.update(cpr);
			cpr = existing;
		}

		cpr.setPpidIfEmpty();
		daoFactory.getCprDao().saveOrUpdate(cpr, true);
		cpr.addOrUpdateExtension();

		if (existing == null && cpr.isSpecimenLabelPrePrintOnRegEnabled()) {
			addVisits(cpr, cpr.getCollectionProtocol().getOrderedCpeList(), collectionSite);
		}

		if (cpr.isDeleted()) {
			DeleteLogUtil.getInstance().log(cpr);
		}

		EventPublisher.getInstance().publish(new CprSavedEvent(cpr));
		return cpr;
	}

	private ParticipantRegistrationsList saveOrUpdateRegistrations(ParticipantRegistrationsList input, boolean update) {
		ParticipantDetail inputParticipant = input.getParticipant();
		if (inputParticipant == null) {
			inputParticipant = new ParticipantDetail();
		}

		if (update) {
			Participant existing = getParticipant(input);
			if (Status.isDisabledStatus(inputParticipant.getActivityStatus())) {
				return deleteParticipant(existing, inputParticipant.isForceDelete(), inputParticipant.getOpComments());
			}

			AccessCtrlMgr.getInstance().ensureUpdateParticipantRights(existing);
			inputParticipant.setId(existing.getId());
		}

		//
		// Step 1: Save/update participant
		//
		ParticipantDetail participantDetail = participantService.saveOrUpdateParticipant(inputParticipant);
		Participant participant = daoFactory.getParticipantDao().getById(participantDetail.getId());

		ParticipantDetail p = new ParticipantDetail();
		p.setId(participantDetail.getId());

		//
		// Step 2: Build a map of participant registrations
		//
		Map<String, CollectionProtocolRegistration> cprMap = participant.getCprs().stream()
			.collect(Collectors.toMap(reg -> reg.getCpShortTitle() + "_" + reg.getPpid(), reg -> reg));

		//
		// Step 3: Run through each registration
		//
		List<CollectionProtocolRegistrationDetail> registrations = new ArrayList<>();
		for (CollectionProtocolRegistrationDetail cprDetail : input.getRegistrations()) {
			cprDetail.setParticipant(p);

			CollectionProtocolRegistration existing = null;
			if (StringUtils.isNotBlank(cprDetail.getCpShortTitle()) && StringUtils.isNotBlank(cprDetail.getPpid())) {
				existing = cprMap.get(cprDetail.getCpShortTitle() + "_" + cprDetail.getPpid());
			}

			cprDetail = saveOrUpdateRegistration(cprDetail, existing, false);

			cprDetail.setParticipant(null);
			registrations.add(cprDetail);
		}

		ParticipantRegistrationsList result = new ParticipantRegistrationsList();
		result.setParticipant(participantDetail);
		result.setRegistrations(registrations);
		return result;
	}

	private void saveParticipant(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr) {		
		Participant existingParticipant = null;
		Participant participant = cpr.getParticipant();
		
		if (existing == null) {
			// new registration
			if (participant.getId() != null) { 
				// existing participant
				existingParticipant = daoFactory.getParticipantDao().getById(participant.getId());
			} else {
				// new participant
			}
		} else { 
			// existing reg, therefore it has to be existing participant
			existingParticipant = mergeParticipant(existing, cpr);
		}

		if (existingParticipant != null) {
			participantService.updateParticipant(existingParticipant, participant);
			cpr.setParticipant(existingParticipant);
		} else {
			participant = participantService.createParticipant(participant);
			cpr.setParticipant(participant);
		}
	}
	
	private Participant mergeParticipant(CollectionProtocolRegistration existingCpr, CollectionProtocolRegistration inputCpr) {
		if (existingCpr.getParticipant().equals(inputCpr.getParticipant())) {
			//
			// No change in participant between registration updates. Do nothing
			//
			return existingCpr.getParticipant();
		}
		
		//
		// we’ve been asked to use participant in inputCpr for updating existingCpr
		//
		// Two broad cases:
		// Case #1: Participants are registered to non-overlapping protocols. 
		// e.g. {cp1, cp4} and {cp2, cp3}
		// In this case, we need to only point registration to appropriate participant
		//
		// Case #2: Participants are registered to overlapping protocols. 
		// e.g. {cp1, cp2, cp4} and {cp2, cp3, cp4}
		// In this case, we need to transfer visits from source participant’s overlapping CPRs 
		// to target participant’s corresponding CPRs
		//
		//
		
		Participant srcParticipant = existingCpr.getParticipant();
		Participant tgtParticipant = null;
		if (inputCpr.getParticipant().getId() != null && inputCpr.getParticipant().getId() != -1L) {
			tgtParticipant = daoFactory.getParticipantDao().getById(inputCpr.getParticipant().getId());
		} else {
			tgtParticipant = inputCpr.getParticipant();
		}

		Set<CollectionProtocolRegistration> tgtCprs = tgtParticipant.getCprs();
		for (CollectionProtocolRegistration srcCpr : srcParticipant.getCprs()) {
			CollectionProtocolRegistration mergedCpr = mergeCpr(srcCpr, tgtCprs);
			if (mergedCpr == null) {
				// case 1
				srcCpr.setParticipant(tgtParticipant);
			} else if (mergedCpr.getCollectionProtocol().equals(existingCpr.getCollectionProtocol())){
				// case 2
				// marking input CPR should not be used
				inputCpr.setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
			}
		}

		if (tgtParticipant.getId() == null) {
			//
			// participant might be sourced from external repository
			//
			daoFactory.getParticipantDao().saveOrUpdate(tgtParticipant);
		}

		srcParticipant.setOldCprIds();
		srcParticipant.getCprs().clear();
		if (srcParticipant.isActive()) {
			srcParticipant.delete();
		}

		return tgtParticipant;
	}
	
	private CollectionProtocolRegistration mergeCpr(CollectionProtocolRegistration srcCpr, Set<CollectionProtocolRegistration> tgtCprs) {
		try {
			AccessCtrlMgr.getInstance().ensureUpdateCprRights(srcCpr);
		} catch (OpenSpecimenException ose) {
			throw OpenSpecimenException.userError(CprErrorCode.CANNOT_MERGE_PARTICIPANT, srcCpr.getCollectionProtocol().getShortTitle());
		}
		
		for (CollectionProtocolRegistration tgtCpr : tgtCprs) {
			if (srcCpr.getCollectionProtocol().equals(tgtCpr.getCollectionProtocol())) {
				tgtCpr.addVisits(srcCpr.getVisits());
				srcCpr.getVisits().clear();
				srcCpr.delete();
				return tgtCpr;
			}
		}
		
		return null;
	}

	private Participant getParticipant(ParticipantRegistrationsList input) {
		for (CollectionProtocolRegistrationDetail cprDetail : input.getRegistrations()) {
			String cpShortTitle = cprDetail.getCpShortTitle();
			String ppid         = cprDetail.getPpid();

			if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(ppid)) {
				CollectionProtocolRegistration cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
				if (cpr != null) {
					return cpr.getParticipant();
				}
			}
		}

		return daoFactory.getParticipantDao().getById(lookupParticipant(input.getParticipant()).getId());
	}

	private ParticipantDetail lookupParticipant(ParticipantDetail detail) {
		RequestEvent<List<ParticipantDetail>> req = new RequestEvent<>(Collections.singletonList(detail));
		ResponseEvent<List<MatchedParticipantsList>> resp = participantService.getMatchingParticipants(req);
		resp.throwErrorIfUnsuccessful();

		List<MatchedParticipant> result = resp.getPayload().get(0).getMatches();
		if (result.isEmpty()) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.NOT_FOUND);
		}

		return result.iterator().next().getParticipant();
	}

	//
	// Deletes all registrations of participant
	//
	private ParticipantRegistrationsList deleteParticipant(Participant participant, boolean forceDelete, String reason) {
		List<CollectionProtocolRegistrationDetail> registrations = new ArrayList<>();
		for (CollectionProtocolRegistration cpr : participant.getCprs()) {
			AccessCtrlMgr.getInstance().ensureDeleteCprRights(cpr);

			cpr.delete(!forceDelete);
			cpr.setOpComments(reason);
			DeleteLogUtil.getInstance().log(cpr);

			registrations.add(CollectionProtocolRegistrationDetail.from(cpr, true));
		}

		ParticipantRegistrationsList result = new ParticipantRegistrationsList();
		result.setParticipant(ParticipantDetail.from(participant, true));
		result.setRegistrations(registrations);
		return result;
	}

	//
	// Checks whether same participant is registered for same protocol already
	//
	private void ensureUniqueParticipantReg(CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (cpr.getParticipant() == null || cpr.getParticipant().getId() == null) {
			return ;
		}
		
		Long participantId = cpr.getParticipant().getId();
		Long cpId = cpr.getCollectionProtocol().getId();
		
		if (daoFactory.getCprDao().getCprByParticipantId(cpId, participantId) != null) {
			ose.addError(CprErrorCode.DUP_REGISTRATION);
		}
	}

	private void ensureValidAndUniquePpid(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (existing != null && existing.getPpid().equals(cpr.getPpid())) {
			return;
		}
		
		CollectionProtocol cp = cpr.getCollectionProtocol();

		String ppid = cpr.getPpid();
		if (StringUtils.isBlank(ppid)) {
			boolean ppidReq = cp.isManualPpidEnabled() || StringUtils.isBlank(cp.getPpidFormat());
			if (ppidReq) {
				ose.addError(CprErrorCode.PPID_REQUIRED);
			}
			
			return;
		}
		
		
		if (StringUtils.isNotBlank(cp.getPpidFormat())) {
			//
			// PPID format is specified
			//
			
			if (!cp.isManualPpidEnabled()) {
				ose.addError(CprErrorCode.MANUAL_PPID_NOT_ALLOWED);
				return;
			}
			
			if (!labelGenerator.validate(cp.getPpidFormat(), cpr, ppid)) {
				ose.addError(CprErrorCode.INVALID_PPID, ppid);
				return;
			}
		}
		
		if (daoFactory.getCprDao().getCprByPpid(cp.getId(), ppid) != null) {
			ose.addError(CprErrorCode.DUP_PPID, ppid);
		}
	}

	private void ensureUniqueBarcode(CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (existing != null && 
			StringUtils.isNotBlank(existing.getBarcode()) && 
			existing.getBarcode().equals(cpr.getBarcode())) { // barcode has not changed
			return;
		}
		
		if (StringUtils.isBlank(cpr.getBarcode())) {
			return;
		}
		
		if (daoFactory.getCprDao().getCprByBarcode(cpr.getBarcode()) != null) {
			ose.addError(CprErrorCode.DUP_BARCODE);
		}
	}
		
	private List<SpecimenDetail> getSpecimensByVisit(CollectionProtocolRegistration cpr, Long visitId, boolean excludePhi, boolean excludeChildren) {
		if (visitId != null) {
			Visit visit = daoFactory.getVisitsDao().getById(visitId);
			if (visit == null || !visit.getRegistration().equals(cpr)) {
				throw OpenSpecimenException.userError(VisitErrorCode.NOT_FOUND, visitId);
			}

			return getSpecimensByVisit(visit, excludePhi, excludeChildren);
		} else {
			return getSpecimensByCpr(cpr, excludePhi, excludeChildren);
		}
	}

	private List<SpecimenDetail> getSpecimensByCpr(CollectionProtocolRegistration cpr, boolean excludePhi, boolean excludeChildren) {
		Map<Long, CollectionProtocolEvent> eventsMap = cpr.getCollectionProtocol().getOrderedCpeList().stream()
			.filter(cpe -> !cpe.isClosed())
			.collect(Collectors.toMap(
				CollectionProtocolEvent::getId,
				Function.identity(),
				(u, v) -> { throw new IllegalStateException(String.format("Duplicate key %s", u)); },
				LinkedHashMap::new));

		List<SpecimenDetail> specimens = new ArrayList<>();
		for (Visit visit : cpr.getOrderedVisits()) {
			if (visit.isPlanned()) {
				eventsMap.remove(visit.getCpEvent().getId());
			}

			specimens.addAll(getSpecimensByVisit(visit, excludePhi, excludeChildren));
		}

		for (CollectionProtocolEvent cpe : eventsMap.values()) {
			specimens.addAll(getAnticipatedSpecimens(cpe, excludeChildren));
		}

		return specimens;
	}

	private List<SpecimenDetail> getSpecimensByVisit(Visit visit, boolean excludePhi, boolean excludeChildren) {
		Set<SpecimenRequirement> anticipatedSpecimens = visit.isUnplanned() ? Collections.emptySet() : visit.getCpEvent().getTopLevelAnticipatedSpecimens();
		Set<Specimen> specimens = visit.getTopLevelSpecimens();

		if (!specimens.isEmpty()) {
			List<Specimen> allSpmns;

			if (excludeChildren) {
				allSpmns = new ArrayList<>(specimens);
			} else {
				allSpmns = specimens.stream().map(Specimen::getDescendants).flatMap(List::stream).collect(Collectors.toList());
			}

			DeObject.createExtensions(true, Specimen.EXTN, visit.getCpId(), allSpmns);
		}

		return SpecimenDetail.getSpecimens(visit, anticipatedSpecimens, specimens, false, excludePhi, excludeChildren);
	}

	private List<SpecimenDetail> getAnticipatedSpecimens(Long eventId, boolean excludeChildren) {
		CollectionProtocolEvent cpe = daoFactory.getCollectionProtocolDao().getCpe(eventId);
		if (cpe == null) {
			throw OpenSpecimenException.userError(CpeErrorCode.NOT_FOUND, eventId, 1);
		}

		return getAnticipatedSpecimens(cpe, excludeChildren);
	}

	private List<SpecimenDetail> getAnticipatedSpecimens(CollectionProtocolEvent cpe, boolean excludeChildren) {
		Set<SpecimenRequirement> anticipatedSpecimens = cpe.getTopLevelAnticipatedSpecimens();
		return SpecimenDetail.getSpecimens(null, anticipatedSpecimens, Collections.emptySet(), false, true, excludeChildren);
	}
	
	private CollectionProtocolRegistration getCpr(Long cprId, Long cpId, String ppid) {
		return getCpr(cprId, cpId, null, ppid, null);
	}

	private CollectionProtocolRegistration getCpr(Long cprId, Long cpId, String cpShortTitle, String ppid, String empi) {
		CollectionProtocolRegistration cpr = null;
		String key = null;
		if (cprId != null) {
			key = cprId.toString();
			cpr = daoFactory.getCprDao().getById(cprId);
		} else if (cpId != null && StringUtils.isNotBlank(ppid)) {
			key = cpId + ":" + ppid;
			cpr = daoFactory.getCprDao().getCprByPpid(cpId, ppid);
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(ppid)) {
			key = cpShortTitle + ":" + ppid;
			cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(cpShortTitle, ppid);
		} else if (StringUtils.isNotBlank(cpShortTitle) && StringUtils.isNotBlank(empi)) {
			key = cpShortTitle + ":" + empi;
			cpr = daoFactory.getCprDao().getCprByEmpi(cpShortTitle, empi);
		}
		
		if (cpr == null) {
			throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, key, 1);
		}
		
		return cpr;
	}

	private List<CollectionProtocolRegistration> getRegistrations(String cpShortTitle, List<String> ppids) {
		List<CollectionProtocolRegistration> regs = daoFactory.getCprDao().getByPpids(cpShortTitle, ppids);
		if (regs.size() != ppids.size()) {
			List<String> notFoundPpids = new ArrayList<>(ppids);
			regs.forEach(reg -> notFoundPpids.remove(reg.getPpid()));
			throw OpenSpecimenException.userError(CprErrorCode.M_NOT_FOUND, notFoundPpids, notFoundPpids.size());
		}

		return regs;
	}

	private List<CollectionProtocolRegistration> getOtherCprs(CollectionProtocolRegistration cpr) {
		Participant participant = cpr.getParticipant();
		if (participant.getCprs().size() < 2) {
			return Collections.emptyList();
		}

		return AccessCtrlMgr.getInstance().getAccessibleCprs(
			participant.getCprs().stream()
				.filter(otherCpr -> !otherCpr.equals(cpr))
				.collect(Collectors.toList())
		);
	}

	private CollectionProtocolRegistration registerParticipant(
		BulkRegistrationsDetail bulkRegDetail,
		Date regDate,
		String mrnSite,
		boolean checkPermission) {

		CollectionProtocolRegistrationDetail cprDetail = new CollectionProtocolRegistrationDetail();
		cprDetail.setRegistrationDate(regDate);
		cprDetail.setCpId(bulkRegDetail.getCpId());
		cprDetail.setCpTitle(bulkRegDetail.getCpTitle());
		cprDetail.setCpShortTitle(bulkRegDetail.getCpShortTitle());
		cprDetail.setParticipant(getParticipantDetail(mrnSite));

		if (checkPermission) {
			return saveOrUpdateRegistration(cprDetail, null, mrnSite, true);
		} else {
			return saveOrUpdateRegistration(cprFactory.createCpr(cprDetail), null, mrnSite, true);
		}
	}

	private ParticipantDetail getParticipantDetail(String mrnSite) {
		if (StringUtils.isBlank(mrnSite)) {
			return null;
		}

		ParticipantDetail participant = new ParticipantDetail();
		PmiDetail pmi = new PmiDetail();
		pmi.setSiteName(mrnSite);
		participant.setPmi(pmi);
		return participant;
	}

	private void addVisits(CollectionProtocolRegistration cpr, List<CollectionProtocolEvent> cpes, String collectionSite) {
		if (CollectionUtils.isEmpty(cpes)) {
			return;
		}

		Integer minOffset = null, maxOffset = null;
		for (CollectionProtocolEvent cpe : cpes) {
			if (cpe.isClosed()) {
				continue;
			}

			Integer offset = Utility.getNoOfDays(cpe.getEventPoint(), cpe.getEventPointUnit());
			if (offset == null) {
				continue;
			}

			if (minOffset == null || offset < minOffset) {
				minOffset = offset;
			}

			if (maxOffset == null || offset > maxOffset) {
				maxOffset = offset;
			}
		}

		for (CollectionProtocolEvent cpe : cpes) {
			if (cpe.isClosed()) {
				continue;
			}

			VisitDetail visitDetail = new VisitDetail();
			visitDetail.setCprId(cpr.getId());
			visitDetail.setEventId(cpe.getId());
			visitDetail.setSite(collectionSite);
			visitDetail.setStatus(Visit.VISIT_STATUS_PENDING);
			if (cpe.getClinicalDiagnosis() != null) {
				visitDetail.setClinicalDiagnoses(Collections.singleton(cpe.getClinicalDiagnosis().getValue()));
			}

			if (cpe.getClinicalStatus() != null) {
				visitDetail.setClinicalStatus(PermissibleValue.getValue(cpe.getClinicalStatus()));
			}



			int interval = 0;
			if (minOffset != null) {
				Integer offset = Utility.getNoOfDays(cpe.getEventPoint(), cpe.getEventPointUnit());
				if (offset != null) {
					interval = offset - minOffset;
				} else {
					interval = (maxOffset - minOffset) + 1;
				}
			}

			Calendar cal = Calendar.getInstance();
			cal.setTime(cpr.getRegistrationDate());
			cal.add(Calendar.DATE, interval);
			visitDetail.setVisitDate(cal.getTime());

			cpr.addVisit(visitSvc.addVisit(visitDetail, false));
		}
	}

	private void raiseErrorIfSpecimenCentric(CollectionProtocolRegistration cpr) {
		if (cpr.getCollectionProtocol().isSpecimenCentric()) {
			throw OpenSpecimenException.userError(CpErrorCode.OP_NOT_ALLOWED_SC, cpr.getCollectionProtocol().getShortTitle());
		}
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

	private boolean deleteCprs(List<CollectionProtocolRegistration> cprs, String reason, boolean forceDelete)
	throws Exception {
		final Authentication auth = AuthUtil.getAuth();

		Future<Boolean> result = taskExecutor.submit(new Callable<Boolean>() {
			@Override
			public Boolean call() throws Exception {
				try {
					SecurityContextHolder.getContext().setAuthentication(auth);

					List<CollectionProtocolRegistration> success = new ArrayList<>();
					Map<CollectionProtocolRegistration, String> failed = new HashMap<>();

					for (CollectionProtocolRegistration cpr : cprs) {
						try {
							deleteCpr(cpr.getId(), reason, forceDelete);
							success.add(cpr);
						} catch (Throwable t) {
							failed.put(cpr, ExceptionUtils.getStackTrace(t));
						}
					}

					notifyOnCprsDeleted(AuthUtil.getCurrentUser(), success);
					notifyOnCprsDeleteErrors(AuthUtil.getCurrentUser(), failed);
				} catch (Throwable t) {
					logger.error("Error deleting registrations", t);
				}

				return true;
			}
		});

		boolean completed = false;
		try {
			completed = result.get(30, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			completed = false;
		}

		return completed;
	}

	@PlusTransactional
	private void deleteCpr(Long cprId, String reason, boolean forceDelete) {
		CollectionProtocolRegistration cpr = daoFactory.getCprDao().getById(cprId);
		deleteCpr(cpr, reason, forceDelete, true);
	}

	private void deleteCpr(CollectionProtocolRegistration cpr, String reason, boolean forceDelete, boolean checkOnlyCollectedSpmns) {
		cpr.setOpComments(reason);
		cpr.delete(!forceDelete, checkOnlyCollectedSpmns);
		DeleteLogUtil.getInstance().log(cpr);
		EventPublisher.getInstance().publish(new CprSavedEvent(cpr));
		if (!cpr.getParticipant().isDeleted()) {
			//
			// OPSMN-5770: This is to allow, in the future, registration and deletion of the
			// same participant multiple times.
			//
			cpr.setParticipant(null);
		}
	}

	private void notifyOnCprsDeleted(User user, List<CollectionProtocolRegistration> cprs) {
		if (cprs == null || cprs.isEmpty()) {
			return;
		}

		String[] rcpts = { user.getEmailAddress() };
		Map<String, Object> props = new HashMap<>();
		props.put("rcpt", user);
		props.put("cprs", cprs);
		props.put("ccAdmin", false);
		EmailUtil.getInstance().sendEmail("cprs_delete_success", rcpts, null, props);
	}

	private void notifyOnCprsDeleteErrors(User user, Map<CollectionProtocolRegistration, String> errors) {
		if (errors == null || errors.isEmpty()) {
			return;
		}

		String[] rcpts = { user.getEmailAddress() };
		Map<String, Object> props = new HashMap<>();
		props.put("rcpt", user);
		props.put("ccAdmin", false);
		props.put("errors", errors);
		EmailUtil.getInstance().sendEmail("cprs_delete_error", rcpts, null, props);
	}

	private abstract class AbstractCprsGenerator implements Function<ExportJob, List<? extends Object>> {
		private boolean endOfCprs;

		private CprListCriteria crit;

		private Long lastId;

		private boolean paramsInited;

		List<CollectionProtocolRegistration> nextCprs(ExportJob job) {
			initParams(job);

			if (endOfCprs) {
				return Collections.emptyList();
			}

			List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprs(crit.lastId(lastId));
			if (CollectionUtils.isNotEmpty(crit.ppids()) || CollectionUtils.isNotEmpty(crit.externalSubjectIds()) || cprs.size() < 100) {
				endOfCprs = true;
			}

			if (!cprs.isEmpty()) {
				lastId = cprs.get(cprs.size() - 1).getId();
			}

			return cprs;
		}

		//
		// -1: error, 0: full access, 1: access without PHI
		//
		int canRead(CollectionProtocolRegistration cpr) {
			try {
				return AccessCtrlMgr.getInstance().ensureReadCprRights(cpr) ? 0 : 1;
			} catch (OpenSpecimenException ose) {
				if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
					logger.error("Error checking participant record access", ose);
				}

				return -1;
			}
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
			AccessCtrlMgr.ParticipantReadAccess access = AccessCtrlMgr.getInstance().getParticipantReadAccess(cpId);
			if (!access.admin && access.noAccessibleSites()) {
				endOfCprs = true;
			} else if (!AccessCtrlMgr.getInstance().hasCprEximRights(cpId)) {
				endOfCprs = true;
			} else {
				crit = new CprListCriteria()
					.cpId(cpId)
					.ppids(Utility.csvToStringList(params.get("ppids")))
					.siteCps(access.siteCps)
					.useMrnSites(AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn());

				if (CollectionUtils.isNotEmpty(crit.ppids()) || CollectionUtils.isNotEmpty(crit.externalSubjectIds())) {
					crit.limitItems(false);
				} else {
					crit.limitItems(true).maxResults(100);
				}
			}

			paramsInited = true;
		}
	}

	private Function<ExportJob, List<? extends Object>> getCprsGenerator() {
		return new AbstractCprsGenerator() {
			@Override
			public List<? extends Object> apply(ExportJob job) {
				List<CollectionProtocolRegistrationDetail> records = new ArrayList<>();
				for (CollectionProtocolRegistration cpr : nextCprs(job)) {
					int accessType = canRead(cpr);
					if (accessType >= 0) {
						records.add(CollectionProtocolRegistrationDetail.from(cpr, accessType == 1));
					}
				}

				return records;
			}
		};
	}

	private Function<ExportJob, List<? extends Object>> getConsentsGenerator() {
		return new AbstractCprsGenerator() {
			//
			// -1: error, 0: full access, 1: access without PHI
			//
			int canRead(CollectionProtocolRegistration cpr) {
				try {
					return AccessCtrlMgr.getInstance().ensureReadConsentRights(cpr) ? 0 : 1;
				} catch (OpenSpecimenException ose) {
					if (!ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
						logger.error("Error checking participant record access", ose);
					}

					return -1;
				}
			}

			@Override
			public List<? extends Object> apply(ExportJob job) {
				List<ConsentDetail> records = new ArrayList<>();

				boolean endOfWork = false;
				while (!endOfWork) {
					List<CollectionProtocolRegistration> cprs = nextCprs(job);
					if (cprs.isEmpty()) {
						break;
					}

					for (CollectionProtocolRegistration cpr : cprs) {
						int accessType = canRead(cpr);
						if (accessType < 0 || !hasConsent(cpr, accessType == 0)) {
							continue;
						}

						ConsentDetail detail = toConsentDetail(cpr, accessType == 0);
						records.add(detail);

						boolean firstResp = true;
						for (ConsentTierResponse resp : getSortedResponses(cpr)) {
							if (!firstResp) {
								detail = copyOf(detail);
								records.add(detail);
							}

							detail.setStatement(resp.getStatement());
							detail.setCode(resp.getStatementCode());
							detail.setResponse(PermissibleValue.getValue(resp.getResponse()));
							firstResp = false;
						}
					}

					endOfWork = !records.isEmpty();
				}

				return records;
			}

			private boolean hasConsent(CollectionProtocolRegistration cpr, boolean hasPhi) {
				return cpr.getConsentSignDate() != null ||
					cpr.getConsentWitness() != null ||
					StringUtils.isNotBlank(cpr.getConsentComments()) ||
					(hasPhi && StringUtils.isNotBlank(cpr.getSignedConsentDocumentName())) ||
					CollectionUtils.isNotEmpty(cpr.getConsentResponses());
			}

			private ConsentDetail toConsentDetail(CollectionProtocolRegistration cpr, boolean hasPhi) {
				ConsentDetail detail = new ConsentDetail();
				detail.setCpShortTitle(cpr.getCpShortTitle());
				detail.setPpid(cpr.getPpid());
				detail.setConsentSignatureDate(cpr.getConsentSignDate());
				detail.setComments(cpr.getConsentComments());

				if (cpr.getConsentWitness() != null) {
					detail.setWitness(UserSummary.from(cpr.getConsentWitness()));
				}

				if (cpr.getSignedConsentDocumentName() != null && hasPhi) {
					detail.setConsentDocumentName(cpr.getSignedConsentDocumentName());
					detail.setDocumentFile(new File(ConfigParams.getConsentsDirPath(), cpr.getSignedConsentDocumentName()));
				}

				return detail;
			}

			private Collection<ConsentTierResponse> getSortedResponses(CollectionProtocolRegistration cpr) {
				return cpr.getConsentResponses().stream()
					.filter(r1 -> r1 != null && r1.getResponse() != null)
					.sorted((r1, r2) -> r1.getConsentTier().getId().compareTo(r2.getConsentTier().getId()))
					.collect(Collectors.toList());
			}

			private ConsentDetail copyOf(ConsentDetail input) {
				try {
					ConsentDetail output = new ConsentDetail();
					BeanUtilsBean.getInstance().copyProperties(output, input);
					output.setConsentDocumentName(null);
					output.setDocumentFile(null);
					return output;
				} catch (Exception e) {
					throw new RuntimeException("Error cloning consent detail object", e);
				}
			}
		};
	}
}
