package com.krishagni.catissueplus.core.importer.services.impl;

import java.util.List;

import org.hibernate.Hibernate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;
import com.krishagni.catissueplus.core.importer.repository.ImportJobDao;
import com.krishagni.catissueplus.core.importer.repository.ListImportJobsCriteria;
import com.krishagni.catissueplus.core.importer.services.ImportService;

@Configurable
public class LoadOfflineJobsTask implements ScheduledTask {

	@Autowired
	private ImportJobDao importJobDao;

	@Autowired
	private ImportService importSvc;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		boolean endOfJobs = false;

		ListImportJobsCriteria criteria = new ListImportJobsCriteria()
			.status(ImportJob.Status.OFFLINE_QUEUED.name())
			.startAt(0)
			.maxResults(25);

		while (!endOfJobs) {
			List<ImportJob> jobs = importJobDao.getImportJobs(criteria);
			endOfJobs = jobs.size() < criteria.maxResults();
			criteria.startAt(criteria.startAt() + jobs.size());
			jobs.forEach(job -> {
				if (!Hibernate.isInitialized(job.getCreatedBy())) {
					Hibernate.initialize(job.getCreatedBy());
				}

				job.getParams().size();
				importSvc.queueJob(job);
			});
		}
	}
}
