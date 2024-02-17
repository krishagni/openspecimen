
package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolRegistrationFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CprErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolRegistrationDetail;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;


public class CollectionProtocolRegistrationFactoryImpl implements CollectionProtocolRegistrationFactory {
	private DaoFactory daoFactory;

	private ParticipantFactory participantFactory;

	public void setParticipantFactory(ParticipantFactory participantFactory) {
		this.participantFactory = participantFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	@Override
	public CollectionProtocolRegistration createCpr(CollectionProtocolRegistrationDetail detail) {
		return createCpr(null, detail);
	}

	@Override
	public CollectionProtocolRegistration createCpr(CollectionProtocolRegistration existing, CollectionProtocolRegistrationDetail detail) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		CollectionProtocolRegistration cpr = new CollectionProtocolRegistration();
		cpr.setId(detail.getId());
		cpr.setForceDelete(detail.isForceDelete());
		cpr.setOpComments(detail.getOpComments());

		setCollectionProtocol(detail, existing, cpr, ose);
		setPpid(detail, existing, cpr, ose);
		setBarcode(detail, existing, cpr, ose);
		setRegDate(detail, existing, cpr, ose);
		setExternalSubjectId(detail, existing, cpr, ose);
		setSite(detail, existing, cpr, ose);
		setActivityStatus(detail, existing, cpr, ose);
		setDataEntryStatus(detail, existing, cpr, ose);
		setParticipant(detail, existing, cpr, ose);
		setExtension(detail, existing, cpr, ose);

		ose.checkAndThrow();
		return cpr;
	}

	private void setBarcode(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {

		if (existing == null || detail.isAttrModified("barcode")) {
			cpr.setBarcode(detail.getBarcode());
		} else {
			cpr.setBarcode(existing.getBarcode());
		}
	}

	private void setRegDate(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (detail.getRegistrationDate() == null) {
			ose.addError(CprErrorCode.REG_DATE_REQUIRED);
			return;
		}
		
		cpr.setRegistrationDate(detail.getRegistrationDate());
	}

	private void setRegDate(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {

		if (existing == null || detail.isAttrModified("registrationDate")) {
			setRegDate(detail, cpr, ose);
		} else {
			cpr.setRegistrationDate(existing.getRegistrationDate());
		}
	}

	private void setExternalSubjectId(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		cpr.setExternalSubjectId(detail.getExternalSubjectId());
	}

	private void setExternalSubjectId(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("externalSubjectId")) {
			setExternalSubjectId(detail, cpr, ose);
		} else {
			cpr.setExternalSubjectId(existing.getExternalSubjectId());
		}
	}

	private void setSite(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getSite())) {
			return;
		}

		Site site = daoFactory.getSiteDao().getSiteByName(detail.getSite());
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, detail.getSite());
			return;
		}

		cpr.setSite(site);
	}

	private void setSite(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("site")) {
			setSite(detail, cpr, ose);
		} else {
			cpr.setSite(existing.getSite());
		}
	}

	private void setActivityStatus(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		
		if (StringUtils.isBlank(activityStatus)) {
			cpr.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(activityStatus)) {
			cpr.setActivityStatus(activityStatus);
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID);
		}
	}

	private void setActivityStatus(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {

		if (existing == null || detail.isAttrModified("activityStatus")) {
			setActivityStatus(detail, cpr, ose);
		} else {
			cpr.setActivityStatus(existing.getActivityStatus());
		}
	}

	private void setDataEntryStatus(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		String deStatus = detail.getDataEntryStatus();
		if (StringUtils.isBlank(deStatus)) {
			cpr.setDataEntryStatus(BaseEntity.DataEntryStatus.COMPLETE);
		} else {
			try {
				cpr.setDataEntryStatus(BaseEntity.DataEntryStatus.valueOf(deStatus));
			} catch (Exception e) {
				ose.addError(CommonErrorCode.INVALID_INPUT, "Invalid data entry status: " + deStatus);
			}
		}
	}

	private void setDataEntryStatus(
		CollectionProtocolRegistrationDetail detail,
		CollectionProtocolRegistration existing,
		CollectionProtocolRegistration cpr,
		OpenSpecimenException ose) {

		if (existing == null || detail.isAttrModified("dataEntryStatus")) {
			setDataEntryStatus(detail, cpr, ose);
		} else {
			cpr.setDataEntryStatus(existing.getDataEntryStatus());
		}

		if (cpr.getDataEntryStatus() == BaseEntity.DataEntryStatus.DRAFT &&
			cpr.getCollectionProtocol() != null && !cpr.getCollectionProtocol().draftDataEntryEnabled()) {
			ose.addError(CprErrorCode.DRAFT_NOT_ALLOWED, cpr.getCollectionProtocol().getShortTitle());
		}
	}

	private void setCollectionProtocol(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {
				
		Long cpId = detail.getCpId();
		String title = detail.getCpTitle();
		String shortTitle = detail.getCpShortTitle();
		
		CollectionProtocol cp = null;
		if (cpId != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(detail.getCpId());
		} else if (StringUtils.isNotBlank(title)) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		} else if (StringUtils.isNotBlank(shortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		} else {
			ose.addError(CprErrorCode.CP_REQUIRED);
			return;
		} 
		
		if (cp == null) {
			ose.addError(CpErrorCode.NOT_FOUND);
		} else if (Status.isClosedStatus(cp.getActivityStatus())) {
			ose.addError(CprErrorCode.CP_CLOSED);
		} else if (!Status.isActiveStatus(cp.getActivityStatus())) {
			ose.addError(CpErrorCode.NOT_FOUND);
		}

		cpr.setCollectionProtocol(cp);
	}

	private void setCollectionProtocol(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {

		if (existing == null) {
			setCollectionProtocol(detail, cpr, ose);
		} else {
			cpr.setCollectionProtocol(existing.getCollectionProtocol());
		}
	}

	private void setPpid(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr, 
			OpenSpecimenException ose) {

		if (existing == null || detail.isAttrModified("ppid")) {
			cpr.setPpid(detail.getPpid());
		} else {
			cpr.setPpid(existing.getPpid());
		}
	}
	
	private void setParticipant(
			CollectionProtocolRegistrationDetail detail,
			CollectionProtocolRegistration existing,
			CollectionProtocolRegistration cpr,
			OpenSpecimenException ose) {
		
		ParticipantDetail participantDetail = detail.getParticipant();
		if (participantDetail == null) {
			participantDetail = new ParticipantDetail();
		}

		if (existing != null && participantDetail.getId() == null) {
			participantDetail.setId(existing.getParticipant().getId());
		}

		if (cpr.getCollectionProtocol() != null) {
			participantDetail.setCpId(cpr.getCollectionProtocol().getId());
		}

		if (cpr.getDataEntryStatus() != null) {
			participantDetail.setDataEntryStatus(cpr.getDataEntryStatus().name());
		}

		Participant participant;
		if (participantDetail.getId() == null || participantDetail.getId() == -1L) {
			//
			// -1L is typically used when participant is sourced from an external database
			// using lookup or matching services
			//
			participant = participantFactory.createParticipant(participantDetail);			
			if (participant == null) {
				ose.addError(CprErrorCode.PARTICIPANT_DETAIL_REQUIRED);
			}
		} else {
			participant = daoFactory.getParticipantDao().getById(participantDetail.getId());
			if (participant == null) {
				ose.addError(ParticipantErrorCode.NOT_FOUND);
			} else {
				participant = participantFactory.createParticipant(participant, participantDetail);
			}			
		}
		
		if (participant == null) {
			return;
		}
		
		cpr.setParticipant(participant);
	}

	private void setExtension(CollectionProtocolRegistrationDetail detail, CollectionProtocolRegistration existing, CollectionProtocolRegistration cpr, OpenSpecimenException ose) {
		if (existing == null || detail.isAttrModified("dataEntryStatus")) {
			if (StringUtils.isBlank(detail.getDataEntryStatus())) {
				cpr.setDataEntryStatus(BaseEntity.DataEntryStatus.COMPLETE);
			} else {
				try {
					cpr.setDataEntryStatus(BaseEntity.DataEntryStatus.valueOf(detail.getDataEntryStatus()));
				} catch (Exception e) {
					ose.addError(CommonErrorCode.INVALID_INPUT, "Invalid data entry status: " + detail.getDataEntryStatus());
				}
			}
		} else {
			cpr.setDataEntryStatus(existing.getDataEntryStatus());
		}

		if (existing == null || detail.getParticipant().isAttrModified("extensionDetail")) {
			ExtensionDetail input = detail.getParticipant().getExtensionDetail();
			if (input == null) {
				input = new ExtensionDetail();
			}

			DeObject extension = DeObject.createExtension(input, cpr);
			cpr.setExtension(extension);
		} else {
			cpr.setExtension(existing.getExtension());
		}
	}
}