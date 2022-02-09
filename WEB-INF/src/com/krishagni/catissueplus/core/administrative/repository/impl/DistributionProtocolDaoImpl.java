
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.Junction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenReservedEvent;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolSummary;
import com.krishagni.catissueplus.core.administrative.repository.DistributionProtocolDao;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.Utility;

public class DistributionProtocolDaoImpl extends AbstractDao<DistributionProtocol> implements DistributionProtocolDao {

	@SuppressWarnings("unchecked")
	@Override
	public List<DistributionProtocol> getDistributionProtocols(DpListCriteria crit) {
		return getDpListQuery(crit).addOrder(Order.asc("dp.title"))
			.setFirstResult(crit.startAt()).setMaxResults(crit.maxResults())
			.list();
	}

	@Override
	public Long getDistributionProtocolsCount(DpListCriteria criteria) {
		Number count = (Number) getDpIdsQuery(criteria).getExecutableCriteria(getCurrentSession())
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@SuppressWarnings("unchecked")
	@Override
	public DistributionProtocol getByShortTitle(String shortTitle) {
		return (DistributionProtocol) getCurrentSession().getNamedQuery(GET_DPS_BY_SHORT_TITLE)
			.setParameterList("shortTitles", Collections.singleton(shortTitle))
			.uniqueResult();
	}

	@SuppressWarnings("unchecked")
	@Override
	public DistributionProtocol getDistributionProtocol(String title) {
		List<DistributionProtocol> dps = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_DP_BY_TITLE)
				.setString("title", title)
				.list();
		return CollectionUtils.isNotEmpty(dps) ? dps.iterator().next() : null;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DistributionProtocol> getDistributionProtocols(Collection<String> dpShortTitles) {
		return getCurrentSession().getNamedQuery(GET_DPS_BY_SHORT_TITLE)
			.setParameterList("shortTitles", dpShortTitles)
			.list();
	}

	@SuppressWarnings("unchecked")
 	@Override
	public List<DistributionProtocol> getExpiringDps(Date fromDate, Date toDate) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_EXPIRING_DPS)
				.setDate("fromDate", fromDate)
				.setDate("toDate", toDate)
				.list();
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public Map<Long, Integer> getSpecimensCountByDpIds(Collection<Long> dpIds) {
		List<Object[]> rows = getSessionFactory().getCurrentSession()
				.getNamedQuery(GET_SPMN_COUNT_BY_DPS)
				.setParameterList("dpIds", dpIds)
				.list();

		return rows.stream().collect(
			Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
	}
	
	public Class<DistributionProtocol> getType() {
		return DistributionProtocol.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<DistributionOrderStat> getOrderStats(DistributionOrderStatListCriteria listCrit) {
		Criteria query = getCurrentSession().createCriteria(DistributionOrder.class)
			.createAlias("distributionProtocol", "dp")
			.createAlias("orderItems", "item")
			.createAlias("item.specimen", "specimen",
				JoinType.LEFT_OUTER_JOIN,
				Restrictions.ne("specimen.activityStatus", "Disabled")
			)
			.add(Restrictions.eq("status", DistributionOrder.Status.EXECUTED));

		if (listCrit.dpId() != null) {
			query.add(Restrictions.eq("dp.id", listCrit.dpId()));
		} else if (CollectionUtils.isNotEmpty(listCrit.sites())) {
			query.createAlias("dp.distributingSites", "distSites");
			addSitesCondition(query, listCrit.sites());
		}
		
		addOrderStatProjections(query, listCrit);
		
		List<Object []> rows = query.list();
		List<DistributionOrderStat> result = new ArrayList<>();
		for (Object[] row : rows) {
			DistributionOrderStat detail = getDOStats(row, listCrit);
			result.add(detail);
		}
		
		return result;
	}

	@Override
	public Map<String, Object> getDpIds(String key, Object value) {
		return getObjectIds("dpId", key, value);
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<String> getNonConsentingSpecimens(Long dpId, List<Long> specimenIds, int stmtsCount) {
		return getCurrentSession().getNamedQuery(GET_NON_CONSENTING_SPMNS)
			.setLong("dpId", dpId)
			.setParameterList("specimenIds", specimenIds)
			.setInteger("respCount", stmtsCount)
			.list();
	}

	@Override
	public void saveReservedEvents(Collection<SpecimenReservedEvent> events) {
		events.forEach(event -> getCurrentSession().saveOrUpdate(event));
	}

	@Override
	public void unlinkCustomForm(Long formId) {
		getCurrentSession().getNamedQuery(UNLINK_CUSTOM_FORM).setParameter("formId", formId).executeUpdate();
	}

	@Override
	public Map<String, Integer> getDependents(Long dpId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_DEPENDENT_ENTITY_COUNTS)
			.setParameter("dpId", dpId)
			.list();

		if (rows.isEmpty()) {
			return Collections.emptyMap();
		}

		int idx = 0;
		Object[] row = rows.get(0);

		Map<String, Integer> dependents = new HashMap<>();
		dependents.put(Specimen.getEntityName(), (Integer) row[++idx]);
		dependents.put(StorageContainer.getEntityName(), (Integer) row[++idx]);
		dependents.put(DistributionOrder.getEntityName(), (Integer) row[++idx]);
		return dependents;
	}

	private Criteria getDpListQuery(DpListCriteria crit) {
		return getCurrentSession().createCriteria(DistributionProtocol.class, "dp")
				.add(Subqueries.propertyIn("dp.id", getDpIdsQuery(crit)));
	}

	private DetachedCriteria getDpIdsQuery(DpListCriteria crit) {
		DetachedCriteria detachedCriteria = DetachedCriteria.forClass(DistributionProtocol.class)
			.setProjection(Projections.distinct(Projections.property("id")));
		Criteria query = detachedCriteria.getExecutableCriteria(getCurrentSession());
		query.add(Restrictions.ne("activityStatus", "Disabled"));
		addSearchConditions(query, crit);
		return detachedCriteria;
	}

	private Criteria addSearchConditions(Criteria query, DpListCriteria crit) {
		String searchTerm = crit.query();
		
		if (StringUtils.isBlank(searchTerm)) {
			searchTerm = crit.title();
		}
		
		if (StringUtils.isNotBlank(searchTerm)) {
			Junction searchCond = Restrictions.disjunction()
					.add(Restrictions.ilike("title", searchTerm, MatchMode.ANYWHERE))
					.add(Restrictions.ilike("shortTitle", searchTerm, MatchMode.ANYWHERE));
			
			if (StringUtils.isNotBlank(crit.query())) {
				searchCond.add(Restrictions.ilike("irbId", searchTerm, MatchMode.ANYWHERE));
			}
			
			query.add(searchCond);
		}

		if (StringUtils.isBlank(crit.query()) && StringUtils.isNotBlank(crit.irbIdLike())) {
			query.add(Restrictions.ilike("irbId", crit.irbIdLike(), MatchMode.ANYWHERE));
		}

		applyIdsFilter(query, "id", crit.ids());
		addPiCondition(query, crit);
		addIrbIdCondition(query, crit);
		addInstCondition(query, crit);
		addRecvSiteCondition(query, crit);
		addDistSitesCondition(query, crit);
		addExpiredDpsCondition(query, crit);
		addActivityStatusCondition(query, crit);

		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(Restrictions.not(Restrictions.in("id", crit.notInIds())));
		}

		return query;
	}
	
	private void addPiCondition(Criteria query, DpListCriteria crit) {
		Long piId = crit.piId();
		if (piId == null) {
			return;
		}
		
		query.add(Restrictions.eq("principalInvestigator.id", piId));
	}

	private void addIrbIdCondition(Criteria query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.irbId())) {
			return;
		}

		query.add(Restrictions.eq("irbId", crit.irbId().trim()));
	}
	
	private void addInstCondition(Criteria query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingInstitute())) {
			return;
		}

		query.createAlias("institute", "institute")
			.add(Restrictions.eq("institute.name", crit.receivingInstitute().trim()));
	}

	private void addRecvSiteCondition(Criteria query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingSite())) {
			return;
		}

		query.createAlias("defReceivingSite", "recvSite")
			.add(Restrictions.eq("recvSite.name", crit.receivingSite().trim()));
	}
	
	private void addDistSitesCondition(Criteria query, DpListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.sites())) {
			return;
		}
		
		query.createAlias("distributingSites", "distSites");
		addSitesCondition(query, crit.sites());
	}

	private void addExpiredDpsCondition(Criteria query, DpListCriteria crit) {
		if (!crit.excludeExpiredDps()) {
			return;
		}

		Date today = Utility.chopTime(Calendar.getInstance().getTime());
		query.add(Restrictions.or(
			Restrictions.isNull("endDate"),
			Restrictions.ge("endDate", today)));
	}
	
	private void addActivityStatusCondition(Criteria query, DpListCriteria crit) {
		String activityStatus = crit.activityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			return;
		}
		
		query.add(Restrictions.eq("activityStatus", activityStatus));
	}
	
	private void addOrderStatProjections(Criteria query, DistributionOrderStatListCriteria crit) {
		ProjectionList projs = Projections.projectionList();
		
		projs.add(Projections.groupProperty("id"));
		projs.add(Projections.groupProperty("name"));
		projs.add(Projections.groupProperty("distributionProtocol"));
		projs.add(Projections.groupProperty("executionDate"));
		projs.add(Projections.count("specimen.specimenType"));
		
		Map<String, String> props = getProps();
		
		for (String attr : crit.groupByAttrs()) {
			String prop = props.get(attr);
			query.createAlias(prop, attr + "pv", JoinType.LEFT_OUTER_JOIN);
			projs.add(Projections.groupProperty(attr + "pv.value"));
		}
		
		query.setProjection(projs);
	}
	
	private Map<String, String> getProps() {
		Map<String, String> props = new HashMap<>();
		props.put("specimenType", "specimen.specimenType");
		props.put("anatomicSite", "specimen.tissueSite");
		props.put("pathologyStatus", "specimen.pathologicalStatus");

		return props;
	}
	
	private DistributionOrderStat getDOStats(Object[] row, DistributionOrderStatListCriteria crit) {
		DistributionOrderStat stat = new DistributionOrderStat();
		int index = 0;
		
		stat.setId((Long)row[index++]);
		stat.setName((String)row[index++]);
		stat.setDistributionProtocol(DistributionProtocolSummary.from((DistributionProtocol)row[index++]));
		stat.setExecutionDate((Date)row[index++]);
		stat.setDistributedSpecimenCount((Long)row[index++]);
		
		for (String attr : crit.groupByAttrs()) {
			stat.getGroupByAttrVals().put(attr, row[index++]);
		}
		
		return stat;
	}
	
	private void addSitesCondition(Criteria query, Set<SiteCpPair> sites) {
		Set<Long> siteIds      = new HashSet<>();
		Set<Long> instituteIds = new HashSet<>();
		for (SiteCpPair site : sites) {
			if (site.getSiteId() != null) {
				siteIds.add(site.getSiteId());
			} else {
				instituteIds.add(site.getInstituteId());
			}
		}

		query.createAlias("distSites.site", "distSite", JoinType.LEFT_OUTER_JOIN)
			.createAlias("distSites.institute", "distInst")
			.createAlias("distInst.sites", "instSite");

		Disjunction instituteConds = Restrictions.disjunction();
		if (!siteIds.isEmpty()) {
			instituteConds.add(Restrictions.in("instSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			instituteConds.add(Restrictions.in("distInst.id", instituteIds));
		}

		Disjunction siteConds = Restrictions.disjunction();
		if (!siteIds.isEmpty()) {
			siteConds.add(Restrictions.in("distSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			siteConds.add(isSiteOfInstitute("distSite.id", instituteIds));
		}

		query.add(Restrictions.or(
			Restrictions.and(Restrictions.isNull("distSites.site"), instituteConds),
			Restrictions.and(Restrictions.isNotNull("distSites.site"), siteConds)
		));
	}

	private Criterion isSiteOfInstitute(String property, Collection<Long> instituteIds) {
		DetachedCriteria subQuery = DetachedCriteria.forClass(Site.class)
			.add(Restrictions.in("institute.id", instituteIds))
			.setProjection(Projections.property("id"));
		return Subqueries.propertyIn(property, subQuery);
	}

	private static final String FQN = DistributionProtocol.class.getName();

	private static final String GET_DP_BY_TITLE = FQN + ".getDistributionProtocolByTitle";

	private static final String GET_DPS_BY_SHORT_TITLE = FQN + ".getDistributionProtocolsByShortTitle";
	
	private static final String GET_EXPIRING_DPS = FQN + ".getExpiringDps";
	
	private static final String GET_SPMN_COUNT_BY_DPS = FQN + ".getSpmnCountByDps";

	private static final String GET_NON_CONSENTING_SPMNS = FQN + ".getNonConsentingSpecimens";

	private static final String UNLINK_CUSTOM_FORM = FQN + ".unlinkCustomForm";

	private static final String GET_DEPENDENT_ENTITY_COUNTS = FQN + ".getDependents";
}
