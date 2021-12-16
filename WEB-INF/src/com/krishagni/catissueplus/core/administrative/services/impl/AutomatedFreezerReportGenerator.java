package com.krishagni.catissueplus.core.administrative.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.events.AutoFreezerReportDetail;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class AutomatedFreezerReportGenerator implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(AutomatedFreezerReportGenerator.class);

	@Autowired
	private StorageContainerService storageContainerSvc;

	@Override
	public void doJob(ScheduledJobRun jobRun)
	throws Exception {
		try {
			storageContainerSvc.generateAutoFreezerReport(new AutoFreezerReportDetail());
		} catch (Exception e) {
			logger.error("Error generating automated freezer report", e);
		}
	}
}
