
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolRegistration;
import com.krishagni.catissueplus.core.biospecimen.events.CprSummary;
import com.krishagni.catissueplus.core.biospecimen.events.ParticipantSummary;
import com.krishagni.catissueplus.core.biospecimen.events.PmiDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolRegistrationDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CprListCriteria;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Property;
import com.krishagni.catissueplus.core.common.repository.PropertyBuilder;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Utility;

public class CollectionProtocolRegistrationDaoImpl extends AbstractDao<CollectionProtocolRegistration> implements CollectionProtocolRegistrationDao {
	
	@Override
	public Class<CollectionProtocolRegistration> getType() {
		return CollectionProtocolRegistration.class;
	}

	@Override
	public List<CprSummary> getCprList(CprListCriteria crit) {
		Criteria<Object[]> query = getCprListQuery(crit);
		query.distinct().select(getCprSummaryFields(crit, query))
			.addOrder(query.desc("cpr.registrationDate"));

		List<CprSummary> cprs = new ArrayList<>();
		Map<Long, CprSummary> cprMap = new HashMap<>();
		boolean allCpsAccess = CollectionUtils.isEmpty(crit.siteCps());
		Set<Long> phiCps = getPhiCps(crit.phiSiteCps());
		for (Object[] row : query.list(crit.startAt(), crit.maxResults())) {
			CprSummary cpr = getCprSummary(row, allCpsAccess, phiCps);
			if (crit.includeStat()) {
				cprMap.put(cpr.getCprId(), cpr);
			}
			
			cprs.add(cpr);
		}
		
		if (!crit.includeStat()) {
			return cprs;
		}
		
		List<Object[]> countRows = getScgAndSpecimenCounts(crit);
		for (Object[] row : countRows) {
			CprSummary cpr = cprMap.get((Long)row[0]);
			cpr.setScgCount((Long)row[1]);
			cpr.setSpecimenCount((Long)row[2]);
		}
		
		return cprs;
	}

	@Override
	public Long getCprCount(CprListCriteria cprCrit) {
		Criteria<CollectionProtocolRegistration> query = createCriteria(CollectionProtocolRegistration.class, "cpr");
		return query.add(query.in("cpr.id", getCprIdsQuery(cprCrit, query))).getCount("cpr.id");
	}

	@Override
	public List<CollectionProtocolRegistration> getCprs(CprListCriteria crit) {
		Criteria<CollectionProtocolRegistration> query = createCriteria(CollectionProtocolRegistration.class, "cpr");
		query.add(query.in("cpr.id", getCprIdsQuery(crit, query)));

		String orderBy = StringUtils.isNotBlank(crit.orderBy()) ? crit.orderBy() : "cpr.id";
		query.addOrder(crit.asc() ? query.asc(orderBy) : query.desc(orderBy));

		if (CollectionUtils.isEmpty(crit.ppids()) && CollectionUtils.isEmpty(crit.externalSubjectIds()) && CollectionUtils.isEmpty(crit.empis())) {
			return query.list(crit.startAt(), crit.maxResults());
		}

		return query.list();
	}

	@Override
	public List<CollectionProtocolRegistration> getCprsByCpId(Long cpId, int startAt, int maxResults) {
		return createNamedQuery(GET_BY_CP_ID, CollectionProtocolRegistration.class)
			.setParameter("cpId", cpId)
			.setFirstResult(Math.max(startAt, 0))
			.setMaxResults(maxResults < 0 ? 100 : maxResults)
			.list();
	}

	@Override
	public CollectionProtocolRegistration getCprByBarcode(String barcode) {
		Criteria<CollectionProtocolRegistration> query = createCriteria(CollectionProtocolRegistration.class, "cpr");
		List<CollectionProtocolRegistration> cprs = query.add(query.eq("cpr.barcode", barcode)).list();
		return cprs.isEmpty() ? null : cprs.iterator().next();
	}

	@Override
	public CollectionProtocolRegistration getCprByPpid(Long cpId, String ppid) {
		List<CollectionProtocolRegistration> result = createNamedQuery(GET_BY_CP_ID_AND_PPID, CollectionProtocolRegistration.class)
			.setParameter("cpId", cpId)
			.setParameter("ppid", ppid)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}
	
	@Override
	public CollectionProtocolRegistration getCprByPpid(String cpTitle, String ppid) {
		List<CollectionProtocolRegistration> result = createNamedQuery(GET_BY_CP_TITLE_AND_PPID, CollectionProtocolRegistration.class)
			.setParameter("title", cpTitle)
			.setParameter("ppid", ppid)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}
	
	@Override
	public CollectionProtocolRegistration getCprByCpShortTitleAndPpid(String cpShortTitle, String ppid) {
		List<CollectionProtocolRegistration> result = createNamedQuery(GET_BY_CP_SHORT_TITLE_AND_PPID, CollectionProtocolRegistration.class)
			.setParameter("shortTitle", cpShortTitle)
			.setParameter("ppid", ppid)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	@Override
	public CollectionProtocolRegistration getCprByEmpi(String cpShortTitle, String empi) {
		List<CollectionProtocolRegistration> result = createNamedQuery(GET_BY_CP_SHORT_TITLE_AND_EMPI, CollectionProtocolRegistration.class)
			.setParameter("shortTitle", cpShortTitle)
			.setParameter("empi", empi)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	@Override
	public CollectionProtocolRegistration getCprByUid(String cpShortTitle, String uid) {
		List<CollectionProtocolRegistration> result = createNamedQuery(GET_BY_CP_SHORT_TITLE_AND_UID, CollectionProtocolRegistration.class)
			.setParameter("shortTitle", cpShortTitle)
			.setParameter("uid", uid)
			.list();
		return CollectionUtils.isEmpty(result) ? null : result.iterator().next();
	}

	@Override
	public List<CollectionProtocolRegistration> getCprsByPmis(String cpShortTitle, List<PmiDetail> pmis) {
		Criteria<CollectionProtocolRegistration> query = getByCpShortTitleAndPmisQuery(cpShortTitle, pmis);
		if (query == null) {
			return Collections.emptyList();
		}

		return query.list();
	}

	private Criteria<CollectionProtocolRegistration> getByCpShortTitleAndPmisQuery(String cpShortTitle, List<PmiDetail> pmis) {
		Criteria<CollectionProtocolRegistration> query = createCriteria(CollectionProtocolRegistration.class, "cpr");
		query.join("cpr.participant", "p")
			.join("cpr.collectionProtocol", "cp")
			.join("p.pmis", "pmi")
			.join("pmi.site", "site");

		boolean added = false;
		Disjunction disjunction = query.disjunction();
		for (PmiDetail pmi : pmis) {
			if (StringUtils.isBlank(pmi.getSiteName()) || StringUtils.isBlank(pmi.getMrn())) {
				continue;
			}

			disjunction.add(
				query.and(
					query.eq("site.name", pmi.getSiteName()),
					query.eq("pmi.medicalRecordNumber", pmi.getMrn())
				)
			);

			added = true;
		}

		if (!added) {
			return null;
		}

		return query.add(disjunction).add(query.eq("cp.shortTitle", cpShortTitle));
	}

	@Override
	public CollectionProtocolRegistration getCprByParticipantId(Long cpId, Long participantId) {
		List<CollectionProtocolRegistration> result =  createNamedQuery(GET_BY_CP_ID_AND_PID, CollectionProtocolRegistration.class)
			.setParameter("cpId", cpId)
			.setParameter("participantId", participantId)
			.list();
		return result.isEmpty() ? null : result.iterator().next();
	}

	@Override
	public Map<String, Object> getCprIds(String key, Object value) {
		Criteria<Object[]> query = createCriteria(CollectionProtocolRegistration.class, Object[].class, "cpr");
		List<Object[]> rows = query.join("cpr.collectionProtocol", "cp")
			.select("cpr.id", "cp.id")
			.add(query.eq(key, value))
			.list();

		if (CollectionUtils.isEmpty(rows)) {
			return Collections.emptyMap();
		}

		Object[] row = rows.iterator().next();
		Map<String, Object> ids = new HashMap<>();
		ids.put("cprId", row[0]);
		ids.put("cpId", row[1]);
		return ids;
	}

	@Override
	public Map<String, Integer> getParticipantsBySite(Long cpId, Collection<Long> siteIds) {
		List<Object[]> rows = createNamedQuery(GET_COUNTS_BY_SITE, Object[].class)
			.setParameter("cpId", cpId)
			.setParameterList("siteIds", siteIds)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (String)row[0], row -> ((Number)row[1]).intValue()));
	}

	@Override
	public List<CollectionProtocolRegistration> getByPpids(String cpShortTitle, List<String> ppids) {
		return createNamedQuery(GET_BY_PPIDS, CollectionProtocolRegistration.class)
			.setParameter("cpShortTitle", cpShortTitle)
			.setParameterList("ppids", ppids)
			.list();
	}

	@Override
	public Long getCustomFieldRecordId(Long cprId, Long formId, Long formCtxtId) {
		return (Long) getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_ID)
			.setParameter("cprId", cprId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.uniqueResult();
	}

	@Override
	public Map<Long, Long> getCustomFieldRecordIds(Collection<Long> cprIds, Long formId, Long formCtxtId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_IDS)
			.setParameterList("cprIds", cprIds)
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
	public int insertCustomFieldRecordId(Long cprId, Long formId, Long formCtxtId, Long recordId, String formStatus) {
		int rows = getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_RECORD)
			.setParameter("cprId", cprId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("recordId", recordId)
			.executeUpdate();

		getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_REC_STATUS)
			.setParameter("cprId", cprId)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.setParameter("formStatus", formStatus)
			.executeUpdate();

		return rows;
	}

	@Override
	public int updateCustomFieldRecStatus(Long cprId, Long formId, Long recordId, String formStatus) {
		return getCurrentSession().getNamedQuery(UPDATE_CUSTOM_FIELD_REC_STATUS)
			.setParameter("formStatus", formStatus)
			.setParameter("cprId", cprId)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.executeUpdate();
	}

	private Criteria<Object[]> getCprListQuery(CprListCriteria cprCrit) {
		Criteria<Object[]> query = createCriteria(CollectionProtocolRegistration.class, Object[].class, "cpr");
		query.join("collectionProtocol", "cp")
			.join("participant", "participant")
			.leftJoin("participant.pmis", "pmi")
			.add(query.ne("activityStatus", "Disabled"))
			.add(query.ne("cp.activityStatus", "Disabled"))
			.add(query.ne("participant.activityStatus", "Disabled"));

		addCpRestrictions(query, cprCrit);
		addRegDateCondition(query, cprCrit);
		addMrnEmpiUidCondition(query, cprCrit);
		addNamePpidAndUidCondition(query, cprCrit);
		addDobCondition(query, cprCrit);
		addSpecimenCondition(query, cprCrit);
		addSiteCpsCond(query, cprCrit);
		addEmpisCond(query, cprCrit);
		addPpidsCond(query, cprCrit);
		addExternalSubjectIdsCond(query, cprCrit);
		addIdsCond(query, cprCrit);
		return query;		
	}

	private void addCpRestrictions(AbstractCriteria<?, ?> query, CprListCriteria cprCrit) {
		if (cprCrit.cpId() == null || cprCrit.cpId() == -1) {
			return;
		}

		query.add(query.eq("cp.id", cprCrit.cpId()));
	}

	private void addRegDateCondition(AbstractCriteria<?, ?> query, CprListCriteria crit) {
		if (crit.registrationDate() == null) {
			return;
		}

		Date from = Utility.chopTime(crit.registrationDate());
		Date to = Utility.getEndOfDay(crit.registrationDate());
		query.add(query.between("cpr.registrationDate", from, to));
	}
	
	private void addMrnEmpiUidCondition(AbstractCriteria<?, ?> query, CprListCriteria crit) {
		if (!crit.includePhi() || StringUtils.isBlank(crit.participantId())) {
			return;
		}

		query.add(
			query.disjunction()
				.add(query.ilike("pmi.medicalRecordNumber", crit.participantId()))
				.add(query.ilike("participant.empi", crit.participantId()))
				.add(query.ilike("participant.uid", crit.participantId()))
		);
	}
	
	private void addNamePpidAndUidCondition(AbstractCriteria<?, ?> query, CprListCriteria crit) {
		if (StringUtils.isNotBlank(crit.query())) {
			Junction cond = query.disjunction().add(query.ilike("cpr.ppid", crit.query()));

			if (crit.includePhi()) {
				cond.add(query.ilike("participant.firstName", crit.query()));
				cond.add(query.ilike("participant.lastName", crit.query()));
				cond.add(query.ilike("participant.uid", crit.query()));
				cond.add(query.ilike("participant.empi", crit.query()));
				cond.add(query.ilike("pmi.medicalRecordNumber", crit.query()));
			}
			
			query.add(cond);
			return;
		}

		if (StringUtils.isNotBlank(crit.ppid())) {
			query.add(query.ilike("cpr.ppid", crit.ppid()));
		}

		if (!crit.includePhi()) {
			return;
		}

		if (StringUtils.isNotBlank(crit.uid())) {
			query.add(query.eq("participant.uid", crit.uid()));
		}

		if (StringUtils.isNotBlank(crit.name())) {
			query.add(query.disjunction()
				.add(query.ilike("participant.firstName", crit.name()))
				.add(query.ilike("participant.lastName", crit.name()))
			);
		}
	}
	
	private void addDobCondition(AbstractCriteria<?, ?> query, CprListCriteria crit) {
		if (!crit.includePhi() || crit.dob() == null) {
			return;
		}

		Date from = Utility.chopTime(crit.dob());
		Date to = Utility.getEndOfDay(crit.dob());
		query.add(query.between("participant.birthDate", from, to));
	}
	
	private void addSpecimenCondition(AbstractCriteria<?, ?> query, CprListCriteria crit) {
		if (StringUtils.isBlank(crit.specimen())) {
			return;
		}
		
		query.join("cpr.visits", "visit")
			.join("visit.specimens", "specimen")
			.add(query.disjunction()
					.add(query.ilike("specimen.label", crit.specimen()))
					.add(query.ilike("specimen.barcode", crit.specimen())))
			.add(query.ne("specimen.activityStatus", "Disabled"))
			.add(query.ne("visit.activityStatus", "Disabled"));
	}

	private void addEmpisCond(Criteria<Object[]> query, CprListCriteria crit) {
		if (CollectionUtils.isNotEmpty(crit.empis())) {
			query.add(query.in("participant.empi", crit.empis()));
		}
	}

	private void addPpidsCond(Criteria<Object[]> query, CprListCriteria crit) {
		if (CollectionUtils.isNotEmpty(crit.ppids())) {
			query.add(query.in("cpr.ppid", crit.ppids()));
		}
	}

	private void addExternalSubjectIdsCond(Criteria<Object[]> query, CprListCriteria crit) {
		if (CollectionUtils.isNotEmpty(crit.externalSubjectIds())) {
			query.add(query.in("cpr.externalSubjectId", crit.externalSubjectIds()));
		}
	}

	private void addIdsCond(Criteria<Object[]> query, CprListCriteria crit) {
		if (CollectionUtils.isNotEmpty(crit.ids())) {
			applyIdsFilter(query, "cpr.id", crit.ids());
		}
	}

	private void addSiteCpsCond(Criteria<Object[]> query, CprListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.siteCps())) {
			return;
		}

		Set<SiteCpPair> siteCps;
		if (crit.includePhi() && crit.hasPhiFields()) {
			//
			// User has phi access and list search criteria is based on one or
			// more phi fields
			//
			siteCps = crit.phiSiteCps();
		} else {
			siteCps = crit.siteCps();
		}

		query.join("cp.sites", "cpSite").join("cpSite.site", "site");
		if (crit.useMrnSites()) {
			if (StringUtils.isBlank(crit.participantId())) {
				query.leftJoin("participant.pmis", "pmi");
			}

			query.leftJoin("pmi.site", "mrnSite");
		}

		Disjunction cpSitesCond = query.disjunction();
		for (SiteCpPair siteCp : siteCps) {
			Junction siteCond = query.disjunction();
			if (crit.useMrnSites()) {
				//
				// When MRNs exist, site ID should be one of the MRN site
				//
				Junction mrnSite = query.conjunction()
					.add(query.isNotNull("pmi.id"))
					.add(getSiteIdRestriction(query, "mrnSite.id", siteCp));

				//
				// When no MRNs exist, site ID should be one of CP site
				//
				Junction cpSite = query.conjunction()
					.add(query.isNull("pmi.id"))
					.add(getSiteIdRestriction(query, "site.id", siteCp));

				siteCond.add(mrnSite).add(cpSite);
			} else {
				//
				// Site ID should be CP site
				//
				siteCond.add(getSiteIdRestriction(query, "site.id", siteCp));
			}

			Junction cond = query.conjunction().add(siteCond);
			if (siteCp.getCpId() != null && !siteCp.getCpId().equals(crit.cpId())) {
				cond.add(query.eq("cp.id", siteCp.getCpId()));
			}

			cpSitesCond.add(cond);
		}

		query.add(cpSitesCond);
	}

	private Set<Long> getPhiCps(Collection<SiteCpPair> siteCps) {
		Set<Long> result = new HashSet<>();
		if (CollectionUtils.isEmpty(siteCps)) {
			return result;
		}

		Criteria<Long> query = createCriteria(CollectionProtocol.class, Long.class, "cp");
		query.join("cp.sites", "cpSite")
			.join("cpSite.site", "site")
			.join("site.institute", "institute")
			.distinct().select("cp.id");

		Disjunction siteCond = query.disjunction();
		for (SiteCpPair siteCp : siteCps) {
			if (siteCp.getCpId() != null) {
				result.add(siteCp.getCpId());
			} else if (siteCp.getSiteId() != null) {
				siteCond.add(query.eq("site.id", siteCp.getSiteId()));
			} else if (siteCp.getInstituteId() != null) {
				siteCond.add(query.eq("institute.id", siteCp.getInstituteId()));
			}
		}

		if (siteCond.hasConditions()) {
			result.addAll(query.add(siteCond).list());
		}

		return result;
	}

	private List<Property> getCprSummaryFields(CprListCriteria cprCrit, Criteria<Object[]> query) {
		PropertyBuilder pb = query.propertyBuilder();
		List<Property> props = new ArrayList<>();
		props.add(pb.property("cpr.id"));
		props.add(pb.property("cpr.ppid"));
		props.add(pb.property("cpr.registrationDate"));
		props.add(pb.property("cp.id"));
		props.add(pb.property("cp.shortTitle"));
		props.add(pb.property("participant.id"));
		props.add(pb.property("participant.source"));

		if (cprCrit.includePhi()) {
			props.add(pb.property("participant.firstName"));
			props.add(pb.property("participant.lastName"));
			props.add(pb.property("participant.empi"));
			props.add(pb.property("participant.uid"));
			props.add(pb.property("participant.emailAddress"));
		}

		return props;
	}
	
	private CprSummary getCprSummary(Object[] row, boolean allCpsAccess, Set<Long> phiCps) {
		int idx = -1;
		CprSummary cpr = new CprSummary();
		cpr.setCprId((Long)row[++idx]);
		cpr.setPpid((String)row[++idx]);
		cpr.setRegistrationDate((Date)row[++idx]);
		cpr.setCpId((Long)row[++idx]);
		cpr.setCpShortTitle((String)row[++idx]);

		ParticipantSummary participant = new ParticipantSummary();
		cpr.setParticipant(participant);
		participant.setId((Long)row[++idx]);
		participant.setSource((String)row[++idx]);
		if (row.length > (idx + 1) && (allCpsAccess || (phiCps != null && phiCps.contains(cpr.getCpId())))) {
			participant.setFirstName((String)row[++idx]);
			participant.setLastName((String) row[++idx]);
			participant.setEmpi((String) row[++idx]);
			participant.setUid((String) row[++idx]);
			participant.setEmailAddress((String) row[++idx]);
		}
		
		return cpr;
	}

	private List<Object[]> getScgAndSpecimenCounts(CprListCriteria cprCrit) {
		Criteria<Object[]> query = getCprListQuery(cprCrit);
		query.addOrder(query.asc("cpr.id"));

		if (StringUtils.isBlank(cprCrit.specimen())) {
			query
				.leftJoin("visits", "visit",
					() -> query.eq("visit.status", "Complete"))
				.leftJoin("visit.specimens", "specimen",
					() -> query.eq("specimen.collectionStatus", "Collected"));
		}

		return query.select(
			query.column("cpr.id"),
			query.distinctCount("visit.id"),
			query.distinctCount("specimen.id")
		).groupBy("cpr.id").list(cprCrit.startAt(), cprCrit.maxResults());
	}

	private SubQuery<Long> getCprIdsQuery(CprListCriteria crit, AbstractCriteria<?, ?> mainQuery) {
		SubQuery<Long> subQuery = mainQuery.createSubQuery(CollectionProtocolRegistration.class, "cpr")
			.join("cpr.participant", "participant")
			.distinct().select("cpr.id");

		if (crit.lastId() != null && crit.lastId() >= 0L) {
			subQuery.add(subQuery.gt("cpr.id", crit.lastId()));
		}

		if (crit.cpId() != null) {
			subQuery.join("cpr.collectionProtocol", "cp")
				.add(subQuery.eq("cp.id", crit.cpId()));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			applyIdsFilter(subQuery, "cpr.id", crit.ids());
		}

		if (CollectionUtils.isNotEmpty(crit.empis())) {
			subQuery.add(subQuery.in("participant.empi", crit.empis()));
		}

		if (CollectionUtils.isNotEmpty(crit.ppids())) {
			subQuery.add(subQuery.in("cpr.ppid", crit.ppids()));
		}

		if (CollectionUtils.isNotEmpty(crit.externalSubjectIds())) {
			subQuery.add(subQuery.in("cpr.externalSubjectId", crit.externalSubjectIds()));
		}

		BiospecimenDaoHelper.getInstance().addSiteCpsCond(subQuery, crit.siteCps(), crit.useMrnSites(), false);
		if (CollectionUtils.isEmpty(crit.siteCps()) && crit.includePhi()) {
			subQuery.leftJoin("participant.pmis", "pmi");
		}

		addCpRestrictions(subQuery, crit);
		addRegDateCondition(subQuery, crit);
		addMrnEmpiUidCondition(subQuery, crit);
		addNamePpidAndUidCondition(subQuery, crit);
		addDobCondition(subQuery, crit);
		addSpecimenCondition(subQuery, crit);
		return subQuery;
	}

	private Restriction getSiteIdRestriction(AbstractCriteria<?, ?> query, String property, SiteCpPair siteCp) {
		if (siteCp.getSiteId() != null) {
			return query.eq(property, siteCp.getSiteId());
		}

		SubQuery<Long> subQuery = query.createSubQuery(Site.class, "site")
			.join("site.institute", "institute")
			.select("site.id");
		return query.in(property, subQuery.eq("institute.id", siteCp.getInstituteId()));
	}
	
	private static final String FQN = CollectionProtocolRegistration.class.getName();
	
	private static final String GET_BY_CP_ID_AND_PPID = FQN + ".getCprByCpIdAndPpid";
	
	private static final String GET_BY_CP_TITLE_AND_PPID = FQN + ".getCprByCpTitleAndPpid";
	
	private static final String GET_BY_CP_SHORT_TITLE_AND_PPID = FQN + ".getCprByCpShortTitleAndPpid";

	private static final String GET_BY_CP_SHORT_TITLE_AND_EMPI = FQN + ".getCprByCpShortTitleAndEmpi";

	private static final String GET_BY_CP_SHORT_TITLE_AND_UID = FQN + ".getCprByCpShortTitleAndUid";

	private static final String GET_BY_CP_ID_AND_PID = FQN + ".getCprByCpIdAndPid";

	private static final String GET_BY_CP_ID = FQN + ".getCprsByCpId";

	private static final String GET_COUNTS_BY_SITE = FQN + ".getParticipantsCountBySite";

	private static final String GET_BY_PPIDS = FQN + ".getByPpids";

	private static final String GET_CUSTOM_FIELD_RECORD_ID = FQN + ".getCustomFieldRecordId";

	private static final String GET_CUSTOM_FIELD_RECORD_IDS = FQN + ".getCustomFieldRecordIds";

	private static final String INSERT_CUSTOM_FIELD_RECORD = FQN + ".insertCustomFieldRecord";

	private static final String INSERT_CUSTOM_FIELD_REC_STATUS = FQN + ".insertCustomFieldRecordStatus";

	private static final String UPDATE_CUSTOM_FIELD_REC_STATUS = FQN + ".updateCustomFieldRecordStatus";
}
