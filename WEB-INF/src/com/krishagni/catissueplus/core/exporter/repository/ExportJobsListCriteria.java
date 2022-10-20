package com.krishagni.catissueplus.core.exporter.repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ExportJobsListCriteria extends AbstractListCriteria<ExportJobsListCriteria> {
	private List<Long> userIds;

	private Long instituteId;

	private String status;

	private List<String> objectTypes;

	private Map<String, String> params;

	private Date fromDate;

	private Date toDate;

	@Override
	public ExportJobsListCriteria self() {
		return this;
	}

	public List<Long> userIds() {
		return userIds;
	}

	public ExportJobsListCriteria userIds(List<Long> userIds) {
		this.userIds = userIds;
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public ExportJobsListCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}

	public String status() {
		return status;
	}

	public ExportJobsListCriteria status(String status) {
		this.status = status;
		return self();
	}

	public List<String> objectTypes() {
		return objectTypes;
	}

	public ExportJobsListCriteria objectTypes(List<String> objectTypes) {
		this.objectTypes = objectTypes;
		return self();
	}

	public Map<String, String> params() {
		return params;
	}

	public ExportJobsListCriteria params(Map<String, String> params) {
		this.params = params;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	public ExportJobsListCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Date toDate() {
		return toDate;
	}

	public ExportJobsListCriteria toDate(Date toDate) {
		this.toDate = toDate;
		return self();
	}
}
