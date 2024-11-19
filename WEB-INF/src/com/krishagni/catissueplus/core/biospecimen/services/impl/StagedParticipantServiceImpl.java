package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;


import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipantSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ParticipantErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.ParticipantService;
import com.krishagni.catissueplus.core.biospecimen.services.StagedParticipantService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.PvAttributes;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class StagedParticipantServiceImpl implements StagedParticipantService {
	private static final LogUtil logger = LogUtil.getLogger(StagedParticipantServiceImpl.class);

	private DaoFactory daoFactory;

	private ParticipantService participantSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setparticipantSvc(ParticipantService participantSvc) {
		this.participantSvc = participantSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<StagedParticipantDetail> saveOrUpdateParticipant(RequestEvent<StagedParticipantDetail> req) {
		try {
			StagedParticipantDetail input = req.getPayload();
			updateParticipantIfExists(input);

			StagedParticipant newEmpiParticipant = null;
			if (StringUtils.isNotBlank(input.getNewEmpi())) {
				newEmpiParticipant = getMatchingParticipant(input.getNewEmpi());
			}

			StagedParticipant existing = null;
			if (StringUtils.isNotBlank(input.getEmpi())) {
				StagedParticipant oldEmpiParticipant = getMatchingParticipant(input);
				if (oldEmpiParticipant != null) {
					if (newEmpiParticipant != null) {
						clearCollection(oldEmpiParticipant.getPmiList());
						clearCollection(oldEmpiParticipant.getVisits());
						clearCollection(oldEmpiParticipant.getRaces());
						clearCollection(oldEmpiParticipant.getEthnicities());
						clearCollection(oldEmpiParticipant.getConsents());
						daoFactory.getStagedParticipantDao().delete(oldEmpiParticipant);
						existing = newEmpiParticipant;
					} else {
						existing = oldEmpiParticipant;
					}
				} else {
					if (newEmpiParticipant != null) {
						existing = newEmpiParticipant;
					}
				}
			}

			StagedParticipant savedParticipant = saveOrUpdateParticipant(existing, input);
			savedParticipant.setConsents(input.getConsents());

			EventPublisher.getInstance().publish(new StagedParticipantSavedEvent(savedParticipant));
			return ResponseEvent.response(StagedParticipantDetail.from(savedParticipant));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void updateParticipantIfExists(StagedParticipantDetail input) {
		Participant existing = daoFactory.getParticipantDao().getByEmpi(input.getEmpi());
		if (existing == null) {
			if (CollectionUtils.isNotEmpty(input.getPmis())) {
				List<Participant> matches = daoFactory.getParticipantDao().getByPmis(input.getPmis());
				if (matches.size() == 1) {
					existing = matches.iterator().next();
				} else if (matches.size() > 1) {
					logger.error("Multiple matches for " + PmiDetail.toString(input.getPmis()));
					throw OpenSpecimenException.userError(ParticipantErrorCode.MRN_DIFF, PmiDetail.toString(input.getPmis()));
				}
			}

			if (existing == null) {
				return;
			}
		}

		ParticipantDetail detail = ParticipantDetail.from(existing, false);
		if (input.isAttrModified("newEmpi")) {
			detail.setEmpi(input.getNewEmpi());
		}

		if (input.isAttrModified("uid")) {
			detail.setUid(input.getUid());
		}

		if (input.isAttrModified("ethnicities")) {
			detail.setEthnicities(input.getEthnicities());
		}

		if (input.isAttrModified("gender")) {
			detail.setGender(input.getGender());
		}

		if (input.isAttrModified("lastName")) {
			detail.setLastName(input.getLastName());
		}

		if (input.isAttrModified("firstName")) {
			detail.setFirstName(input.getFirstName());
		}

		if (input.isAttrModified("middleName")) {
			detail.setMiddleName(input.getMiddleName());
		}

		if (input.isAttrModified("birthDate")) {
			detail.setBirthDate(input.getBirthDate());
		}

		if (input.isAttrModified("deathDate")) {
			detail.setDeathDate(input.getDeathDate());
		}

		if (input.isAttrModified("vitalStatus")) {
			detail.setVitalStatus(input.getVitalStatus());
		}

		if (input.isAttrModified("races")) {
			detail.setRaces(input.getRaces());
		}

		if (input.isAttrModified("pmis")) {
			detail.setPmis(input.getPmis());
		}

		if (input.isAttrModified("source")) {
			detail.setSource(input.getSource());
		}

		ResponseEvent<ParticipantDetail> resp = participantSvc.patchParticipant(new RequestEvent<>(detail));
		if (resp.isSuccessful()) {
			logger.info("Matching participant (eMPI: '" + detail.getEmpi() + "') found and updated!");
		} else {
			logger.info("Matching participant update failed: " + resp.getError().getMessage());
		}
	}

	private StagedParticipant getMatchingParticipant(StagedParticipantDetail detail) {
		return getMatchingParticipant(detail.getEmpi());
	}

	private StagedParticipant getMatchingParticipant(String empi) {
		if (StringUtils.isBlank(empi)) {
			return null;
		}

		return daoFactory.getStagedParticipantDao().getByEmpi(empi);
	}

	private StagedParticipant saveOrUpdateParticipant(StagedParticipant existing, StagedParticipantDetail input) {
		StagedParticipant participant = createParticipant(existing, input);
		if (existing != null) {
			existing.update(participant);
			participant = existing;
		}

		daoFactory.getStagedParticipantDao().saveOrUpdate(participant);
		return participant;
	}

	private StagedParticipant createParticipant(StagedParticipant existing, StagedParticipantDetail detail) {
		StagedParticipant participant = new StagedParticipant();
		if (existing != null) {
			BeanUtils.copyProperties(existing, participant);
		}

		setParticipantAtrrs(detail, participant);
		return participant;
	}

	private void setParticipantAtrrs(StagedParticipantDetail detail, StagedParticipant participant) {
		participant.setFirstName(detail.getFirstName());
		participant.setMiddleName(detail.getMiddleName());
		participant.setLastName(detail.getLastName());
		participant.setEmailAddress(detail.getEmailAddress());
		participant.setPhoneNumber(Utility.normalizePhoneNumber(detail.getPhoneNumber()));
		participant.setBirthDate(detail.getBirthDate());
		participant.setDeathDate(detail.getDeathDate());
		participant.setGender(getPv(PvAttributes.GENDER, detail.getGender(), ParticipantErrorCode.INVALID_GENDER));
		participant.setVitalStatus(getPv(PvAttributes.VITAL_STATUS, detail.getVitalStatus(), ParticipantErrorCode.INVALID_VITAL_STATUS));
		participant.setUpdatedTime(Calendar.getInstance().getTime());

		if (StringUtils.isNotBlank(detail.getSource())) {
			participant.setSource(detail.getSource());
		}

		if (StringUtils.isNotBlank(detail.getNewEmpi())) {
			participant.setEmpi(detail.getNewEmpi());
		} else {
			participant.setEmpi(detail.getEmpi());
		}

		participant.setPmiList(getPmis(participant, detail.getPmis()));

		Set<String> races = detail.getRaces();
		if (CollectionUtils.isNotEmpty(races)) {
			participant.setRaces(getPvs(PvAttributes.RACE, races, ParticipantErrorCode.INVALID_RACE));
		}

		Set<String> ethnicities = detail.getEthnicities();
		if (CollectionUtils.isNotEmpty(ethnicities)) {
			participant.setEthnicities(getPvs(PvAttributes.ETHNICITY, ethnicities, ParticipantErrorCode.INVALID_ETHNICITY));
		}
	}

	private Set<StagedParticipantMedicalIdentifier> getPmis(StagedParticipant participant, List<PmiDetail> pmis) {
		if (CollectionUtils.isEmpty(pmis)) {
			return Collections.emptySet();
		}

		return pmis.stream().map(
			id -> {
				StagedParticipantMedicalIdentifier pmi = new StagedParticipantMedicalIdentifier();
				pmi.setSite(id.getSiteName());
				pmi.setMedicalRecordNumber(id.getMrn());
				pmi.setParticipant(participant);
				return pmi;
			}
		).collect(Collectors.toSet());
	}

	private PermissibleValue getPv(String attr, String value, ErrorCode invErrorCode) {
		if (StringUtils.isBlank(value)) {
			return null;
		}

		PermissibleValue pv = daoFactory.getPermissibleValueDao().getPv(attr, value);
		if (pv == null) {
			throw OpenSpecimenException.userError(invErrorCode);
		}

		return pv;
	}

	private Set<PermissibleValue> getPvs(String attr, Collection<String> values, ErrorCode invErrorCode) {
		if (CollectionUtils.isEmpty(values)) {
			return new HashSet<>();
		}

		List<PermissibleValue> pvs = daoFactory.getPermissibleValueDao().getPvs(attr, values);
		if (pvs.size() != values.size()) {
			throw OpenSpecimenException.userError(invErrorCode);
		}

		return new HashSet<>(pvs);
	}

	private <T> void clearCollection(Collection<T> coll) {
		if (coll != null) {
			coll.clear();
		}
	}
}
