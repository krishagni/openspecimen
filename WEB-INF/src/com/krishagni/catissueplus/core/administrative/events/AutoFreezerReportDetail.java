package com.krishagni.catissueplus.core.administrative.events;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.common.util.Utility;

public class AutoFreezerReportDetail {
	private Date date;

	private List<Long> failedStoreListIds;

	private File report;

	private int stored;

	private int retrieved;

	private int failedLists;

	private int failedStores;

	private int failedRetrieves;

	public AutoFreezerReportDetail() {
		date = Calendar.getInstance().getTime();
	}

	public AutoFreezerReportDetail(List<Long> failedStoreListIds) {
		this.failedStoreListIds = failedStoreListIds;
	}

	public Date getDate() {
		return date;
	}

	public void setDate(Date date) {
		this.date = date;
	}

	public Date getFromDate() {
		return Utility.chopTime(date);
	}

	public Date getToDate() {
		return Utility.getEndOfDay(date);
	}

	public List<Long> getFailedStoreListIds() {
		return failedStoreListIds;
	}

	public void setFailedStoreListIds(List<Long> failedStoreListIds) {
		this.failedStoreListIds = failedStoreListIds;
	}

	public boolean reportForFailedOps() {
		return CollectionUtils.isNotEmpty(failedStoreListIds);
	}

	public File getReport() {
		return report;
	}

	public void setReport(File report) {
		this.report = report;
	}

	public String getReportFilename() {
		return report != null ? report.getName() : null;
	}

	public int getStored() {
		return stored;
	}

	public void setStored(int stored) {
		this.stored = stored;
	}

	public int getRetrieved() {
		return retrieved;
	}

	public void setRetrieved(int retrieved) {
		this.retrieved = retrieved;
	}

	public int getFailedLists() {
		return failedLists;
	}

	public void setFailedLists(int failedLists) {
		this.failedLists = failedLists;
	}

	public int getFailedStores() {
		return failedStores;
	}

	public void setFailedStores(int failedStores) {
		this.failedStores = failedStores;
	}

	public int getFailedRetrieves() {
		return failedRetrieves;
	}

	public void setFailedRetrieves(int failedRetrieves) {
		this.failedRetrieves = failedRetrieves;
	}

	public boolean hasAnyActivity() {
		return getStored() > 0 || getRetrieved() > 0 || getFailedStores() > 0 || getFailedRetrieves() > 0;
	}

	public Map<String, Object> toMap() {
		Map<String, Object> result = new HashMap<>();
		result.put("stored", getStored());
		result.put("retrieved", getRetrieved());
		result.put("failedStores", getFailedStores());
		result.put("failedRetrieves", getFailedRetrieves());
		result.put("reportFile", getReport());
		result.put("reportFilename", getReportFilename());
		result.put("reportForFailedOps", reportForFailedOps());
		return result;
	}
}
