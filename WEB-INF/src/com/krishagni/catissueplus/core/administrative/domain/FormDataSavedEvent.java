package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

import edu.common.dynamicextensions.napi.FormData;

public class FormDataSavedEvent extends OpenSpecimenEvent<FormData> {

	private static final long serialVersionUID = 5255220925616156515L;

	private String entityType;

	private Object object;

	public FormDataSavedEvent(String entityType, Object object, FormData eventData) {
		super(null, eventData);
		this.entityType = entityType;
		this.object = object;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}
}
