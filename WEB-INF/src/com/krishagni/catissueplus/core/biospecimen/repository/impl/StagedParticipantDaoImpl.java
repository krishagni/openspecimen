package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;

public class StagedParticipantDaoImpl extends AbstractDao<StagedParticipant> implements StagedParticipantDao {

	@Override
	public Class<StagedParticipant> getType() {
		return StagedParticipant.class;
	}

	@Override
	public List<StagedParticipant> getByPmis(List<PmiDetail> pmis) {
		List<Long> participantIds = getMatchingParticipantIds(pmis);
		if (participantIds == null || participantIds.isEmpty()) {
			return Collections.emptyList();
		}

		Criteria<StagedParticipant> query = createCriteria(StagedParticipant.class, "sp");
		return query.add(query.in("sp.id", participantIds)).list();
	}
	
	@Override
	public StagedParticipant getByEmpi(String empi) {
		Criteria<StagedParticipant> query = createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(query.eq("sp.empi", empi));
		} else {
			query.add(query.eq(query.lower("sp.empi"), empi.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public StagedParticipant getByUid(String uid) {
		Criteria<StagedParticipant> query = createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(query.eq("sp.uid", uid));
		} else {
			query.add(query.eq(query.lower("sp.uid"), uid.toLowerCase()));
		}

		return query.uniqueResult();
	}

	@Override
	public List<StagedParticipant> getByMrn(String mrn) {
		Criteria<StagedParticipant> query = createCriteria(StagedParticipant.class, "sp")
			.join("sp.pmiList", "pmi");
		if (isMySQL()) {
			query.add(query.eq("pmi.medicalRecordNumber", mrn));
		} else {
			query.add(query.eq(query.lower("pmi.medicalRecordNumber"), mrn.toLowerCase()));
		}

		return query.list();
	}

	@Override
	public int deleteOldParticipants(int olderThanDays) {
		Date olderThanDt = Date.from(Instant.now().minus(Duration.ofDays(olderThanDays)));
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_PMIS, olderThanDt);
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_RACES, olderThanDt);
		deleteOldParticipantRecs(DEL_OLD_PARTICIPANT_ETHNICITIES, olderThanDt);
		return deleteOldParticipantRecs(DEL_OLD_PARTICIPANTS, olderThanDt);
	}

	private List<Long> getMatchingParticipantIds(List<PmiDetail> pmis) {
		Criteria<Long> query = createCriteria(StagedParticipantMedicalIdentifier.class, Long.class, "pmi")
			.join("pmi.participant", "participant")
			.select("participant.id");

		boolean added = false;
		Disjunction disjunctions = query.disjunction();
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			added = true;
			if (isMySQL()) {
				disjunctions.add(
					query.and(
						query.eq("pmi.medicalRecordNumber", pmi.getMrn()),
						query.eq("pmi.site", pmi.getSiteName())
					)
				);
			} else {
				disjunctions.add(
					query.and(
						query.eq(query.lower("pmi.medicalRecordNumber"), pmi.getMrn().toLowerCase()),
						query.eq(query.lower("pmi.site"), pmi.getSiteName().toLowerCase())
					)
				);
			}
		}

		if (!added) {
			return Collections.emptyList();
		}

		query.add(disjunctions);

		return query.list();
	}

	private int deleteOldParticipantRecs(String query, Date olderThanDt) {
		return createNamedQuery(query)
			.setParameter("olderThanDt", olderThanDt)
			.executeUpdate();
	}

	private static final String FQN = StagedParticipant.class.getName();

	private static final String DEL_OLD_PARTICIPANTS = FQN + ".deleteOldParticipants";

	private static final String DEL_OLD_PARTICIPANT_PMIS = FQN + ".deleteOldParticipantPmis";

	private static final String DEL_OLD_PARTICIPANT_RACES = FQN + ".deleteOldParticipantRaces";

	private static final String DEL_OLD_PARTICIPANT_ETHNICITIES = FQN + ".deleteOldParticipantEthnicities";
}
