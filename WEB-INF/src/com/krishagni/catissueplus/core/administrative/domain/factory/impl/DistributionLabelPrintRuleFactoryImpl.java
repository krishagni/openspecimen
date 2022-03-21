package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.DistributionLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class DistributionLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {
	@Override
	public LabelPrintRule fromRuleDef(Map<String, Object> ruleDef, boolean failOnError, OpenSpecimenException ose) {
		DistributionLabelPrintRule rule = new DistributionLabelPrintRule();
		setDps(ruleDef, failOnError, rule, ose);
		return rule;
	}

	private void setDps(Map<String, Object> ruleDef, boolean failOnError, DistributionLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> inputList = objToList(ruleDef.get("dps"));
		if (inputList.isEmpty()) {
			return;
		}

		List<DistributionProtocol> result = new ArrayList<>();
		Pair<List<Long>, List<String>> idsAndTitles = getIdsAndNames(inputList);

		if (!idsAndTitles.first().isEmpty()) {
			List<DistributionProtocol> dps = getList(
				(ids) -> daoFactory.getDistributionProtocolDao().getByIds(ids),
				idsAndTitles.first(), (dp) -> dp.getId(),
				failOnError ? ose : null, failOnError ? DistributionProtocolErrorCode.INV_DPS : null);
			result.addAll(dps);
		}

		if (!idsAndTitles.second().isEmpty()) {
			List<DistributionProtocol> dps = getList(
				(titles) -> daoFactory.getDistributionProtocolDao().getDistributionProtocols(titles),
				idsAndTitles.second(), (dp) -> dp.getShortTitle(),
				failOnError ? ose : null, failOnError ? DistributionProtocolErrorCode.INV_DPS : null);
			result.addAll(dps);
		}

		rule.setDps(result);
	}
}