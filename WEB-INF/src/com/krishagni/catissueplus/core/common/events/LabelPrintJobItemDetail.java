package com.krishagni.catissueplus.core.common.events;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class LabelPrintJobItemDetail {
	private Long id;

	private Long jobId;

	private String itemType;

	private String itemLabel;

	private Long itemId;

	private int copies;

	private Date submitTime;

	private String printerName;

	private String labelType;

	private String labelDesign;

	private String data;

	private List<Map<String, Object>> content;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getJobId() {
		return jobId;
	}

	public void setJobId(Long jobId) {
		this.jobId = jobId;
	}

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}

	public String getItemLabel() {
		return itemLabel;
	}

	public void setItemLabel(String itemLabel) {
		this.itemLabel = itemLabel;
	}

	public Long getItemId() {
		return itemId;
	}

	public void setItemId(Long itemId) {
		this.itemId = itemId;
	}

	public int getCopies() {
		return copies;
	}

	public void setCopies(int copies) {
		this.copies = copies;
	}

	public Date getSubmitTime() {
		return submitTime;
	}

	public void setSubmitTime(Date submitTime) {
		this.submitTime = submitTime;
	}

	public String getPrinterName() {
		return printerName;
	}

	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}

	public String getLabelType() {
		return labelType;
	}

	public void setLabelType(String labelType) {
		this.labelType = labelType;
	}

	public String getLabelDesign() {
		return labelDesign;
	}

	public void setLabelDesign(String labelDesign) {
		this.labelDesign = labelDesign;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}

	public List<Map<String, Object>> getContent() {
		return content;
	}

	public void setContent(List<Map<String, Object>> content) {
		this.content = content;
	}

	public static LabelPrintJobItemDetail from(LabelPrintJobItem item, boolean includeData) {
		LabelPrintJobItemDetail result = new LabelPrintJobItemDetail();
		result.setId(item.getId());
		result.setJobId(item.getJob().getId());
		result.setItemType(item.getJob().getItemType());
		result.setItemLabel(item.getItemLabel());
		result.setItemId(item.getItemId());
		result.setCopies(item.getCopies());
		result.setSubmitTime(item.getJob().getSubmissionDate());
		result.setPrinterName(item.getPrinterName());
		result.setLabelType(item.getLabelType());
		result.setLabelDesign(item.getLabelDesign());

		if (includeData) {
			result.setData(item.getData());
		} else {
			result.setContent(StringUtils.isBlank(item.getContent()) ? null : Utility.jsonToObject(item.getContent(), List.class));
		}
		return result;
	}

	public static List<LabelPrintJobItemDetail> from(List<LabelPrintJobItem> items, boolean includeData) {
		return Utility.nullSafeStream(items).map(item -> from(item, includeData)).collect(Collectors.toList());
	}
}
