
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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Property;
import com.krishagni.catissueplus.core.common.repository.PropertyBuilder;
import com.krishagni.catissueplus.core.common.util.Status;

public class CollectionProtocolDaoImpl extends AbstractDao<CollectionProtocol> implements CollectionProtocolDao {
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
			rows = createNamedQuery(GET_PARTICIPANT_N_SPECIMEN_CNT, Object[].class)
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
	public List<CollectionProtocol> getCollectionProtocolsList(CpListCriteria crit) {
		Criteria<CollectionProtocol> query = getCpQuery(crit, CollectionProtocol.class);
		return query.add(query.gt("cp.id", crit.lastId() != null ? crit.lastId() : 0L))
			.addOrder(query.asc(StringUtils.isNotBlank(crit.orderBy()) ? crit.orderBy() : "cp.shortTitle"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public List<Long> getAllCpIds() {
		return createNamedQuery(GET_ALL_CP_IDS, Long.class).list();
	}

	@Override
	public Long getCpCount(CpListCriteria criteria) {
		return getCpQuery(criteria, CollectionProtocol.class).getCount("cp.id");
	}

	@Override
	public CollectionProtocol getCollectionProtocol(String cpTitle) {
		List<CollectionProtocol> cpList = createNamedQuery(GET_CPS_BY_TITLE, CollectionProtocol.class)
			.setParameterList("titles" , Collections.singletonList(cpTitle))
			.list();
		return cpList == null || cpList.isEmpty() ? null : cpList.iterator().next();
	}

	@Override
	public List<CollectionProtocol> getCpsByTitle(Collection<String> titles) {
		return createNamedQuery(GET_CPS_BY_TITLE, CollectionProtocol.class)
			.setParameterList("titles", titles)
			.list();
	}

	@Override
	public CollectionProtocol getCpByShortTitle(String shortTitle) {
		List<CollectionProtocol> cpList = getCpsByShortTitle(Collections.singleton(shortTitle));
		return cpList == null || cpList.isEmpty() ? null : cpList.iterator().next();
	}

	@Override
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles) {
		return createNamedQuery(GET_CPS_BY_SHORT_TITLE, CollectionProtocol.class)
			.setParameterList("shortTitles", shortTitles)
			.list();
	}

	@Override
	public List<CollectionProtocol> getCpsByShortTitle(Collection<String> shortTitles, String siteName) {
		return createNamedQuery(GET_CPS_BY_SHORT_TITLE_N_SITE, CollectionProtocol.class)
			.setParameterList("shortTitles", shortTitles)
			.setParameter("siteName", siteName)
			.list();
	}

 	@Override
	public List<CollectionProtocol> getExpiringCps(Date fromDate, Date toDate) {
		return createNamedQuery(GET_EXPIRING_CPS, CollectionProtocol.class)
			.setParameter("fromDate", fromDate)
			.setParameter("toDate", toDate)
			.list();
	}

	@Override
	public CollectionProtocol getCpByCode(String code) {
		List<CollectionProtocol> cps = createNamedQuery(GET_CP_BY_CODE, CollectionProtocol.class)
			.setParameter("code", code)
			.list();
		return CollectionUtils.isEmpty(cps) ? null : cps.iterator().next();
	}

	@Override
	public List<Long> getCpIdsBySiteIds(Collection<Long> instituteIds, Collection<Long> siteIds, Collection<String> sites) {
		Criteria<Long> query = createCriteria(CollectionProtocol.class, Long.class, "cp")
			.join("cp.sites", "cpSite")
			.join("cpSite.site", "site")
			.distinct().select("cp.id");

		if (CollectionUtils.isNotEmpty(sites)) {
			query.add(query.in("site.name", sites));
		}

		Disjunction siteCond = query.disjunction();
		if (CollectionUtils.isNotEmpty(instituteIds)) {
			query.join("site.institute", "institute");
			siteCond.add(query.in("institute.id", instituteIds));
		}

		if (CollectionUtils.isNotEmpty(siteIds)) {
			siteCond.add(query.in("site.id", siteIds));
		}

		return query.add(siteCond).list();
	}

	@Override
	public Map<String, Object> getCpIds(String key, Object value) {
		return getObjectIds("cpId", key, value);
	}

	@Override
	public Set<SiteCpPair> getSiteCps(Collection<Long> cpIds) {
		List<Object[]> rows = createNamedQuery(GET_SITE_IDS_BY_CP_IDS, Object[].class)
			.setParameterList("cpIds", cpIds)
			.list();
		return rows.stream().map(row -> SiteCpPair.make((Long) row[1], (Long) row[2], (Long) row[0])).collect(Collectors.toSet());
	}

	@Override
	public boolean isCpAffiliatedToUserInstitute(Long cpId, Long userId) {
		Integer count = createNamedQuery(IS_CP_RELATED_TO_USER_INSTITUTE, Integer.class)
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

	@Override
	public List<CollectionProtocolEvent> getCpes(Collection<Long> cpeIds) {
		return createNamedQuery(GET_CPE_BY_IDS, CollectionProtocolEvent.class)
			.setParameterList("cpeIds", cpeIds)
			.list();
	}

	@Override
	public CollectionProtocolEvent getCpeByEventLabel(Long cpId, String label) {
		List<CollectionProtocolEvent> events = createNamedQuery(GET_CPE_BY_CP_AND_LABEL, CollectionProtocolEvent.class)
			.setParameter("cpId", cpId)
			.setParameter("label", label)
			.list();
		return events != null && !events.isEmpty() ? events.iterator().next() : null;
	}

	@Override
	public CollectionProtocolEvent getCpeByEventLabel(String title, String label) {
		List<CollectionProtocolEvent> events = createNamedQuery(GET_CPE_BY_CP_TITLE_AND_LABEL, CollectionProtocolEvent.class)
			.setParameter("title", title)
			.setParameter("label", label)
			.list();
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}

	@Override
	public CollectionProtocolEvent getCpeByShortTitleAndEventLabel(String shortTitle, String label) {
		List<CollectionProtocolEvent> events = getCpesByShortTitleAndEventLabels(shortTitle, Collections.singleton(label));
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}

	@Override
	public List<CollectionProtocolEvent> getCpesByShortTitleAndEventLabels(String shortTitle, Collection<String> labels) {
		return createNamedQuery(GET_CPES_BY_CP_SHORT_TITLE_AND_LABELS, CollectionProtocolEvent.class)
			.setParameter("shortTitle", shortTitle)
			.setParameterList("labels", labels)
			.list();
	}

	@Override
	public CollectionProtocolEvent getCpeByCode(String shortTitle, String code) {
		List<CollectionProtocolEvent> events = createNamedQuery(GET_CPE_BY_CODE, CollectionProtocolEvent.class)
			.setParameter("shortTitle", shortTitle)
			.setParameter("code", code)
			.list();
		return CollectionUtils.isEmpty(events) ? null : events.iterator().next();
	}

	@Override
	public void saveCpe(CollectionProtocolEvent cpe) {
		saveCpe(cpe, false);		
	}

	@Override
	public void saveCpe(CollectionProtocolEvent cpe, boolean flush) {
		getCurrentSession().saveOrUpdate(cpe);
		if (flush) {
			getCurrentSession().flush();
		}		
	}

	@Override
	public int getAllVisitsCount(Long cpeId) {
		Long count = createNamedQuery(GET_ALL_VISITS_COUNT_BY_CPE, Long.class)
			.setParameter("cpeId", cpeId)
			.uniqueResult();
		return count != null ? count.intValue() : 0;
	}

	@Override
	public SpecimenRequirement getSpecimenRequirement(Long requirementId) {
		return getCurrentSession().get(SpecimenRequirement.class, requirementId);
	}

	@Override	
	public SpecimenRequirement getSrByCode(String code) {
		List<SpecimenRequirement> srs = createNamedQuery(GET_SR_BY_CODE, SpecimenRequirement.class)
			.setParameter("code", code)
			.list();
		return CollectionUtils.isEmpty(srs) ? null : srs.iterator().next();
	}

	@Override
	public List<CpWorkflowConfig> getCpWorkflows(Collection<Long> cpIds) {
		Criteria<CpWorkflowConfig> query = createCriteria(CpWorkflowConfig.class, "wf");
		return query.add(query.in("wf.id", cpIds)).list();
	}

	@Override
	public void saveCpWorkflows(CpWorkflowConfig cfg) {
		getCurrentSession().saveOrUpdate(cfg);
	}

	@Override
	public CpWorkflowConfig getCpWorkflows(Long cpId) {		
		List<CpWorkflowConfig> cfgs = getCpWorkflows(Collections.singleton(cpId));
		return CollectionUtils.isEmpty(cfgs) ? null : cfgs.iterator().next();
	}

	@Override
	public CpConsentTier getConsentTier(Long consentId) {
		return createNamedQuery(GET_CONSENT_TIER, CpConsentTier.class)
			.setParameter("consentId", consentId)
			.uniqueResult();
	}
	
	@Override
	public CpConsentTier getConsentTierByStatement(Long cpId, String statement) {
		return createNamedQuery(GET_CONSENT_TIER_BY_STATEMENT, CpConsentTier.class)
			.setParameter("cpId", cpId)
			.setParameter("statement", statement)
			.uniqueResult();
	}
	
	@Override
	public int getConsentRespsCount(Long consentId) {
		Long count = createNamedQuery(GET_CONSENT_RESP_COUNT, Long.class)
			.setParameter("consentId", consentId)
			.uniqueResult();
		return count != null ? count.intValue() : 0;
	}

	@Override
	public boolean anyBarcodingEnabledCpExists() {
		List<Long> result = createNamedQuery(GET_BARCODING_ENABLED_CP_IDS, Long.class)
			.setMaxResults(1)
			.list();
		return CollectionUtils.isNotEmpty(result);
	}

	@Override
	public List<String> getDependentContainers(Long cpId, Collection<Long> siteIds) {
		return createNamedQuery(GET_DEPENDENT_CONTAINERS, String.class)
			.setParameter("cpId", cpId)
			.setParameterList("siteIds", siteIds)
			.setMaxResults(10)
			.list();
	}

	@Override
	public Class<CollectionProtocol> getType() {
		return CollectionProtocol.class;
	}

	private List<Object[]> getCpList(CpListCriteria crit) {
		Criteria<Object[]> query = getCpQuery(crit, Object[].class);
		return addProjections(query, crit)
			.addOrder(query.asc("shortTitle"))
			.list(crit.startAt(), crit.maxResults());
	}
	
	private <T> Criteria<T> getCpQuery(CpListCriteria crit, Class<T> klass) {
		Criteria<T> query = createCriteria(CollectionProtocol.class, klass, "cp")
			.join("cp.principalInvestigator", "pi");
		query.add(query.ne("cp.activityStatus", Status.ACTIVITY_STATUS_DISABLED.getStatus()));
		return addSearchConditions(query, crit);
	}

	private <T> Criteria<T> addSearchConditions(Criteria<T> query, CpListCriteria crit) {
		String searchString = crit.query();
		if (StringUtils.isBlank(searchString)) {
			searchString = crit.title();
		} 
		
		if (StringUtils.isNotBlank(searchString)) {
			Junction searchCond = query.disjunction()
				.add(query.ilike("cp.title", searchString))
				.add(query.ilike("cp.shortTitle", searchString));
			
			if (StringUtils.isNotBlank(crit.query())) {
				searchCond.add(query.ilike("cp.irbIdentifier", searchString));
			}	
			
			query.add(searchCond);
		}

		if (StringUtils.isBlank(crit.query()) && StringUtils.isNotBlank(crit.irbId())) {
			query.add(query.ilike("irbIdentifier", crit.irbId()));
		}

		if (crit.piId() != null) {
			query.add(query.eq("pi.id", crit.piId()));
		}
		
		String repositoryName = crit.repositoryName();
		if (StringUtils.isNotBlank(repositoryName)) {
			query.join("cp.sites", "cpSite")
				.join("cpSite.site", "site")
				.add(query.eq("site.name", repositoryName));
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

		if (crit.onlyParticipantConsentCps()) {
			query.add(query.or(query.eq("cp.visitLevelConsents", false)));
		}

		applyIdsFilter(query, "cp.id", crit.ids());
		addInClauses(query, "cp.shortTitle", crit.shortTitles());
		addSiteCpsCond(query, crit.siteCps());
		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(query.not(query.in("cp.id", crit.notInIds())));
		}

		return query;
	}
	
	private Criteria<Object[]> addProjections(Criteria<Object[]> query, CpListCriteria crit) {
		PropertyBuilder pb = query.propertyBuilder();
		List<Property> props = new ArrayList<>();
		props.add(pb.property("cp.id"));
		props.add(pb.property("cp.shortTitle"));
		props.add(pb.property("cp.title"));
		props.add(pb.property("cp.code"));
		props.add(pb.property("cp.startDate"));
		props.add(pb.property("cp.ppidFormat"));
		props.add(pb.property("cp.manualPpidEnabled"));
		props.add(pb.property("cp.specimenCentric"));
		props.add(pb.property("cp.catalogId"));

		if (crit.includePi()) {
			props.add(pb.property("pi.id"));
			props.add(pb.property("pi.firstName"));
			props.add(pb.property("pi.lastName"));
			props.add(pb.property("pi.loginName"));
		}

		return query.select(props);
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

	private <T> void addSiteCpsCond(Criteria<T> query, Collection<SiteCpPair> siteCps) {
		if (CollectionUtils.isEmpty(siteCps)) {
			return;
		}

		query.add(query.in("cp.id", BiospecimenDaoHelper.getInstance().getCpIdsFilter(query, siteCps)));
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

	private static final String GET_ALL_VISITS_COUNT_BY_CPE = CPE_FQN + ".getAllVisitsCount";
	
	private static final String SR_FQN = SpecimenRequirement.class.getName();
	
	private static final String GET_SR_BY_CODE = SR_FQN + ".getByCode";

	private static final String GET_BARCODING_ENABLED_CP_IDS = FQN + ".getBarcodingEnabledCpIds";
}
