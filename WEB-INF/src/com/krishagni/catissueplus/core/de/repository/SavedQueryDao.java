package com.krishagni.catissueplus.core.de.repository;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ListSavedQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;

public interface SavedQueryDao extends Dao<SavedQuery>{
	Long getQueriesCount(ListSavedQueriesCriteria crit);

	List<SavedQuerySummary> getQueries(ListSavedQueriesCriteria crit);

	List<SavedQuerySummary> getQueries(Long userId, int startAt, int maxRecords, String ... searchString);
		
	SavedQuery getQuery(Long queryId);
	
	List<SavedQuery> getQueriesByIds(List<Long> queries);
	
	Long getQueriesCountByFolderId(Long folderId, String searchString);
	
	List<SavedQuerySummary> getQueriesByFolderId(Long folderId, int startAt, int maxRecords, String searchString);
	
	boolean isQuerySharedWithUser(Long queryId, Long userId, boolean forEdits);
	
	Map<String, Object> getQueryChangeLogDetails(String file);
	
	void insertQueryChangeLog(String file, String digest, String status, Long queryId);
}
