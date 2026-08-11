package com.krishagni.catissueplus.core.de.events;

import java.util.Map;

public class UpdateFormPvAttributesOp {
	private Long formId;

	//
	// renamed PV attributes map
	// key -> old name
	// value -> new name
	//
	private Map<String, String> attributes;

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public Map<String, String> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, String> attributes) {
		this.attributes = attributes;
	}
}
