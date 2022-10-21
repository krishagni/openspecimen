package com.krishagni.catissueplus.core.audit.repository;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;
import com.krishagni.catissueplus.core.common.util.Utility;

public class RevisionsListCriteria extends AbstractListCriteria<RevisionsListCriteria> {

	private Date startDate;

	private Date endDate;

	private List<Long> userIds;

	private boolean includeModifiedProps;

	private List<String> entities;

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

	public String toString() {
		return new StringBuilder().append(super.toString()).append(", ")
			.append("start date = ").append(startDate()).append(", ")
			.append("end date = ").append(endDate()).append(", ")
			.append("users = ").append(userIds()).append(", ")
			.append("include modified props = ").append(includeModifiedProps())
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
			result.put("userIds", userIds.stream().map(id -> id.toString()).collect(Collectors.joining(", ")));
		}

		return result;
	}
}
