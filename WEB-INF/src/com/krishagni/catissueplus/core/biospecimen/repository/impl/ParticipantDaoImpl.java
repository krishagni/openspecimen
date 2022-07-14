
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.SimpleExpression;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.ParticipantDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ParticipantDaoImpl extends AbstractDao<Participant> implements ParticipantDao {
	
	@Override
	@SuppressWarnings("unchecked")
	public Participant getByUid(String uid) {		
		List<Participant> participants = getCurrentSession().getNamedQuery(GET_BY_UID)
			.setParameter("uid", uid.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Participant getByEmpi(String empi) {
		List<Participant> participants = getCurrentSession().getNamedQuery(GET_BY_EMPI)
			.setParameter("empi", empi.toLowerCase())
			.list();
		return participants == null || participants.isEmpty() ? null : participants.iterator().next();
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<Participant> getByLastNameAndBirthDate(String lname, Date dob) {
		ZonedDateTime zdt = ZonedDateTime.ofInstant(new Date(dob.getTime()).toInstant(), ZoneId.systemDefault());
		Date dobStart     = Date.from(zdt.with(LocalTime.MIN).toInstant());
		Date dobEnd       = Date.from(zdt.with(LocalTime.MAX).toInstant());

		return getCurrentSession().getNamedQuery(GET_BY_LNAME_AND_DOB)
			.setString("lname", lname.toLowerCase())
			.setTimestamp("dobStart", dobStart)
			.setTimestamp("dobEnd", dobEnd)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")	
	public List<Participant> getByPmis(List<PmiDetail> pmis) {
		DetachedCriteria subQuery = getByPmisQuery(pmis);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return getCurrentSession().createCriteria(Participant.class, "p")
			.add(Subqueries.propertyIn("p.id", subQuery))
			.list();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getParticipantIdsByPmis(List<PmiDetail> pmis) {
		DetachedCriteria subQuery = getByPmisQuery(pmis);
		if (subQuery == null) {
			return Collections.emptyList();
		}

		return subQuery.getExecutableCriteria(getCurrentSession()).list();
	}
	
	@Override
	public boolean isUidUnique(String uid) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PARTICIPANT_ID_BY_UID);
		query.setString("uid", uid.toLowerCase());
		return query.list().isEmpty();
	}

	@Override
	public boolean isPmiUnique(String siteName, String mrn) {
		Query query = sessionFactory.getCurrentSession().getNamedQuery(GET_PMI_ID_BY_SITE_MRN);
		query.setString("siteName", siteName.toLowerCase());
		query.setString("mrn", mrn.toLowerCase());
		return query.list().isEmpty();
	}
	
	@Override
	public Class<Participant> getType() {
		return Participant.class;
	}
	
	private DetachedCriteria getByPmisQuery(List<PmiDetail> pmis) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Participant.class, "p")
			.setProjection(Projections.distinct(Projections.property("p.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession())
			.createAlias("p.pmis", "pmi")
			.createAlias("pmi.site", "site");

		Disjunction junction = Restrictions.disjunction();
		boolean added = false;
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			SimpleExpression eqMrn  = Restrictions.eq("pmi.medicalRecordNumber", pmi.getMrn());
			SimpleExpression eqSite = Restrictions.eq("site.name", pmi.getSiteName());
			if (!isMySQL()) {
				eqMrn  = eqMrn.ignoreCase();
				eqSite = eqSite.ignoreCase();
			}

			junction.add(Restrictions.and(eqMrn, eqSite));
			added = true;
		}

		if (added) {
			query.add(junction);
			return detachedCriteria;
		} else {
			return null;
		}
	}

	private static final String FQN = Participant.class.getName();

	private static final String GET_PARTICIPANT_ID_BY_UID = FQN + ".getParticipantIdByUid";

	private static final String GET_PMI_ID_BY_SITE_MRN = FQN + ".getPmiIdBySiteMrn";
	
	private static final String GET_BY_UID = FQN + ".getByUid";
	
	private static final String GET_BY_EMPI = FQN + ".getByEmpi";

	private static final String GET_BY_LNAME_AND_DOB = FQN + ".getByLnameAndDob";
}
