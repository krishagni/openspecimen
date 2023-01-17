package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;

import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipant;
import com.krishagni.catissueplus.core.biospecimen.domain.StagedParticipantMedicalIdentifier;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.StagedParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class StagedParticipantDaoImpl extends AbstractDao<StagedParticipant> implements StagedParticipantDao {

	@Override
	public Class<StagedParticipant> getType() {
		return StagedParticipant.class;
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<StagedParticipant> getByPmis(List<PmiDetail> pmis) {
	    List<Long> participantIds = getMatchingParticipantIds(pmis);
		if (participantIds == null || participantIds.isEmpty()) {
			return Collections.emptyList();
		}

		return getCurrentSession().createCriteria(StagedParticipant.class, "sp")
			.add(Restrictions.in("sp.id", participantIds))
			.list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public StagedParticipant getByEmpi(String empi) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(Restrictions.eq("sp.empi", empi));
		} else {
			query.add(Restrictions.eq("sp.empi", empi).ignoreCase());
		}

		return (StagedParticipant) query.uniqueResult();
	}

	@Override
	public StagedParticipant getByUid(String uid) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp");
		if (isMySQL()) {
			query.add(Restrictions.eq("sp.uid", uid));
		} else {
			query.add(Restrictions.eq("sp.uid", uid).ignoreCase());
		}

		return (StagedParticipant) query.uniqueResult();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<StagedParticipant> getByMrn(String mrn) {
		Criteria query = getCurrentSession().createCriteria(StagedParticipant.class, "sp")
			.createAlias("sp.pmiList", "pmi");
		if (isMySQL()) {
			query.add(Restrictions.eq("pmi.medicalRecordNumber", mrn));
		} else {
			query.add(Restrictions.eq("pmi.medicalRecordNumber", mrn).ignoreCase());
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
		boolean added = false;
		Disjunction disjunctions = Restrictions.disjunction();
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			added = true;
			if (isMySQL()) {
				disjunctions.add(
					Restrictions.and(
						Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn()),
						Restrictions.eq("pmi.site", pmi.getSiteName())
					)
				);
			} else {
				disjunctions.add(
					Restrictions.and(
						Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn()).ignoreCase(),
						Restrictions.eq("pmi.site", pmi.getSiteName()).ignoreCase()
					)
				);
			}
		}

		if (!added) {
			return Collections.emptyList();
		}

		Criteria query = getCurrentSession().createCriteria(StagedParticipantMedicalIdentifier.class, "pmi")
			.createAlias("pmi.participant", "participant")
			.add(disjunctions)
			.setProjection(Projections.property("participant.id"));

		List<Object> result = query.list();
		return result.stream().map(id -> ((Number) id).longValue()).collect(Collectors.toList());
	}

	private int deleteOldParticipantRecs(String query, Date olderThanDt) {
		return getCurrentSession().getNamedQuery(query).setTimestamp("olderThanDt", olderThanDt).executeUpdate();
	}

	private static final String FQN = StagedParticipant.class.getName();

	private static final String DEL_OLD_PARTICIPANTS = FQN + ".deleteOldParticipants";

	private static final String DEL_OLD_PARTICIPANT_PMIS = FQN + ".deleteOldParticipantPmis";

	private static final String DEL_OLD_PARTICIPANT_RACES = FQN + ".deleteOldParticipantRaces";

	private static final String DEL_OLD_PARTICIPANT_ETHNICITIES = FQN + ".deleteOldParticipantEthnicities";
}
