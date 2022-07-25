
package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CpConsentTier;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.CollectionProtocolDao;
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Status;

public class CollectionProtocolDaoImpl extends AbstractDao<CollectionProtocol> implements CollectionProtocolDao {

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocolSummary> getCollectionProtocols(CpListCriteria crit) {
		List<CollectionProtocolSummary> cpList = new ArrayList<>();
		Map<Long, CollectionProtocolSummary> cpMap = new HashMap<>();
		
		boolean includePi = crit.includePi();
		boolean includeStats = crit.includeStat();
		
		List<Object[]> rows = getCpList(crit);
		for (Object[] row : rows) {
			CollectionProtocolSummary cp = getCp(row, includePi);
			if (includeStats) {
				cpMap.put(cp.getId(), cp);
			}
			
			cpList.add(cp);
		}

		if (includeStats && !cpMap.isEmpty()) {
			rows = getCurrentSession().getNamedQuery(GET_PARTICIPANT_N_SPECIMEN_CNT)
				.setParameterList("cpIds", cpMap.keySet())
				.list();
			
			for (Object[] row : rows) {
				Long cpId = (Long)row[0];
				CollectionProtocolSummary cp = cpMap.get(cpId);
				if (!cp.isSpecimenCentric()) {
					cp.setParticipantCount((Long)row[1]);
				}

				cp.setSpecimenCount((Long)row[2]);			
			}			
		}
				
		return cpList;
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Long> getAllCpIds() {
		return (List<Long>) getCurrentSession().getNamedQuery(GET_ALL_CP_IDS).list();
	}

	@Override
	public Long getCpCount(CpListCriteria criteria) {
		Number count = ((Number)getCpQuery(criteria)
				.setProjection(Projections.rowCount())
				.uniqueResult());
		return count.longValue();
	}

	@Override
	@SuppressWarnings(value = {"unchecked"})
	public CollectionProtocol getCollectionProtocol(String cpTitle) {
		List<CollectionProtocol> cpList = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPS_BY_TITLE)
				.setParameterList("titles" , Collections.singletonList(cpTitle))
				.list();
		return cpList == null || cpList.isEmpty() ? null : cpList.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocol> getCpsByTitle(Collection<String> titles) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPS_BY_TITLE)
				.setParameterList("titles", titles)
				.list();
	}

	@Override
	public CollectionProtocol getCpByShortTitle(String shortTitle) {
		List<CollectionProtocol> cpList = getCpsByShortTitle(Collections.singleton(shortTitle));
		return cpList == null || cpList.isEmpty() ? null : cpList.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPS_BY_SHORT_TITLE)
				.setParameterList("shortTitles", shortTitles)
				.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles, String siteName) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPS_BY_SHORT_TITLE_N_SITE)
				.setParameterList("shortTitles", shortTitles)
				.setString("siteName", siteName)
				.list();
	}
	
	@SuppressWarnings("unchecked")
 	@Override
	public List<CollectionProtocol> getExpiringCps(Date fromDate, Date toDate) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_EXPIRING_CPS)
				.setDate("fromDate", fromDate)
				.setDate("toDate", toDate)
				.list();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public CollectionProtocol getCpByCode(String code) {
		List<CollectionProtocol> cps = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CP_BY_CODE)
				.setString("code", code)
				.list();
		return CollectionUtils.isEmpty(cps) ? null : cps.iterator().next();
	}

	
	@SuppressWarnings("unchecked")
	@Override
	public List<Long> getCpIdsBySiteIds(Collection<Long> instituteIds, Collection<Long> siteIds, Collection<String> sites) {
		Criteria query = getCurrentSession().createCriteria(CollectionProtocol.class, "cp")
			.createAlias("cp.sites", "cpSite")
			.createAlias("cpSite.site", "site")
			.setProjection(Projections.distinct(Projections.property("cp.id")));

		if (CollectionUtils.isNotEmpty(sites)) {
			query.add(Restrictions.in("site.name", sites));
		}

		Disjunction siteCond = Restrictions.disjunction();
		if (CollectionUtils.isNotEmpty(instituteIds)) {
			query.createAlias("site.institute", "institute");
			siteCond.add(Restrictions.in("institute.id", instituteIds));
		}

		if (CollectionUtils.isNotEmpty(siteIds)) {
			siteCond.add(Restrictions.in("site.id", siteIds));
		}

		return (List<Long>) query.add(siteCond).list();
	}

	@Override
	public Map<String, Object> getCpIds(String key, Object value) {
		return getObjectIds("cpId", key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Set<SiteCpPair> getSiteCps(Collection<Long> cpIds) {
		List<Object[]> rows = getSessionFactory().getCurrentSession()
				.getNamedQuery(GET_SITE_IDS_BY_CP_IDS)
				.setParameterList("cpIds", cpIds)
				.list();
		return rows.stream().map(row -> SiteCpPair.make((Long) row[1], (Long) row[2], (Long) row[0])).collect(Collectors.toSet());
	}

	@Override
	public boolean isCpAffiliatedToUserInstitute(Long cpId, Long userId) {
		Integer count = (Integer) getCurrentSession().getNamedQuery(IS_CP_RELATED_TO_USER_INSTITUTE)
			.setParameter("cpId", cpId)
			.setParameter("userId", userId)
			.uniqueResult();
		return count != null && count > 0;
	}

	@Override
	public CollectionProtocolEvent getCpe(Long cpeId) {
		List<CollectionProtocolEvent> events = getCpes(Collections.singleton(cpeId));
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocolEvent> getCpes(Collection<Long> cpeIds) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPE_BY_IDS)
				.setParameterList("cpeIds", cpeIds)
				.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public CollectionProtocolEvent getCpeByEventLabel(Long cpId, String label) {
		List<CollectionProtocolEvent> events = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPE_BY_CP_AND_LABEL)
				.setLong("cpId", cpId)
				.setString("label", label)
				.list();
		
		return events != null && !events.isEmpty() ? events.iterator().next() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public CollectionProtocolEvent getCpeByEventLabel(String title, String label) {
		List<CollectionProtocolEvent> events = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPE_BY_CP_TITLE_AND_LABEL)
				.setString("title", title)
				.setString("label", label)
				.list();
		
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public CollectionProtocolEvent getCpeByShortTitleAndEventLabel(String shortTitle, String label) {
		List<CollectionProtocolEvent> events = getCpesByShortTitleAndEventLabels(shortTitle, Collections.singleton(label));
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<CollectionProtocolEvent> getCpesByShortTitleAndEventLabels(String shortTitle, Collection<String> labels) {
		return sessionFactory.getCurrentSession()
			.getNamedQuery(GET_CPES_BY_CP_SHORT_TITLE_AND_LABELS)
			.setString("shortTitle", shortTitle)
			.setParameterList("labels", labels)
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public CollectionProtocolEvent getCpeByCode(String shortTitle, String code) {
		List<CollectionProtocolEvent> events = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CPE_BY_CODE)
				.setString("shortTitle", shortTitle)
				.setString("code", code)
				.list();
		
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}

	@Override
	public void saveCpe(CollectionProtocolEvent cpe) {
		saveCpe(cpe, false);		
	}

	@Override
	public void saveCpe(CollectionProtocolEvent cpe, boolean flush) {
		getSessionFactory().getCurrentSession().saveOrUpdate(cpe);
		if (flush) {
			getSessionFactory().getCurrentSession().flush();
		}		
	}

	@Override
	public int getAllVisitsCount(Long cpeId) {
		Number count = (Number) getCurrentSession().getNamedQuery(GET_ALL_VISITS_COUNT_BY_CPE)
			.setParameter("cpeId", cpeId)
			.uniqueResult();
		return count != null ? count.intValue() : 0;
	}

	@Override
	public SpecimenRequirement getSpecimenRequirement(Long requirementId) {
		return (SpecimenRequirement) sessionFactory.getCurrentSession()
				.get(SpecimenRequirement.class, requirementId);
	}

	@SuppressWarnings("unchecked")
	@Override	
	public SpecimenRequirement getSrByCode(String code) {
		List<SpecimenRequirement> srs = sessionFactory.getCurrentSession()
			.getNamedQuery(GET_SR_BY_CODE)
			.setString("code", code)
			.list();
		
		return CollectionUtils.isEmpty(srs) ? null : srs.iterator().next();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public List<CpWorkflowConfig> getCpWorkflows(Collection<Long> cpIds) {
		return sessionFactory.getCurrentSession()
			.createCriteria(CpWorkflowConfig.class)
			.add(Restrictions.in("id", cpIds))
			.list();
	}

	@Override
	public void saveCpWorkflows(CpWorkflowConfig cfg) {
		sessionFactory.getCurrentSession().saveOrUpdate(cfg);
	}

	@Override
	@SuppressWarnings("unchecked")
	public CpWorkflowConfig getCpWorkflows(Long cpId) {		
		List<CpWorkflowConfig> cfgs = getCpWorkflows(Collections.singleton(cpId));
		return CollectionUtils.isEmpty(cfgs) ? null : cfgs.iterator().next();
	}

	@Override
	public CpConsentTier getConsentTier(Long consentId) {
		return (CpConsentTier) sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CONSENT_TIER)
				.setLong("consentId", consentId)
				.uniqueResult();
	}
	
	@Override
	public CpConsentTier getConsentTierByStatement(Long cpId, String statement) {
		return (CpConsentTier) sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CONSENT_TIER_BY_STATEMENT)
				.setLong("cpId", cpId)
				.setString("statement", statement);
	}
	
	@Override
	public int getConsentRespsCount(Long consentId) {
		return ((Number)sessionFactory.getCurrentSession()
				.getNamedQuery(GET_CONSENT_RESP_COUNT)
				.setLong("consentId", consentId)
				.uniqueResult()).intValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public boolean anyBarcodingEnabledCpExists() {
		List<Object> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_BARCODING_ENABLED_CP_IDS)
				.setMaxResults(1)
				.list();
		return CollectionUtils.isNotEmpty(result);
	}

	@Override
	public List<String> getDependentContainers(Long cpId, Collection<Long> siteIds) {
		return getCurrentSession().getNamedQuery(GET_DEPENDENT_CONTAINERS)
			.setParameter("cpId", cpId)
			.setParameterList("siteIds", siteIds)
			.setMaxResults(10)
			.list();
	}

	@Override
	public Class<CollectionProtocol> getType() {
		return CollectionProtocol.class;
	}

	@SuppressWarnings("unchecked")
	private List<Object[]> getCpList(CpListCriteria crit) {
		Criteria query = getCpQuery(crit);
		return addProjections(query, crit)
				.setMaxResults(crit.maxResults())
				.addOrder(Order.asc("shortTitle"))
				.list();
	}
	
	private Criteria getCpQuery(CpListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(CollectionProtocol.class)
				.setFirstResult(crit.startAt())
				.add(Restrictions.ne("activityStatus", Status.ACTIVITY_STATUS_DISABLED.getStatus()))
				.createAlias("principalInvestigator", "pi");
		
		return addSearchConditions(query, crit);
	}

	private Criteria addSearchConditions(Criteria query, CpListCriteria crit) {
		String searchString = crit.query();
		if (StringUtils.isBlank(searchString)) {
			searchString = crit.title();
		} 
		
		if (StringUtils.isNotBlank(searchString)) {
			Junction searchCond = Restrictions.disjunction()
					.add(Restrictions.ilike("title", searchString, MatchMode.ANYWHERE))
					.add(Restrictions.ilike("shortTitle", searchString, MatchMode.ANYWHERE));
			
			if (StringUtils.isNotBlank(crit.query())) {
				searchCond.add(Restrictions.ilike("irbIdentifier", searchString, MatchMode.ANYWHERE));
			}	
			
			query.add(searchCond);
		}

		if (StringUtils.isBlank(crit.query()) && StringUtils.isNotBlank(crit.irbId())) {
			query.add(Restrictions.ilike("irbIdentifier", crit.irbId(), MatchMode.ANYWHERE));
		}

		if (crit.piId() != null) {
			query.add(Restrictions.eq("pi.id", crit.piId()));
		}
		
		String repositoryName = crit.repositoryName();
		if (StringUtils.isNotBlank(repositoryName)) {
			query.createCriteria("sites", "cpSite")
				.createAlias("cpSite.site", "site")
				.add(Restrictions.eq("site.name", repositoryName));
		} else if (crit.instituteId() != null) {
			boolean addInst = CollectionUtils.isEmpty(crit.siteCps());
			if (!addInst) {
				addInst = crit.siteCps().stream().noneMatch(scp -> scp.getInstituteId().equals(crit.instituteId()));
			}

			if (addInst) {
				SiteCpPair siteCp = new SiteCpPair();
				siteCp.setInstituteId(crit.instituteId());
				addSiteCpsCond(query, Collections.singleton(siteCp));
			}
		}

		applyIdsFilter(query, "id", crit.ids());
		addSiteCpsCond(query, crit.siteCps());
		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(Restrictions.not(Restrictions.in("id", crit.notInIds())));
		}

		return query;
	}
	
	private Criteria addProjections(Criteria query, CpListCriteria crit) {
		ProjectionList projs = Projections.projectionList();
		query.setProjection(projs);
		
		projs.add(Projections.property("id"));
		projs.add(Projections.property("shortTitle"));
		projs.add(Projections.property("title"));
		projs.add(Projections.property("code"));
		projs.add(Projections.property("startDate"));
		projs.add(Projections.property("ppidFormat"));
		projs.add(Projections.property("manualPpidEnabled"));
		projs.add(Projections.property("specimenCentric"));
		projs.add(Projections.property("catalogId"));

		if (crit.includePi()) {
			projs.add(Projections.property("pi.id"));
			projs.add(Projections.property("pi.firstName"));
			projs.add(Projections.property("pi.lastName"));
			projs.add(Projections.property("pi.loginName"));
		}

		return query;
	}

	private CollectionProtocolSummary getCp(Object[] fields, boolean includePi) {
		int idx = 0;
		CollectionProtocolSummary cp = new CollectionProtocolSummary();		
		cp.setId((Long)fields[idx++]);
		cp.setShortTitle((String)fields[idx++]);
		cp.setTitle((String)fields[idx++]);
		cp.setCode((String)fields[idx++]);
		cp.setStartDate((Date)fields[idx++]);
		cp.setPpidFmt((String)fields[idx++]);
		cp.setManualPpidEnabled((Boolean)fields[idx++]);
		cp.setSpecimenCentric((Boolean)fields[idx++]);
		cp.setCatalogId((Long)fields[idx++]);

		if (includePi) {
			UserSummary user = new UserSummary();
			user.setId((Long)fields[idx++]);
			user.setFirstName((String)fields[idx++]);
			user.setLastName((String)fields[idx++]);
			user.setLoginName((String)fields[idx++]);
			cp.setPrincipalInvestigator(user);
		}

		return cp;
	}

	private void addSiteCpsCond(Criteria query, Collection<SiteCpPair> siteCps) {
		if (CollectionUtils.isEmpty(siteCps)) {
			return;
		}

		query.add(Subqueries.propertyIn("id", BiospecimenDaoHelper.getInstance().getCpIdsFilter(siteCps)));
	}

	private static final String FQN = CollectionProtocol.class.getName();
	
	private static final String GET_PARTICIPANT_N_SPECIMEN_CNT = FQN + ".getParticipantAndSpecimenCount";

	private static final String GET_ALL_CP_IDS = FQN + ".getAllCpIds";
	
	private static final String GET_CPE_BY_CP_AND_LABEL = FQN + ".getCpeByCpIdAndEventLabel";
	
	private static final String GET_CPE_BY_CP_TITLE_AND_LABEL = FQN + ".getCpeByTitleAndEventLabel";
	
	private static final String GET_CPES_BY_CP_SHORT_TITLE_AND_LABELS = FQN + ".getCpesByShortTitleAndEventLabels";

	private static final String GET_CPS_BY_TITLE = FQN + ".getCpsByTitle";

	private static final String GET_CPS_BY_SHORT_TITLE = FQN + ".getCpsByShortTitle";

	private static final String GET_CPS_BY_SHORT_TITLE_N_SITE = FQN + ".getCpsByShortTitleAndSite";
	
	private static final String GET_EXPIRING_CPS = FQN + ".getExpiringCps";
	
	private static final String GET_CP_BY_CODE = FQN + ".getByCode";

	private static final String IS_CP_RELATED_TO_USER_INSTITUTE = FQN + ".ensureCpIsAffiliatedtoUserInstitute";
	
	private static final String GET_SITE_IDS_BY_CP_IDS = FQN + ".getRepoIdsByCps";
	
	private static final String GET_CONSENT_TIER = FQN + ".getConsentTier";

	private static final String GET_CONSENT_TIER_BY_STATEMENT = FQN + ".getConsentTierByStatement";
	
	private static final String GET_CONSENT_RESP_COUNT = FQN + ".getConsentResponsesCount";

	private static final String GET_DEPENDENT_CONTAINERS = FQN + ".getContainersWithCpRestrictions";

	private static final String CPE_FQN = CollectionProtocolEvent.class.getName();
	
	private static final String GET_CPE_BY_IDS = CPE_FQN + ".getCpeByIds";
	
	private static final String GET_CPE_BY_CODE = CPE_FQN + ".getByCode";

	private static final String GET_MIN_CPE_CAL_POINT = CPE_FQN + ".getMinEventPoint";

	private static final String GET_ALL_VISITS_COUNT_BY_CPE = CPE_FQN + ".getAllVisitsCount";
	
	private static final String SR_FQN = SpecimenRequirement.class.getName();
	
	private static final String GET_SR_BY_CODE = SR_FQN + ".getByCode";

	private static final String GET_BARCODING_ENABLED_CP_IDS = FQN + ".getBarcodingEnabledCpIds";
}
