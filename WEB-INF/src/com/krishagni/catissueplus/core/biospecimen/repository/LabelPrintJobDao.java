package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJobItem;
import com.krishagni.catissueplus.core.common.events.LabelPrintStat;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface LabelPrintJobDao extends Dao<LabelPrintJob> {
	List<LabelPrintStat> getPrintStats(String type, Date start, Date end);

	List<LabelPrintJobItem> getPrintJobItems(Long jobId, int startAt, int maxItems);

	List<LabelPrintJobItem> getPrintJobItems(LabelPrintJobItemListCriteria criteria);
}
