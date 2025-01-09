
package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.repository.SiteDao;
import com.krishagni.catissueplus.core.administrative.repository.SiteListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.util.Status;

public class SiteDaoImpl extends AbstractDao<Site> implements SiteDao {
	
	@Override
	public Class<?> getType() {
		return Site.class;
	}

	@Override
	public List<Site> getSites(SiteListCriteria listCrit) {
		Criteria<Site> query = getSitesListQuery(listCrit);
		return query.orderBy(query.asc("site.name")).list(listCrit.startAt(), listCrit.maxResults());
	}

	@Override
	public Long getSitesCount(SiteListCriteria crit) {
		return getSitesListQuery(crit).getCount("site.id");
	}

	@Override
	public List<Site> getSitesByNames(Collection<String> siteNames) {
		return createNamedQuery(GET_SITES_BY_NAMES, Site.class)
			.setParameterList("siteNames", siteNames)
			.list();
	}
	
	@Override
	public Site getSiteByName(String siteName) {
		List<Site> result = getSitesByNames(Collections.singletonList(siteName));
		return result.isEmpty() ? null : result.get(0);
	}
	
	@Override
	public Site getSiteByCode(String siteCode) {
		return createNamedQuery(GET_SITE_BY_CODE, Site.class)
			.setParameter("siteCode", siteCode)
			.uniqueResult();
	}
	
	@Override
	public Map<Long, Integer> getCpCountBySite(Collection<Long> siteIds) {		
		List<Object[]> rows = createNamedQuery(GET_CP_COUNT_BY_SITES, Object[].class)
			.setParameterList("siteIds", siteIds)
			.list();
		
		Map<Long, Integer> countMap = new HashMap<>();
		for (Object[] row : rows) {
			Long siteId = (Long)row[0];
			Integer count = ((Long)row[1]).intValue();
			countMap.put(siteId, count);
		}
		
		return countMap;
	}

	@Override
	public Map<String, Object> getSiteIds(String key, Object value) {
		return getObjectIds("siteId", key, value);
	}

	@Override
	public boolean isAffiliatedToUserInstitute(Long siteId, Long userId) {
		Integer count = createNamedQuery(IS_AFFILIATED_TO_USER_INST, Integer.class)
			.setParameter("siteId", siteId)
			.setParameter("userId", userId)
			.uniqueResult();
		return count != null && count > 0;
	}

	private Criteria<Site> getSitesListQuery(SiteListCriteria crit) {
		Criteria<Site> query = createCriteria(Site.class, "site")
			.leftJoin("site.type", "type");
		query.add(query.eq("site.activityStatus", Status.ACTIVITY_STATUS_ACTIVE.getStatus()));
		return addSearchConditions(query, crit);
	}

	private Criteria<Site> addSearchConditions(Criteria<Site> query, SiteListCriteria listCrit) {
		if (CollectionUtils.isNotEmpty(listCrit.ids())) {
			query.add(query.in("site.id", listCrit.ids()));
		}

		if (StringUtils.isNotBlank(listCrit.query())) {
			query.add(query.ilike("site.name", listCrit.query()));
		}

		if (CollectionUtils.isNotEmpty(listCrit.names())) {
			query.add(query.in("site.name", listCrit.names()));
		}

		if (StringUtils.isNotBlank(listCrit.institute())) {
			query.join("site.institute", "i")
				.add(query.eq("i.name", listCrit.institute()));
		}

		if (CollectionUtils.isNotEmpty(listCrit.includeTypes())) {
			query.add(query.in("type.value", listCrit.includeTypes()));
		}

		if (CollectionUtils.isNotEmpty(listCrit.excludeTypes())) {
			query.add(query.notIn("type.value", listCrit.excludeTypes()));
		}

		return query;
	}
			
	private static final String FQN = Site.class.getName();

	private static final String GET_SITES_BY_NAMES = FQN + ".getSitesByNames";
	
	private static final String GET_SITE_BY_CODE = FQN + ".getSiteByCode";
	
	private static final String GET_CP_COUNT_BY_SITES = FQN + ".getCpCountBySites";

	private static final String IS_AFFILIATED_TO_USER_INST = FQN + ".ensureSiteIsAffiliatedtoUserInstitute";
}
