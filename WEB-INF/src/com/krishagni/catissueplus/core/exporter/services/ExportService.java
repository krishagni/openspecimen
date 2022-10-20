package com.krishagni.catissueplus.core.exporter.services;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.exporter.domain.ExportJob;
import com.krishagni.catissueplus.core.exporter.events.ExportDetail;
import com.krishagni.catissueplus.core.exporter.events.ExportJobDetail;

public interface ExportService {
	ResponseEvent<ExportJobDetail> exportObjects(RequestEvent<ExportDetail> req);

	ResponseEvent<String> getExportFile(RequestEvent<Long> req);

	void registerObjectsGenerator(String type, Supplier<Function<ExportJob, List<? extends Object>>> genFactory);

	void saveJob(String entityName, Date startTime, Map<String, String> params);
}
