package com.krishagni.catissueplus.core.de.domain;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;

public class QueryAuditLog {
	private Long id;
	
	private SavedQuery query;
	
	private User runBy;
	
	private Date timeOfExecution;
	
	private Long timeToFinish;
	
	private String runType;

	private String aql;
	
	private Long recordCount;
	
	private String sql;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SavedQuery getQuery() {
		return query;
	}

	public void setQuery(SavedQuery query) {
		this.query = query;
	}

	public User getRunBy() {
		return runBy;
	}

	public void setRunBy(User runBy) {
		this.runBy = runBy;
	}

	public Date getTimeOfExecution() {
		return timeOfExecution;
	}

	public void setTimeOfExecution(Date timeOfExecution) {
		this.timeOfExecution = timeOfExecution;
	}

	public Long getTimeToFinish() {
		return timeToFinish;
	}

	public void setTimeToFinish(Long timeToFinish) {
		this.timeToFinish = timeToFinish;
	}

	public String getAql() {
		return aql;
	}

	public void setAql(String aql) {
		this.aql = aql;
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

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}		
}
