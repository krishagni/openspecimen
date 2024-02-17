package com.krishagni.catissueplus.core.importer.events;

import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.importer.domain.ObjectSchema;

public class FileRecordsDetail {
	private String fileId;

	private List<ObjectSchema.Field> fields;

	private String schema;

	private Map<String, String> params;

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public List<ObjectSchema.Field> getFields() {
		return fields;
	}

	public void setFields(List<ObjectSchema.Field> fields) {
		this.fields = fields;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	public Map<String, String> getParams() {
		return params;
	}

	public void setParams(Map<String, String> params) {
		this.params = params;
	}
}
