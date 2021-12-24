package com.krishagni.catissueplus.core.importer.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class ImportJob extends BaseEntity {
	public enum Status {
		COMPLETED,
		FAILED,
		QUEUED,
		IN_PROGRESS,
		STOPPED,
		TXN_SIZE_EXCEEDED
	}
	
	public enum Type {
		CREATE,
		UPDATE,
		UPSERT
	}
	
	public enum CsvType {
		SINGLE_ROW_PER_OBJ,
		MULTIPLE_ROWS_PER_OBJ
	}
	
	private String name; 
	
	private Type type;
	
	private CsvType csvtype;

	private String dateFormat;

	private String timeFormat;

	private String timeZone;

	private String fieldSeparator;

	private volatile Status status;
	
	private Long totalRecords;
	
	private Long failedRecords;
	
	private User createdBy;
	
	private Date creationTime;
	
	private Date endTime;

	private Boolean atomic;

	private String runByNode;

	private volatile Boolean stopRunning = Boolean.FALSE;
	
	private Map<String, String> params = new HashMap<>();
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public CsvType getCsvtype() {
		return csvtype;
	}

	public void setCsvtype(CsvType csvtype) {
		this.csvtype = csvtype;
	}

	public String getDateFormat() {
		return dateFormat;
	}

	public void setDateFormat(String dateFormat) {
		this.dateFormat = dateFormat;
	}

	public String getTimeFormat() {
		return timeFormat;
	}

	public void setTimeFormat(String timeFormat) {
		this.timeFormat = timeFormat;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(String fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Long getTotalRecords() {
		return totalRecords;
	}

	public void setTotalRecords(Long totalRecords) {
		this.totalRecords = totalRecords;
	}

	public Long getFailedRecords() {
		return failedRecords;
	}

	public void setFailedRecords(Long failedRecords) {
		this.failedRecords = failedRecords;
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

	public Boolean getAtomic() {
		return atomic;
	}

	public void setAtomic(Boolean atomic) {
		this.atomic = atomic;
	}

	public String getRunByNode() {
		return runByNode;
	}

	public void setRunByNode(String runByNode) {
		this.runByNode = runByNode;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public Boolean getStopRunning() {
		return stopRunning;
	}

	public void setStopRunning(Boolean stopRunning) {
		this.stopRunning = stopRunning;
	}

	public void stopRunning() {
		stopRunning = true;
	}

	public boolean isAskedToStop() {
		return stopRunning;
	}

	public boolean isQueued() {
		return getStatus() == Status.QUEUED;
	}

	public boolean isInProgress() {
		return getStatus() == Status.IN_PROGRESS;
	}

	public boolean isStopped() {
		return getStatus() == Status.STOPPED;
	}

	public boolean isFailed() {
		return getStatus() == Status.FAILED;
	}
}
