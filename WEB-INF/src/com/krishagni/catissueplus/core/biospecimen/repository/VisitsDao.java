
package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.events.VisitSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface VisitsDao extends Dao<Visit> {
	void loadCreatedVisitStats(Map<Long, ? extends VisitSummary> visits);

	void loadAnticipatedVisitStats(Map<Long, ? extends VisitSummary> visits);

	Long getVisitsCount(VisitsListCriteria crit);

	List<Visit> getVisitsList(VisitsListCriteria crit);
	
	Visit getByName(String name);
	
	List<Visit> getByName(Collection<String> names);

	List<Visit> getByIds(Collection<Long> ids);

	List<Visit> getBySpr(String sprNumber);

	Map<String, Object> getCprVisitIds(String key, Object value);

	Visit getLatestVisit(Long cprId);

	List<Visit> getByEmpiOrMrn(Long cpId, String empiOrMrn);

	List<Visit> getBySpr(Long cpId, String sprNumber);

	List<Visit> getByEvent(Long cprId, String eventLabel);

	Long getCustomFieldRecordId(Long visitId, Long formId, Long formCtxtId);

	Map<Long, Long> getCustomFieldRecordIds(Collection<Long> visitIds, Long formId, Long formCtxtId);

	int insertCustomFieldRecordId(Long visitId, Long formId, Long formCtxtId, Long recordId);
}
