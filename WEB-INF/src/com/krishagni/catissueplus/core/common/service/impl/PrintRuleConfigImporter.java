package com.krishagni.catissueplus.core.common.service.impl;

import com.krishagni.catissueplus.core.common.events.PrintRuleConfigDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.PrintRuleConfigService;
import com.krishagni.catissueplus.core.importer.events.ImportObjectDetail;
import com.krishagni.catissueplus.core.importer.services.ObjectImporter;

public class PrintRuleConfigImporter implements ObjectImporter<PrintRuleConfigDetail, PrintRuleConfigDetail> {
	private PrintRuleConfigService printRuleConfigSvc;

	public void setPrintRuleConfigSvc(PrintRuleConfigService printRuleConfigSvc) {
		this.printRuleConfigSvc = printRuleConfigSvc;
	}

	@Override
	public ResponseEvent<PrintRuleConfigDetail> importObject(RequestEvent<ImportObjectDetail<PrintRuleConfigDetail>> req) {
		ImportObjectDetail<PrintRuleConfigDetail> importDetail = req.getPayload();
		PrintRuleConfigDetail ruleDetail = importDetail.getObject();
		ruleDetail.setObjectType(getObjectType(importDetail.getObjectName()));

		if (ruleDetail.getId() != null) {
			return printRuleConfigSvc.updatePrintRuleConfig(RequestEvent.wrap(ruleDetail));
		} else {
			return printRuleConfigSvc.createPrintRuleConfig(RequestEvent.wrap(ruleDetail));
		}
	}

	private String getObjectType(String objectName) {
		return switch (objectName) {
			case "containerPrintRule" -> "CONTAINER";
			case "orderItemPrintRule" -> "ORDER_ITEM";
			case "visitPrintRule" -> "VISIT";
			case "specimenPrintRule" -> "SPECIMEN";
			default -> null;
		};
	}
}
