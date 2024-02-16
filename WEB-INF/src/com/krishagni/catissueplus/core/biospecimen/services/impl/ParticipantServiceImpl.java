
package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.impl.ParticipantLookupFactoryImpl;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipantsList;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.MpiGenerator;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ParticipantServiceImpl implements ParticipantService, ObjectAccessor {
	private static final LogUtil logger = LogUtil.getLogger(ParticipantServiceImpl.class);

	private DaoFactory daoFactory;

	private ParticipantFactory participantFactory;

	private ParticipantLookupFactoryImpl lookupFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setParticipantFactory(ParticipantFactory participantFactory) {
		this.participantFactory = participantFactory;
	}

	public void setLookupFactory(ParticipantLookupFactoryImpl lookupFactory) {
		this.lookupFactory = lookupFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantDetail> getParticipant(RequestEvent<Long> req) {
		Participant participant = daoFactory.getParticipantDao().getById(req.getPayload());
		if (participant == null) {
			return ResponseEvent.userError(ParticipantErrorCode.NOT_FOUND);
		}

		boolean phiAccess = AccessCtrlMgr.getInstance().ensureReadParticipantRights(participant);
		return ResponseEvent.response(ParticipantDetail.from(participant, !phiAccess));
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantDetail> createParticipant(RequestEvent<ParticipantDetail> req) {
		try {
			Participant participant = participantFactory.createParticipant(req.getPayload());
			participant = createParticipant(participant);
			return ResponseEvent.response(ParticipantDetail.from(participant, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantDetail> updateParticipant(RequestEvent<ParticipantDetail> req) {
		try {
			ParticipantDetail detail = req.getPayload();
			Participant existing = getParticipant(detail, true);
			if (existing == null) {
				return ResponseEvent.userError(ParticipantErrorCode.NOT_FOUND);
			}

			
			Participant participant = participantFactory.createParticipant(detail);
			updateParticipant(existing, participant);			
			return ResponseEvent.response(ParticipantDetail.from(existing, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	public ResponseEvent<ParticipantDetail> patchParticipant(RequestEvent<ParticipantDetail> req) {
		try {
			ParticipantDetail detail = req.getPayload();
			Participant existing = getParticipant(detail, true);
			if (existing == null) {
				return ResponseEvent.userError(ParticipantErrorCode.NOT_FOUND);
			}
			
			Participant participant = participantFactory.createParticipant(existing, detail);
			updateParticipant(existing, participant);			
			return ResponseEvent.response(ParticipantDetail.from(existing, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}	
		
	@Override
	@PlusTransactional
	public ResponseEvent<ParticipantDetail> delete(RequestEvent<Long> req) {
		try {
			Long participantId = req.getPayload();
			Participant participant = daoFactory.getParticipantDao().getById(participantId);
			if (participant == null) {
				return ResponseEvent.userError(ParticipantErrorCode.NOT_FOUND);
			}
			
			participant.delete();
			daoFactory.getParticipantDao().saveOrUpdate(participant);
			return ResponseEvent.response(ParticipantDetail.from(participant, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<MatchedParticipantsList>> getMatchingParticipants(RequestEvent<List<ParticipantDetail>> req) {
		try {
			List<MatchedParticipantsList> result = new ArrayList<>();

			for (ParticipantDetail inputCrit : req.getPayload()) {
				List<MatchedParticipant> matchedParticipants = lookupFactory.getLookupLogic().getMatchingParticipants(inputCrit);
				if (inputCrit.isReqRegInfo()) {
					addRegInfo(matchedParticipants);
				}

				result.add(MatchedParticipantsList.from(inputCrit, matchedParticipants));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	public Participant createParticipant(Participant participant) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ParticipantUtil.ensureUniqueUid(daoFactory, participant.getUid(), ose);
		ParticipantUtil.ensureUniquePmis(daoFactory, PmiDetail.from(participant.getPmis(), false), participant, ose);
		ParticipantUtil.ensureUniqueEmpi(daoFactory, participant.getEmpi(), ose);

		ose.checkAndThrow();

		participant.setEmpiIfEmpty();
		daoFactory.getParticipantDao().saveOrUpdate(participant, true);
		return participant;
	}

	public void updateParticipant(Participant existing, Participant newParticipant) {
		ParticipantUtil.ensureLockedFieldsAreUntouched(existing, newParticipant);
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		String existingUid = existing.getUid();
		String newUid = newParticipant.getUid();
		if (StringUtils.isNotBlank(newUid) && !newUid.equals(existingUid)) {
			ParticipantUtil.ensureUniqueUid(daoFactory, newUid, ose);
		}
		
		String existingEmpi = existing.getEmpi();
		String newEmpi = newParticipant.getEmpi();
		MpiGenerator generator = ParticipantUtil.getMpiGenerator();
		if (generator != null && !StringUtils.equals(newEmpi, existingEmpi)) {
			ose.addError(ParticipantErrorCode.MANUAL_MPI_NOT_ALLOWED);
		} else if (generator == null && StringUtils.isNotBlank(newEmpi) && !newEmpi.equals(existingEmpi)) {
			ParticipantUtil.ensureUniqueEmpi(daoFactory, newEmpi, ose);
		}


		List<PmiDetail> pmis = PmiDetail.from(newParticipant.getPmis(), false);
		ParticipantUtil.ensureUniquePmis(daoFactory, pmis, existing, ose);
		ose.checkAndThrow();
		
		existing.update(newParticipant);
		daoFactory.getParticipantDao().saveOrUpdate(existing);
	}

	@Override
	public ParticipantDetail saveOrUpdateParticipant(ParticipantDetail detail) {
		Participant existing = getParticipant(detail, false);

		if (existing == null) {
			Participant participant = participantFactory.createParticipant(detail);
			participant = createParticipant(participant);
			return ParticipantDetail.from(participant, false);
		} else {
			Participant participant = participantFactory.createParticipant(existing, detail);
			updateParticipant(existing, participant);
			return ParticipantDetail.from(existing, false);
		}
	}

	@Override
	public String getObjectName() {
		return Participant.getEntityName();
	}

	@Override
	public Map<String, Object> resolveUrl(String key, Object value) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public String getAuditTable() {
		return "CATISSUE_PARTICIPANT_AUD";
	}

	@Override
	public void ensureReadAllowed(Long objectId) {
		AccessCtrlMgr.getInstance().ensureReadParticipantRights(objectId);
	}

	private Participant getParticipant(ParticipantDetail detail, boolean forUpdate) {
		Participant result = null;
		if (detail.getId() != null) {
			result = daoFactory.getParticipantDao().getById(detail.getId());
		} else {
			boolean blankEmpi = StringUtils.isBlank(detail.getEmpi());
			if (!blankEmpi) {
				result = daoFactory.getParticipantDao().getByEmpi(detail.getEmpi());
			}
			
			if (blankEmpi || (result == null && !forUpdate)) {
				result = getByPmis(detail);
			}
		}
		
		return result;
	}
	
	private Participant getByPmis(ParticipantDetail detail) {
		Participant result = null;
		
		if (CollectionUtils.isEmpty(detail.getPmis())) {
			return result;
		}
		
		List<Participant> participants = daoFactory.getParticipantDao().getByPmis(detail.getPmis());
		if (participants.size() > 1) {
			throw OpenSpecimenException.userError(ParticipantErrorCode.DUP_MRN);
		} else if (participants.size() == 1) {
			result = participants.iterator().next();
		}
		
		return result;
	}

	//
	// TODO: We are assuming there won't be many matched participants;
	// If this is slow then we need to issue a single query to obtain
	// reg info of all matched participants at one go
	//
	private void addRegInfo(List<MatchedParticipant> matchedParticipants) {
		matchedParticipants.forEach(this::addRegInfo);
	}

	private void addRegInfo(MatchedParticipant matchedParticipant) {
		ParticipantDetail detail = matchedParticipant.getParticipant();
		if (detail.getId() == null) {
			return;
		}

		Participant participant = daoFactory.getParticipantDao().getById(detail.getId());
		detail.setRegisteredCps(ParticipantDetail.getCprSummaries(getCprs(participant)));
	}

	private List<CollectionProtocolRegistration> getCprs(Participant participant) {
		return AccessCtrlMgr.getInstance().getAccessibleCprs(participant.getCprs());
	}

	private List<ParticipantMedicalIdentifier> getNewlyAddedPmis(Participant existing, Participant newParticipant) {
		Map<Site, String> existingPmis = new HashMap<>();
		for (ParticipantMedicalIdentifier pmi : existing.getPmis()) {
			existingPmis.put(pmi.getSite(), pmi.getMedicalRecordNumber());
		}

		List<ParticipantMedicalIdentifier> newPmis = new ArrayList<>();
		for (ParticipantMedicalIdentifier newPmi : newParticipant.getPmis()) {
			String existingMrn = existingPmis.get(newPmi.getSite());
			if (!StringUtils.equals(existingMrn, newPmi.getMedicalRecordNumber())) {
				newPmis.add(newPmi);
			}
		}

		return newPmis;
	}
}
