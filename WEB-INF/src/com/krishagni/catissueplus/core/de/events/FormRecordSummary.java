package com.krishagni.catissueplus.core.de.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.UserSummary;

import edu.common.dynamicextensions.napi.ControlValue;

public class FormRecordSummary {
	private Long id;
	
	private Long fcId;

	private boolean sysForm;
	
	private Long recordId;
	
	private UserSummary user;
	
	private Date updateTime;
	
	private List<FieldValue> fieldValues = new ArrayList<>();

	private String entityType;

	private Long formId;

	private String formCaption;

	private String formStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getFcId() {
		return fcId;
	}

	public void setFcId(Long fcId) {
		this.fcId = fcId;
	}

	public boolean isSysForm() {
		return sysForm;
	}

	public void setSysForm(boolean sysForm) {
		this.sysForm = sysForm;
	}

	public Long getRecordId() {
		return recordId;
	}

	public void setRecordId(Long recordId) {
		this.recordId = recordId;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}
	
	public List<FieldValue> getFieldValues() {
		return fieldValues;
	}

	public void setFieldValues(List<FieldValue> fieldValues) {
		this.fieldValues = fieldValues;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public String getFormCaption() {
		return formCaption;
	}

	public void setFormCaption(String formCaption) {
		this.formCaption = formCaption;
	}

	public String getFormStatus() {
		return formStatus;
	}

	public void setFormStatus(String formStatus) {
		this.formStatus = formStatus;
	}

	public void addFieldValue(ControlValue cv) {
		fieldValues.add(FieldValue.from(cv));
	}
	
	public void addFieldValues(Collection<ControlValue> cvs) {
		for (ControlValue cv : cvs) {
			addFieldValue(cv);
		}
	}
}
