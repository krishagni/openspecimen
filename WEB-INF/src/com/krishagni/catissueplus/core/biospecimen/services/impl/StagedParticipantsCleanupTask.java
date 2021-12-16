package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.ConfigParams;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class StagedParticipantsCleanupTask implements ScheduledTask {

	private static final LogUtil logger = LogUtil.getLogger(StagedParticipantsCleanupTask.class);

	private static final int DEF_CLEANUP_INT = 90;

	@Autowired
	private DaoFactory daoFactory;
	
	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) throws Exception {
		int olderThanDays = ConfigUtil.getInstance().getIntSetting(
			ConfigParams.MODULE, ConfigParams.STAGED_PART_CLEANUP_INT, DEF_CLEANUP_INT);
		if (olderThanDays <= 0) {
			return;
		}

		int deletedVisitsCount = daoFactory.getStagedVisitDao().deleteOldVisits(olderThanDays);
		logger.info("Cleaned up " + deletedVisitsCount + " old visits from staging table");

		int deletedParticipantsCount = daoFactory.getStagedParticipantDao().deleteOldParticipants(olderThanDays);
		logger.info("Cleaned up " + deletedParticipantsCount + " old participants from staging table");
	}
}
