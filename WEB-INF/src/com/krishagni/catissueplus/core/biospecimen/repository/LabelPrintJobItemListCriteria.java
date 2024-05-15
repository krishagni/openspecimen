package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class LabelPrintJobItemListCriteria extends AbstractListCriteria<LabelPrintJobItemListCriteria> {

	private List<String> printerNames;

	private Boolean hasContent;

	private Date fromDate;

	private Boolean includeData;

	@Override
	public LabelPrintJobItemListCriteria self() {
		return this;
	}

	public List<String> printerNames() {
		return printerNames;
	}

	public LabelPrintJobItemListCriteria printerNames(List<String> printerNames) {
		this.printerNames = printerNames;
		return self();
	}

	public Boolean hasContent() {
		return hasContent;
	}

	public LabelPrintJobItemListCriteria hasContent(Boolean hasContent) {
		this.hasContent = hasContent;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	public LabelPrintJobItemListCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Boolean includeData() {
		return includeData;
	}

	public LabelPrintJobItemListCriteria includeData(Boolean includeData) {
		this.includeData = includeData;
		return self();
	}
}
