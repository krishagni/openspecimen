package com.krishagni.catissueplus.core.common.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.SessionFactory;
import org.hibernate.type.StandardBasicTypes;

import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.events.SearchResult;
import com.krishagni.catissueplus.core.common.service.SearchResultProcessor;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public abstract class AbstractSearchResultProcessor implements SearchResultProcessor {
	@Override
	@SuppressWarnings("unchecked")
	public List<SearchResult> search(String searchTerm, long lastId, int maxResults) {
		String query = getQuery();
		if (StringUtils.isBlank(query)) {
			return Collections.emptyList();
		}

		if (!isPredictiveSearchEnabled()) {
			int orderIdx = query.lastIndexOf("order by");
			if (orderIdx >= 0) {
				query = query.substring(0, orderIdx);
			}
		}

		List<Object[]> rows = (List<Object[]>) getSessionFactory().getCurrentSession().createNativeQuery(query)
			.addScalar("identifier", StandardBasicTypes.LONG)
			.addScalar("entity", StandardBasicTypes.STRING)
			.addScalar("entity_id", StandardBasicTypes.LONG)
			.addScalar("name", StandardBasicTypes.STRING)
			.addScalar("value", StandardBasicTypes.STRING)
			.setParameter(1, searchTerm + "%")
			.setParameter(2, lastId)
			.setMaxResults(maxResults)
			.list();

		List<SearchResult> results = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;
			SearchResult result = new SearchResult();
			result.setId((Long) row[++idx]);
			result.setEntity((String) row[++idx]);
			result.setEntityId((Long) row[++idx]);
			result.setKey((String) row[++idx]);
			result.setValue((String) row[++idx]);
			results.add(result);
		}

		return results;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Map<Long, Map<String, Object>> getEntityProps(Collection<Long> entityIds) {
		String propsQuery = getEntityPropsQuery();
		if (StringUtils.isBlank(propsQuery) || CollectionUtils.isEmpty(entityIds)) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = (List<Object[]>) getSessionFactory().getCurrentSession().createNativeQuery(propsQuery)
			.addScalar("entityId", StandardBasicTypes.LONG)
			.addScalar("name", StandardBasicTypes.STRING)
			.addScalar("value", StandardBasicTypes.STRING)
			.setParameterList("entityIds", entityIds)
			.list();

		Map<Long, Map<String, Object>> result = new HashMap<>();
		for (Object[] row : rows) {
			int idx = -1;
			Long entityId = (Long) row[++idx];
			Map<String, Object> entityProps = result.computeIfAbsent(entityId, (k) -> new HashMap<>());
			entityProps.put((String) row[++idx], row[++idx]);
		}

		return result;
	}

	protected String getQuery() {
		return null;
	}

	protected String getEntityPropsQuery() {
		return null;
	}

	protected boolean isPredictiveSearchEnabled() {
		return ConfigUtil.getInstance().getBoolSetting("common", "predictive_search_results", true);
	}

	private SessionFactory getSessionFactory() {
		return OpenSpecimenAppCtxProvider.getBean("sessionFactory");
	}
}