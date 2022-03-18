package com.krishagni.catissueplus.core.de.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;

public interface QueryAuditLogDao extends Dao<QueryAuditLog>{
	Long getLogsCount(QueryAuditLogsListCriteria crit);

	List<QueryAuditLog> getLogs(QueryAuditLogsListCriteria crit);
}
