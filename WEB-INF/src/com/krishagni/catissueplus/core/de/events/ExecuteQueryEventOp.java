package com.krishagni.catissueplus.core.de.events;


import org.apache.commons.lang3.StringUtils;

public class ExecuteQueryEventOp  {

	private String querySpace;
	
	private Long cpId;

	private Long cpGroupId;
	
	private String drivingForm;

	private String aql;
	
	private String wideRowMode = "OFF";
	
	private Long savedQueryId;
	
	private String runType = "Data";
	
	private String indexOf;

	private boolean outputIsoDateTime;

	private boolean outputColumnExprs;

	private boolean caseSensitive = true;

	private boolean synchronous;

	//
	// mostly used by the list generator API
	//
	private boolean disableAccessChecks;

	//
	// mostly used by the list generator API
	//
	private boolean disableAuditing;

	private int timeoutInSeconds = 55;

	private String reportName;

	public String getQuerySpace() {
		return querySpace;
	}

	public void setQuerySpace(String querySpace) {
		this.querySpace = querySpace;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getCpGroupId() {
		return cpGroupId;
	}

	public void setCpGroupId(Long cpGroupId) {
		this.cpGroupId = cpGroupId;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	public String getAql() {
		return aql;
	}

	public void setAql(String aql) {
		this.aql = aql;
	}

	public String getWideRowMode() {
		return wideRowMode;
	}

	public void setWideRowMode(String wideRowMode) {
		this.wideRowMode = wideRowMode;
	}

	public Long getSavedQueryId() {
		return savedQueryId;
	}

	public void setSavedQueryId(Long savedQueryId) {
		this.savedQueryId = savedQueryId;
	}

	public String getRunType() {
		return StringUtils.isBlank(runType) ? "Data" : runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public String getIndexOf() {
		return indexOf;
	}

	public void setIndexOf(String indexOf) {
		this.indexOf = indexOf;
	}

	public boolean isOutputIsoDateTime() {
		return outputIsoDateTime;
	}

	public void setOutputIsoDateTime(boolean outputIsoDateTime) {
		this.outputIsoDateTime = outputIsoDateTime;
	}

	public boolean isOutputColumnExprs() {
		return outputColumnExprs;
	}

	public void setOutputColumnExprs(boolean outputColumnExprs) {
		this.outputColumnExprs = outputColumnExprs;
	}

	public boolean isCaseSensitive() {
		return caseSensitive;
	}

	public void setCaseSensitive(boolean caseSensitive) {
		this.caseSensitive = caseSensitive;
	}

	public boolean isSynchronous() {
		return synchronous;
	}

	public void setSynchronous(boolean synchronous) {
		this.synchronous = synchronous;
	}

	public boolean isDisableAccessChecks() {
		return disableAccessChecks;
	}

	public void setDisableAccessChecks(boolean disableAccessChecks) {
		this.disableAccessChecks = disableAccessChecks;
	}

	public boolean isDisableAuditing() {
		return disableAuditing;
	}

	public void setDisableAuditing(boolean disableAuditing) {
		this.disableAuditing = disableAuditing;
	}

	public int getTimeout() {
		return timeoutInSeconds;
	}

	public void setTimeout(int timeoutInSeconds) {
		this.timeoutInSeconds = timeoutInSeconds;
	}

	public String getReportName() {
		return reportName;
	}

	public void setReportName(String reportName) {
		this.reportName = reportName;
	}
}
