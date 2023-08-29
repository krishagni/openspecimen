package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.biospecimen.repository.LabelPrintJobItemListCriteria;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobItemDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;

public interface LabelPrintJobService {
	ResponseEvent<List<LabelPrintJobItemDetail>> getPrintJobItems(RequestEvent<LabelPrintJobItemListCriteria> req);
}
