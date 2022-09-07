package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

public class MergedObject implements Serializable {
	
	private static final long serialVersionUID = 7778246175343562046L;

	private String key;
	
	private Object object;
	
	private List<List<String>> rows = new ArrayList<List<String>>();
	
	private String errMsg;

	private boolean processed;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
	
	public void merge(Object other) {
		if (!isErrorneous()) {
			((Mergeable) this.object).merge(other);
		}
	}
				
	public void addRow(List<String> row) {
		rows.add(row);
	}
	
	public void addErrMsg(String errMsg) {
		if (errMsg == null) {
			return;
		}
		
		if (StringUtils.isBlank(this.errMsg)) {
			this.errMsg = errMsg;
		} else {
			this.errMsg = new StringBuilder(this.errMsg).append(", ").append(errMsg).toString();
		}
		
		object = null;
	}
	
	public boolean isErrorneous() {
		return StringUtils.isNotBlank(errMsg);
	}

	public boolean isProcessed() {
		return processed;
	}

	public void setProcessed(boolean processed) {
		this.processed = processed;
	}

	public List<String[]> getRowsWithStatus() {
		List<String[]> processedRows = new ArrayList<String[]>();
		for (List<String> row : rows) {
			String status = "", message = "";
			if (isProcessed()) {
				if (isErrorneous()) {
					status = "FAIL";
					message = errMsg;
				} else {
					status = "SUCCESS";
				}
			}

			row.add(status);
			row.add(message);
			processedRows.add(row.toArray(new String[0]));
		}
		
		return processedRows;
	}
}

