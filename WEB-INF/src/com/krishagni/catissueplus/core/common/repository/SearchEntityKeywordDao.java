package com.krishagni.catissueplus.core.common.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.SearchEntityKeyword;

public interface SearchEntityKeywordDao extends Dao<SearchEntityKeyword> {
	List<SearchEntityKeyword> getKeywords(String entity, Long entityId, String key);

	List<SearchEntityKeyword> getMatches(String entity, String searchTerm, int maxResults);

	List<String> getMatchingEntities(String entity, String searchTerm);

	boolean isValidEntityShortName(String shortName);
}
