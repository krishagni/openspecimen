
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.ParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class ParticipantDaoImpl extends AbstractDao<Participant> implements ParticipantDao {
	
	@Override
	public Participant getByUid(String uid) {		
		List<Participant> participants = createNamedQuery(GET_BY_UID, Participant.class)
			.setParameter("uid", uid.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}
	
	@Override
	public Participant getByEmpi(String empi) {
		List<Participant> participants = createNamedQuery(GET_BY_EMPI, Participant.class)
			.setParameter("empi", empi.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}

	@Override
	public List<Participant> getByLastNameAndBirthDate(String lname, Date dob) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(new Date(dob.getTime()).toInstant(), ZoneId.systemDefault());
		Date dobStart     = Date.from(zdt.with(LocalTime.MIN).toInstant());
		Date dobEnd       = Date.from(zdt.with(LocalTime.MAX).toInstant());

		return createNamedQuery(GET_BY_LNAME_AND_DOB, Participant.class)
			.setParameter("lname", lname.toLowerCase())
			.setParameter("dobStart", dobStart)
			.setParameter("dobEnd", dobEnd)
			.list();
	}

	@Override
	public List<Participant> getByPmis(List<PmiDetail> pmis) {
		Criteria<Participant> query = createCriteria(Participant.class, "p");
		SubQuery<Long> subQuery = getByPmisQuery(pmis, query);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return query.add(query.in("p.id", subQuery)).list();
	}
	
	@Override
	public List<Long> getParticipantIdsByPmis(List<PmiDetail> pmis) {
		Criteria<Long> query = createCriteria(Participant.class, Long.class, "p");
		SubQuery<Long> subQuery = getByPmisQuery(pmis, query);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return query.select("p.id").add(query.in("p.id", subQuery)).list();
	}
	
	@Override
	public boolean isUidUnique(String uid) {
		 return createNamedQuery(GET_PARTICIPANT_ID_BY_UID, Long.class)
			 .setParameter("uid", uid.toLowerCase())
			 .list()
			 .isEmpty();
	}

	@Override
	public boolean isPmiUnique(String siteName, String mrn) {
		return createNamedQuery(GET_PMI_ID_BY_SITE_MRN, Long.class)
			.setParameter("siteName", siteName.toLowerCase())
			.setParameter("mrn", mrn.toLowerCase())
			.list()
			.isEmpty();
	}
	
	@Override
	public Class<Participant> getType() {
		return Participant.class;
	}
	
	private SubQuery<Long> getByPmisQuery(List<PmiDetail> pmis, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Participant.class, "p")
			.distinct().select("p.id")
			.join("p.pmis", "pmi")
			.join("pmi.site", "site");

		Disjunction junction = query.disjunction();
		boolean added = false;
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			if (isMySQL()) {
				junction.add(query.and(
					query.eq("pmi.medicalRecordNumber", pmi.getMrn()),
					query.eq("site.name", pmi.getSiteName())
				));
			} else {
				junction.add(query.and(
					query.eq(query.lower("pmi.medicalRecordNumber"), pmi.getMrn().toLowerCase()),
					query.eq(query.lower("site.name"), pmi.getSiteName().toLowerCase())
				));
			}

			added = true;
		}

		return added ? query.add(junction) : null;
	}

	private static final String FQN = Participant.class.getName();

	private static final String GET_PARTICIPANT_ID_BY_UID = FQN + ".getParticipantIdByUid";

	private static final String GET_PMI_ID_BY_SITE_MRN = FQN + ".getPmiIdBySiteMrn";
	
	private static final String GET_BY_UID = FQN + ".getByUid";
	
	private static final String GET_BY_EMPI = FQN + ".getByEmpi";

	private static final String GET_BY_LNAME_AND_DOB = FQN + ".getByLnameAndDob";
}
