package com.krishagni.catissueplus.core.common.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigDao;
import com.krishagni.catissueplus.core.common.repository.PrintRuleConfigsListCriteria;

public class PrintRuleConfigDaoImpl extends AbstractDao<PrintRuleConfig> implements PrintRuleConfigDao {
	@Override
	public Class<?> getType() {
		return PrintRuleConfig.class;
	}

	@Override
	public List<PrintRuleConfig> getPrintRules(PrintRuleConfigsListCriteria crit) {
		Criteria<PrintRuleConfig> query = getPrintRulesConfigListQuery(crit);
		return query.orderBy(query.desc("pr.id")).list(crit.startAt(), crit.maxResults());
	}

	private Criteria<PrintRuleConfig> getPrintRulesConfigListQuery(PrintRuleConfigsListCriteria crit) {
		return addSearchConditions(createCriteria(PrintRuleConfig.class, "pr"), crit);
	}

	private Criteria<PrintRuleConfig> addSearchConditions(Criteria<PrintRuleConfig> query, PrintRuleConfigsListCriteria crit) {
		addObjectTypeRestriction(query, crit.objectType());
		addDescriptionRestriction(query, crit.query());
		addUserNameRestriction(query, crit.userName());
		addInstituteRestriction(query, crit.instituteName());
		return query;
	}

	private void addObjectTypeRestriction(Criteria<PrintRuleConfig> query, String objectType) {
		if (StringUtils.isBlank(objectType)) {
			return;
		}

		query.add(query.eq("pr.objectType", objectType));
	}

	private void addDescriptionRestriction(Criteria<PrintRuleConfig> query, String description) {
		if (StringUtils.isBlank(description)) {
			return;
		}

		query.add(query.like("pr.description", description));
	}

	private void addUserNameRestriction(Criteria<PrintRuleConfig> query, String name) {
		if (StringUtils.isBlank(name)) {
			return;
		}

		query.join("pr.updatedBy", "u")
			.add(query.disjunction()
				.add(query.ilike("u.firstName", name))
				.add(query.ilike("u.lastName",  name))
				.add(query.ilike("u.loginName", name))
			);
	}

	private void addInstituteRestriction(Criteria<PrintRuleConfig> query, String instituteName) {
		if (StringUtils.isBlank(instituteName)) {
			return;
		}

		query.join("pr.institute", "institute")
			.add(query.eq("institute.name", instituteName));
	}
}
