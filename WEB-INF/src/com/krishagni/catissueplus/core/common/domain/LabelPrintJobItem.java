package com.krishagni.catissueplus.core.common.domain;

import java.util.Date;
import java.util.Map;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class LabelPrintJobItem extends BaseEntity {
	public enum Status {
		QUEUED,
		PRINTED,
		EXPIRED
	};
	
	private LabelPrintJob job;
	
	private String itemLabel;

	private Long itemId;

	private int copies;
	
	private Date printDate;
	
	private String printerName;
	
	private Status status;
	
	private String labelType;

	private String labelDesign;
	
	private String data;

	private String content;

	private Boolean createFile;

	private String filename;

	private PrintRuleConfig rule;

	private transient Object object;

	private transient Map<String, String> dataItems;

	public LabelPrintJob getJob() {
		return job;
	}

	public void setJob(LabelPrintJob job) {
		this.job = job;
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

	public Date getPrintDate() {
		return printDate;
	}

	public void setPrintDate(Date printDate) {
		this.printDate = printDate;
	}

	public String getPrinterName() {
		return printerName;
	}

	public void setPrinterName(String printerName) {
		this.printerName = printerName;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
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

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Boolean getCreateFile() {
		return createFile;
	}

	public void setCreateFile(Boolean createFile) {
		this.createFile = createFile;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public PrintRuleConfig getRule() {
		return rule;
	}

	public void setRule(PrintRuleConfig rule) {
		this.rule = rule;
	}

	public Object getObject() {
		return object;
	}

	public void setObject(Object object) {
		this.object = object;
	}

	public Map<String, String> getDataItems() {
		return dataItems;
	}

	public void setDataItems(Map<String, String> dataItems) {
		this.dataItems = dataItems;
	}
}