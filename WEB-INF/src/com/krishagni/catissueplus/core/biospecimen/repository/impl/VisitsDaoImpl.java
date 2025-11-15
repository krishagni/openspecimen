
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsDao;
import com.krishagni.catissueplus.core.biospecimen.repository.VisitsListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class VisitsDaoImpl extends AbstractDao<Visit> implements VisitsDao {
	
	@Override
	public Class<Visit> getType() {
		return Visit.class;
	}

	@Override
	public void loadCreatedVisitStats(Map<Long, ? extends VisitSummary> visitsMap) {
		if (visitsMap == null || visitsMap.isEmpty()) {
			return;
		}

		List<Object[]> rows = createNamedQuery(GET_VISIT_STATS, Object[].class)
			.setParameterList("visitIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = -1;
			Long visitId = (Long) row[++idx];
			VisitSummary visit = visitsMap.get(visitId);
			visit.setTotalPendingSpmns((Integer) row[++idx]);
			visit.setPendingPrimarySpmns((Integer) row[++idx]);
			visit.setPlannedPrimarySpmnsColl((Integer) row[++idx]);
			visit.setUnplannedPrimarySpmnsColl((Integer) row[++idx]);
			visit.setUncollectedPrimarySpmns((Integer) row[++idx]);
			visit.setStoredSpecimens((Integer) row[++idx]);
			visit.setNotStoredSpecimens((Integer) row[++idx]);
			visit.setDistributedSpecimens((Integer) row[++idx]);
			visit.setClosedSpecimens((Integer) row[++idx]);
		}
	}

	@Override
	public void loadAnticipatedVisitStats(Map<Long, ? extends VisitSummary> visitsMap) {
		if (visitsMap == null || visitsMap.isEmpty()) {
			return;
		}

		List<Object[]> rows = createNamedQuery(GET_ANTICIPATED_VISIT_STATS, Object[].class)
			.setParameterList("eventIds", visitsMap.keySet())
			.list();

		for (Object[] row : rows) {
			int idx = -1;
			Long eventId = (Long) row[++idx];
			VisitSummary visit = visitsMap.get(eventId);
			visit.setTotalPendingSpmns((Integer) row[++idx]);
			visit.setPendingPrimarySpmns((Integer) row[++idx]);
		}
	}

	@Override
	public Long getVisitsCount(VisitsListCriteria crit) {
		Criteria<Visit> query = createCriteria(Visit.class, "visit");
		return query.add(query.in("visit.id", getVisitIdsListQuery(crit, query))).getCount("visit.id");
	}

	@Override
	public List<Visit> getVisitsList(VisitsListCriteria crit) {
		Criteria<Visit> query = createCriteria(Visit.class, "visit");
		query.add(query.in("visit.id", getVisitIdsListQuery(crit, query)))
			.orderBy(query.asc("visit.id"));
		if (CollectionUtils.isEmpty(crit.names())) {
			return query.list(crit.startAt(), crit.maxResults());
		}

		return query.list();
	}

	@Override
	public Visit getByName(String name) {
		List<Visit> visits = getByName(Collections.singleton(name));
		return !visits.isEmpty() ? visits.iterator().next() : null;
	}

	@Override
	public List<Visit> getByName(Collection<String> names) {
		return createNamedQuery(GET_VISIT_BY_NAME, Visit.class)
			.setParameterList("names", names)
			.list();
	}

	@Override
	public List<Visit> getByIds(Collection<Long> ids) {
		return createNamedQuery(GET_VISITS_BY_IDS, Visit.class)
			.setParameterList("ids", ids)
			.list();
	}

	@Override
	public List<Visit> getBySpr(String sprNumber) {
		return createNamedQuery(GET_VISIT_BY_SPR, Visit.class)
			.setParameter("sprNumber", sprNumber)
			.list();
	}

	@Override
	public List<Visit> getByEvent(Long cprId, String eventLabel) {
		Criteria<Visit> query = createCriteria(Visit.class, "v");
		return query.join("v.cpEvent", "event")
			.join("v.registration", "reg")
			.add(query.eq("reg.id", cprId))
			.add(query.eq("event.eventLabel", eventLabel))
			.list();
	}

	@Override
	public Map<String, Object> getCprVisitIds(String key, Object value) {
		Criteria<Object[]> query = createCriteria(Visit.class, Object[].class, "visit");
		List<Object[]> rows = query.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp")
			.select("visit.id", "cpr.id", "cp.id")
			.add(query.eq(key, value))
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
	public Visit getLatestVisit(Long cprId) {
		List<Visit> visits = createNamedQuery(GET_LATEST_VISIT_BY_CPR_ID, Visit.class)
			.setParameter("cprId", cprId)
			.setMaxResults(1)
			.list();
		return visits.isEmpty() ? null :  visits.get(0);
	}

	@Override
	public List<Visit> getByEmpiOrMrn(Long cpId, String empiOrMrn) {
		return createNamedQuery(GET_BY_EMPI_OR_MRN, Visit.class)
			.setParameter("cpId", cpId)
			.setParameter("empiOrMrn", empiOrMrn.toLowerCase())
			.list();
	}

	@Override
	public List<Visit> getBySpr(Long cpId, String sprNumber) {
		Criteria<Visit> query = createCriteria(Visit.class, "visit")
			.join("visit.registration", "cpr")
			.join("cpr.collectionProtocol", "cp");
		query.add(query.eq("cp.id", cpId));

		if (isMySQL()) {
			query.add(query.eq("visit.surgicalPathologyNumber", sprNumber));
		} else {
			query.add(query.eq("lower(visit.surgicalPathologyNumber)", sprNumber.toLowerCase()));
		}

		return query.list();
	}

	@Override
	public Long getCustomFieldRecordId(Long visitId, Long formId, Long formCtxtId) {
		return (Long) getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_ID)
			.setParameter("visitId", visitId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.uniqueResult();
	}

	@Override
	public Map<Long, Long> getCustomFieldRecordIds(Collection<Long> visitIds, Long formId, Long formCtxtId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_IDS)
			.setParameterList("visitIds", visitIds)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.list();

		Map<Long, Long> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Long) row[1]);
		}

		return result;
	}

	@Override
	public int insertCustomFieldRecordId(Long visitId, Long formId, Long formCtxtId, Long recordId) {
		int rows = getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_RECORD)
			.setParameter("visitId", visitId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("recordId", recordId)
			.executeUpdate();

		getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_REC_STATUS)
			.setParameter("visitId", visitId)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.executeUpdate();

		return rows;
	}

	private SubQuery<Long> getVisitIdsListQuery(VisitsListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> query = mainQuery.createSubQuery(Visit.class, "visit").select("visit.id");
		if (crit.lastId() != null && crit.lastId() >= 0L) {
			query.add(query.gt("visit.id", crit.lastId()));
		}

		if (crit.cpId() != null) {
			query.join("visit.registration", "cpr")
				.join("cpr.collectionProtocol", "cp")
				.add(query.eq("cp.id", crit.cpId()));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			applyIdsFilter(query, "visit.id", crit.ids());
		}

		if (CollectionUtils.isNotEmpty(crit.names())) {
			query.add(query.in("visit.name", crit.names()));
		}

		if (CollectionUtils.isNotEmpty(crit.siteCps())) {
			BiospecimenDaoHelper.getInstance().addSiteCpsCond(query, crit.siteCps(), crit.useMrnSites(), false);
		}

		return query;
	}

	private static final String FQN = Visit.class.getName();

	private static final String GET_VISIT_STATS = FQN + ".getVisitStats";

	private static final String GET_ANTICIPATED_VISIT_STATS = FQN + ".getAnticipatedVisitStats";

	private static final String GET_VISITS_BY_IDS = FQN + ".getVisitsByIds";

	private static final String GET_VISIT_BY_NAME = FQN + ".getVisitByName";

	private static final String GET_VISIT_BY_SPR = FQN + ".getVisitBySpr";

	private static final String GET_LATEST_VISIT_BY_CPR_ID = FQN + ".getLatestVisitByCprId";

	private static final String GET_BY_EMPI_OR_MRN = FQN + ".getVisitsByEmpiOrMrn";

	private static final String GET_CUSTOM_FIELD_RECORD_ID = FQN + ".getCustomFieldRecordId";

	private static final String GET_CUSTOM_FIELD_RECORD_IDS = FQN + ".getCustomFieldRecordIds";

	private static final String INSERT_CUSTOM_FIELD_RECORD = FQN + ".insertCustomFieldRecord";

	private static final String INSERT_CUSTOM_FIELD_REC_STATUS = FQN + ".insertCustomFieldRecordStatus";
}

