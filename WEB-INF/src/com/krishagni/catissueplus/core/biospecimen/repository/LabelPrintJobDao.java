package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.domain.LabelPrintFileItem;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.events.LabelPrintStat;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabelPrintJobDao extends Dao<LabelPrintJob> {
	List<LabelPrintStat> getPrintStats(String type, Date start, Date end);

	void savePrintFileItem(LabelPrintFileItem item);

	List<LabelPrintFileItem> getPrintFileItems(Long jobId, int startAt, int maxItems);

	void deletePrintFileItems(Long jobId);

	void deletePrintFileItemsOlderThan(Date time);
}
