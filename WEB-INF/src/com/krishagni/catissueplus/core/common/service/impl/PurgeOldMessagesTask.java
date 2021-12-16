package com.krishagni.catissueplus.core.common.service.impl;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;


import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Configurable
public class PurgeOldMessagesTask implements ScheduledTask {

	private static final LogUtil logger = LogUtil.getLogger(PurgeOldMessagesTask.class);

	@Autowired
	private DaoFactory daoFactory;

	@Override
	@PlusTransactional
	public void doJob(ScheduledJobRun jobRun) {
		try {
			int retentionPeriod = getMessageRetentionPeriod();
			if (retentionPeriod <= 0) {
				return;
			}

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.DATE, -retentionPeriod);
			Date olderThanDt = Utility.chopTime(cal.getTime());

			logger.info("Purging messages that were received prior to " + olderThanDt + ".");
			int deleted = daoFactory.getMessageLogDao().deleteOldMessages(olderThanDt);
			logger.info("Purged " + deleted + " old messages from the logs table.");
		} catch (Exception e) {
			logger.error("Error purging old messages from the logs table.", e);
		}
	}


	private int getMessageRetentionPeriod() {
		return ConfigUtil.getInstance().getIntSetting("common", "eapp_msg_retention_period", 90);
	}
}
