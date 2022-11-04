package com.krishagni.catissueplus.core.de.events;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;
import com.krishagni.catissueplus.core.common.util.Utility;

public class QueryAuditLogsListCriteria extends AbstractListCriteria<QueryAuditLogsListCriteria> {
	private Date startDate;

	private Date endDate;

	private List<Long> userIds;

	private Long instituteId;

	@JsonProperty("startDate")
	public QueryAuditLogsListCriteria startDate(Date startDate) {
		this.startDate = startDate;
		return self();
	}

	public Date startDate() {
		return startDate;
	}

	@JsonProperty("endDate")
	public QueryAuditLogsListCriteria endDate(Date endDate) {
		this.endDate = endDate;
		return self();
	}

	public Date endDate() {
		return endDate;
	}

	@JsonProperty("userId")
	public QueryAuditLogsListCriteria userId(Long userId) {
		if (userId != null) {
			this.userIds = Collections.singletonList(userId);
		} else {
			this.userIds = Collections.emptyList();
		}

		return self();
	}

	@JsonProperty("userIds")
	public QueryAuditLogsListCriteria userIds(List<Long> userIds) {
		this.userIds = userIds;
		return self();
	}

	public List<Long> userIds() {
		return userIds;
	}

	@JsonProperty("instituteId")
	public Long instituteId() {
		return instituteId;
	}

	public QueryAuditLogsListCriteria instituteId(Long instituteId) {
		this.instituteId = instituteId;
		return self();
	}

	@Override
	public QueryAuditLogsListCriteria self() {
		return this;
	}

	@Override
	public String toString() {
		return new StringBuilder().append(super.toString()).append(", ")
			.append("start date = ").append(startDate()).append(", ")
			.append("end date = ").append(endDate()).append(", ")
			.append("users = ").append(userIds()).append(", ")
			.append("institute = ").append(instituteId())
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

		if (CollectionUtils.isNotEmpty(userIds)) {
			result.put("userIds", userIds.stream().map(id -> id.toString()).collect(Collectors.joining(", ")));
		}

		if (instituteId != null) {
			result.put("instituteId", instituteId.toString());
		}

		return result;
	}
}
