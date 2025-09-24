package com.krishagni.catissueplus.core.importer.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.importer.domain.ImportJob;

public class ImportJobDetail {
	private Long id;
	
	private String name; 
	
	private String type;
	
	private String status;
	
	private Long totalRecords;
	
	private Long failedRecords;
	
	private UserSummary createdBy;
	
	private Date creationTime;
	
	private Date endTime;
	
	private Map<String, String> params;

	private String displayName;

	private String dateFormat;

	private String timeFormat;

	private String fieldSeparator;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
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

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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

	public String getFieldSeparator() {
		return fieldSeparator;
	}

	public void setFieldSeparator(String fieldSeparator) {
		this.fieldSeparator = fieldSeparator;
	}

	public static ImportJobDetail from(ImportJob job) {
		ImportJobDetail detail = new ImportJobDetail();
		detail.setId(job.getId());
		detail.setName(job.getName());
		detail.setType(job.getType().name());
		detail.setStatus(job.getStatus().name());
		detail.setCreatedBy(UserSummary.from(job.getCreatedBy()));
		detail.setCreationTime(job.getCreationTime());
		detail.setEndTime(job.getEndTime());
		detail.setTotalRecords(job.getTotalRecords());
		detail.setFailedRecords(job.getFailedRecords());
		detail.setParams(job.getParams());
		detail.setDisplayName(job.getDisplayName());
		detail.setDateFormat(job.getDateFormat());
		detail.setTimeFormat(job.getTimeFormat());
		detail.setFieldSeparator(job.getFieldSeparator());
		return detail;
	}
	
	public static List<ImportJobDetail> from(List<ImportJob> jobs) {
		return jobs.stream().map(ImportJobDetail::from).collect(Collectors.toList());
	}

	public static ImportJobDetail status(ImportJob.Status status, int inputRecordsCnt) {
		ImportJobDetail detail = new ImportJobDetail();
		detail.setStatus(status.name());
		detail.setTotalRecords((long) inputRecordsCnt);
		return detail;
	}
}
