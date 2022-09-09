package com.krishagni.catissueplus.core.de.events;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class QueryAuditLogSummary {	
	private Long id;
	
	private Long queryId;
	
	private String queryTitle;
	
	private UserSummary runBy;
	
	private Date timeOfExecution;
	
	private Long timeToFinish;
	
	private String runType;
	
	private Long recordCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getQueryId() {
		return queryId;
	}

	public void setQueryId(Long queryId) {
		this.queryId = queryId;
	}

	public String getQueryTitle() {
		return queryTitle;
	}

	public void setQueryTitle(String queryTitle) {
		this.queryTitle = queryTitle;
	}

	public UserSummary getRunBy() {
		return runBy;
	}

	public void setRunBy(UserSummary runBy) {
		this.runBy = runBy;
	}

	public Date getTimeOfExecution() {
		return timeOfExecution;
	}

	public void setTimeOfExecution(Date runtime) {
		this.timeOfExecution = runtime;
	}

	public Long getTimeToFinish() {
		return timeToFinish;
	}

	public void setTimeToFinish(Long timeToFinish) {
		this.timeToFinish = timeToFinish;
	}

	public String getRunType() {
		return runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public Long getRecordCount() {
		return recordCount;
	}

	public void setRecordCount(Long recordCount) {
		this.recordCount = recordCount;
	}

	public static <T extends QueryAuditLogSummary> T copyTo(QueryAuditLog log, T result) {
		result.setId(log.getId());

		if (log.getQuery() != null) {
			result.setQueryId(log.getQuery().getId());
			result.setQueryTitle(log.getQuery().getTitle());
		}

		result.setRunType(log.getRunType());
		result.setRunBy(UserSummary.from(log.getRunBy()));
		result.setTimeOfExecution(log.getTimeOfExecution());
		result.setTimeToFinish(log.getTimeToFinish());
		result.setRecordCount(log.getRecordCount());
		return result;
	}

	public static QueryAuditLogSummary from(QueryAuditLog log) {
		QueryAuditLogSummary result = new QueryAuditLogSummary();
		copyTo(log, result);
		return result;
	}

	public static List<QueryAuditLogSummary> from(List<QueryAuditLog> logs) {
		return logs.stream().map(QueryAuditLogSummary::from).collect(Collectors.toList());
	}
}