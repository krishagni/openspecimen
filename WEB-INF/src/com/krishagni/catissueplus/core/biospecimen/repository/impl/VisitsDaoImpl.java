
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsDao;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class VisitsDaoImpl extends AbstractDao<Visit> implements VisitsDao {
	
	@Override
	public Class<Visit> getType() {
		return Visit.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadCreatedVisitStats(Map<Long, ? extends VisitSummary> visitsMap) {
		if (visitsMap == null || visitsMap.isEmpty()) {
			return;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_VISIT_STATS)
			.setParameterList("visitIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = 0;
			Long visitId = (Long) row[idx++];
			VisitSummary visit = visitsMap.get(visitId);
			visit.setTotalPendingSpmns((Integer) row[idx++]);
			visit.setPendingPrimarySpmns((Integer) row[idx++]);
			visit.setPlannedPrimarySpmnsColl((Integer) row[idx++]);
			visit.setUnplannedPrimarySpmnsColl((Integer) row[idx++]);
			visit.setUncollectedPrimarySpmns((Integer) row[idx++]);
			visit.setStoredSpecimens((Integer) row[idx++]);
			visit.setNotStoredSpecimens((Integer) row[idx++]);
			visit.setDistributedSpecimens((Integer) row[idx++]);
			visit.setClosedSpecimens((Integer) row[idx++]);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public void loadAnticipatedVisitStats(Map<Long, ? extends VisitSummary> visitsMap) {
		if (visitsMap == null || visitsMap.isEmpty()) {
			return;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ANTICIPATED_VISIT_STATS)
			.setParameterList("eventIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = 0;
			Long eventId = (Long) row[idx++];
			VisitSummary visit = visitsMap.get(eventId);
			visit.setTotalPendingSpmns((Integer) row[idx++]);
			visit.setPendingPrimarySpmns((Integer) row[idx++]);
		}
	}

	@Override
	public Long getVisitsCount(VisitsListCriteria crit) {
		Number count = (Number) getVisitIdsListQuery(crit).getExecutableCriteria(getCurrentSession())
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getVisitsList(VisitsListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(Visit.class, "visit")
			.add(Subqueries.propertyIn("visit.id", getVisitIdsListQuery(crit)));

		if (CollectionUtils.isEmpty(crit.names())) {
			query.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults());
		}

		return query.addOrder(Order.asc("id")).list();
	}

	@Override
	public Visit getByName(String name) {
		List<Visit> visits = getByName(Collections.singleton(name));
		return !visits.isEmpty() ? visits.iterator().next() : null;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByName(Collection<String> names) {
		return getCurrentSession().getNamedQuery(GET_VISIT_BY_NAME)
			.setParameterList("names", names)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByIds(Collection<Long> ids) {
		return getCurrentSession().getNamedQuery(GET_VISITS_BY_IDS)
			.setParameterList("ids", ids)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getBySpr(String sprNumber) {
		return getCurrentSession().getNamedQuery(GET_VISIT_BY_SPR)
			.setParameter("sprNumber", sprNumber)
			.list();
	}

	@Override
	public List<Visit> getByEvent(Long cprId, String eventLabel) {
		return getCurrentSession().createCriteria(Visit.class, "v")
			.createAlias("v.cpEvent", "event")
			.createAlias("v.registration", "reg")
			.add(Restrictions.eq("reg.id", cprId))
			.add(Restrictions.eq("event.eventLabel", eventLabel))
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<String, Object> getCprVisitIds(String key, Object value) {
		List<Object[]> rows = getCurrentSession().createCriteria(Visit.class)
			.createAlias("registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.setProjection(
				Projections.projectionList()
					.add(Projections.property("id"))
					.add(Projections.property("cpr.id"))
					.add(Projections.property("cp.id")))
			.add(Restrictions.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("visitId", row[0]);
		ids.put("cprId", row[1]);
		ids.put("cpId", row[2]);
		return ids;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Visit getLatestVisit(Long cprId) {
		List<Visit> visits = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_LATEST_VISIT_BY_CPR_ID)
			.setLong("cprId", cprId)
			.setMaxResults(1)
			.list();

		return visits.isEmpty() ? null :  visits.get(0);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getByEmpiOrMrn(Long cpId, String empiOrMrn) {
		return getCurrentSession().getNamedQuery(GET_BY_EMPI_OR_MRN)
			.setParameter("cpId", cpId)
			.setParameter("empiOrMrn", empiOrMrn.toLowerCase())
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Visit> getBySpr(Long cpId, String sprNumber) {
		Criteria query = getCurrentSession().createCriteria(Visit.class, "visit")
			.createAlias("visit.registration", "cpr")
			.createAlias("cpr.collectionProtocol", "cp")
			.add(Restrictions.eq("cp.id", cpId));
		if (isMySQL()) {
			query.add(Restrictions.eq("visit.surgicalPathologyNumber", sprNumber));
		} else {
			query.add(Restrictions.eq("visit.surgicalPathologyNumber", sprNumber).ignoreCase());
		}

		return query.list();
	}

	private DetachedCriteria getVisitIdsListQuery(VisitsListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(Visit.class, "visit")
			.setProjection(Projections.distinct(Projections.property("visit.id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());

		if (crit.lastId() != null && crit.lastId() >= 0L) {
			query.add(Restrictions.gt("id", crit.lastId()));
		}

		String startAlias = "cpr";
		if (crit.cpId() != null) {
			startAlias = "cpSite";
			query.createAlias("visit.registration", "cpr")
				.createAlias("cpr.collectionProtocol", "cp")
				.add(Restrictions.eq("cp.id", crit.cpId()));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			applyIdsFilter(query, "visit.id", crit.ids());
		}

		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(Restrictions.in("name", crit.names()));
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps())) {
			BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), startAlias, false);
		}

		return detachedCriteria;
	}

	private static final String FQN = Visit.class.getName();

	private static final String GET_VISIT_STATS = FQN + ".getVisitStats";

	private static final String GET_ANTICIPATED_VISIT_STATS = FQN + ".getAnticipatedVisitStats";

	private static final String GET_VISITS_BY_IDS = FQN + ".getVisitsByIds";

	private static final String GET_VISIT_BY_NAME = FQN + ".getVisitByName";

	private static final String GET_VISIT_BY_SPR = FQN + ".getVisitBySpr";

	private static final String GET_LATEST_VISIT_BY_CPR_ID = FQN + ".getLatestVisitByCprId";

	private static final String GET_BY_EMPI_OR_MRN = FQN + ".getVisitsByEmpiOrMrn";
}

