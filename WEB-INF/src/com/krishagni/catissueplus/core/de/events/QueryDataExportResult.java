package com.krishagni.catissueplus.core.de.events;


import java.io.File;
import java.util.concurrent.Future;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class QueryDataExportResult {
	private String dataFile;
	
	private boolean completed;

	private Future<Boolean> promise;

	public String getDataFile() {
		return dataFile;
	}

	public void setDataFile(String dataFile) {
		this.dataFile = dataFile;
	}

	@JsonIgnore
	public File getDataFileHandle() {
		if (StringUtils.isBlank(dataFile)) {
			return null;
		}

		File exportDir = new File(ConfigUtil.getInstance().getDataDir(), "query-exported-data");
		return new File(exportDir, dataFile);
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	@JsonIgnore
	public Future<Boolean> getPromise() {
		return promise;
	}

	public void setPromise(Future<Boolean> promise) {
		this.promise = promise;
	}

	public static QueryDataExportResult create(String dataFile, boolean completed, Future<Boolean> promise) {
		QueryDataExportResult resp = new QueryDataExportResult();
		resp.setCompleted(completed);
		resp.setDataFile(dataFile);
		resp.setPromise(promise);
		return resp;
	}
}
