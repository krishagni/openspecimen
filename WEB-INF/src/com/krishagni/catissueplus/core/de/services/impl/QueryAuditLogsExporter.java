package com.krishagni.catissueplus.core.de.services.impl;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.CsvFileWriter;
import com.krishagni.catissueplus.core.common.util.CsvWriter;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogsListCriteria;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

@Configurable
public class QueryAuditLogsExporter implements Runnable {

	private static final LogUtil logger = LogUtil.getLogger(QueryAuditLogsExporter.class);

	private static final String AUDIT_LOGS_EXPORTED = "query_audit_logs_exported";

	private static final String AUDIT_LOG_EXPORT_FAILED = "query_audit_logs_export_failed";

	private QueryAuditLogsListCriteria crit;

	private User exportedBy;

	private List<User> runBy;

	private File exportedFile;

	@Autowired
	private DaoFactory deDaoFactory;

	public QueryAuditLogsExporter(QueryAuditLogsListCriteria crit, User exportedBy, List<User> runBy) {
		this.crit = crit;
		this.exportedBy = exportedBy;
		this.runBy = runBy;
	}

	@Override
	public void run() {
		try {
			AuthUtil.setCurrentUser(exportedBy);

			String baseDir = UUID.randomUUID().toString();
			Date exportedOn = Calendar.getInstance().getTime();

			logger.info("Exporting query audit logs ...");
			File auditLogsFile = exportAuditLogs(baseDir, exportedOn);

			logger.info("Creating query audit logs ZIP file ...");
			exportedFile = new File(getAuditDir(), baseDir + "_" + getTs(exportedOn) + "_" + exportedBy.getId());
			Utility.zipFiles(Collections.singletonList(auditLogsFile.getAbsolutePath()), exportedFile.getAbsolutePath());

			logger.info("Cleaning up query audit directory: " + baseDir);
			cleanupAuditDir(baseDir);

			logger.info("Sending query audit logs email to " + exportedBy.formattedName(true));
			sendEmailNotif();
		} catch (Exception e) {
			logger.error("Error generating the query audit logs report", e);
			sendFailedEmailNotif(e);
		}
	}

	public File getExportedFile() {
		return exportedFile;
	}

	private File exportAuditLogs(String dir, Date exportedOn) {
		long startTime = System.currentTimeMillis();
		CsvFileWriter csvWriter = null;

		try {
			File outputFile = getOutputFile(dir, exportedOn);
			csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

			writeHeader(csvWriter, exportedOn);

			long lastId = 0;
			crit.startAt(0).asc(true).maxResults(50);

			int count = 0;
			boolean endOfLogs = false;
			while (!endOfLogs) {
				List<QueryAuditLog> logs = deDaoFactory.getQueryAuditLogDao().getLogs(crit.lastId(lastId));
				for (QueryAuditLog log : logs) {
					writeRow(csvWriter, log);
					++count;
					if (count % 25 == 0) {
						csvWriter.flush();
					}

					lastId = log.getId();
				}

				endOfLogs = logs.size() < 50;
			}

			csvWriter.flush();

			return outputFile;
		} catch (Exception e) {
			logger.error("Error exporting query audit logs", e);
			throw OpenSpecimenException.serverError(e);
		} finally {
			IOUtils.closeQuietly(csvWriter);
			logger.info("Query audit logs export finished in " +  (System.currentTimeMillis() - startTime) + " ms");
		}
	}

	private void writeHeader(CsvWriter writer, Date exportedOn) {
		writeHeader0(writer, exportedOn);

		String[] keys = {
			"query_audit_logs_id", "query_audit_logs_tstmp",
			"query_audit_logs_user", "query_audit_logs_user_email", "query_audit_logs_user_login",
			"query_audit_logs_run_type", "query_audit_logs_exec_time", "query_audit_logs_records_count",
			"query_audit_logs_aql", "query_audit_logs_sql"
		};

		writer.writeNext(Stream.of(keys).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));
	}

	private void writeHeader0(CsvWriter writer, Date exportedOn) {
		writeRow(writer, toMsg("common_server_url"), ConfigUtil.getInstance().getAppUrl());
		writeRow(writer, toMsg("common_server_env"), ConfigUtil.getInstance().getDeployEnv());
		writeRow(writer, toMsg("query_audit_logs_exported_by"), exportedBy.formattedName(true));
		writeRow(writer, toMsg("query_audit_logs_exported_on"), Utility.getDateTimeString(exportedOn));

		if (CollectionUtils.isNotEmpty(runBy)) {
			List<String> userNames = runBy.stream().map(u -> u.formattedName(true)).collect(Collectors.toList());
			userNames.add(0, toMsg("query_audit_logs_run_by"));
			writeRow(writer, userNames.toArray(new String[0]));
		}

		if (crit.startDate() != null) {
			writeRow(writer, toMsg("query_audit_logs_start_date"), Utility.getDateTimeString(crit.startDate()));
		}

		if (crit.endDate() != null) {
			writeRow(writer, toMsg("query_audit_logs_end_date"), Utility.getDateTimeString(crit.endDate()));
		}

		writeRow(writer, "");
	}

	private void writeRow(CsvWriter writer, QueryAuditLog log) {
		String id        = log.getId().toString();
		String dateTime  = Utility.getDateTimeString(log.getTimeOfExecution());
		String user      = null;
		String emailId   = null;
		String loginId   = null;
		if (log.getRunBy() != null) {
			user = log.getRunBy().formattedName();
			emailId = log.getRunBy().getEmailAddress();
			loginId = log.getRunBy().getLoginName();
		}

		String timeToFinish = "N/A";
		if (log.getTimeToFinish() != null) {
			timeToFinish = String.valueOf(log.getTimeToFinish() / 1000);
		}

		String recordsCount = log.getRecordCount() != null ? log.getRecordCount().toString() : "N/A";
		writer.writeNext(new String[] {
			id, dateTime, user, emailId, loginId,
			log.getRunType(), timeToFinish, recordsCount,
			log.getAql(), log.getSql()
		});
	}

	private void writeRow(CsvWriter writer, String ... columns) {
		writer.writeNext(columns);
	}

	private void sendEmailNotif() {
		sendNotif(AUDIT_LOGS_EXPORTED, null);
	}

	private void sendFailedEmailNotif(Exception e) {
		Map<String, Object> props = new HashMap<>();
		props.put("errorMsg", Utility.getErrorMessage(e));
		props.put("errorStack", ExceptionUtils.getFullStackTrace(e));
		sendNotif(AUDIT_LOG_EXPORT_FAILED, props);
	}

	private void sendNotif(String tmpl, Map<String, Object> props) {
		if (props == null) {
			props = new HashMap<>();
		}

		String startDate = Utility.getDateTimeString(crit.startDate());
		String endDate   = Utility.getDateTimeString(crit.endDate());

		props.put("$subject", new Object[] { startDate, endDate });
		props.put("startDate", startDate);
		props.put("endDate",   endDate);
		String userNames = Utility.nullSafeStream(runBy).map(User::formattedName).collect(Collectors.joining(", "));
		props.put("users",     !userNames.isEmpty() ? userNames : null);
		props.put("fileId",    getFileId(exportedFile));
		props.put("rcpt",      exportedBy.formattedName());

		EmailUtil.getInstance().sendEmail(
			tmpl,
			new String[] { exportedBy.getEmailAddress() },
			null,
			props
		);
	}

	private String getFileId(File file) {
		if (file == null) {
			return null;
		}

		return file.getName().substring(0, file.getName().lastIndexOf("_"));
	}

	private File getOutputFile(String dir, Date exportedOn) {
		return new File(getAuditDir(dir), "os_query_audit_logs_" + getTs(exportedOn) + ".csv");
	}

	private File getAuditDir() {
		return new File(ConfigUtil.getInstance().getDataDir(), "audit");
	}

	private File getAuditDir(String dir) {
		File result = new File(getAuditDir(), dir);
		if (!result.exists()) {
			result.mkdirs();
		}

		return result;
	}

	private void cleanupAuditDir(String baseDir)
	throws IOException {
		File dir = getAuditDir(baseDir);
		FileUtils.deleteDirectory(dir);
	}

	private String getTs(Date date) {
		return new SimpleDateFormat("yyyyMMdd_HHmm").format(date);
	}

	private String toMsg(String key) {
		try {
			int idx = key.indexOf("-");
			if (idx > 0) {
				key = key.substring(0, idx);
			}

			return MessageUtil.getInstance().getMessage(key);
		} catch (Exception e) {
			return key;
		}
	}
}
