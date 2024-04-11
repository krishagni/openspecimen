package com.krishagni.catissueplus.core.audit.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;
import com.krishagni.catissueplus.core.common.util.Utility;

public class RevisionsListCriteria extends AbstractListCriteria<RevisionsListCriteria> {
	private Date startDate;

	private Date endDate;

	private List<Long> userIds;

	private boolean includeModifiedProps;

	private List<String> entities;

	private List<String> reportTypes;

	private String recordType; // core, form, form_data

	private List<String> records; // Core object names or form names

	private List<Long> recordIds;

	@Override
	public RevisionsListCriteria self() {
		return this;
	}

	public Date startDate() {
		return startDate;
	}

	@JsonProperty("startDate")
	public RevisionsListCriteria startDate(Date startDate) {
		this.startDate = startDate;
		return self();
	}

	public Date endDate() {
		return endDate;
	}

	@JsonProperty("endDate")
	public RevisionsListCriteria endDate(Date endDate) {
		this.endDate = endDate;
		return self();
	}

	@JsonProperty("userId")
	public RevisionsListCriteria userId(Long userId) {
		if (userId != null) {
			userIds(Collections.singletonList(userId));
		} else {
			userIds(null);
		}

		return self();
	}

	public List<Long> userIds() {
		return userIds;
	}

	@JsonProperty("userIds")
	public RevisionsListCriteria userIds(List<Long> userIds) {
		this.userIds = userIds;
		return self();
	}

	public boolean includeModifiedProps() {
		return includeModifiedProps;
	}

	@JsonProperty("includeModifiedProps")
	public RevisionsListCriteria includeModifiedProps(boolean includeModifiedProps) {
		this.includeModifiedProps = includeModifiedProps;
		return self();
	}

	public List<String> entities() {
		return entities;
	}

	@JsonProperty("entities")
	public RevisionsListCriteria entities(List<String> entities) {
		this.entities = entities;
		return self();
	}

	public List<String> reportTypes() {
		return reportTypes;
	}

	@JsonProperty("reportTypes")
	public RevisionsListCriteria reportTypes(List<String> reportTypes) {
		this.reportTypes = reportTypes;
		return self();
	}

	public boolean includeReport(String type) {
		return reportTypes != null && reportTypes.contains(type);
	}

	public String recordType() {
		return recordType;
	}

	@JsonProperty("recordType")
	public RevisionsListCriteria recordType(String recordType) {
		this.recordType = recordType;
		return self();
	}

	public List<String> records() {
		return records;
	}

	@JsonProperty("records")
	public RevisionsListCriteria records(List<String> records) {
		this.records = records;
		return self();
	}

	public List<Long> recordIds() {
		return recordIds;
	}

	@JsonProperty("recordIds")
	public RevisionsListCriteria recordIds(List<Long> recordIds) {
		this.recordIds = recordIds;
		return self();
	}

	public String toString() {
		return new StringBuilder().append(super.toString()).append(", ")
			.append("start date = ").append(startDate()).append(", ")
			.append("end date = ").append(endDate()).append(", ")
			.append("users = ").append(userIds()).append(", ")
			.append("include modified props = ").append(includeModifiedProps()).append(", ")
			.append("report types = ").append(StringUtils.join(reportTypes(), ";")).append(", ")
			.append("record type = ").append(recordType()).append(", ")
			.append("records = ").append(StringUtils.join(records(), ";")).append(", ")
			.append("record ids = ").append(StringUtils.join(recordIds(), ";"))
			.toString();
	}

	public Map<String, String> toStrMap() {
		Map<String, String> result = new HashMap<>();
		if (startDate != null) {
			result.put("startDate", Utility.getDateTimeString(startDate));
		}

		if (endDate != null) {
			result.put("endDate", Utility.getDateTimeString(endDate));
		}

		if (userIds != null && !userIds.isEmpty()) {
			result.put("userIds", userIds.stream().map(Object::toString).collect(Collectors.joining(", ")));
		}

		if (reportTypes != null && !reportTypes.isEmpty()) {
			result.put("reportTypes", String.join(", ", reportTypes));
		}

		result.put("includeModifiedProps", String.valueOf(includeModifiedProps));
		if (CollectionUtils.isNotEmpty(entities)) {
			result.put("entities", StringUtils.join(entities, ", "));
		}

		if (CollectionUtils.isNotEmpty(reportTypes)) {
			result.put("reportTypes", StringUtils.join(reportTypes, ", "));
		}

		if (StringUtils.isNotBlank(recordType)) {
			result.put("recordType", recordType);
		}

		if (CollectionUtils.isNotEmpty(records)) {
			result.put("records", StringUtils.join(records, ", "));
		}

		if (CollectionUtils.isNotEmpty(recordIds)) {
			result.put("recordIds", StringUtils.join(recordIds, ", "));
		}

		return result;
	}
}
