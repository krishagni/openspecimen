
package com.krishagni.catissueplus.core.biospecimen.matching;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.ParticipantDao;
import com.krishagni.catissueplus.core.common.PlusTransactional;

public class LocalDbParticipantLookupImpl implements ParticipantLookupLogic {

	private DaoFactory daoFactory;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail participant) {
		Map<Participant, List<String>> matchedParticipants = new HashMap<>();
		
		ParticipantDao dao = daoFactory.getParticipantDao();
		if (StringUtils.isNotBlank(participant.getEmpi())) {
			Participant matched = dao.getByEmpi(participant.getEmpi());			
			if (matched != null) {
				addParticipant(matchedParticipants, matched, "empi");
			}
		}
		
		if (StringUtils.isNotBlank(participant.getUid())) {
			Participant matched = dao.getByUid(participant.getUid());
			if (matched != null) {
				addParticipant(matchedParticipants, matched, "uid");
			}
		}
		
		if (CollectionUtils.isNotEmpty(participant.getPmis())) {
			List<Participant> matched = dao.getByPmis(participant.getPmis());
			if (CollectionUtils.isNotEmpty(matched)) {
				addParticipant(matchedParticipants, matched, "pmi");
			}
		}
		
		if (StringUtils.isNotBlank(participant.getLastName()) && participant.getBirthDate() != null) {
			List<Participant> matched = dao.getByLastNameAndBirthDate(participant.getLastName(), participant.getBirthDate());
			if (CollectionUtils.isNotEmpty(matched)) {
				addParticipant(matchedParticipants, matched, "lnameAndDob");
			}
		}
		
		return MatchedParticipant.from(matchedParticipants);
	}
	
	private void addParticipant(Map<Participant, List<String>> matchedParticipants, List<Participant> participants, String matchedAttr) {
		participants.forEach(p -> addParticipant(matchedParticipants, p, matchedAttr));
	}
	
	private void addParticipant(Map<Participant, List<String>> matchedParticipants, Participant participant, String matchedAttr) {
		List<String> matchedAttrs = matchedParticipants.computeIfAbsent(participant, (k) -> new ArrayList<>());
		matchedAttrs.add(matchedAttr);
	}
}
