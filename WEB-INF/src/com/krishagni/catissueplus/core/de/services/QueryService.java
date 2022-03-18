package com.krishagni.catissueplus.core.de.services;

import java.io.File;
import java.io.OutputStream;
import java.util.List;
import java.util.function.BiConsumer;

import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.ExportedFileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.ExecuteSavedQueryOp;
import com.krishagni.catissueplus.core.de.events.FacetDetail;
import com.krishagni.catissueplus.core.de.events.GetFacetValuesOp;
import com.krishagni.catissueplus.core.de.events.ListFolderQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;
import com.krishagni.catissueplus.core.de.events.ListSavedQueriesCriteria;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDetail;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogSummary;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.events.QueryExecResult;
import com.krishagni.catissueplus.core.de.events.QueryFolderDetails;
import com.krishagni.catissueplus.core.de.events.QueryFolderSummary;
import com.krishagni.catissueplus.core.de.events.SavedQueriesList;
import com.krishagni.catissueplus.core.de.events.SavedQueryDetail;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;
import com.krishagni.catissueplus.core.de.events.ShareQueryFolderOp;
import com.krishagni.catissueplus.core.de.events.UpdateFolderQueriesOp;

import edu.common.dynamicextensions.query.QueryResultData;

public interface QueryService {	
	ResponseEvent<SavedQueriesList> getSavedQueries(RequestEvent<ListSavedQueriesCriteria> req);

	ResponseEvent<Long> getSavedQueriesCount(RequestEvent<ListSavedQueriesCriteria> req);

	ResponseEvent<SavedQueryDetail> getSavedQuery(RequestEvent<Long> req);

	ResponseEvent<SavedQueryDetail> saveQuery(RequestEvent<SavedQueryDetail> req);

	ResponseEvent<SavedQueryDetail> updateQuery(RequestEvent<SavedQueryDetail> req);

	ResponseEvent<Long> deleteQuery(RequestEvent<Long> req);

	//
	// query execution APIs
	//

	ResponseEvent<QueryExecResult> executeQuery(RequestEvent<ExecuteQueryEventOp> req);

	ResponseEvent<QueryExecResult> executeSavedQuery(RequestEvent<ExecuteSavedQueryOp> req);

	ResponseEvent<QueryDataExportResult> exportQueryData(RequestEvent<ExecuteQueryEventOp> req);

	ResponseEvent<File> getExportDataFile(RequestEvent<String> req);

	ResponseEvent<List<FacetDetail>> getFacetValues(RequestEvent<GetFacetValuesOp> req);

	//
	// folder related APIs
	//

	ResponseEvent<List<QueryFolderSummary>> getUserFolders(RequestEvent<?> req);

	ResponseEvent<QueryFolderDetails> getFolder(RequestEvent<EntityQueryCriteria> req);

	ResponseEvent<QueryFolderDetails> createFolder(RequestEvent<QueryFolderDetails> req);

	ResponseEvent<QueryFolderDetails> updateFolder(RequestEvent<QueryFolderDetails> req);

	ResponseEvent<Long> deleteFolder(RequestEvent<Long> req);

	ResponseEvent<SavedQueriesList> getFolderQueries(RequestEvent<ListFolderQueriesCriteria> req);

	ResponseEvent<Long> getFolderQueriesCount(RequestEvent<ListFolderQueriesCriteria> req);

	ResponseEvent<List<SavedQuerySummary>> updateFolderQueries(RequestEvent<UpdateFolderQueriesOp> req);

	ResponseEvent<List<UserSummary>> shareFolder(RequestEvent<ShareQueryFolderOp> req);

	//
	// query audit logs related APIs
	//
	ResponseEvent<Long> getAuditLogsCount(RequestEvent<QueryAuditLogsListCriteria> req);

	ResponseEvent<List<QueryAuditLogSummary>> getAuditLogs(RequestEvent<QueryAuditLogsListCriteria> req);

	ResponseEvent<QueryAuditLogDetail> getAuditLog(RequestEvent<Long> req);

	ResponseEvent<ExportedFileDetail> exportAuditLogs(RequestEvent<QueryAuditLogsListCriteria> req);

	ResponseEvent<File> getExportedAuditLogsFile(RequestEvent<String> req);

	//
	// query export APIs
	//
	ResponseEvent<String> getQueryDef(RequestEvent<Long> req);

	//
	// internal use
	// 
	interface ExportProcessor {
		String filename();

		void headers(OutputStream out);
	}	

	QueryDataExportResult exportQueryData(ExecuteQueryEventOp opDetail, ExportProcessor processor);

	QueryDataExportResult exportQueryData(ExecuteQueryEventOp opDetail, BiConsumer<QueryResultData, OutputStream> qdConsumer);

	//
	// internal use
	// 
	String insertCustomQueryForms(String dirName) ;

	void registerQuerySpaceProvider(QuerySpaceProvider qsProvider);

	boolean toggleStarredQuery(Long queryId, boolean starred);
}
