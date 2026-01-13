package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;

@Configurable
public class FilesBacklogCleaner implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(FilesBacklogCleaner.class);

	private static final String QUERY_EXPORT_DIR = "query-exported-data";

	private static final String LOG_FILES_DIR = "logs";

	private static final String AUDIT_FILES_DIR = "audit";

	private static final int FILE_RETENTION_DAYS = 30; //30 days

	private static final int API_LOG_RETENTION_DAYS = 90;

	@Autowired
	private DaoFactory daoFactory;
	
	@Override
	public void doJob(ScheduledJobRun jobRun) 
	throws Exception {

		Map<String, String> argsMap = new HashMap<>();
		if (StringUtils.isNotBlank(jobRun.getScheduledJob().getFixedArgs())) {
			String[] args = jobRun.getScheduledJob().getFixedArgs().split(",");
			for (String arg : args) {
				String[] parts = arg.trim().split("=");
				if (parts.length == 1) {
					argsMap.put("filesRetentionDays", parts[0]);
				} else {
					argsMap.put(parts[0].trim(), parts[1].trim());
				}
			}
		}

		Integer inputPeriod = null;
		try {
			String input = argsMap.get("filesRetentionDays");
			if (StringUtils.isNotBlank(input)) {
				inputPeriod = Integer.parseInt(input);
			}
		} catch (Exception e) {
			logger.error("Error parsing the retention period from the job configuration. Defaulting to " + FILE_RETENTION_DAYS + " days", e);
		}


		int effectivePeriod = inputPeriod != null ? inputPeriod : FILE_RETENTION_DAYS;
		cleanupOlderFiles(getQueryExportDataDir(), effectivePeriod, null, true);
		cleanupOlderFiles(getLogFilesDir(), getLogFilesRetainPeriod(), null, true);
		cleanupOlderFiles(getAuditFilesDir(), effectivePeriod, null, true);
		cleanupOlderFiles(getLoginActivityReportsDir(), effectivePeriod, null, true);
		cleanupOlderFiles(getImportJobsDir(), effectivePeriod, null, true);
		cleanupOlderFiles(getExportJobsDir(), effectivePeriod, null, true);
		cleanupOlderFiles(getDirPath("oc", "import-logs"), effectivePeriod, null, true);
		cleanupOlderFiles(getDirPath("oc", "auto-importer"), effectivePeriod, null, true);
		cleanupOlderFiles(getDataDir(), effectivePeriod, (dir, name) -> !new File(dir, name).isDirectory() && name.endsWith(".csv"), false);

		Integer apiLogsRetentionDays = null;
		try {
			String input = argsMap.get("apiLogsRetentionDays");
			if (StringUtils.isNotBlank(input)) {
				apiLogsRetentionDays = Integer.parseInt(input);
			}
		} catch (Exception e) {
			logger.error("Error parsing the retention period from the job configuration. Defaulting to " + API_LOG_RETENTION_DAYS + " days", e);
		}

		apiLogsRetentionDays = apiLogsRetentionDays != null ? apiLogsRetentionDays : API_LOG_RETENTION_DAYS;
		if (apiLogsRetentionDays > 0) {
			int deleted = cleanupApiCallsLog(apiLogsRetentionDays);
			if (deleted > 0) {
				logger.info("Deleted " + deleted + " API calls log entries that are older than " + apiLogsRetentionDays + " days");
			}
		}
	}

	private String getDataDir() {
		return ConfigUtil.getInstance().getDataDir();
	}

	private String getQueryExportDataDir() {
		return getDirPath(QUERY_EXPORT_DIR);
	}

	private String getLogFilesDir() {
		return getDirPath(LOG_FILES_DIR);
	}

	private String getAuditFilesDir() {
		return getDirPath(AUDIT_FILES_DIR);
	}

	private String getLoginActivityReportsDir() {
		return getDirPath("login-activity-reports");
	}

	private String getImportJobsDir() {
		return getDirPath("bulk-import", "jobs");
	}

	private String getExportJobsDir() {
		return getDirPath("export-jobs");
	}

	private String getDirPath(String... more) {
		return Path.of(getDataDir(), more).toFile().getAbsolutePath();
	}

	private int getLogFilesRetainPeriod() {
		return ConfigUtil.getInstance().getIntSetting("common", "log_files_retain_period", FILE_RETENTION_DAYS);
	}

	private void cleanupOlderFiles(String dataDir, int period, FilenameFilter filter, boolean rmDirs) {
		if (period <= 0) {
			return;
		}

		Calendar timeBefore = Calendar.getInstance();
		timeBefore.setTime(new Date());
		timeBefore.add(Calendar.DATE, -period);
		Long timeInMilliseconds = timeBefore.getTimeInMillis();

		cleanupDir(dataDir, timeInMilliseconds, filter, rmDirs);
	}

	private void cleanupDir(String directory, Long timeBefore, FilenameFilter filter, boolean rmDirs) {
		try {
			File dir = new File(directory);
			if (!dir.exists() || !dir.isDirectory()) {
				return;
			}

			for (File file : Objects.requireNonNull(dir.listFiles(filter))) {
				BasicFileAttributes fileAttrs = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				if (fileAttrs.lastModifiedTime().toMillis() >= timeBefore) {
					continue;
				}

				if (fileAttrs.isDirectory() && rmDirs) {
					FileUtils.deleteDirectory(file);
				} else if (!file.isDirectory()) {
					FileUtils.delete(file);
				}
			}
		} catch (Exception e) {
			if (rmDirs) {
				logger.error("Error deleting the directory: " + directory, e);
			} else {
				logger.error("Error deleting files from directory: " + directory, e);
			}
		}
	}

	@PlusTransactional
	private int cleanupApiCallsLog(int apiLogsRetentionDays) {
		return daoFactory.getAuditDao().deleteApiCallLogs(apiLogsRetentionDays);
	}
}
