package com.krishagni.catissueplus.core.importer.events;

import java.util.HashMap;
import java.util.Map;

public class ImportObjectDetail<T> {
	private String objectName;

	private String id;

	private T object;
	
	private boolean create;

	private String type;
	
	private Map<String, String> params = new HashMap<>();

	private String uploadedFilesDir;

	public String getObjectName() {
		return objectName;
	}

	public void setObjectName(String objectName) {
		this.objectName = objectName;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public T getObject() {
		return object;
	}

	public void setObject(T object) {
		this.object = object;
	}

	public boolean isCreate() {
		return create;
	}

	public void setCreate(boolean create) {
		this.create = create;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Map<String, String> getParams() {
		return params;
	}
	
	public void setParams(Map<String, String> params) {
		this.params = params;
	}

	public String getUploadedFilesDir() {
		return uploadedFilesDir;
	}

	public void setUploadedFilesDir(String uploadedFilesDir) {
		this.uploadedFilesDir = uploadedFilesDir;
	}
}
