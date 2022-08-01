package com.krishagni.catissueplus.core.administrative.services.impl;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.domain.factory.ScheduledJobErrorCode;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class ExternalScheduledTask implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(ExternalScheduledTask.class);

	@Override
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		logger.info("Execution of external commands within OpenSpecimen process context is forbidden: " + jobRun.getScheduledJob().getCommand());
		throw OpenSpecimenException.userError(ScheduledJobErrorCode.EXT_TASK_FORBIDDEN);
	}
}
