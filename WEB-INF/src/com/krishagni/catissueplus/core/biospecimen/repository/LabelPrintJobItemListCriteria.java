package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class LabelPrintJobItemListCriteria extends AbstractListCriteria<LabelPrintJobItemListCriteria> {

	private String printerName;

	private Boolean hasContent;

	@Override
	public LabelPrintJobItemListCriteria self() {
		return this;
	}

	public String printerName() {
		return printerName;
	}

	public LabelPrintJobItemListCriteria printerName(String printerName) {
		this.printerName = printerName;
		return self();
	}

	public Boolean hasContent() {
		return hasContent;
	}

	public LabelPrintJobItemListCriteria hasContent(Boolean hasContent) {
		this.hasContent = hasContent;
		return self();
	}
}
