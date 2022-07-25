package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

public class FilesBacklogCleaner implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(FilesBacklogCleaner.class);

	private static final String QUERY_EXPORT_DIR = "query-exported-data";

	private static final String LOG_FILES_DIR = "logs";

	private static final String AUDIT_FILES_DIR = "audit";

	private static final int period = 30; //30 days
	
	@Override
	public void doJob(ScheduledJobRun jobRun) 
	throws Exception {
		cleanupOlderFiles(getQueryExportDataDir(), period, null);
		cleanupOlderFiles(getLogFilesDir(), getLogFilesRetainPeriod(), null);
		cleanupOlderFiles(getAuditFilesDir(), period, null);
		cleanupOlderFiles(getLoginActivityReportsDir(), period, null);
		cleanupOlderFiles(getDataDir(), period, (dir, name) -> !new File(dir, name).isDirectory() && name.endsWith(".csv"));
	}

	private String getDataDir() {
		return ConfigUtil.getInstance().getDataDir();
	}

	private String getQueryExportDataDir() {
		return new File(getDataDir(), QUERY_EXPORT_DIR).getAbsolutePath();
	}

	private String getLogFilesDir() {
		return new File(getDataDir(), LOG_FILES_DIR).getAbsolutePath();
	}

	private String getAuditFilesDir() {
		return new File(getDataDir(), AUDIT_FILES_DIR).getAbsolutePath();
	}

	private String getLoginActivityReportsDir() {
		return new File(getDataDir(), "login-activity-reports").getAbsolutePath();
	}

	private int getLogFilesRetainPeriod() {
		return ConfigUtil.getInstance().getIntSetting("common", "log_files_retain_period", period);
	}

	private static void cleanupOlderFiles(String dataDir, int period, FilenameFilter filter) {
		if (period <= 0) {
			return;
		}

		Calendar timeBefore = Calendar.getInstance();
		timeBefore.setTime(new Date());
		timeBefore.add(Calendar.DATE, -period);
		Long timeInMilliseconds = timeBefore.getTimeInMillis();

		cleanupDir(dataDir, timeInMilliseconds, filter);
	}

	private static void cleanupDir(String directory, Long timeBefore, FilenameFilter filter) {
		try {
			File dir = new File(directory);
			if (!dir.exists() || !dir.isDirectory()) {
				return;
			}

			for (File file : dir.listFiles(filter)) {
				if (!file.isDirectory() && (file.lastModified() < timeBefore)) {
					file.delete();
				}
			}
		} catch (Exception e) {
			logger.error("Error deleting files from directory: " + directory, e);
		}
	}
}
