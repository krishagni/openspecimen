package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.de.domain.QueryAuditLog;

public class QueryAuditLogDetail extends QueryAuditLogSummary {

	private String aql;
	
	private String sql;

	public String getAql() {
		return aql;
	}

	public void setAql(String aql) {
		this.aql = aql;
	}

	public String getSql() {
		return sql;
	}

	public void setSql(String sql) {
		this.sql = sql;
	}
	
	public static QueryAuditLogDetail from(QueryAuditLog auditLog){
		QueryAuditLogDetail detail = new QueryAuditLogDetail();
		copyTo(auditLog, detail);
		detail.setAql(auditLog.getAql());
		detail.setSql(auditLog.getSql());
		return detail;
	}
}
