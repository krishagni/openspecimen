package com.krishagni.catissueplus.core.importer.services;

import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.events.FileDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.events.FileRecordsDetail;
import com.krishagni.catissueplus.core.importer.events.ImportDetail;
import com.krishagni.catissueplus.core.importer.events.ImportJobDetail;
import com.krishagni.catissueplus.core.importer.events.ObjectSchemaCriteria;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;

public interface ImportService {
	public ResponseEvent<List<ImportJobDetail>> getImportJobs(RequestEvent<ListImportJobsCriteria> req);
	
	public ResponseEvent<ImportJobDetail> getImportJob(RequestEvent<Long> req);
			
	public ResponseEvent<String> getImportJobFile(RequestEvent<Long> req);
	
	public ResponseEvent<String> uploadImportJobFile(RequestEvent<InputStream> in);
	
	public ResponseEvent<ImportJobDetail> importObjects(RequestEvent<ImportDetail> req);
	
	public ResponseEvent<String> getInputFileTemplate(RequestEvent<ObjectSchemaCriteria> req);

	public ResponseEvent<List<Map<String, Object>>> processFileRecords(RequestEvent<FileRecordsDetail> req);

	public ResponseEvent<ImportJobDetail> stopJob(RequestEvent<Long> req);

	public ResponseEvent<Integer> scheduleImportJobs(RequestEvent<FileDetail> req);

	public ImportJob saveJob(String entity, String op, Date startTime, Map<String, String> params);

	void queueJob(ImportJob job);
}
