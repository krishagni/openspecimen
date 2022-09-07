package com.krishagni.catissueplus.core.biospecimen.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.events.StagedParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;

public class StagedParticipantsDbLookup implements ParticipantLookupLogic {
	private ParticipantLookupLogic osDbLookup;

	private DaoFactory daoFactory;

	public void setOsDbLookup(LocalDbParticipantLookupImpl osDbLookup) {
		this.osDbLookup = osDbLookup;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail detail) {
		Map<StagedParticipant, List<String>> stagedParticipants = getMatchingStagedParticipants(detail);
		if (stagedParticipants.isEmpty()) {
			//
			// If no matching found in the staging DB then get matching from the OS DB and return
			//
			return osDbLookup.getMatchingParticipants(detail);
		}

		//
		// Matching found in the staging DB, now check if patient with similar identifiers exist in the OS database
		//
		Map<Participant, List<String>> osParticipants = getMatchingOsParticipants(detail);
		if (osParticipants.isEmpty()) {
			return toMatchedParticipants(stagedParticipants, this::createStagedParticipantDetail);
		} else {
			return toMatchedParticipants(osParticipants, (p) -> ParticipantDetail.from(p, false));
		}
	}

	private Map<StagedParticipant, List<String>> getMatchingStagedParticipants(ParticipantDetail criteria) {
		Map<StagedParticipant, List<String>> stagedParticipants = new HashMap<>();

		if (StringUtils.isNotBlank(criteria.getEmpi())) {
			//
			// match by eMPI
			//
			StagedParticipant empiMatch = daoFactory.getStagedParticipantDao().getByEmpi(criteria.getEmpi());
			if (empiMatch != null) {
				addParticipant(stagedParticipants, empiMatch, "empi");
			}
		}

		if (StringUtils.isNotBlank(criteria.getUid())) {
			//
			// match by UID
			//
			StagedParticipant uidMatch = daoFactory.getStagedParticipantDao().getByUid(criteria.getUid());
			if (uidMatch != null) {
				addParticipant(stagedParticipants, uidMatch, "uid");
			}
		}

		if (CollectionUtils.isNotEmpty(criteria.getPmis())) {
			//
			// match by MRN/site
			//
			List<StagedParticipant> pmiMatches = daoFactory.getStagedParticipantDao().getByPmis(criteria.getPmis());
			addParticipant(stagedParticipants, pmiMatches, "pmi");
		}

		return stagedParticipants;
	}

	private Map<Participant, List<String>> getMatchingOsParticipants(ParticipantDetail criteria) {
		Map<Participant, List<String>> osParticipants = new HashMap<>();

		if (StringUtils.isNotBlank(criteria.getEmpi())) {
			//
			// match by eMPI
			//
			Participant empiMatch = daoFactory.getParticipantDao().getByEmpi(criteria.getEmpi());
			if (empiMatch != null) {
				addParticipant(osParticipants, empiMatch, "empi");
			}
		}

		if (StringUtils.isNotBlank(criteria.getUid())) {
			//
			// match by UID
			//
			Participant uidMatch = daoFactory.getParticipantDao().getByUid(criteria.getUid());
			if (uidMatch != null) {
				addParticipant(osParticipants, uidMatch, "uid");
			}
		}

		if (CollectionUtils.isNotEmpty(criteria.getPmis())) {
			//
			// match by MRN/site
			//
			List<Participant> pmiMatches = daoFactory.getParticipantDao().getByPmis(criteria.getPmis());
			addParticipant(osParticipants, pmiMatches, "pmi");
		}

		return osParticipants;
	}

	private <T extends Participant> List<MatchedParticipant> toMatchedParticipants(Map<T, List<String>> matches, Function<T, ParticipantDetail> transformer) {
		return matches.entrySet().stream()
			.map(entry -> new MatchedParticipant(transformer.apply(entry.getKey()), entry.getValue()))
			.collect(Collectors.toList());
	}

	//Populating the participant details from staging participant
	private ParticipantDetail createStagedParticipantDetail(StagedParticipant participant) {
		StagedParticipantDetail detail = StagedParticipantDetail.from(participant);
		detail.setUpdatedTime(null);
		return detail;
	}

	private <T extends Participant> void addParticipant(Map<T, List<String>> matchedParticipants, List<T> participants, String matchedAttr) {
		participants.forEach(p -> addParticipant(matchedParticipants, p, matchedAttr));
	}

	private <T extends Participant> void addParticipant(Map<T, List<String>> matchedParticipants, T participant, String matchedAttr) {
		List<String> matchedAttrs = matchedParticipants.computeIfAbsent(participant, (k) -> new ArrayList<>());
		matchedAttrs.add(matchedAttr);
	}
}
