package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.LabelTmplToken;
import com.krishagni.catissueplus.core.common.domain.PrintItem;

public interface LabelPrinter<T> {
	List<LabelTmplToken> getTokens();

	LabelPrintJob print(List<PrintItem<T>> printItems);
}
