package com.krishagni.catissueplus.core.exporter.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;

public interface ExportJobDao extends Dao<ExportJob> {
	List<ExportJob> getExportJobs(ExportJobsListCriteria crit);
}
