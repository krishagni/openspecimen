package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerLabelPrintRule;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRule;
import com.krishagni.catissueplus.core.common.domain.factory.impl.AbstractLabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class ContainerLabelPrintRuleFactoryImpl extends AbstractLabelPrintRuleFactory {

	@Override
	public String getItemType() {
		return StorageContainer.getEntityName();
	}

	@Override
	public LabelPrintRule fromRuleDef(Map<String, Object> ruleDef, boolean failOnError, OpenSpecimenException ose) {
		ContainerLabelPrintRule rule = new ContainerLabelPrintRule();
		setSite(ruleDef, failOnError, rule, ose);
		return rule;
	}

	private void setSite(Map<String, Object> ruleDef, boolean failOnError, ContainerLabelPrintRule rule, OpenSpecimenException ose) {
		if (isEmptyString(ruleDef.get("site"))) {
			return;
		}

		String input = ruleDef.get("site").toString();
		Site site = null;
		try {
			site = daoFactory.getSiteDao().getById(Long.parseLong(input));
		} catch (NumberFormatException nfe) {
			site = daoFactory.getSiteDao().getSiteByName(input);
		}

		if (failOnError && site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND, input);
		}

		rule.setSite(site);
	}
}
