package com.krishagni.catissueplus.core.de.domain;

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.envers.NotAudited;

import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishagni.catissueplus.core.administrative.domain.ScheduledJob;
import com.krishagni.catissueplus.core.administrative.domain.User;

import edu.common.dynamicextensions.query.QuerySpace;

public class SavedQuery {
	private Long id;

	private String title;

	private User createdBy;

	private User lastUpdatedBy;

	private Date lastRunOn;

	private Date lastUpdated;

	private Long lastRunCount;
	
	private Long cpId;

	private Long cpGroupId;
	
	private String drivingForm;

	private Filter[] filters;

	private QueryExpressionNode[] queryExpression;

	private Object[] selectList;

	private String havingClause;
	
	private ReportSpec reporting;

	private Set<Long> subQueries = new HashSet<>();

	private Set<Long> dependentQueries = new HashSet<>();
	
	private Set<QueryFolder> folders = new HashSet<>();

	private Set<ScheduledJob> scheduledJobs = new HashSet<>();

	private transient QuerySpace qs;
	
	private String wideRowMode = "DEEP";

	private boolean outputColumnExprs;

	private boolean caseSensitive = true;

	private Date deletedOn;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}
	
	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Date getLastRunOn() {
		return lastRunOn;
	}

	public void setLastRunOn(Date lastRunOn) {
		this.lastRunOn = lastRunOn;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public User getLastUpdatedBy() {
		return lastUpdatedBy;
	}

	public void setLastUpdatedBy(User lastUpdatedBy) {
		this.lastUpdatedBy = lastUpdatedBy;
	}

	public Long getLastRunCount() {
		return lastRunCount;
	}

	public void setLastRunCount(Long lastRunCount) {
		this.lastRunCount = lastRunCount;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		if (cpId != null && cpId == -1L) {
			cpId = null;
		}

		this.cpId = cpId;
	}

	public Long getCpGroupId() {
		return cpGroupId;
	}

	public void setCpGroupId(Long cpGroupId) {
		if (cpGroupId != null && cpGroupId == -1L) {
			cpGroupId = null;
		}

		this.cpGroupId = cpGroupId;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	@NotAudited
	public Filter[] getFilters() {
		return filters;
	}

	public void setFilters(Filter[] filters) {
		this.filters = filters;
	}

	@NotAudited
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

	@NotAudited
	public ReportSpec getReporting() {
		return reporting;
	}

	public void setReporting(ReportSpec reporting) {
		this.reporting = reporting;
	}

	@NotAudited
	public Set<Long> getSubQueries() {
		return subQueries;
	}

	public void setSubQueries(Set<Long> subQueries) {
		this.subQueries = subQueries;
	}

	@NotAudited
	public Set<Long> getDependentQueries() {
		return dependentQueries;
	}

	public void setDependentQueries(Set<Long> dependentQueries) {
		this.dependentQueries = dependentQueries;
	}

	@NotAudited
	public Set<QueryFolder> getFolders() {
		return folders;
	}

	public void setFolders(Set<QueryFolder> folders) {
		this.folders = folders;
	}

	@NotAudited
	public Set<ScheduledJob> getScheduledJobs() {
		return scheduledJobs;
	}

	public void setScheduledJobs(Set<ScheduledJob> scheduledJobs) {
		this.scheduledJobs = scheduledJobs;
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

	public Date getDeletedOn() {
		return deletedOn;
	}

	public void setDeletedOn(Date deletedOn) {
		this.deletedOn = deletedOn;
	}

	public String getQueryDefJson() {
		return getQueryDefJson(false);
	}
	
	public String getQueryDefJson(boolean includeTitle) {
		SavedQuery query = new SavedQuery();
		
		if (includeTitle) {
			query.title = title;
		}
		
		query.cpId = cpId;
		query.cpGroupId = cpGroupId;
		query.selectList = selectList;
		query.filters = filters;
		query.queryExpression = queryExpression;
		query.drivingForm = drivingForm;
		query.folders = null;
		query.havingClause = havingClause;
		query.reporting = reporting;
		query.wideRowMode = wideRowMode;
		query.outputColumnExprs = outputColumnExprs;
		query.caseSensitive = caseSensitive;
		
		try {
			return getWriteMapper().writeValueAsString(query);
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling saved query to JSON", e);
		}				
	}

	public void setQueryDefJson(String queryDefJson) {
		setQueryDefJson(queryDefJson, false);
	}
	
	public void setQueryDefJson(String queryDefJson, boolean includeTitle) {
		SavedQuery query = null;
		try {
			query = getReadMapper().readValue(queryDefJson, SavedQuery.class);
		} catch (Exception e) {
			throw new RuntimeException("Error marshalling JSON to saved query: " + e.getMessage(), e);
		}
		if(includeTitle){
			this.title = query.title;
		}
		this.cpId = (query.cpId != null && query.cpId == -1L) ? null : query.cpId;
		this.cpGroupId = (query.cpGroupId != null && query.cpGroupId == -1L) ? null : query.cpGroupId;
		this.selectList = query.selectList;
		this.filters = query.filters;
		this.queryExpression = query.queryExpression;
		this.drivingForm = query.drivingForm;
		this.havingClause = query.havingClause;
		this.reporting = query.reporting;
		this.wideRowMode = query.wideRowMode;
		this.outputColumnExprs = query.outputColumnExprs;
		this.caseSensitive = query.caseSensitive;
	}

	public QuerySpace getQuerySpace() {
		return qs;
	}

	public void setQuerySpace(QuerySpace qs) {
		this.qs = qs;
	}

	public String getAql() {
		return AqlBuilder.getInstance().getQuery(this, new Filter[0]);
	}
	
	public String getAql(Filter[] conjunctionFilters) {
		return AqlBuilder.getInstance().getQuery(this, conjunctionFilters);
	}

	public String getAql(String conjunction) {
		return AqlBuilder.getInstance().getQuery(this, conjunction);
	}

	public String getAql(String conjunction, String orderBy) {
		return AqlBuilder.getInstance().getQuery(this, conjunction, orderBy);
	}
	
	public void update(SavedQuery query) {
		setTitle(query.getTitle());
		setCpId(query.getCpId());
		setCpGroupId(query.getCpGroupId());
		setDrivingForm(query.getDrivingForm());
		setLastUpdatedBy(query.getLastUpdatedBy());
		setLastUpdated(query.getLastUpdated());
		setSelectList(query.getSelectList());
		setFilters(query.getFilters());
		setSubQueries(query.getSubQueries());
		setQueryExpression(query.getQueryExpression());
		setHavingClause(query.getHavingClause());
		setReporting(query.getReporting());
		setWideRowMode(query.getWideRowMode());
		setOutputColumnExprs(query.isOutputColumnExprs());
		setCaseSensitive(query.isCaseSensitive());
	}
	
	@Override
	public int hashCode() {
		return 31 * 1 + ((id == null) ? 0 : id.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		
		if (obj == null) {
			return false;
		}
		
		if (getClass() != obj.getClass()) {
			return false;
		}
		
		SavedQuery other = (SavedQuery) obj;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public SavedQuery copy() {
		SavedQuery copy = new SavedQuery();
		copy.setQueryDefJson(getQueryDefJson(true), true);
		copy.selectList = curateSelectList(copy.selectList);
		return copy;
	}
		
	private ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}
	
	private ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibilityChecker(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(Visibility.ANY)
				.withGetterVisibility(Visibility.NONE)
				.withSetterVisibility(Visibility.NONE)
				.withCreatorVisibility(Visibility.NONE));
		return mapper;		
	}

	private Object[] curateSelectList(Object[] selectList) {
		Object[] result = new Object[selectList.length];
		int idx = 0;
		for (Object field : selectList) {
			if (field instanceof Map) {
				field = new ObjectMapper().convertValue(field, SelectField.class);
			}

			result[idx++] = field;
		}

		return result;
	}
}