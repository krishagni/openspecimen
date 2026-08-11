package com.krishagni.catissueplus.core.administrative.events;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ListPvAttributesCriteria extends AbstractListCriteria<ListPvAttributesCriteria> {
	private String attribute;

	private String pv;

	private Long formId;

	private Boolean formScoped;

	@Override
	public ListPvAttributesCriteria self() {
		return this;
	}

	public String attribute() {
		return attribute;
	}

	public ListPvAttributesCriteria attribute(String attribute) {
		this.attribute = attribute;
		return self();
	}

	public String pv() {
		return pv;
	}

	public ListPvAttributesCriteria pv(String pv) {
		this.pv = pv;
		return self();
	}

	public Long formId() {
		return formId;
	}

	public ListPvAttributesCriteria formId(Long formId) {
		this.formId = formId;
		return self();
	}

	public Boolean formScoped() {
		return formScoped;
	}

	public ListPvAttributesCriteria formScoped(Boolean formScoped) {
		this.formScoped = formScoped;
		return self();
	}
}
