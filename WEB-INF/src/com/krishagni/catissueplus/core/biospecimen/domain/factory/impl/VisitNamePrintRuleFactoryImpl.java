package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.services.impl.VisitLabelPrintRule;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class VisitNamePrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {

	@Override
	public String getItemType() {
		return Visit.getEntityName();
	}

	@Override
	public LabelPrintRule fromRuleDef(Map<String, Object> ruleDef, boolean failOnError, OpenSpecimenException ose) {
		VisitLabelPrintRule rule = new VisitLabelPrintRule();

		setCps(ruleDef, failOnError, rule, ose);
		setVisitSite(ruleDef, failOnError, rule, ose);
		return rule;
	}

	private void setCps(Map<String, Object> inputMap, boolean failOnError, VisitLabelPrintRule rule, OpenSpecimenException ose) {
		List<String> inputList = objToList(inputMap.get("cps"));
		if (inputList.isEmpty()) {
			inputList = objToList(inputMap.get("cpShortTitle")); // backward compatibility
			if (inputList.isEmpty()) {
				return;
			}
		}

		List<CollectionProtocol> result = new ArrayList<>();
		Pair<List<Long>, List<String>> idsAndTitles = getIdsAndNames(inputList);
		if (!idsAndTitles.first().isEmpty()) {
			List<CollectionProtocol> cps = getList(
				(ids) -> daoFactory.getCollectionProtocolDao().getByIds(ids),
				idsAndTitles.first(), CollectionProtocol::getId,
				failOnError ? ose : null, failOnError ? CpErrorCode.DOES_NOT_EXIST : null);
			result.addAll(cps);
		}

		if (!idsAndTitles.second().isEmpty()) {
			List<CollectionProtocol> cps = getList(
				(titles) -> daoFactory.getCollectionProtocolDao().getCpsByShortTitle(titles),
				idsAndTitles.second(), CollectionProtocol::getShortTitle,
				failOnError ? ose : null, failOnError ? CpErrorCode.DOES_NOT_EXIST : null);
			result.addAll(cps);
		}

		rule.setCps(result);
	}

	private void setVisitSite(Map<String, Object> input, boolean failOnError, VisitLabelPrintRule rule, OpenSpecimenException ose) {
		if (isEmptyString(input.get("visitSite"))) {
			return;
		}

		String visitSite = input.get("visitSite").toString();
		Site site = null;
		try {
			site = daoFactory.getSiteDao().getById(Long.parseLong(visitSite));
		} catch (NumberFormatException nfe) {
			site = daoFactory.getSiteDao().getSiteByName(visitSite);
		}

		if (failOnError && site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, visitSite);
		}

		rule.setVisitSite(site);
	}
}
