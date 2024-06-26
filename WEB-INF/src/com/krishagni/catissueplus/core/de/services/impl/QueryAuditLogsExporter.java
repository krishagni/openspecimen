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

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
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
import com.krishagni.catissueplus.core.exporter.services.ExportService;

@Configurable
public class QueryAuditLogsExporter implements Runnable {

	private static final LogUtil logger = LogUtil.getLogger(QueryAuditLogsExporter.class);

	private static final String AUDIT_LOGS_EXPORTED = "query_audit_logs_exported";

	private static final String AUDIT_LOG_EXPORT_FAILED = "query_audit_logs_export_failed";

	private QueryAuditLogsListCriteria crit;

	private User exportedBy;

	private String ipAddress;

	private List<User> runBy;

	private File exportedFile;

	@Autowired
	private DaoFactory deDaoFactory;

	@Autowired
	private ExportService exportSvc;

	public QueryAuditLogsExporter(QueryAuditLogsListCriteria crit, User exportedBy, List<User> runBy) {
		this.crit = crit;
		this.exportedBy = exportedBy;
		this.runBy = runBy;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	@Override
	public void run() {
		try {
			Date startTime = Calendar.getInstance().getTime();
			AuthUtil.setCurrentUser(exportedBy, ipAddress);

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
			exportSvc.saveJob("query_audit_logs", startTime, crit.toStrMap());
		} catch (Exception e) {
			logger.error("Error generating the query audit logs report", e);
			sendFailedEmailNotif(e);
		}
	}

	public File getExportedFile() {
		return exportedFile;
	}

	public File exportAuditLogs(String dir, Date exportedOn) {
		return exportAuditLogs(getOutputFile(dir, exportedOn), exportedOn);
	}

	public File exportAuditLogs(File outputFile, Date exportedOn) {
		long startTime = System.currentTimeMillis();
		CsvFileWriter csvWriter = null;

		try {
			csvWriter = CsvFileWriter.createCsvFileWriter(outputFile);

			writeHeader(csvWriter, exportedOn);

			long lastId = 0;
			crit.startAt(0).asc(false).maxResults(50);

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
			"audit_rev_id", "audit_start_time", "audit_end_time",
			"audit_rev_user", "audit_rev_user_email", "audit_rev_domain", "audit_rev_user_login",
			"audit_rev_institute", "audit_rev_ip_address", "audit_rev_entity_op",
			"query_audit_logs_exec_time", "query_audit_logs_records_count",
			"query_audit_logs_aql", "query_audit_logs_sql"
		};
		writer.writeNext(Stream.of(keys).map(MessageUtil.getInstance()::getMessage).toArray(String[]::new));
	}

	private void writeHeader0(CsvWriter writer, Date exportedOn) {
		writeRow(writer, toMsg("common_server_url"), ConfigUtil.getInstance().getAppUrl());
		writeRow(writer, toMsg("common_server_env"), ConfigUtil.getInstance().getDeployEnv());
		writeRow(writer, toMsg("audit_rev_exported_by"), exportedBy.formattedName(true));
		writeRow(writer, toMsg("audit_rev_exported_on"), Utility.getDateTimeString(exportedOn));

		if (CollectionUtils.isNotEmpty(runBy)) {
			List<String> userNames = runBy.stream().map(u -> u.formattedName(true)).collect(Collectors.toList());
			userNames.add(0, toMsg("audit_rev_audited_users"));
			writeRow(writer, userNames.toArray(new String[0]));
		}

		if (crit.startDate() != null) {
			writeRow(writer, toMsg("audit_rev_start_date"), Utility.getDateTimeString(crit.startDate()));
		}

		if (crit.endDate() != null) {
			writeRow(writer, toMsg("audit_rev_end_date"), Utility.getDateTimeString(crit.endDate()));
		}

		writeRow(writer, "");
	}

	private void writeRow(CsvWriter writer, QueryAuditLog log) {
		String id        = log.getId().toString();
		String startTime = Utility.getDateTimeString(log.getTimeOfExecution());
		String user      = null;
		String emailId   = null;
		String domain    = null;
		String loginId   = null;
		String institute = null;
		if (log.getRunBy() != null) {
			user = log.getRunBy().formattedName();
			emailId = log.getRunBy().getEmailAddress();
			domain = log.getRunBy().getAuthDomain().getName();
			loginId = log.getRunBy().getLoginName();
			institute = log.getRunBy().getInstitute() != null ? log.getRunBy().getInstitute().getName() : null;
		}

		String endTime = "N/A";
		String timeToFinish = "N/A";
		if (log.getTimeToFinish() != null) {
			Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(log.getTimeOfExecution().getTime());
			cal.add(Calendar.MILLISECOND, log.getTimeToFinish().intValue());
			endTime = Utility.getDateTimeString(cal.getTime());
			timeToFinish = log.getTimeToFinish().toString();
		}

		String recordsCount = log.getRecordCount() != null ? log.getRecordCount().toString() : "N/A";
		writer.writeNext(new String[] {
			id, startTime, endTime, user, emailId, domain, loginId, institute, log.getIpAddress(),
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
		props.put("errorStack", ExceptionUtils.getStackTrace(e));
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
