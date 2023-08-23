package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.OutputStream;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.catissueplus.core.de.services.impl.DefaultQueryExportProcessor;

@Configurable
public class ScheduledQueryTask implements ScheduledTask {

	@Autowired
	private QueryService queryService;

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) {
		runQuery(jobRun);
	}

	@PlusTransactional
	private void runQuery(ScheduledJobRun jobRun) {
		ScheduledJob job = jobRun.getScheduledJob();
		SavedQuery query = job.getSavedQuery();
		if (query == null || query.getDeletedOn() != null) {
			throw OpenSpecimenException.userError(SavedQueryErrorCode.NOT_FOUND);
		}

		ExecuteQueryEventOp op = new ExecuteQueryEventOp();
		op.setCpId(query.getCpId());
		op.setCpGroupId(query.getCpGroupId());
		op.setDrivingForm(query.getDrivingForm());
		op.setAql(query.getAql());
		op.setWideRowMode(query.getWideRowMode());
		op.setSavedQueryId(query.getId());
		op.setOutputColumnExprs(query.isOutputColumnExprs());
		op.setCaseSensitive(query.isCaseSensitive());
		op.setSynchronous(true);

		QueryService.ExportProcessor processor = new DefaultQueryExportProcessor(query.getCpId(), query.getCpGroupId()) {
			@Override
			public String filename() {
				return "scheduled_query_" + query.getId() + "_" + jobRun.getId() + ".csv.zip";
			}
		};

		QueryDataExportResult exportResult = queryService.exportQueryData(op, processor);

		ResponseEvent<File> resp = queryService.getExportDataFile(new RequestEvent<>(exportResult.getDataFile()));
		resp.throwErrorIfUnsuccessful();
		jobRun.setLogFilePath(resp.getPayload().getAbsolutePath());
	}
}