package com.krishagni.catissueplus.core.importer.repository;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ListImportJobsCriteria extends AbstractListCriteria<ListImportJobsCriteria> {
	
	private List<Long> userIds;
	
	private Long instituteId;

	private String status;

	private List<String> objectTypes;

	private Map<String, String> params;

	private Date fromDate;

	private Date toDate;

	@Override
	public ListImportJobsCriteria self() {
		return this;
	}

	public List<Long> userIds() {
		return userIds;
	}

	public ListImportJobsCriteria userIds(List<Long> userIds) {
		this.userIds = userIds;
		return self();
	}
	
	public ListImportJobsCriteria userId(Long userId) {
		this.userIds = Collections.singletonList(userId);
		return self();
	}

	public Long instituteId() {
		return instituteId;
	}

	public ListImportJobsCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}

	public String status() {
		return status;
	}

	public ListImportJobsCriteria status(String status) {
		this.status = status;
		return self();
	}

	public List<String> objectTypes() {
		return objectTypes;
	}
	
	public ListImportJobsCriteria objectTypes(List<String> objectTypes) {
		this.objectTypes = objectTypes;
		return self();
	}

	public Map<String, String> params() {
		return params;
	}

	public ListImportJobsCriteria params(Map<String, String> params) {
		this.params = params;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	public ListImportJobsCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Date toDate() {
		return toDate;
	}

	public ListImportJobsCriteria toDate(Date toDate) {
		this.toDate = toDate;
		return self();
	}
}
