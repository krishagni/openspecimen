package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.common.domain.SearchEntityKeyword;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Query;
import com.krishagni.catissueplus.core.common.repository.SearchEntityKeywordDao;

public class SearchEntityKeywordDaoImpl extends AbstractDao<SearchEntityKeyword> implements SearchEntityKeywordDao {

	@Override
	public Class<SearchEntityKeyword> getType() {
		return SearchEntityKeyword.class;
	}

	@Override
	public List<SearchEntityKeyword> getKeywords(String entity, Long entityId, String key) {
		return createNamedQuery(GET_KEYWORDS, SearchEntityKeyword.class)
			.setParameter("entity", entity)
			.setParameter("entityId", entityId)
			.setParameter("key", key)
			.list();
	}

	@Override
	public List<SearchEntityKeyword> getMatches(String entity, String searchTerm, int maxResults) {
		String sql = String.format(GET_MATCHES_SQL, entity != null ? " r.short_name = :entity and " : "");

		Query<Object[]> query = createNativeQuery(sql, Object[].class);
		if (entity != null) {
			query.setParameter("entity", entity);
		}

		List<Object[]> rows = query.setParameter("value", searchTerm + "%")
			.setMaxResults(maxResults <= 0 ? 100 : maxResults)
			.list();

		List<SearchEntityKeyword> result = new ArrayList<>();
		for (Object[] row : rows) {
			int idx = -1;

			SearchEntityKeyword keyword = new SearchEntityKeyword();
			keyword.setId(((Number) row[++idx]).longValue());
			keyword.setEntity((String) row[++idx]);
			keyword.setEntityId(((Number) row[++idx]).longValue());
			keyword.setKey((String) row[++idx]);
			keyword.setValue((String) row[++idx]);
			keyword.setStatus(((Number) row[++idx]).intValue());
			result.add(keyword);
		}

		return result;
	}

	@Override
	public List<String> getMatchingEntities(String entity, String searchTerm) {
		String sql = String.format(GET_MATCHING_ENTITIES_SQL, entity != null ? " and r.short_name = :entity" : "");

		Query<String> query = createNativeQuery(sql, String.class)
			.addStringScalar("entity");
		if (entity != null) {
			query.setParameter("entity", entity);
		}

		return query.setParameter("value", searchTerm + "%").list();
	}

	@Override
	public boolean isValidEntityShortName(String shortName) {
		List<Object[]> rows = createNamedQuery(GET_ENTITY_RANK_BY_SHORT_NAME, Object[].class)
			.setParameter("shortName", shortName)
			.list();
		return CollectionUtils.isNotEmpty(rows);
	}

	private static final String FQN = SearchEntityKeyword.class.getName();

	private static final String GET_KEYWORDS = FQN + ".getKeywords";

	private static final String GET_ENTITY_RANK_BY_SHORT_NAME = FQN + ".getEntityRankByShortName";

	private static final String GET_MATCHES_SQL =
		"select " +
		"  k.identifier, k.entity, k.entity_id, k.name, k.value, k.status " +
		"from " +
		"  os_search_entity_keywords k " +
		"  inner join os_search_entity_ranks r on r.entity = k.entity " +
		"where " +
		"  %s" +
		"  k.value like :value and " +
		"  k.status = 1 " +
		"order by " +
		"  r.sort_order";

	private static final String GET_MATCHING_ENTITIES_SQL =
		"select " +
		"  r.entity as entity " +
		"from " +
		"  os_search_entity_ranks r " +
		"where " +
		"  exists ( " +
		"    select " +
		"      k.entity " +
		"    from " +
		"      os_search_entity_keywords k " +
		"    where " +
		"      k.value like :value and " +
		"      k.status = 1 and " +
		"      k.entity = r.entity " +
		"  ) " +
		"  %s " +
		"order by " +
		"  r.sort_order";
}
