package com.krishagni.catissueplus.core.de.events;

import java.util.Collections;
import java.util.List;

public class ExecuteSavedQueryOp {
	private Long savedQueryId;

	private String runType = "DATA";

	private String drivingForm = "Participant";

	private String wideRowMode = "DEEP";

	private int startAt = 0;

	private int maxResults = 100;

	private List<Criterion> criteria = Collections.emptyList();

	//
	// Not for use by the query client
	//
	private boolean useReportingDataSource;

	public Long getSavedQueryId() {
		return savedQueryId;
	}

	public void setSavedQueryId(Long savedQueryId) {
		this.savedQueryId = savedQueryId;
	}

	public List<Criterion> getCriteria() {
		return criteria;
	}

	public void setCriteria(List<Criterion> criteria) {
		this.criteria = criteria;
	}

	public Criterion getCriterion(int filterId) {
		return criteria.stream().filter(criterion -> criterion.getFilterId() == filterId).findFirst().orElse(null);
	}

	public String getRunType() {
		return runType;
	}

	public void setRunType(String runType) {
		this.runType = runType;
	}

	public String getDrivingForm() {
		return drivingForm;
	}

	public void setDrivingForm(String drivingForm) {
		this.drivingForm = drivingForm;
	}

	public String getWideRowMode() {
		return wideRowMode;
	}

	public void setWideRowMode(String wideRowMode) {
		this.wideRowMode = wideRowMode;
	}

	public int getStartAt() {
		return startAt;
	}

	public void setStartAt(int startAt) {
		this.startAt = startAt;
	}

	public int getMaxResults() {
		return maxResults;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}

	public boolean isUseReportingDataSource() {
		return useReportingDataSource;
	}

	public void setUseReportingDataSource(boolean useReportingDataSource) {
		this.useReportingDataSource = useReportingDataSource;
	}

	public enum SearchType {
		EQUALS, RANGE
	}

	public static class Criterion {
		private int filterId;

		private SearchType searchType = SearchType.EQUALS;

		private List<Object> values = Collections.emptyList();

		public int getFilterId() {
			return filterId;
		}

		public void setFilterId(int filterId) {
			this.filterId = filterId;
		}

		public SearchType getSearchType() {
			return searchType;
		}

		public void setSearchType(SearchType searchType) {
			this.searchType = searchType;
		}

		public List<Object> getValues() {
			return values;
		}

		public void setValues(List<Object> values) {
			this.values = values;
		}
	}
}
