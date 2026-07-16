package com.krishagni.catissueplus.core.de.events;

import java.util.List;

public class GetFacetValuesOp {
	private String querySpace;

	private Long cpId;

	private Long cpGroupId;

	private List<String> facets;

	private String searchTerm;

	private String restriction;

	private boolean disableAccessChecks;

	//
	// Not for use by the query client
	//
	private boolean useReportingDataSource;

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

	public List<String> getFacets() {
		return facets;
	}

	public void setFacets(List<String> facets) {
		this.facets = facets;
	}

	public String getSearchTerm() {
		return searchTerm;
	}

	public void setSearchTerm(String searchTerm) {
		this.searchTerm = searchTerm;
	}

	public String getRestriction() {
		return restriction;
	}

	public void setRestriction(String restriction) {
		this.restriction = restriction;
	}

	public boolean isDisableAccessChecks() {
		return disableAccessChecks;
	}

	public void setDisableAccessChecks(boolean disableAccessChecks) {
		this.disableAccessChecks = disableAccessChecks;
	}

	public boolean isUseReportingDataSource() {
		return useReportingDataSource;
	}

	public void setUseReportingDataSource(boolean useReportingDataSource) {
		this.useReportingDataSource = useReportingDataSource;
	}
}
