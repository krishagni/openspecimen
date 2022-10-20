package com.krishagni.catissueplus.core.exporter.domain;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;

public class ExportJob extends BaseEntity {
	public enum Status {
		COMPLETED,
		FAILED,
		IN_PROGRESS,
		STOPPED
	}

	private String name;

	private volatile ExportJob.Status status;

	private Long totalRecords = 0L;

	private User createdBy;

	private Date creationTime;

	private Date endTime;

	private Map<String, String> params = new HashMap<>();

	private String ipAddress;

	private transient ObjectSchema schema;

	private transient List<Long> recordIds;

	private transient boolean disableNotifs;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public ExportJob.Status getStatus() {
		return status;
	}

	public void setStatus(ExportJob.Status status) {
		this.status = status;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public User getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(User createdBy) {
		this.createdBy = createdBy;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Date getEndTime() {
		return endTime;
	}

	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public void param(String name, String value) {
		if (params == null) {
			params = new HashMap<>();
		}

		params.putIfAbsent(name, value);
	}

	public String param(String name) {
		if (params == null) {
			return null;
		}

		return params.get(name);
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public ObjectSchema getSchema() {
		return schema;
	}

	public void setSchema(ObjectSchema schema) {
		this.schema = schema;
	}

	public List<Long> getRecordIds() {
		return recordIds;
	}

	public void setRecordIds(List<Long> recordIds) {
		this.recordIds = recordIds;
	}

	public boolean isDisableNotifs() {
		return disableNotifs;
	}

	public void setDisableNotifs(boolean disableNotifs) {
		this.disableNotifs = disableNotifs;
	}

	public ExportJob markCompleted() {
		setStatus(Status.COMPLETED);
		return this;
	}

	public ExportJob markInProgress() {
		setStatus(Status.IN_PROGRESS);
		return this;
	}

	public ExportJob markFailed() {
		setStatus(Status.FAILED);
		return this;
	}

	public boolean isOutputAccessibleBy(User user) {
		if (user.isAdmin() || getCreatedBy().equals(user)) {
			return true;
		}

		String users = getParams().get("$$users");
		if (StringUtils.isBlank(users)) {
			return false;
		}

		String userIdStr = user.getId().toString();
		return Arrays.stream(users.split(",")).anyMatch(userId -> userId.trim().equals(userIdStr));
	}
}
