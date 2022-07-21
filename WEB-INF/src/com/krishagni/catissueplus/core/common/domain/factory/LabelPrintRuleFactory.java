package com.krishagni.catissueplus.core.common.domain.factory;

import java.util.Map;

import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;

public interface LabelPrintRuleFactory {
	String getItemType();

	LabelPrintRule createLabelPrintRule(Map<String, Object> ruleDef);

	LabelPrintRule createLabelPrintRule(Map<String, Object> ruleDef, boolean failOnError);
}
