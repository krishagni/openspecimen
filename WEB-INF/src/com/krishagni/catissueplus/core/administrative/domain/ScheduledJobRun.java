package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class ScheduledJobRun extends BaseEntity {
	public enum Status {
		IN_PROGRESS,
		SUCCEEDED,
		FAILED
	}

	private static final String ENTITY_NAME = "scheduled_job_run";

	private Date startedAt;
	
	private Date finishedAt;
	
	private Status status;
	
	private String logFilePath;
	
	private String message;
	
	private ScheduledJob scheduledJob;

	private User runBy;
	
	private String rtArgs;

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public Date getStartedAt() {
		return startedAt;
	}

	public void setStartedAt(Date startedAt) {
		this.startedAt = startedAt;
	}

	public Date getFinishedAt() {
		return finishedAt;
	}

	public void setFinishedAt(Date finishedAt) {
		this.finishedAt = finishedAt;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getLogFilePath() {
		return logFilePath;
	}

	public void setLogFilePath(String logFilePath) {
		this.logFilePath = logFilePath;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}
	
	public ScheduledJob getScheduledJob() {
		return scheduledJob;
	}

	public void setScheduledJob(ScheduledJob scheduledJob) {
		this.scheduledJob = scheduledJob;
	}

	public User getRunBy() {
		return runBy;
	}

	public void setRunBy(User runBy) {
		this.runBy = runBy;
	}

	public String getRtArgs() {
		return rtArgs;
	}

	public void setRtArgs(String rtArgs) {
		this.rtArgs = rtArgs;
	}

	public void inProgress(ScheduledJob job) {
		startedAt = Calendar.getInstance().getTime();
		status = Status.IN_PROGRESS;
		scheduledJob = job;
	}
	
	public void completed() {
		completed(null);
	}
	
	public void completed(String logFile) {
		finishedAt = Calendar.getInstance().getTime();
		status = Status.SUCCEEDED;
		
		if (StringUtils.isNotBlank(logFile)) {
			logFilePath = logFile;
		}		
	}
	
	public void failed(Exception e) {
		finishedAt = Calendar.getInstance().getTime();
		status = Status.FAILED;
		
		if (e != null) {
			message = e.getMessage();
		}
	}

	public void update(ScheduledJobRun other) {
		setStatus(other.status);
		setLogFilePath(other.logFilePath);
		setMessage(other.message);
		setStartedAt(other.startedAt);
		setFinishedAt(other.finishedAt);
		setRunBy(other.getRunBy());
	}
}