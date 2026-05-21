package com.krishagni.catissueplus.core.de.services.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJobRun;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTask;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogDigest;
import com.krishagni.catissueplus.core.de.events.QueryAuditLogPerfStat;
import com.krishagni.catissueplus.core.de.repository.DaoFactory;

@Configurable
public class QueryAuditLogDigestTask implements ScheduledTask {
	private static final LogUtil logger = LogUtil.getLogger(QueryAuditLogDigestTask.class);

	@Autowired
	private DaoFactory daoFactory;

	@Override
	public void doJob(ScheduledJobRun jobRun) {
		String toEmail = ConfigUtil.getInstance().getItAdminEmailId();
		if (StringUtils.isBlank(toEmail)) {
			logger.info("Query audit log digest skipped. IT admin email ID is not configured.");
			return;
		}

		Date endDate = Calendar.getInstance().getTime();
		Date startDate = getStartDate(jobRun.getScheduledJob(), endDate);
		QueryAuditLogDigest digest = getDigest(startDate, endDate);
		List<Long> runtimes = getSuccessfulRuntimes(startDate, endDate);
		sendDigest(toEmail, digest, runtimes, startDate, endDate);
	}

	private Date getStartDate(ScheduledJob job, Date endDate) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(endDate);

		ScheduledJob.RepeatSchedule repeatSchedule = job != null ? job.getRepeatSchedule() : null;
		if (repeatSchedule == null) {
			repeatSchedule = ScheduledJob.RepeatSchedule.ONDEMAND;
		}

		switch (repeatSchedule) {
			case MINUTELY -> cal.add(Calendar.MINUTE, -1);
			case HOURLY -> cal.add(Calendar.HOUR_OF_DAY, -1);
			case DAILY -> cal.add(Calendar.HOUR_OF_DAY, -24);
			case WEEKLY -> cal.add(Calendar.DAY_OF_MONTH, -7);
			case MONTHLY, ONDEMAND -> cal.add(Calendar.MONTH, -1);
		}

		return cal.getTime();
	}

	@PlusTransactional
	private QueryAuditLogDigest getDigest(Date startDate, Date endDate) {
		return daoFactory.getQueryAuditLogDao().getDigest(startDate, endDate);
	}

	@PlusTransactional
	private List<Long> getSuccessfulRuntimes(Date startDate, Date endDate) {
		return daoFactory.getQueryAuditLogDao().getSuccessfulRuntimes(startDate, endDate);
	}

	private void sendDigest(String toEmail, QueryAuditLogDigest digest, List<Long> runtimes, Date startDate, Date endDate) {
		String startDateStr = Utility.getDateTimeString(startDate);
		String endDateStr = Utility.getDateTimeString(endDate);

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] {startDateStr, endDateStr});
		props.put("startTime", startDateStr);
		props.put("endTime", endDateStr);
		props.put("digest", digest);
		props.put("failedLogsUrl", getFailedLogsUrl(startDate, endDate));
		props.put("perfStats", getPerfStats(runtimes));
		props.put("ccAdmin", false);
		EmailUtil.getInstance().sendEmail(DIGEST_EMAIL_TMPL, new String[] {toEmail}, null, props);
	}

	private String getFailedLogsUrl(Date startDate, Date endDate) {
		String appUrl = ConfigUtil.getInstance().getAppUrl();
		if (StringUtils.isBlank(appUrl)) {
			appUrl = "";
		} else if (appUrl.endsWith("/")) {
			appUrl = appUrl.substring(0, appUrl.length() - 1);
		}

		return appUrl + "/ui-app/#/queries/audit-logs?filters=" +
			URLEncoder.encode(getFailedLogsFilters(startDate, endDate), StandardCharsets.UTF_8);
	}

	private String getFailedLogsFilters(Date startDate, Date endDate) {
		Map<String, Object> filters = new HashMap<>();
		filters.put("failed", true);
		filters.put("startDate", getIsoDate(startDate));
		filters.put("endDate", getIsoDate(endDate));

		String json = Utility.mapToJson(filters);
		String uriEncoded = URLEncoder.encode(json, StandardCharsets.UTF_8);
		return Base64.getEncoder().encodeToString(uriEncoded.getBytes(StandardCharsets.UTF_8));
	}

	private List<QueryAuditLogPerfStat> getPerfStats(List<Long> runtimes) {
		List<QueryAuditLogPerfStat> result = new ArrayList<>();
		result.add(getPerfStat(QueryAuditLogPerfStat.P50, 50, runtimes));
		result.add(getPerfStat(QueryAuditLogPerfStat.P90, 90, runtimes));
		result.add(getPerfStat(QueryAuditLogPerfStat.P95, 95, runtimes));
		result.add(getPerfStat(QueryAuditLogPerfStat.P99, 99, runtimes));
		return result;
	}

	private QueryAuditLogPerfStat getPerfStat(String name, int percentile, List<Long> runtimes) {
		long runtime = getPercentileRuntime(percentile, runtimes);
		return new QueryAuditLogPerfStat(name, runtime, String.format("%.3f", runtime / 1000.0));
	}

	private long getPercentileRuntime(int percentile, List<Long> runtimes) {
		if (runtimes == null || runtimes.isEmpty()) {
			return 0L;
		}

		int idx = (int) Math.ceil((percentile / 100.0) * runtimes.size()) - 1;
		return runtimes.get(Math.max(idx, 0));
	}

	private String getIsoDate(Date date) {
		return new SimpleDateFormat("yyyy-MM-dd").format(date);
	}

	private static final String DIGEST_EMAIL_TMPL = "query_audit_log_digest";
}
