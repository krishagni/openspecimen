package com.krishagni.catissueplus.core.common.domain;

import java.io.File;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;


import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LabelPrintJob extends BaseEntity {
	private static final LogUtil logger = LogUtil.getLogger(LabelPrintJob.class);

	private String itemType;
	
	private User submittedBy;
	
	private Date submissionDate;
	
	private Set<LabelPrintJobItem> items = new LinkedHashSet<>();

	public String getItemType() {
		return itemType;
	}

	public void setItemType(String itemType) {
		this.itemType = itemType;
	}
	
	public User getSubmittedBy() {
		return submittedBy;
	}

	public void setSubmittedBy(User submittedBy) {
		this.submittedBy = submittedBy;
	}

	public Date getSubmissionDate() {
		return submissionDate;
	}

	public void setSubmissionDate(Date submissionDate) {
		this.submissionDate = submissionDate;
	}

	public Set<LabelPrintJobItem> getItems() {
		return items;
	}

	public void setItems(Set<LabelPrintJobItem> items) {
		this.items = items;
	}

	public void generateLabelsDataFile() {
		boolean downloadPrintLabelsFile = Objects.requireNonNull(AuthUtil.getCurrentUser()).isDownloadLabelsPrintFile();
		if (!downloadPrintLabelsFile) {
			downloadPrintLabelsFile = ConfigUtil.getInstance().getBoolSetting(
				"administrative", "download_labels_print_file", false);
		}

		if (!downloadPrintLabelsFile) {
			return;
		}

		File jobDir = new File(ConfigUtil.getInstance().getDataDir(), "print-jobs");
		if (!jobDir.exists() && !jobDir.mkdirs()) {
			logger.error("Error creating print jobs directory");
		}

		File output = new File(jobDir, AuthUtil.getCurrentUser().getId() + "_" + getId() + ".csv");
		List<Map<String, String>> labels = getItems().stream()
			.map(item -> Collections.nCopies(item.getCopies(), item.getDataItems()))
			.flatMap(List::stream)
			.collect(Collectors.toList());
		Utility.writeToCsv(output.getAbsolutePath(), labels);
	}
}