package com.krishagni.catissueplus.core.importer.events;

import java.util.HashMap;
import java.util.Map;

public class ImportDetail {
	private String objectType;
	
	private String importType;
	
	private String csvType;

	private String dateFormat;

	private String timeFormat;

	private String timeZone;

	private String fieldSeparator;

	private String inputFileId;

	private Map<String, String> objectParams = new HashMap<>();

	private boolean atomic;

	private boolean offlineQueue;

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public String getImportType() {
		return importType;
	}

	public void setImportType(String importType) {
		this.importType = importType;
	}

	public String getCsvType() {
		return csvType;
	}

	public void setCsvType(String csvType) {
		this.csvType = csvType;
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

	public String getInputFileId() {
		return inputFileId;
	}

	public void setInputFileId(String inputFileId) {
		this.inputFileId = inputFileId;
	}

	public Map<String, String> getObjectParams() {
		return objectParams;
	}

	public void setObjectParams(Map<String, String> objectParams) {
		this.objectParams = objectParams;
	}

	public boolean isAtomic() {
		return atomic;
	}

	public void setAtomic(boolean atomic) {
		this.atomic = atomic;
	}

	public boolean isOfflineQueue() {
		return offlineQueue;
	}

	public void setOfflineQueue(boolean offlineQueue) {
		this.offlineQueue = offlineQueue;
	}
}
