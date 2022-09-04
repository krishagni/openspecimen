
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

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenReservedEvent;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStat;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderStatListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.DistributionProtocolDao;
import com.krishagni.catissueplus.core.administrative.repository.DpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.repository.AbstractCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.Junction;
import com.krishagni.catissueplus.core.common.repository.Property;
import com.krishagni.catissueplus.core.common.repository.Restriction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;
import com.krishagni.catissueplus.core.common.util.Utility;

public class DistributionProtocolDaoImpl extends AbstractDao<DistributionProtocol> implements DistributionProtocolDao {

	@Override
	public List<DistributionProtocol> getDistributionProtocols(DpListCriteria crit) {
		Criteria<DistributionProtocol> query = getDpListQuery(crit);
		return query.orderBy(query.asc("dp.title")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getDistributionProtocolsCount(DpListCriteria criteria) {
		return getDpListQuery(criteria).getCount("dp.id");
	}

	@Override
	public DistributionProtocol getByShortTitle(String shortTitle) {
		return createNamedQuery(GET_DPS_BY_SHORT_TITLE, DistributionProtocol.class)
			.setParameterList("shortTitles", Collections.singleton(shortTitle))
			.uniqueResult();
	}

	@Override
	public DistributionProtocol getDistributionProtocol(String title) {
		List<DistributionProtocol> dps = createNamedQuery(GET_DP_BY_TITLE, DistributionProtocol.class)
			.setParameter("title", title)
			.list();
		return CollectionUtils.isNotEmpty(dps) ? dps.iterator().next() : null;
	}

	@Override
	public List<DistributionProtocol> getDistributionProtocols(Collection<String> dpShortTitles) {
		return createNamedQuery(GET_DPS_BY_SHORT_TITLE, DistributionProtocol.class)
			.setParameterList("shortTitles", dpShortTitles)
			.list();
	}

 	@Override
	public List<DistributionProtocol> getExpiringDps(Date fromDate, Date toDate) {
		return createNamedQuery(GET_EXPIRING_DPS, DistributionProtocol.class)
			.setParameter("fromDate", fromDate)
			.setParameter("toDate", toDate)
			.list();
	}

	@Override
	public Map<Long, Integer> getSpecimensCountByDpIds(Collection<Long> dpIds) {
		List<Object[]> rows = createNamedQuery(GET_SPMN_COUNT_BY_DPS, Object[].class)
			.setParameterList("dpIds", dpIds)
			.list();
		return rows.stream().collect(
			Collectors.toMap(row -> (Long) row[0], row -> ((Long) row[1]).intValue()));
	}
	
	public Class<DistributionProtocol> getType() {
		return DistributionProtocol.class;
	}

	@Override
	public List<DistributionOrderStat> getOrderStats(DistributionOrderStatListCriteria listCrit) {
		Criteria<Object[]> query = createCriteria(DistributionOrder.class, Object[].class, "order")
			.join("order.distributionProtocol", "dp")
			.join("order.orderItems", "item");

		query.leftJoin("item.specimen", "specimen", () -> query.ne("specimen.activityStatus", "Disabled"))
			.join("specimen.specimenType", "specimenType")
			.add(query.eq("order.status", DistributionOrder.Status.EXECUTED));

		if (listCrit.dpId() != null) {
			query.add(query.eq("dp.id", listCrit.dpId()));
		} else if (CollectionUtils.isNotEmpty(listCrit.sites())) {
			query.join("dp.distributingSites", "distSites");
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

	@Override
	public List<String> getNonConsentingSpecimens(Long dpId, List<Long> specimenIds, int stmtsCount) {
		return createNamedQuery(GET_NON_CONSENTING_SPMNS, String.class)
			.setParameter("dpId", dpId)
			.setParameterList("specimenIds", specimenIds)
			.setParameter("respCount", stmtsCount)
			.list();
	}

	@Override
	public void saveReservedEvents(Collection<SpecimenReservedEvent> events) {
		events.forEach(event -> getCurrentSession().saveOrUpdate(event));
	}

	@Override
	public void unlinkCustomForm(Long formId) {
		createNamedQuery(UNLINK_CUSTOM_FORM).setParameter("formId", formId).executeUpdate();
	}

	@Override
	public Map<String, Integer> getDependents(Long dpId) {
		List<Object[]> rows = createNamedQuery(GET_DEPENDENT_ENTITY_COUNTS, Object[].class)
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

	private Criteria<DistributionProtocol> getDpListQuery(DpListCriteria crit) {
		Criteria<DistributionProtocol> query = createCriteria(DistributionProtocol.class, "dp");
		return query.add(query.in("dp.id", getDpIdsQuery(crit, query)));
	}

	private SubQuery<Long> getDpIdsQuery(DpListCriteria crit, Criteria<DistributionProtocol> query) {
		SubQuery<Long> subQuery = query.createSubQuery(DistributionProtocol.class, "dp")
			.distinct().select("dp.id");
		subQuery.add(subQuery.ne("dp.activityStatus", "Disabled"));
		return addSearchConditions(subQuery, crit);
	}

	private SubQuery<Long> addSearchConditions(SubQuery<Long> query, DpListCriteria crit) {
		String searchTerm = crit.query();
		
		if (StringUtils.isBlank(searchTerm)) {
			searchTerm = crit.title();
		}
		
		if (StringUtils.isNotBlank(searchTerm)) {
			Junction searchCond = query.disjunction()
					.add(query.ilike("dp.title", searchTerm))
					.add(query.ilike("dp.shortTitle", searchTerm));
			
			if (StringUtils.isNotBlank(crit.query())) {
				searchCond.add(query.ilike("dp.irbId", searchTerm));
			}
			
			query.add(searchCond);
		}

		if (StringUtils.isBlank(crit.query()) && StringUtils.isNotBlank(crit.irbIdLike())) {
			query.add(query.ilike("dp.irbId", crit.irbIdLike()));
		}

		applyIdsFilter(query, "dp.id", crit.ids());
		addPiCondition(query, crit);
		addIrbIdCondition(query, crit);
		addInstCondition(query, crit);
		addRecvSiteCondition(query, crit);
		addDistSitesCondition(query, crit);
		addExpiredDpsCondition(query, crit);
		addActivityStatusCondition(query, crit);

		if (CollectionUtils.isNotEmpty(crit.notInIds())) {
			query.add(query.notIn("dp.id", crit.notInIds()));
		}

		return query;
	}
	
	private void addPiCondition(SubQuery<?> query, DpListCriteria crit) {
		Long piId = crit.piId();
		if (piId == null) {
			return;
		}

		query.join("dp.principalInvestigator", "pi")
			.add(query.eq("pi.id", piId));
	}

	private void addIrbIdCondition(SubQuery<?> query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.irbId())) {
			return;
		}

		query.add(query.eq("dp.irbId", crit.irbId().trim()));
	}
	
	private void addInstCondition(SubQuery<?> query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingInstitute())) {
			return;
		}

		query.join("dp.institute", "institute")
			.add(query.eq("institute.name", crit.receivingInstitute().trim()));
	}

	private void addRecvSiteCondition(SubQuery<?> query, DpListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingSite())) {
			return;
		}

		query.join("dp.defReceivingSite", "recvSite")
			.add(query.eq("recvSite.name", crit.receivingSite().trim()));
	}
	
	private void addDistSitesCondition(SubQuery<?> query, DpListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.sites())) {
			return;
		}
		
		query.join("dp.distributingSites", "distSites");
		addSitesCondition(query, crit.sites());
	}

	private void addExpiredDpsCondition(SubQuery<?> query, DpListCriteria crit) {
		if (!crit.excludeExpiredDps()) {
			return;
		}

		Date today = Utility.chopTime(Calendar.getInstance().getTime());
		query.add(query.or(
			query.isNull("dp.endDate"),
			query.ge("dp.endDate", today)));
	}
	
	private void addActivityStatusCondition(SubQuery<?> query, DpListCriteria crit) {
		String activityStatus = crit.activityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			return;
		}
		
		query.add(query.eq("dp.activityStatus", activityStatus));
	}
	
	private void addOrderStatProjections(Criteria<?> query, DistributionOrderStatListCriteria crit) {
		List<Property> columns = new ArrayList<>();
		columns.add(query.column("order.id"));
		columns.add(query.column("order.name"));
		columns.add(query.column("dp.id"));
		columns.add(query.column("dp.shortTitle"));
		columns.add(query.column("order.executionDate"));

		Map<String, String> props = getProps();
		for (String attr : crit.groupByAttrs()) {
			String prop = props.get(attr);
			query.leftJoin(prop, attr + "pv");
			columns.add(query.column(attr + "pv.value"));
		}

		columns.add(query.count("specimenType.value"));
		query.select(columns).groupBy(columns.subList(0, columns.size() - 1));
	}
	
	private Map<String, String> getProps() {
		Map<String, String> props = new HashMap<>();
		props.put("specimenType", "specimen.specimenType");
		props.put("anatomicSite", "specimen.tissueSite");
		props.put("pathologyStatus", "specimen.pathologicalStatus");

		return props;
	}
	
	private DistributionOrderStat getDOStats(Object[] row, DistributionOrderStatListCriteria crit) {
		int index = -1;
		DistributionOrderStat stat = new DistributionOrderStat();
		stat.setId((Long)row[++index]);
		stat.setName((String)row[++index]);
		stat.setDpId((Long)row[++index]);
		stat.setDpShortTitle((String)row[++index]);
		stat.setExecutionDate((Date)row[++index]);
		for (String attr : crit.groupByAttrs()) {
			stat.getGroupByAttrVals().put(attr, row[++index]);
		}

		stat.setDistributedSpecimenCount((Long)row[++index]);
		return stat;
	}
	
	private void addSitesCondition(AbstractCriteria<?, ?> query, Set<SiteCpPair> sites) {
		Set<Long> siteIds      = new HashSet<>();
		Set<Long> instituteIds = new HashSet<>();
		for (SiteCpPair site : sites) {
			if (site.getSiteId() != null) {
				siteIds.add(site.getSiteId());
			} else {
				instituteIds.add(site.getInstituteId());
			}
		}

		query.leftJoin("distSites.site", "distSite")
			.join("distSites.institute", "distInst")
			.join("distInst.sites", "instSite");

		Disjunction instituteConds = query.disjunction();
		if (!siteIds.isEmpty()) {
			instituteConds.add(query.in("instSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			instituteConds.add(query.in("distInst.id", instituteIds));
		}

		Disjunction siteConds = query.disjunction();
		if (!siteIds.isEmpty()) {
			siteConds.add(query.in("distSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			siteConds.add(isSiteOfInstitute(query, "distSite.id", instituteIds));
		}

		query.add(query.or(
			query.and(query.isNull("distSites.site"), instituteConds.getRestriction()),
			query.and(query.isNotNull("distSites.site"), siteConds.getRestriction())
		));
	}

	private Restriction isSiteOfInstitute(AbstractCriteria<?, ?> mainQuery, String property, Collection<Long> instituteIds) {
		SubQuery<Long> sq = mainQuery.createSubQuery(Site.class, "site")
			.join("site.institute", "institute");
		sq.add(sq.in("institute.id", instituteIds)).select("site.id");
		return mainQuery.in(property, sq);
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
