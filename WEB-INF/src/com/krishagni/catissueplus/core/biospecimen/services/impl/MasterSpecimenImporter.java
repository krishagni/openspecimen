package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;

import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.MasterSpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ReceivedEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolRegistrationService;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.biospecimen.services.VisitService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.de.services.impl.ExtensionsUtil;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class MasterSpecimenImporter implements ObjectImporter<MasterSpecimenDetail, MasterSpecimenDetail> {

	private DaoFactory daoFactory;
	
	private CollectionProtocolRegistrationService cprSvc;
	
	private VisitService visitSvc;
	
	private SpecimenService specimenSvc;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setCprSvc(CollectionProtocolRegistrationService cprSvc) {
		this.cprSvc = cprSvc;
	}

	public void setVisitSvc(VisitService visitSvc) {
		this.visitSvc = visitSvc;
	}

	public void setSpecimenSvc(SpecimenService specimenSvc) {
		this.specimenSvc = specimenSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<MasterSpecimenDetail> importObject(RequestEvent<ImportObjectDetail<MasterSpecimenDetail>> req) {
		try {
			ImportObjectDetail<MasterSpecimenDetail> detail = req.getPayload();
			upsertCpr(detail.getObject());
			upsertVisit(detail.getObject());

			ExtensionsUtil.initFileFields(detail.getUploadedFilesDir(), detail.getObject().getExtensionDetail());
			upsertSpecimen(detail.getObject());
			return ResponseEvent.response(detail.getObject());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	private void upsertCpr(MasterSpecimenDetail detail) {
		if (!isPrimarySpecimen(detail)) {
			return;
		}

		if (StringUtils.isBlank(detail.getCpShortTitle())) {
			throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
		}

		CollectionProtocolRegistration cpr = null;
		if (StringUtils.isNotBlank(detail.getPpid())) {
			cpr = daoFactory.getCprDao().getCprByCpShortTitleAndPpid(detail.getCpShortTitle(), detail.getPpid());
		} else if (StringUtils.isNotBlank(detail.getEmpi())) {
			cpr = daoFactory.getCprDao().getCprByEmpi(detail.getCpShortTitle(), detail.getEmpi());
		} else if (StringUtils.isNotBlank(detail.getUid())) {
			cpr = daoFactory.getCprDao().getCprByUid(detail.getCpShortTitle(), detail.getUid());
		} else if (CollectionUtils.isNotEmpty(detail.getPmis())) {
			cpr = getCprByPmis(detail.getCpShortTitle(), detail.getPmis());
		}

		CollectionProtocolRegistrationDetail cprDetail = new CollectionProtocolRegistrationDetail();
		if (detail.isAttrModified("ppid")) {
			cprDetail.setPpid(detail.getPpid());
		}

		if (detail.isAttrModified("cpShortTitle")) {
			cprDetail.setCpShortTitle(detail.getCpShortTitle());
		}

		if (cpr == null || detail.isAttrModified("registrationDate")) {
			cprDetail.setRegistrationDate(detail.getRegistrationDate());
		}

		if (detail.isAttrModified("regSite")) {
			cprDetail.setSite(detail.getRegSite());
		}

		if (detail.isAttrModified("externalSubjectId")) {
			cprDetail.setExternalSubjectId(detail.getExternalSubjectId());
		}

		setParticipant(detail, cprDetail);

		ResponseEvent<CollectionProtocolRegistrationDetail> resp;
		if (cpr == null) {
			resp = cprSvc.createRegistration(RequestEvent.wrap(cprDetail));
		} else {
			cprDetail.setId(cpr.getId());
			resp = cprSvc.updateRegistration(RequestEvent.wrap(cprDetail));
		}

		resp.throwErrorIfUnsuccessful();
		detail.setPpid(resp.getPayload().getPpid());

		if (cpr != null) {
			if (StringUtils.isBlank(detail.getVisitName())) {
				cpr.getVisits().stream()
					.filter(visit -> isVisitOfSameEvent(visit, detail.getEventLabel()))
					.filter(visit -> detail.getVisitDate() != null && DateUtils.isSameDay(visit.getVisitDate(), detail.getVisitDate()))
					.findAny().ifPresent(matchedVisit -> detail.setVisitId(matchedVisit.getId()));
			} else {
				cpr.getVisits().stream()
					.filter(visit -> detail.getVisitName().equals(visit.getName()))
					.findAny().ifPresent(matchedVisit -> detail.setVisitId(matchedVisit.getId()));
			}
		}
	}

	private CollectionProtocolRegistration getCprByPmis(String cpShortTitle, List<PmiDetail> pmis) {
		List<CollectionProtocolRegistration> cprs = daoFactory.getCprDao().getCprsByPmis(cpShortTitle, pmis);
		if (cprs.size() > 1) {
			throw OpenSpecimenException.userError(CprErrorCode.MUL_REGS_FOR_PMIS);
		}

		return cprs.isEmpty() ? null : cprs.iterator().next();
	}

	private boolean isVisitOfSameEvent(Visit visit, String eventLabel) {
		return StringUtils.isBlank(eventLabel) || visit.isOfEvent(eventLabel);
	}

	private void upsertVisit(MasterSpecimenDetail detail) {
		if (!isPrimarySpecimen(detail)) {
			return;
		}
		
		VisitDetail visitDetail = new VisitDetail();
		visitDetail.setId(detail.getVisitId());
		visitDetail.setCpShortTitle(detail.getCpShortTitle());
		visitDetail.setPpid(detail.getPpid());

		if (detail.isAttrModified("visitName")) {
			visitDetail.setName(detail.getVisitName());
		}

		if (detail.isAttrModified("eventLabel")) {
			visitDetail.setEventLabel(getEventLabel(detail));
		}

		if (detail.isAttrModified("visitDate")) {
			visitDetail.setVisitDate(detail.getVisitDate());
		}

		if (detail.isAttrModified("collectionSite")) {
			visitDetail.setSite(detail.getCollectionSite());
		}

		if (detail.isAttrModified("clinicalDiagnoses")) {
			visitDetail.setClinicalDiagnoses(detail.getClinicalDiagnoses());
		}

		if (detail.isAttrModified("clinicalStatus")) {
			visitDetail.setClinicalStatus(detail.getClinicalStatus());
		}

		if (detail.isAttrModified("surgicalPathologyNumber")) {
			visitDetail.setSurgicalPathologyNumber(detail.getSurgicalPathologyNumber());
		}

		if (detail.isAttrModified("visitComments")) {
			visitDetail.setComments(detail.getVisitComments());
		}

		ResponseEvent<VisitDetail> resp;
		if (visitDetail.getId() != null) {
			if (detail.isAttrModified("status")) {
				visitDetail.setStatus(detail.getStatus());
			}

			resp = visitSvc.patchVisit(RequestEvent.wrap(visitDetail));
		} else {
			visitDetail.setVisitDate(detail.getVisitDate() == null ? detail.getCollectionDate() : detail.getVisitDate());
			visitDetail.setStatus(StringUtils.isBlank(detail.getStatus()) ? Visit.VISIT_STATUS_COMPLETED : detail.getStatus());
			resp = visitSvc.addVisit(RequestEvent.wrap(visitDetail));
		}

		resp.throwErrorIfUnsuccessful();
		detail.setVisitId(resp.getPayload().getId());
	}
	
	private void upsertSpecimen(MasterSpecimenDetail detail) {
		SpecimenDetail specimenDetail = new SpecimenDetail();
		if (detail.isAttrModified("reqCode")) {
			specimenDetail.setReqCode(detail.getReqCode());
		}

		if (detail.isAttrModified("label")) {
			specimenDetail.setLabel(detail.getLabel());
		}

		if (detail.isAttrModified("barcode")) {
			specimenDetail.setBarcode(detail.getBarcode());
		}

		if (detail.isAttrModified("imageId")) {
			specimenDetail.setImageId(detail.getImageId());
		}

		if (detail.isAttrModified("specimenClass")) {
			specimenDetail.setSpecimenClass(detail.getSpecimenClass());
		}

		if (detail.isAttrModified("type")) {
			specimenDetail.setType(detail.getType());
		}

		if (detail.isAttrModified("lineage")) {
			specimenDetail.setLineage(detail.getLineage());
		}

		if (detail.isAttrModified("anatomicSite")) {
			specimenDetail.setAnatomicSite(detail.getAnatomicSite());
		}

		if (detail.isAttrModified("laterality")) {
			specimenDetail.setLaterality(detail.getLaterality());
		}

		if (detail.isAttrModified("pathology")) {
			specimenDetail.setPathology(detail.getPathology());
		}

		if (detail.isAttrModified("initialQty")) {
			specimenDetail.setInitialQty(detail.getInitialQty());
		}

		if (detail.isAttrModified("availableQty")) {
			specimenDetail.setAvailableQty(detail.getAvailableQty());
		}

		if (detail.isAttrModified("concentration")) {
			specimenDetail.setConcentration(detail.getConcentration());
		}

		if (detail.isAttrModified("freezeThawCycles")) {
			specimenDetail.setFreezeThawCycles(detail.getFreezeThawCycles());
		}

		if (detail.isAttrModified("createdOn")) {
			specimenDetail.setCreatedOn(detail.getCreatedOn());
		}

		if (detail.isAttrModified("createdBy")) {
			specimenDetail.setCreatedBy(detail.getCreatedBy());
		}

		if (detail.isAttrModified("comments")) {
			specimenDetail.setComments(detail.getComments());
		}

		if (detail.isAttrModified("extensionDetail")) {
			specimenDetail.setExtensionDetail(detail.getExtensionDetail());
		}

		if (detail.isAttrModified("externalIds")) {
			specimenDetail.setExternalIds(detail.getExternalIds());
		}

		if (detail.isAttrModified("collectionStatus")) {
			specimenDetail.setStatus(detail.getCollectionStatus());
		}

		setParentLabel(detail, specimenDetail);
		setLocation(detail, specimenDetail);
		setCollectionDetail(detail, specimenDetail);
		setReceiveDetail(detail, specimenDetail);
		if (specimenDetail.modifiedAttrsCount() == 0) {
			return;
		}

		Specimen spmn = null;
		if (StringUtils.isNotBlank(specimenDetail.getLabel())) {
			spmn = daoFactory.getSpecimenDao().getByLabelAndCp(specimenDetail.getCpShortTitle(), specimenDetail.getLabel());
		}

		if (spmn == null && StringUtils.isNotBlank(specimenDetail.getBarcode())) {
			spmn = daoFactory.getSpecimenDao().getByBarcodeAndCp(specimenDetail.getCpShortTitle(), specimenDetail.getBarcode());
		}

		specimenDetail.setCpShortTitle(detail.getCpShortTitle());
		specimenDetail.setVisitId(detail.getVisitId());
		specimenDetail.setVisitName(detail.getVisitName());

		ResponseEvent<SpecimenDetail> resp;
		if (spmn == null) {
			if (StringUtils.isBlank(specimenDetail.getStatus())) {
				specimenDetail.setStatus(Specimen.COLLECTED);
			}

			if (detail.getCollectionDate() == null) {
				Date collectionDate = detail.getVisitDate();
				if (collectionDate == null) {
					throw OpenSpecimenException.userError(SpecimenErrorCode.COLL_DATE_REQUIRED);
				}

				if (specimenDetail.getCollectionEvent() == null) {
					specimenDetail.setCollectionEvent(new CollectionEventDetail());
				}

				specimenDetail.getCollectionEvent().setTime(collectionDate);
			}

			resp = specimenSvc.createSpecimen(RequestEvent.wrap(specimenDetail));
		} else {
			specimenDetail.setId(spmn.getId());
			resp = specimenSvc.updateSpecimen(RequestEvent.wrap(specimenDetail));
		}

		resp.throwErrorIfUnsuccessful();
		detail.setLabel(resp.getPayload().getLabel());
	}
	
	private void setParticipant(MasterSpecimenDetail detail, CollectionProtocolRegistrationDetail cprDetail) {
		Participant existingParticipant = null;
		if (StringUtils.isNotBlank(detail.getEmpi())) {
			existingParticipant = daoFactory.getParticipantDao().getByEmpi(detail.getEmpi());
		} else if (StringUtils.isNotBlank(detail.getUid())) {
			existingParticipant = daoFactory.getParticipantDao().getByUid(detail.getUid());
		} else if (detail.isAttrModified("pmis") && CollectionUtils.isNotEmpty(detail.getPmis())) {
			List<Participant> matches = daoFactory.getParticipantDao().getByPmis(detail.getPmis());
			for (Participant match : matches) {
				if (existingParticipant == null) {
					existingParticipant = match;
				} else if (!existingParticipant.equals(match)) {
					throw OpenSpecimenException.userError(ParticipantErrorCode.MRN_DIFF, PmiDetail.toString(detail.getPmis()));
				}
			}
		}

		ParticipantDetail participantDetail = new ParticipantDetail();
		if (existingParticipant != null) {
			participantDetail.setId(existingParticipant.getId());
		}

		if (detail.isAttrModified("firstName")) {
			participantDetail.setFirstName(detail.getFirstName());
		}

		if (detail.isAttrModified("lastName")) {
			participantDetail.setLastName(detail.getLastName());
		}

		if (detail.isAttrModified("middleName")) {
			participantDetail.setMiddleName(detail.getMiddleName());
		}

		if (detail.isAttrModified("emailAddress")) {
			participantDetail.setEmailAddress(detail.getEmailAddress());
		}

		if (detail.isAttrModified("phoneNumber")) {
			participantDetail.setPhoneNumber(detail.getPhoneNumber());
		}

		if (detail.isAttrModified("birthDate")) {
			participantDetail.setBirthDate(detail.getBirthDate());
		}

		if (detail.isAttrModified("deathDate")) {
			participantDetail.setDeathDate(detail.getDeathDate());
		}

		if (detail.isAttrModified("gender")) {
			participantDetail.setGender(detail.getGender());
		}

		if (detail.isAttrModified("races")) {
			participantDetail.setRaces(detail.getRaces());
		}

		if (detail.isAttrModified("ethnicities")) {
			participantDetail.setEthnicities(detail.getEthnicities());
		}

		if (detail.isAttrModified("vitalStatus")) {
			participantDetail.setVitalStatus(detail.getVitalStatus());
		}

		if (detail.isAttrModified("uid")) {
			participantDetail.setUid(detail.getUid());
		}

		if (detail.isAttrModified("empi")) {
			participantDetail.setEmpi(detail.getEmpi());
		}

		if (detail.isAttrModified("pmis")) {
			participantDetail.setPmis(detail.getPmis());
		}

		cprDetail.setParticipant(participantDetail);
	}
	
	private void setParentLabel(MasterSpecimenDetail detail, SpecimenDetail specimenDetail) {
		if (isPrimarySpecimen(detail)) {
			return;
		}

		if (detail.isAttrModified("parentLabel")) {
			specimenDetail.setParentLabel(detail.getParentLabel());
		}
	}
	
	private void setLocation(MasterSpecimenDetail detail, SpecimenDetail specimenDetail) {
		if (!detail.isAttrModified("container")) {
			return;
		}

		StorageLocationSummary storageLocation = new StorageLocationSummary();
		storageLocation.setName(detail.getContainer());
		storageLocation.setPositionX(detail.getPositionX());
		storageLocation.setPositionY(detail.getPositionY());
		storageLocation.setPosition(detail.getPosition());
		specimenDetail.setStorageLocation(storageLocation);
	}
	
	private void setCollectionDetail(MasterSpecimenDetail detail, SpecimenDetail specimenDetail) {
		if (!isPrimarySpecimen(detail)) {
			return;
		}
		
		CollectionEventDetail collectionEvent = new CollectionEventDetail();
		boolean modified = false;
		if (detail.isAttrModified("collectionProcedure")) {
			collectionEvent.setProcedure(detail.getCollectionProcedure());
			modified = true;
		}

		if (detail.isAttrModified("collectionContainer")) {
			collectionEvent.setContainer(detail.getCollectionContainer());
			modified = true;
		}

		if (detail.isAttrModified("collectionDate")) {
			collectionEvent.setTime(detail.getCollectionDate());
			modified = true;
		}

		if (detail.isAttrModified("collector")) {
			collectionEvent.setUser(getUser(detail.getCollector()));
			modified = true;
		}

		if (modified) {
			specimenDetail.setCollectionEvent(collectionEvent);
		}
	}
	
	private void setReceiveDetail(MasterSpecimenDetail detail, SpecimenDetail specimenDetail) {
		if (!isPrimarySpecimen(detail)) {
			return;
		}
		
		ReceivedEventDetail receivedEvent = new ReceivedEventDetail();
		boolean modified = false;

		if (detail.isAttrModified("receivedQuality")) {
			receivedEvent.setReceivedQuality(detail.getReceivedQuality());
			modified = true;
		}

		if (detail.isAttrModified("receivedDate")) {
			receivedEvent.setTime(detail.getReceivedDate());
			modified = true;
		}

		if (detail.isAttrModified("receiver")) {
			receivedEvent.setUser(getUser(detail.getReceiver()));
			modified = true;
		}

		if (modified) {
			specimenDetail.setReceivedEvent(receivedEvent);
		}
	}
	
	private String getEventLabel(MasterSpecimenDetail detail) {
		if (StringUtils.isNotBlank(detail.getEventLabel())) {
			return detail.getEventLabel();
		}
		
		CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
		CollectionProtocolEvent event = cp.firstEvent();
		if (event != null) {
			return event.getEventLabel();
		}
		
		return null;
	}
	
	private UserSummary getUser(String emailAddress) {
		if (StringUtils.isBlank(emailAddress)) {
			emailAddress = AuthUtil.getCurrentUser().getEmailAddress();
		}
		
		UserSummary userSummary = new UserSummary();
		userSummary.setEmailAddress(emailAddress);
		return userSummary;
	}
	
	private boolean isPrimarySpecimen(MasterSpecimenDetail detail) {
		return StringUtils.isBlank(detail.getLineage()) || detail.getLineage().equals(Specimen.NEW);
	}
}
