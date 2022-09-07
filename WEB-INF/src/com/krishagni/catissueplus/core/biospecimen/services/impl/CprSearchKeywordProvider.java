package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostUpdateEvent;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.domain.ParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.common.domain.SearchEntityKeyword;
import com.krishagni.catissueplus.core.common.service.SearchEntityKeywordProvider;
import com.krishagni.catissueplus.core.common.service.impl.AbstractSearchEntityKeywordProvider;

public class CprSearchKeywordProvider extends AbstractSearchEntityKeywordProvider implements SearchEntityKeywordProvider {
	@Override
	public String getEntity() {
		return CollectionProtocolRegistration.class.getName();
	}

	@Override
	public List<SearchEntityKeyword> getKeywords(PostInsertEvent event) {
		CollectionProtocolRegistration cpr = (CollectionProtocolRegistration) event.getEntity();
		Participant participant = cpr.getParticipant();

		Set<Long> entityIds = getEntityIds(cpr);
		List<SearchEntityKeyword> keywords = createKeywords(entityIds, "ppid", null, cpr.getPpid(), 0);

		if (StringUtils.isNotBlank(participant.getFirstName())) {
			keywords.addAll(createKeywords(entityIds, "firstName", null, participant.getFirstName(), 0));
		}

		if (StringUtils.isNotBlank(participant.getLastName())) {
			keywords.addAll(createKeywords(entityIds, "lastName", null, participant.getLastName(), 0));
		}

		if (StringUtils.isNotBlank(participant.getUid())) {
			keywords.addAll(createKeywords(entityIds, "uid", null, participant.getUid(), 0));
		}

		if (StringUtils.isNotBlank(participant.getEmpi())) {
			keywords.addAll(createKeywords(entityIds, "empi", null, participant.getEmpi(), 0));
		}

		if (CollectionUtils.isNotEmpty(participant.getPmis())) {
			for (ParticipantMedicalIdentifier pmi : participant.getPmis()) {
				if (StringUtils.isNotBlank(pmi.getMedicalRecordNumber())) {
					keywords.addAll(createKeywords(entityIds, "medicalRecordNumber", null, pmi.getMedicalRecordNumber(), 0));
				}
			}
		}

		if (participant.getNewCprIds() == null) {
			participant.setNewCprIds(new HashSet<>());
		}

		participant.getNewCprIds().addAll(entityIds);
		return keywords;
	}

	@Override
	public List<SearchEntityKeyword> getKeywords(PostUpdateEvent event) {
		List<SearchEntityKeyword> keywords = super.getKeywords(event);

		CollectionProtocolRegistration cpr = (CollectionProtocolRegistration) event.getEntity();
		if (!cpr.isDeleted()) {
			Participant oldParticipant = (Participant) getObject("participant", event.getPersister(), event.getOldState());
			Participant newParticipant = (Participant) getObject("participant", event.getPersister(), event.getState());
			if (Objects.equals(oldParticipant, newParticipant)) {
				return keywords;
			}

			List<SearchEntityKeyword> toDelete = getParticipantKeywords(cpr, oldParticipant);
			toDelete.forEach(k -> k.setStatus(0));
			keywords.addAll(toDelete);
			oldParticipant.addKwAddedCprId(cpr.getId());

			List<SearchEntityKeyword> toAdd = getParticipantKeywords(cpr, newParticipant);
			keywords.addAll(toAdd);
			newParticipant.addKwAddedCprId(cpr.getId());
			return keywords;
		}

		Participant participant = cpr.getParticipant();
		if (participant == null || participant.isDeleted()) {
			return keywords;
		}

		keywords.addAll(getParticipantKeywords(cpr, participant));
		keywords.forEach(keyword -> keyword.setStatus(0));
		participant.addKwAddedCprId(cpr.getId());
		return keywords;
	}

	@Override
	public List<SearchEntityKeyword> getKeywords(PostDeleteEvent event) {
		return super.getKeywords(event);
	}

	@Override
	public Set<Long> getEntityIds(Object entity) {
		return Collections.singleton(((CollectionProtocolRegistration) entity).getId());
	}

	@Override
	public List<String> getKeywordProps() {
		return Collections.singletonList("ppid");
	}

	@Override
	public String getEntityName() {
		return CollectionProtocolRegistration.getEntityName();
	}

	@Override
	public boolean isEntityDeleted(Object entity) {
		return ((CollectionProtocolRegistration) entity).isDeleted();
	}

	private List<SearchEntityKeyword> getParticipantKeywords(CollectionProtocolRegistration cpr, Participant participant) {
		List<SearchEntityKeyword> keywords = new ArrayList<>();

		Set<Long> entityIds = getEntityIds(cpr);
		keywords.addAll(createKeywords(entityIds, "firstName", participant.getFirstName(), participant.getFirstName(), 1));
		keywords.addAll(createKeywords(entityIds, "lastName",  participant.getLastName(),  participant.getLastName(),  1));
		keywords.addAll(createKeywords(entityIds, "uid",       participant.getUid(),       participant.getUid(),       1));
		keywords.addAll(createKeywords(entityIds, "empi",      participant.getEmpi(),      participant.getEmpi(),      1));

		if (CollectionUtils.isNotEmpty(participant.getPmis())) {
			for (ParticipantMedicalIdentifier pmi : participant.getPmis()) {
				if (StringUtils.isNotBlank(pmi.getMedicalRecordNumber())) {
					keywords.addAll(createKeywords(entityIds, "medicalRecordNumber", pmi.getMedicalRecordNumber(), pmi.getMedicalRecordNumber(), 1));
				}
			}
		}

		return keywords;
	}
}
