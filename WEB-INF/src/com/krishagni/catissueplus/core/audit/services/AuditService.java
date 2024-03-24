package com.krishagni.catissueplus.core.audit.services;

import java.io.File;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.events.AuditDetail;
import com.krishagni.catissueplus.core.audit.events.AuditEntityQueryCriteria;
import com.krishagni.catissueplus.core.audit.events.RevisionDetail;
import com.krishagni.catissueplus.core.audit.repository.RevisionsListCriteria;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface AuditService {
	ResponseEvent<List<AuditDetail>> getEntityAuditDetail(RequestEvent<List<AuditEntityQueryCriteria>> req);

	ResponseEvent<List<RevisionDetail>> getEntityRevisions(RequestEvent<List<AuditEntityQueryCriteria>> req);

	ResponseEvent<ExportedFileDetail> exportRevisions(RequestEvent<RevisionsListCriteria> req);

	ResponseEvent<File> getExportedRevisionsFile(RequestEvent<String> req);

	ResponseEvent<List<Map<String, String>>> getRevisionEntities();

	// Internal APIs

	void insertApiCallLog(UserApiCallLog userAuditLog);
	
	long getTimeSinceLastApiCall(Long userId, String token);

	void registerExporter(AuditLogExporter exporter);
}
