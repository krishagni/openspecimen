package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;

public class SpecimenQueryCriteria extends EntityQueryCriteria {
	private String cpShortTitle;

	private String barcode;

	private boolean includeChildren = true;

	public SpecimenQueryCriteria() {
		super(-1L);
	}

	public SpecimenQueryCriteria(Long id) {
		super(id);
	}

	public SpecimenQueryCriteria(String cpShortTitle, String label) {
		super(label);
		this.cpShortTitle = cpShortTitle;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public boolean isIncludeChildren() {
		return includeChildren;
	}

	public void setIncludeChildren(boolean includeChildren) {
		this.includeChildren = includeChildren;
	}
}
