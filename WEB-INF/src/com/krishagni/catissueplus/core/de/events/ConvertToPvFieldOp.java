package com.krishagni.catissueplus.core.de.events;

public class ConvertToPvFieldOp {
	private Long formId;

	private String name;

	private String attribute;

	private String newAttributeCaption;

	private boolean useFormOptions;

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getNewAttributeCaption() {
		return newAttributeCaption;
	}

	public void setNewAttributeCaption(String newAttributeCaption) {
		this.newAttributeCaption = newAttributeCaption;
	}

	public boolean isUseFormOptions() {
		return useFormOptions;
	}

	public void setUseFormOptions(boolean useFormOptions) {
		this.useFormOptions = useFormOptions;
	}
}
