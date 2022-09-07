package com.krishagni.catissueplus.core.biospecimen.matching;

import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.biospecimen.events.MatchedParticipant;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantDetail;

public class DefaultParticipantLookupFlow implements ParticipantLookupLogic {
	private ParticipantLookupLogic localDbLookup;

	private ParticipantLookupLogic externalDbLookup;

	public void setLocalDbLookup(ParticipantLookupLogic localDbLookup) {
		this.localDbLookup = localDbLookup;
	}

	public void setExternalDbLookup(ParticipantLookupLogic externalDbLookup) {
		this.externalDbLookup = externalDbLookup;
	}

	@Override
	public List<MatchedParticipant> getMatchingParticipants(ParticipantDetail criteria) {
		List<MatchedParticipant> result = localDbLookup.getMatchingParticipants(criteria);
		if (CollectionUtils.isEmpty(result) && externalDbLookup != null) {
			result = externalDbLookup.getMatchingParticipants(criteria);
		}

		return result;
	}
}
