
package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenExternalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenReceivedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolRegistrationFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SrErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.VisitFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ReceivedEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenEventDetail;
import com.krishagni.catissueplus.core.biospecimen.events.VisitDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.NameValuePair;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.importer.services.impl.ImporterContextHolder;

import static com.krishagni.catissueplus.core.common.PvAttributes.BIOHAZARD;
import static com.krishagni.catissueplus.core.common.PvAttributes.COLL_PROC;
import static com.krishagni.catissueplus.core.common.PvAttributes.CONTAINER;
import static com.krishagni.catissueplus.core.common.PvAttributes.PATH_STATUS;
import static com.krishagni.catissueplus.core.common.PvAttributes.RECV_QUALITY;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_ANATOMIC_SITE;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_CLASS;
import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_LATERALITY;

public class SpecimenFactoryImpl implements SpecimenFactory {

	private DaoFactory daoFactory;

	private SpecimenResolver specimenResolver;

	private StorageContainerService containerSvc;

	private CollectionProtocolRegistrationFactory cprFactory;

	private VisitFactory visitFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setContainerSvc(StorageContainerService containerSvc) {
		this.containerSvc = containerSvc;
	}

	public void setCprFactory(CollectionProtocolRegistrationFactory cprFactory) {
		this.cprFactory = cprFactory;
	}

	public void setVisitFactory(VisitFactory visitFactory) {
		this.visitFactory = visitFactory;
	}

	@Override
	public Specimen createSpecimen(Specimen existing, SpecimenDetail detail, Specimen parent) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		if (parent == null) {
			parent = getSpecimen(detail.getParentId(), detail.getCpShortTitle(), detail.getParentLabel(), ose);
			ose.checkAndThrow();
		}
		
		Visit visit = getVisit(detail, existing, parent, ose);
		SpecimenRequirement sr = getSpecimenRequirement(detail, existing, visit, ose);
		ose.checkAndThrow();
		
		if (sr != null && !sr.getCollectionProtocolEvent().equals(visit.getCpEvent())) {
			ose.addError(SpecimenErrorCode.INVALID_VISIT);
		}
		
		if (parent != null && !parent.getVisit().equals(visit)) {
			ose.addError(SpecimenErrorCode.INVALID_VISIT);
		}

		ose.checkAndThrow();
		
		Specimen specimen = null;
		if (sr != null) {
			specimen = sr.getSpecimen();
		} else {
			specimen = new Specimen();
		}
		
		
		if (existing != null) {
			specimen.setId(existing.getId());
		} else {
			specimen.setId(detail.getId());
		}
		
		specimen.setCollectionProtocol(visit.getCollectionProtocol());
		specimen.setVisit(visit);
		specimen.setForceDelete(detail.isForceDelete());
		specimen.setPrintLabel(detail.isPrintLabel());
		specimen.setAutoCollectParents(true);
		specimen.setOpComments(detail.getOpComments());

		setCollectionStatus(detail, existing, specimen, ose);
		setLineage(detail, existing, specimen, ose);
		setParentSpecimen(detail, existing, parent, specimen, ose);

		setLabel(detail, existing, specimen, ose);
		setAdditionalLabel(detail, existing, specimen, ose);
		setBarcode(detail, existing, specimen, ose);
		setImageId(detail, existing, specimen, ose);
		setActivityStatus(detail, existing, specimen, ose);
						
		setAnatomicSite(detail, existing, specimen, ose);
		setLaterality(detail, existing, specimen, ose);
		setPathologicalStatus(detail, existing, specimen, ose);
		setSpecimenClass(detail, existing, specimen, ose);
		setSpecimenType(detail, existing, specimen, ose);
		setQuantity(detail, existing, specimen, ose);
		setConcentration(detail, existing, specimen, ose);
		setBiohazards(detail, existing, specimen, ose);
		setFreezeThawCycles(detail, existing, specimen, ose);
		setExternalIds(detail, existing, specimen, ose);
		setComments(detail, existing, specimen, ose);
		setSpecimenPosition(detail, existing, specimen, ose);
		setHoldingLocation(detail, existing, specimen, ose);
		setCollectionDetail(detail, existing, specimen, ose);
		setReceiveDetail(detail, existing, specimen, ose);
		setCreatedOn(detail, existing, specimen, ose);
		setCreatedBy(detail, existing, specimen, ose);
		setExtension(detail, existing, specimen, ose);
		if (sr != null) {
			if (!specimen.getLineage().equals(sr.getLineage())) {
				ose.addError(SpecimenErrorCode.REQ_LINEAGE_MISMATCH, specimen.getLineage(), sr.getLineage());
			} else if (!specimen.getSpecimenType().equals(sr.getSpecimenType())) {
				ose.addError(SpecimenErrorCode.REQ_TYPE_MISMATCH, specimen.getSpecimenType().getValue(), sr.getSpecimenType().getValue());
			}
		}

		ose.checkAndThrow();
		return specimen;
	}

	private void setAdditionalLabel(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getAdditionalLabel())) {
			return;
		}

		specimen.setAdditionalLabel(detail.getAdditionalLabel());
	}

	private void setAdditionalLabel(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("additionalLabel")) {
			setAdditionalLabel(detail, specimen, ose);
		} else {
			specimen.setAdditionalLabel(existing.getAdditionalLabel());
		}
	}

	private void setBarcode(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getBarcode())) {
			return;
		}

		specimen.setBarcode(detail.getBarcode());
	}
	
	private void setBarcode(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("barcode")) {
			setBarcode(detail, specimen, ose);
		} else {
			specimen.setBarcode(existing.getBarcode());
		}
	}

	private void setImageId(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getImageId())) {
			return;
		}

		specimen.setImageId(detail.getImageId());
	}

	private void setImageId(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("imageId")) {
			setImageId(detail, specimen, ose);
		} else {
			specimen.setImageId(existing.getImageId());
		}
	}

	private void setActivityStatus(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		String status = detail.getActivityStatus();
		if (StringUtils.isBlank(status)) {
			specimen.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(status)) {
			specimen.setActivityStatus(status);
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID);
		}
	}
	
	private void setActivityStatus(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, specimen, ose);
		} else {
			specimen.setActivityStatus(existing.getActivityStatus());
		}
	}
	
	private void setLabel(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isNotBlank(detail.getLabel())) {
			specimen.setLabel(detail.getLabel());
		}
	}
	
	private void setLabel(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("label")) {
			setLabel(detail, specimen, ose);
		} else {
			specimen.setLabel(existing.getLabel());
		}
	}

	private Visit getVisit(SpecimenDetail detail, Specimen existing, Specimen parent, OpenSpecimenException ose) {
		Long visitId = detail.getVisitId();
		String visitName = detail.getVisitName();

		Object key = null;
		Visit visit = null;
		if (visitId != null) {
			visit = daoFactory.getVisitsDao().getById(visitId);
			key = visitId;
		} else if (StringUtils.isNotBlank(visitName)) {
			visit = daoFactory.getVisitsDao().getByName(visitName);
			key = visitName;
		} else if (existing != null) {
			visit = existing.getVisit();
		} else if (parent != null) {
			visit = parent.getVisit();
		} else {
			if (detail.getCpId() != null) {
				CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
				if (cp == null) {
					ose.addError(CpErrorCode.NOT_FOUND, detail.getCpId());
				} else if (cp.isSpecimenCentric()) {
					visit = getVisitFor(cp);
				}
			} else if (StringUtils.isNotBlank(detail.getCpShortTitle())) {
				CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
				if (cp == null) {
					ose.addError(CpErrorCode.NOT_FOUND, detail.getCpShortTitle());
				} else if (cp.isSpecimenCentric()) {
					visit = getVisitFor(cp);
				}
			}

			if (visit == null) {
				ose.addError(SpecimenErrorCode.VISIT_REQUIRED);
				return null;
			}
		}
		
		if (visit == null) {
			ose.addError(VisitErrorCode.NOT_FOUND, key);
			return null;
		}
		
		return visit;
	}

	private Visit getVisitFor(CollectionProtocol cp) {
		String visitName = cp.getVisitName();
		Visit cpVisit = daoFactory.getVisitsDao().getByName(visitName);
		if (cpVisit != null) {
			return cpVisit;
		}

		String ppid = cp.getPpid();
		CollectionProtocolRegistration cpReg = daoFactory.getCprDao().getCprByPpid(cp.getId(), ppid);
		if (cpReg == null) {
			CollectionProtocolRegistrationDetail cprInput = new CollectionProtocolRegistrationDetail();
			cprInput.setCpId(cp.getId());
			cprInput.setPpid(ppid);
			cprInput.setRegistrationDate(Calendar.getInstance().getTime());
			cpReg = cprFactory.createCpr(cprInput);

			daoFactory.getParticipantDao().saveOrUpdate(cpReg.getParticipant());
			daoFactory.getCprDao().saveOrUpdate(cpReg);
		}

		VisitDetail visitInput = new VisitDetail();
		visitInput.setCpId(cp.getId());
		visitInput.setPpid(ppid);
		visitInput.setName(visitName);
		visitInput.setVisitDate(Calendar.getInstance().getTime());
		visitInput.setSite(cp.getRepositories().iterator().next().getName());
		if (cp.firstEvent() != null) {
			visitInput.setEventLabel(cp.firstEvent().getEventLabel());
		}

		cpVisit = visitFactory.createVisit(visitInput);
		daoFactory.getVisitsDao().saveOrUpdate(cpVisit);
		return cpVisit;
	}
	
	private void setLineage(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		String lineage = detail.getLineage();
		if (lineage == null) {
			if (specimen.getSpecimenRequirement() == null) {
				lineage = Specimen.NEW;
			} else {
				lineage = specimen.getSpecimenRequirement().getLineage();
			}
		}
		
		if (!Specimen.isValidLineage(lineage)) {
			ose.addError(SpecimenErrorCode.INVALID_LINEAGE);
			return;
		}
		
		specimen.setLineage(lineage);
	}
	
	private void setLineage(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("lineage")) {
			setLineage(detail, specimen, ose);
		} else {
			specimen.setLineage(existing.getLineage());
		}
	}
	
	private void setParentSpecimen(SpecimenDetail detail, Specimen parent, Specimen specimen, OpenSpecimenException ose) {
		if (StringUtils.isBlank(specimen.getLineage()) || specimen.getLineage().equals(Specimen.NEW)) {
			return;
		}
		
		if (parent != null) {
			specimen.setParentSpecimen(parent);
			if (!parent.isCollected()) {
				specimen.setAutoCollectParents(true);
			}
			return;
		}
		
		Long parentId = detail.getParentId();
		String parentLabel = detail.getParentLabel();
		if (parentId != null || StringUtils.isNotBlank(parentLabel)) {
			parent = getSpecimen(parentId, detail.getCpShortTitle(), parentLabel, ose);
		} else if (specimen.getVisit() != null && specimen.getSpecimenRequirement() != null) {			
			Long visitId = specimen.getVisit().getId();
			Long srId = specimen.getSpecimenRequirement().getId();			
			parent = daoFactory.getSpecimenDao().getParentSpecimenByVisitAndSr(visitId, srId);
			if (parent == null) {
				parent = createParent(specimen);
			}
		} else {
			ose.addError(SpecimenErrorCode.PARENT_REQUIRED);
		}
		
		specimen.setParentSpecimen(parent);
		if (parent != null && !parent.isCollected()) {
			specimen.setAutoCollectParents(true);
		}
	}

	private Specimen createParent(Specimen specimen) {
		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		if (sr == null) {
			return null;
		}

		SpecimenRequirement parentSr = sr.getParentSpecimenRequirement();
		Specimen parent = parentSr.getSpecimen();
		parent.setCollectionStatus(Specimen.PENDING);
		parent.setAvailabilityStatus(Specimen.PENDING);
		parent.setVisit(specimen.getVisit());
		parent.setCollectionProtocol(specimen.getVisit().getCollectionProtocol());
		parent.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		if (!parent.isPrimary()) {
			Specimen parentOfParent = daoFactory.getSpecimenDao().getParentSpecimenByVisitAndSr(specimen.getVisit().getId(), parentSr.getId());
			if (parentOfParent == null) {
				parentOfParent = createParent(parent);
			}

			parent.setParentSpecimen(parentOfParent);
		}

		parent.setLabelIfEmpty();
		if (parent.isPrimary()) {
			daoFactory.getSpecimenDao().saveOrUpdate(parent);
		} else {
			parent.setAutoCollectParents(true);
			parent.getParentSpecimen().addChildSpecimen(parent);
		}

		return parent;
	}

	private Specimen getSpecimen(Long id, String cpShortTitle, String label, OpenSpecimenException ose) {
		Specimen specimen = null;
		if (id != null || StringUtils.isNotBlank(label)) {
			specimen = specimenResolver.getSpecimen(id, cpShortTitle, label, ose);
		}

		return specimen;
	}
	
	private void setParentSpecimen(SpecimenDetail detail, Specimen existing, Specimen parent, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("parentLabel")) {
			setParentSpecimen(detail, parent, specimen, ose);
		} else {
			specimen.setParentSpecimen(existing.getParentSpecimen());
		}
	}

	private SpecimenRequirement getSpecimenRequirement(SpecimenDetail detail, Specimen existing, Visit visit, OpenSpecimenException ose) {
		Long reqId = detail.getReqId();
		String reqCode = detail.getReqCode();

		SpecimenRequirement existingReq = null;
		if (existing != null) {
			if (!existing.isPrimary() || existing.getVisit().equals(visit)) {
				existingReq = existing.getSpecimenRequirement();
			}
		}

		if (reqId == null && !detail.isAttrModified("reqId") &&
			!isReqCodeSpecified(detail, visit) && !detail.isAttrModified("reqCode")) {
			return existingReq;
		}

		Long existingReqId = existingReq != null ? existingReq.getId() : null;
		if (reqId != null && reqId.equals(existingReqId)) {
			return existingReq;
		}
		
		String existingReqCode = existingReq != null ? existingReq.getCode() : null;
		if (reqCode != null && reqCode.equals(existingReqCode)) {
			return existingReq;
		}

		Object key = null;
		SpecimenRequirement sr = null;
		if (reqId != null) {
			key = reqId;
			sr = daoFactory.getSpecimenRequirementDao().getById(reqId);
		} else if (detail.getCpId() != null && StringUtils.isNotBlank(reqCode) && visit != null && visit.getCpEvent() != null) {
			key = detail.getCpId() + ":" + reqCode;
			sr = daoFactory.getSpecimenRequirementDao().getByCpEventLabelAndSrCode(
				detail.getCpId(), visit.getCpEvent().getEventLabel(), reqCode);
		} else if (StringUtils.isNotBlank(detail.getCpShortTitle()) && StringUtils.isNotBlank(reqCode) && visit != null && visit.getCpEvent() != null) {
			key = detail.getCpShortTitle() + ":" + reqCode;
			sr = daoFactory.getSpecimenRequirementDao().getByCpEventLabelAndSrCode(
				detail.getCpShortTitle(), visit.getCpEvent().getEventLabel(), reqCode);
		}
		
		if (key != null && sr == null) {
			if (reqId == null) {
				CollectionProtocol cp = null;
				Object cpKey = null;
				if (detail.getCpId() != null) {
					cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
					cpKey = detail.getCpId();
				} else if (StringUtils.isNotBlank(detail.getCpShortTitle())) {
					cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(detail.getCpShortTitle());
					cpKey = detail.getCpShortTitle();
				}

				if (cp == null && cpKey != null) {
					ose.addError(CpErrorCode.NOT_FOUND, cpKey);
					return null;
				}
			}

			ose.addError(SrErrorCode.NOT_FOUND);
			return null;
		}

		return sr;
	}

	private boolean isReqCodeSpecified(SpecimenDetail detail, Visit visit) {
		return (detail.getCpId() != null || StringUtils.isNotBlank(detail.getCpShortTitle())) && // cp
			visit != null && visit.getCpEvent() != null &&         // visit
			StringUtils.isNotBlank(detail.getReqCode());           // req code
	}
	
	private void setCollectionStatus(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		String status = detail.getStatus();
		if (StringUtils.isBlank(status)) {
			status = Specimen.COLLECTED;
		}

		if (!status.equals(Specimen.COLLECTED) && 
			!status.equals(Specimen.PENDING) && 
			!status.equals(Specimen.MISSED_COLLECTION) &&
			!status.equals(Specimen.NOT_COLLECTED)) {
			ose.addError(SpecimenErrorCode.INVALID_COLL_STATUS, status);
			return;
		}

		specimen.setCollectionStatus(status);
	}
	
	private void setCollectionStatus(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("status")) {
			setCollectionStatus(detail, specimen, ose);
		} else {
			specimen.setCollectionStatus(existing.getCollectionStatus());
		}
	}

	private void setAnatomicSite(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		String anatomicSite = detail.getAnatomicSite();

		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		boolean fromParent = shouldUseParentValue(specimen, anatomicSite, sr != null ? sr.getAnatomicSite() : null);
		if (fromParent) {
			if (specimen.getParentSpecimen() != null) {
				specimen.setTissueSite(specimen.getParentSpecimen().getTissueSite());

				String parentValue = PermissibleValue.getValue(specimen.getParentSpecimen().getTissueSite());
				if (!isEmptyOrEquals(anatomicSite, parentValue)) {
					ose.addError(SpecimenErrorCode.ANATOMIC_SITE_NOT_SAME_AS_PARENT, anatomicSite, parentValue);
				}
			}

		} else if (isNotSpecified(anatomicSite, false)) {
			specimen.setTissueSite(sr != null ? sr.getAnatomicSite() : getNotSpecified(SPECIMEN_ANATOMIC_SITE));
		} else {
			PermissibleValue site = getPv(SPECIMEN_ANATOMIC_SITE, anatomicSite, true);
			if (site == null) {
				ose.addError(SpecimenErrorCode.INVALID_ANATOMIC_SITE, anatomicSite);
				return;
			}

			specimen.setTissueSite(site);
		}
	}
	
	private void setAnatomicSite(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("anatomicSite")) {
			setAnatomicSite(detail, specimen, ose);
		} else {
			specimen.setTissueSite(existing.getTissueSite());
		}
	}

	private void setLaterality(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		String laterality = detail.getLaterality();

		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		boolean fromParent = shouldUseParentValue(specimen, laterality, sr != null ? sr.getLaterality() : null);
		if (fromParent) {
			if (specimen.getParentSpecimen() != null) {
				specimen.setTissueSide(specimen.getParentSpecimen().getTissueSide());

				String parentValue = PermissibleValue.getValue(specimen.getParentSpecimen().getTissueSide());
				if (!isEmptyOrEquals(laterality, parentValue)) {
					ose.addError(SpecimenErrorCode.LATERALITY_NOT_SAME_AS_PARENT, laterality, parentValue);
				}
			}
		} else if (isNotSpecified(laterality, false)) {
			specimen.setTissueSide(sr != null ? sr.getLaterality() : getNotSpecified(SPECIMEN_LATERALITY));
		} else {
			PermissibleValue pv = getPv(SPECIMEN_LATERALITY, laterality, false);
			if (pv == null) {
				ose.addError(SpecimenErrorCode.INVALID_LATERALITY, laterality);
				return;
			}

			specimen.setTissueSide(pv);
		}
	}
	
	private void setLaterality(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("laterality")) {
			setLaterality(detail, specimen, ose);
		} else {
			specimen.setTissueSide(existing.getTissueSide());
		}
	}
	
	private void setPathologicalStatus(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {		
		String pathology = detail.getPathology();

		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		boolean fromParent = shouldUseParentValue(specimen, pathology, sr != null ? sr.getPathologyStatus() : null);
		if (fromParent) {
			if (specimen.getParentSpecimen() != null) {
				PermissibleValue parentValue = specimen.getParentSpecimen().getPathologicalStatus();
				specimen.setPathologicalStatus(parentValue);

				if (!isEmptyOrEquals(pathology, PermissibleValue.getValue(parentValue))) {
					ose.addError(SpecimenErrorCode.PATHOLOGY_NOT_SAME_AS_PARENT, pathology, parentValue);
				}
			}
		} else if (isNotSpecified(pathology, false)) {
			specimen.setPathologicalStatus(sr != null ? sr.getPathologyStatus() : getNotSpecified(PATH_STATUS));
		} else {
			PermissibleValue pv = getPv(PATH_STATUS, pathology, false);
			if (pv == null) {
				ose.addError(SpecimenErrorCode.INVALID_PATHOLOGY_STATUS, pathology);
				return;
			}

			specimen.setPathologicalStatus(pv);
		}
	}
	
	private void setPathologicalStatus(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("pathology")) {
			setPathologicalStatus(detail, specimen, ose);
		} else {
			specimen.setPathologicalStatus(existing.getPathologicalStatus());
		}
	}
	
	private void setQuantity(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("initialQty")) {
			setInitialQty(detail, specimen, ose);
		} else {
			specimen.setInitialQuantity(existing.getInitialQuantity());
		}
		
		if (existing == null || existing.isPending() || detail.isAttrModified("availableQty")) {
			setAvailableQty(detail, existing, specimen, ose);
		} else {
			specimen.setAvailableQuantity(existing.getAvailableQuantity());
		}		
	}
	
	private void setInitialQty(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		BigDecimal qty = detail.getInitialQty();
		if (NumUtil.lessThanZero(qty)) {
			ose.addError(SpecimenErrorCode.INVALID_QTY);
			return;
		}

		if (specimen.isAliquot() && qty == null) {
			if (specimen.getSpecimenRequirement() != null) {
				qty = specimen.getSpecimenRequirement().getInitialQuantity();
			}
			
			if (qty == null && isAliquotQtyReq()) {
				ose.addError(SpecimenErrorCode.ALIQUOT_QTY_REQ);
				return;
			}
		}

		specimen.setInitialQuantity(qty);
	}
	
	private void setAvailableQty(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		BigDecimal availableQty = detail.getAvailableQty();
		if (availableQty == null && (existing == null || existing.isPending())) {
			availableQty = specimen.getInitialQuantity();
		}
		
		if (NumUtil.lessThanZero(availableQty)){
			ose.addError(SpecimenErrorCode.INVALID_QTY);
			return;
		}

		if (specimen.isAliquot() && availableQty == null && isAliquotQtyReq()) {
			ose.addError(SpecimenErrorCode.ALIQUOT_QTY_REQ);
			return;
		}

		if (NumUtil.lessThan(specimen.getInitialQuantity(), availableQty)) {
			ose.addError(SpecimenErrorCode.AVBL_QTY_GT_INIT_QTY);
			return;
		}

		specimen.setAvailableQuantity(availableQty);
	}
	
	private void setConcentration(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("concentration")) {
			setConcentration(detail, specimen, ose);			
		} else {
			specimen.setConcentration(existing.getConcentration());
		}
	}
	
	private void setConcentration(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		Specimen parent = specimen.getParentSpecimen();
		if (!specimen.isAliquot() || detail.getConcentration() != null) {
			specimen.setConcentration(detail.getConcentration());
		} else if (parent != null){
			specimen.setConcentration(parent.getConcentration());
		}
	}
	
	private void setSpecimenClass(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.getParentSpecimen() != null && specimen.isAliquot()) {
			specimen.setSpecimenClass(specimen.getParentSpecimen().getSpecimenClass());
			return;
		}
		
		if (specimen.isAliquot()) {
			return; // parent not specified case
		}
				
		String specimenClass = detail.getSpecimenClass();
		PermissibleValue classPv = null;
		if (StringUtils.isBlank(specimenClass)) {
			if (specimen.getSpecimenRequirement() != null) {
				classPv = specimen.getSpecimenRequirement().getSpecimenClass();
			} else if (StringUtils.isNotBlank(detail.getType())) {
				PermissibleValue typePv = getPv(SPECIMEN_CLASS, detail.getType(), false);
				if (typePv != null) {
					classPv = typePv.getParent();
				}
			}

			if (classPv == null) {
				ose.addError(SpecimenErrorCode.SPECIMEN_CLASS_REQUIRED);
			} else {
				detail.setSpecimenClass(classPv.getValue());
			}
		} else {
			classPv = getPv(SPECIMEN_CLASS, specimenClass, false);
			if (classPv == null) {
				ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, specimenClass);
			}
		}
		
		specimen.setSpecimenClass(classPv);
	}
	
	private void setSpecimenClass(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("specimenClass")) {
			setSpecimenClass(detail, specimen, ose);
		} else {
			specimen.setSpecimenClass(existing.getSpecimenClass());
		}
	}
	
	private void setSpecimenType(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.getParentSpecimen() != null && specimen.isAliquot()) {
			specimen.setSpecimenType(specimen.getParentSpecimen().getSpecimenType());
			return;
		}
		
		if (specimen.isAliquot()) {
			return; // parent not specified case
		}
		
		String type = detail.getType();
		if (StringUtils.isBlank(type)) {
			if (specimen.getSpecimenRequirement() == null) {
				ose.addError(SpecimenErrorCode.SPECIMEN_TYPE_REQUIRED);
			}
			
			return;
		}

		PermissibleValue typePv = getPv(SPECIMEN_CLASS, detail.getSpecimenClass(), type);
		if (typePv == null || typePv.getParent() == null) {
			String args = type;
			if (StringUtils.isNotBlank(detail.getSpecimenClass())) {
				args = detail.getSpecimenClass() + ": " + type;
			}

			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, args);
			return;
		}
		
		specimen.setSpecimenType(typePv);
	}
	
	private void setSpecimenType(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("type")) {
			setSpecimenType(detail, specimen, ose);
		} else {
			specimen.setSpecimenType(existing.getSpecimenType());
		}
	}

	private void setCreatedOn(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (!specimen.isCollected()) {
			//
			// Created on date/time doesn't have any meaning unless the specimen is collected
			//
			return;
		}

		//
		// user specified date is preferred
		//
		if (detail.getCreatedOn() != null) {
			specimen.setCreatedOn(detail.getCreatedOn());
			return;
		}

		//
		// existing date is copied
		//
		if (existing != null && existing.isCollected()) {
			specimen.setCreatedOn(existing.getCreatedOn());
			return;
		}

		//
		// there is either no pre-existing specimen or the pre-existing specimen is not collected
		//
		ReceivedEventDetail recvEvent = detail.getReceivedEvent();
		if (specimen.isPrimary() && recvEvent != null && recvEvent.getTime() != null) {
			specimen.setCreatedOn(recvEvent.getTime());
		} else {
			specimen.setCreatedOn(Calendar.getInstance().getTime());
		}
	}
	
	private void setBiohazards(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		Specimen parentSpecimen = specimen.getParentSpecimen();
		
		if (specimen.isAliquot()) {
			if (parentSpecimen != null) {
				specimen.setBiohazards(new HashSet<>(parentSpecimen.getBiohazards()));
			}
			
			return;
		}
		
		Set<String> biohazards = detail.getBiohazards();
		if (CollectionUtils.isEmpty(biohazards)) {
			return;
		}

		List<PermissibleValue> biohazardPvs = getPvs(BIOHAZARD, biohazards);
		if (biohazardPvs.size() != biohazards.size()) {
			ose.addError(SpecimenErrorCode.INVALID_BIOHAZARDS);
			return;
		}
		
		specimen.setBiohazards(new HashSet<>(biohazardPvs));
	}
	
	private void setBiohazards(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("biohazards")) {
			setBiohazards(detail, specimen, ose);
		} else {
			specimen.setBiohazards(existing.getBiohazards());
		}
	}

	private void setFreezeThawCycles(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (detail.getFreezeThawCycles() == null) {
			return;
		}

		if (detail.getFreezeThawCycles() < 0) {
			ose.addError(SpecimenErrorCode.INVALID_FREEZE_THAW_CYCLES, detail.getFreezeThawCycles());
			return;
		}

		specimen.setFreezeThawCycles(detail.getFreezeThawCycles());
	}

	private void setFreezeThawCycles(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("freezeThawCycles")) {
			setFreezeThawCycles(detail, specimen, ose);
		} else {
			specimen.setFreezeThawCycles(existing.getFreezeThawCycles());
		}
	}

	private void setExternalIds(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(detail.getExternalIds())) {
			return;
		}

		Set<String> sources = new HashSet<>();
		Set<SpecimenExternalIdentifier> externalIds = new HashSet<>();
		for (NameValuePair id : detail.getExternalIds()) {
			if (StringUtils.isBlank(id.getName()) && StringUtils.isBlank(id.getValue())) {
				continue;
			}

			if (StringUtils.isBlank(id.getName()) || StringUtils.isBlank(id.getValue())) {
				ose.addError(SpecimenErrorCode.EXT_ID_NO_NAME_VALUE);
				break;
			}

			if (!sources.add(id.getName())) {
				ose.addError(SpecimenErrorCode.EXT_ID_DUP_NAME, id.getName());
				break;
			}

			SpecimenExternalIdentifier externalId = new SpecimenExternalIdentifier();
			externalId.setSpecimen(specimen);
			externalId.setName(id.getName());
			externalId.setValue(id.getValue());
			externalIds.add(externalId);
		}

		specimen.setExternalIds(externalIds);
	}

	private void setExternalIds(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("externalIds")) {
			setExternalIds(detail, specimen, ose);
		} else {
			specimen.setExternalIds(existing.getExternalIds());
		}
	}

	private void setComments(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("comments")) {
			specimen.setComment(detail.getComments());
		} else {
			specimen.setComment(existing.getComment());
		}
	}

	private void setSpecimenPosition(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (Boolean.TRUE.equals(detail.getCheckout())) {
			detail.setStorageLocation(null);
		}

		StorageContainerPosition position = getPosition(
			specimen, detail.getStorageLocation(),
			detail.getContainerLocation(), detail.getContainerTypeId(), detail.getContainerTypeName(),
			ose);

		specimen.setTransferUser(getUser(detail.getTransferUser(), ose));;
		specimen.setTransferTime(detail.getTransferTime());
		specimen.setTransferComments(detail.getTransferComments());
		if (StringUtils.isBlank(specimen.getTransferComments())) {
			specimen.setTransferComments(detail.getOpComments());
		}

		specimen.setPosition(position);
		if (position != null) {
			position.setOccupyingSpecimen(specimen);
		}
		specimen.setCheckout(detail.getCheckout());
	}

	private void setSpecimenPosition(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("storageLocation") || Boolean.TRUE.equals(detail.getCheckout())) {
			setSpecimenPosition(detail, specimen, ose);
		} else if (existing.getPosition() != null) {
			//
			// Validate container restrictions if specimen class or specimen type is modified
			//
			if (detail.isAttrModified("specimenClass") || detail.isAttrModified("specimenType")) {
				StorageContainer container = existing.getPosition().getContainer();
				if (!container.canContain(specimen)) {
					ose.addError(StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
					return;
				}
			}
			
			specimen.setPosition(existing.getPosition());
		}
	}

	private void setHoldingLocation(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (detail.getHoldingLocation() == null || StringUtils.isBlank(detail.getHoldingLocation().getName())) {
			return;
		}

		if (detail.getDpId() == null) {
			ose.addError(DistributionProtocolErrorCode.DP_REQUIRED);
			return;
		}

		DistributionProtocol dp = daoFactory.getDistributionProtocolDao().getById(detail.getDpId());
		if (dp == null) {
			ose.addError(DistributionProtocolErrorCode.NOT_FOUND, detail.getDpId());
			return;
		}

		specimen.setDp(dp);
		specimen.setHoldingLocation(getPosition(specimen, detail.getHoldingLocation(), ose));
	}
	
	private void setCollectionDetail(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.isAliquot() || specimen.isDerivative()) {
			return;
		}
				
		CollectionEventDetail collDetail = detail.getCollectionEvent();
		if (collDetail == null) {
			return;
		}
		
		SpecimenCollectionEvent event = SpecimenCollectionEvent.getFor(specimen);
		setEventAttrs(collDetail, false, event, ose);

		String collCont = collDetail.getContainer();
		if (StringUtils.isNotBlank(collCont)) {
			PermissibleValue contPv = getPv(CONTAINER, collCont, false);
			if (contPv != null) {
				event.setContainer(contPv);
			} else {
				ose.addError(SpecimenErrorCode.INVALID_COLL_CONTAINER, collCont);
			}

		}
			
		String proc = collDetail.getProcedure();
		if (StringUtils.isNotBlank(proc)) {
			PermissibleValue procPv = getPv(COLL_PROC, proc, false);
			if (procPv != null) {
				event.setProcedure(procPv);
			} else {
				ose.addError(SpecimenErrorCode.INVALID_COLL_PROC, proc);
			}
		}
		
		specimen.setCollectionEvent(event);
	}
	
	private void setCollectionDetail(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("collectionEvent")) {
			setCollectionDetail(detail, specimen, ose);
		} else {
			specimen.setCollectionEvent(existing.getCollectionEvent());
		}
	}
	
	private void setReceiveDetail(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.isAliquot() || specimen.isDerivative()) {
			return;			
		}
		
		ReceivedEventDetail recvDetail = detail.getReceivedEvent();
		if (recvDetail == null) {
			return;
		}
		
		SpecimenReceivedEvent event = SpecimenReceivedEvent.getFor(specimen);
		String recvQuality = recvDetail.getReceivedQuality();
		if (StringUtils.isNotBlank(recvQuality)) {
			PermissibleValue recvPv = getPv(RECV_QUALITY, recvQuality, false);
			if (recvPv != null) {
				event.setQuality(recvPv);
			} else {
				ose.addError(SpecimenErrorCode.INVALID_RECV_QUALITY, recvQuality);
			}
		}

		setEventAttrs(recvDetail, !event.isReceived(), event, ose);
		specimen.setReceivedEvent(event);
	}
	
	private void setReceiveDetail(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("receivedEvent")) {
			setReceiveDetail(detail, specimen, ose);
		} else {
			specimen.setReceivedEvent(existing.getReceivedEvent());
		}
	}
	
	private void setEventAttrs(SpecimenEventDetail detail, boolean skipUserTime, SpecimenEvent event, OpenSpecimenException ose) {
		if (StringUtils.isNotBlank(detail.getComments())) {
			event.setComments(detail.getComments());
		}

		if (skipUserTime) {
			event.setUser(null);
			event.setTime(null);
		} else {
			User user = getUser(detail, ose);
			if (user != null) {
				event.setUser(user);
			}

			if (detail.getTime() != null) {
				event.setTime(detail.getTime());
			}
		}
	}
	
	private void setExtension(SpecimenDetail detail, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.getCollectionProtocol() == null) {
			return;
		}

		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), specimen);
		specimen.setExtension(extension);
	}
	
	private void setExtension(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("extensionDetail")) {
			setExtension(detail, specimen, ose);
		} else {
			specimen.setExtension(existing.getExtension());
		}
	}

	private void setCreatedBy(SpecimenDetail detail, Specimen existing, Specimen specimen, OpenSpecimenException ose) {
		if (specimen.isPrimary()) {
			return;
		}

		if (existing == null || detail.isAttrModified("createdBy")) {
			specimen.setCreatedBy(getUser(detail.getCreatedBy(), ose));
		}
	}
	
	private User getUser(SpecimenEventDetail detail, OpenSpecimenException ose) {
		return getUser(detail.getUser(), ose);
	}

	private User getUser(UserSummary input, OpenSpecimenException ose) {
		if (input == null) {
			return null;
		}

		User user = null;
		if (input.getId() != null) {
			user = daoFactory.getUserDao().getById(input.getId());
		} else if (StringUtils.isNotBlank(input.getEmailAddress())) {
			user = daoFactory.getUserDao().getUserByEmailAddress(input.getEmailAddress());
		}

		if (user == null) {
			ose.addError(UserErrorCode.NOT_FOUND);
		}

		return user;
	}

	private boolean shouldUseParentValue(Specimen specimen, String value, PermissibleValue anticipatedValue) {
		boolean fromParent = false;
		if (specimen.isAliquot()) {
			fromParent = true;
		} else if (specimen.isDerivative()) {
			if (isNotSpecified(value, true) && isNotSpecified(PermissibleValue.getValue(anticipatedValue), true)) {
				fromParent = true;
			}
		}

		return fromParent;
	}

	private boolean isNotSpecified(String value, boolean trueIfNotSpecified) {
		return StringUtils.isBlank(value) || (trueIfNotSpecified && Specimen.NOT_SPECIFIED.equalsIgnoreCase(value));
	}

	private boolean isAliquotQtyReq() {
		return ConfigUtil.getInstance().getBoolSetting(ConfigParams.MODULE, ConfigParams.ALIQUOT_QTY_REQ, true);
	}

	private boolean isEmptyOrEquals(String input, String expected) {
		return !ImporterContextHolder.getInstance().isImportOp() || StringUtils.isBlank(input) || input.equals(expected);
	}

	private StorageContainerPosition getPosition(Specimen specimen, StorageLocationSummary location, OpenSpecimenException ose) {
		return getPosition(specimen, location, null, null, null, ose);
	}

	private StorageContainerPosition getPosition(
		Specimen specimen, StorageLocationSummary location,
		StorageLocationSummary containerLocation, Long containerTypeId, String containerTypeName,
		OpenSpecimenException ose) {

		if (isVirtual(location) || !specimen.isCollected()) {
			//
			// When specimen location is virtual or specimen is
			// not collected - pending / missed collection
			//
			return null;
		}

		StorageContainer container = null;
		Object key = null;
		if (location.getId() != null && location.getId() != -1) {
			key = location.getId();
			container = daoFactory.getStorageContainerDao().getById(location.getId());
		} else {
			key = location.getName();
			container = daoFactory.getStorageContainerDao().getByName(location.getName());

			if (container == null) {
				//
				// Check the possibility of auto creating container
				//
				container = createContainer(location.getName(), containerLocation, containerTypeId, containerTypeName, ose);
				ose.checkAndThrow();
			}
		}

		if (container == null) {
			ose.addError(StorageContainerErrorCode.NOT_FOUND, key, 1);
			return null;
		} else if (container.isArchived()) {
			ose.addError(StorageContainerErrorCode.ARCHIVED, container.getName());
			return null;
		}

		if (!container.canContain(specimen)) {
			ose.addError(StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
			return null;
		}

		String posOne = null, posTwo = null;
		if (!container.isDimensionless()) {
			posOne = location.getPositionX();
			posTwo = location.getPositionY();
			if (container.usesLinearLabelingMode() && location.getPosition() != null && location.getPosition() != 0) {
				Pair<Integer, Integer> coord = container.getPositionAssigner().fromPosition(container, location.getPosition());
				posTwo = coord.first().toString();
				posOne = coord.second().toString();
			}
		}

		StorageContainerPosition position = null;
		if (StringUtils.isNotBlank(posOne) && StringUtils.isNotBlank(posTwo)) {
			if (StringUtils.isBlank(location.getReservationId())) {
				if (container.canSpecimenOccupyPosition(specimen.getId(), posOne, posTwo)) {
					position = container.createPosition(posOne, posTwo);
					container.setLastAssignedPos(position);
				} else {
					ose.addError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
				}
			} else {
				position = container.getReservedPosition(posTwo, posOne, location.getReservationId());
				if (position != null) {
					container.removePosition(position);
					SessionUtil.getInstance().flush(); // Ugly but need to ensure the database is consistent with in-memory
					position = container.createPosition(posOne, posTwo);
				} else if (container.canSpecimenOccupyPosition(specimen.getId(), posOne, posTwo)) {
					position = container.createPosition(posOne, posTwo);
				} else {
					// TODO: no free space, improve error code
					ose.addError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
				}

				if (position != null) {
					container.setLastAssignedPos(position);
				}
			}
		} else {
			position = container.nextAvailablePosition(true);
			if (position == null) {
				ose.addError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}
		}

		return position;
	}

	private boolean isVirtual(StorageLocationSummary location) {
		if (location == null) {
			return true;
		}

		if (location.getId() != null && location.getId() != -1) {
			return false;
		}

		if (StringUtils.isNotBlank(location.getName())) {
			return false;
		}

		return true;
	}

	private StorageContainer createContainer(
		String name,
		StorageLocationSummary containerLocation, Long containerTypeId, String containerTypeName,
		OpenSpecimenException ose) {

		if (containerLocation == null && containerTypeId == null && StringUtils.isBlank(containerTypeName)) {
			//
			// no auto creation of containers
			//
			return null;
		} else if (containerLocation == null) {
			//
			// auto creation but parent container details missing
			//
			ose.addError(SpecimenErrorCode.PARENT_CONTAINER_REQ);
			return null;
		} else if (containerTypeId == null && StringUtils.isBlank(containerTypeName)) {
			//
			// auto creation but container type details missing
			//
			ose.addError(SpecimenErrorCode.CONTAINER_TYPE_REQ);
			return null;
		}

		//
		// row and columns will be picked from container type
		//
		StorageContainerDetail containerDetail = new StorageContainerDetail();
		containerDetail.setName(name);
		containerDetail.setTypeId(containerTypeId);
		containerDetail.setTypeName(containerTypeName);
		containerDetail.setStorageLocation(containerLocation);
		return containerSvc.createStorageContainer(null, containerDetail);
	}

	private PermissibleValue getNotSpecified(String attribute) {
		return getPv(attribute, Specimen.NOT_SPECIFIED, true);
	}

	private PermissibleValue getPv(String attribute, String value, boolean leafNode) {
		return daoFactory.getPermissibleValueDao().getPv(attribute, value, leafNode);
	}

	private PermissibleValue getPv(String attribute, String parentValue, String value) {
		return daoFactory.getPermissibleValueDao().getPv(attribute, parentValue, value);
	}

	private List<PermissibleValue> getPvs(String attribute, Collection<String> values) {
		return daoFactory.getPermissibleValueDao().getPvs(attribute, values);
	}
}
