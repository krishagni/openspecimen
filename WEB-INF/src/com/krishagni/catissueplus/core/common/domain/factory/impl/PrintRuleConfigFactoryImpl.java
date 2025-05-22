package com.krishagni.catissueplus.core.common.domain.factory.impl;


import java.util.Calendar;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.domain.LabelPrintRuleFactoryRegistrar;
import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;
import com.krishagni.catissueplus.core.common.domain.factory.LabelPrintRuleFactory;
import com.krishagni.catissueplus.core.common.domain.factory.PrintRuleConfigFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.PrintRuleConfigErrorCode;
import com.krishagni.catissueplus.core.common.events.PrintRuleConfigDetail;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class PrintRuleConfigFactoryImpl implements PrintRuleConfigFactory {
	private DaoFactory daoFactory;

	private LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setLabelPrintRuleFactoryRegistrar(LabelPrintRuleFactoryRegistrar labelPrintRuleFactoryRegistrar) {
		this.labelPrintRuleFactoryRegistrar = labelPrintRuleFactoryRegistrar;
	}

	@Override
	public PrintRuleConfig createPrintRuleConfig(PrintRuleConfigDetail detail) {
		PrintRuleConfig rule = new PrintRuleConfig();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		rule.setUpdatedBy(AuthUtil.getCurrentUser());
		rule.setUpdatedOn(Calendar.getInstance().getTime());

		setObjectType(detail, rule, ose);
		setInstitute(detail, rule, ose);
		setActivityStatus(detail, rule, ose);
		setDescription(detail, rule, ose);
		setSortOrder(detail, rule, ose);
		setRule(detail, rule, ose);
		ose.checkAndThrow();
		return rule;
	}

	private void setObjectType(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getObjectType())) {
			ose.addError(PrintRuleConfigErrorCode.OBJECT_TYPE_REQ);
			return;
		}

		rule.setObjectType(detail.getObjectType());
	}

	private void setInstitute(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		Institute institute = null;
		Object key = null;
		if (detail.getInstituteId() != null) {
			institute = daoFactory.getInstituteDao().getById(detail.getInstituteId());
			key = detail.getInstituteId();
		} else if (StringUtils.isNotBlank(detail.getInstituteName())) {
			institute = daoFactory.getInstituteDao().getInstituteByName(detail.getInstituteName());
			key = detail.getInstituteName();
		}

		if (institute == null) {
			if (key != null) {
				ose.addError(InstituteErrorCode.NOT_FOUND, key);
			}

			return;
		}

		rule.setInstitute(institute);
	}

	private void setActivityStatus(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			rule.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}

		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}

		rule.setActivityStatus(activityStatus);
	}

	private void setDescription(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getDescription())) {
			ose.addError(PrintRuleConfigErrorCode.DESC_REQ);
			return;
		}

		rule.setDescription(detail.getDescription());
	}

	private void setSortOrder(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		if (detail.getSortOrder() == null) {
			rule.setSortOrder(null);
		} else if (detail.getSortOrder() >= 0) {
			rule.setSortOrder(detail.getSortOrder());
		} else {
			ose.addError(PrintRuleConfigErrorCode.INV_SORT_ORDER, detail.getSortOrder());
		}
	}

	private void setRule(PrintRuleConfigDetail detail, PrintRuleConfig rule, OpenSpecimenException ose) {
		Map<String, Object> ruleMap = detail.getRule();
		if (ruleMap == null || ruleMap.isEmpty()) {
			ose.addError(PrintRuleConfigErrorCode.RULES_REQ);
			return;
		}

		LabelPrintRuleFactory factory = labelPrintRuleFactoryRegistrar.getFactory(detail.getObjectType());
		if (factory == null) {
			ose.addError(PrintRuleConfigErrorCode.INVALID_OBJECT_TYPE, detail.getObjectType());
			return;
		}

		rule.setRule(factory.createLabelPrintRule(ruleMap));
	}
}