package com.krishagni.catissueplus.core.de.events;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.QueryExpressionNode;
import com.krishagni.catissueplus.core.de.domain.ReportSpec;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

public class SavedQueryDetail extends SavedQuerySummary {
	private Long cpId;

	private String cpShortTitle;

	private Long cpGroupId;

	private String cpGroupName;
	
	private String drivingForm;

	private Filter[] filters;

	private QueryExpressionNode[] queryExpression;

	private Object[] selectList;

	private String havingClause;
	
	private ReportSpec reporting;
	
	private String wideRowMode;

	private boolean outputColumnExprs;

	private boolean caseSensitive;

	private Long[] dependentQueries;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Long getCpGroupId() {
		return cpGroupId;
	}

	public void setCpGroupId(Long cpGroupId) {
		this.cpGroupId = cpGroupId;
	}

	public String getCpGroupName() {
		return cpGroupName;
	}

	public void setCpGroupName(String cpGroupName) {
		this.cpGroupName = cpGroupName;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	public Filter[] getFilters() {
		return filters;
	}

	public void setFilters(Filter[] filters) {
		this.filters = filters;
	}

	public QueryExpressionNode[] getQueryExpression() {
		return queryExpression;
	}

	public void setQueryExpression(QueryExpressionNode[] queryExpression) {
		this.queryExpression = queryExpression;
	}

	public Object[] getSelectList() {
		return selectList;
	}

	public void setSelectList(Object[] selectList) {
		this.selectList = selectList;
	}

	public String getHavingClause() {
		return havingClause;
	}

	public void setHavingClause(String havingClause) {
		this.havingClause = havingClause;
	}

	public ReportSpec getReporting() {
		return reporting;
	}

	public void setReporting(ReportSpec reporting) {
		this.reporting = reporting;
	}

	public String getWideRowMode() {
		return wideRowMode;
	}

	public void setWideRowMode(String wideRowMode) {
		this.wideRowMode = wideRowMode;
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

	public Long[] getDependentQueries() {
		return dependentQueries;
	}

	public void setDependentQueries(Long[] dependentQueries) {
		this.dependentQueries = dependentQueries;
	}

	public static SavedQueryDetail fromSavedQuery(SavedQuery savedQuery){
		SavedQueryDetail detail = new SavedQueryDetail();
		
		detail.setId(savedQuery.getId());
		detail.setTitle(savedQuery.getTitle());
		detail.setCpId(savedQuery.getCpId());
		detail.setCpGroupId(savedQuery.getCpGroupId());
		detail.setDrivingForm(savedQuery.getDrivingForm());		
		detail.setCreatedBy(UserSummary.from(savedQuery.getCreatedBy()));
		detail.setLastModifiedBy(UserSummary.from(savedQuery.getLastUpdatedBy()));
		detail.setLastModifiedOn(savedQuery.getLastUpdated());
		detail.setFilters(savedQuery.getFilters());
		detail.setQueryExpression(savedQuery.getQueryExpression());
		detail.setSelectList(savedQuery.getSelectList());
		detail.setHavingClause(savedQuery.getHavingClause());
		detail.setReporting(savedQuery.getReporting());
		detail.setWideRowMode(savedQuery.getWideRowMode());
		detail.setOutputColumnExprs(savedQuery.isOutputColumnExprs());
		detail.setCaseSensitive(savedQuery.isCaseSensitive());
		detail.setDependentQueries(savedQuery.getDependentQueries().toArray(new Long[0]));
		return detail;
	}	
}
