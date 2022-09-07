package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.ConsentStatement;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ConsentStatementFactory;
import com.krishagni.catissueplus.core.biospecimen.events.ConsentStatementDetail;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;

public class ConsentStatementFactoryImpl implements ConsentStatementFactory {
	
	@Override
	public ConsentStatement createStatement(ConsentStatementDetail detail) {
		ConsentStatement consent = new ConsentStatement();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		setCode(detail, consent, ose);
		setStatement(detail, consent, ose);
		setActivityStatus(detail, consent, ose);
		
		ose.checkAndThrow();
		return consent;
	}
	
	private void setCode(ConsentStatementDetail detail, ConsentStatement consent, OpenSpecimenException ose) {
		String code = detail.getCode();
		if (StringUtils.isBlank(code)) {
			ose.addError(ConsentStatementErrorCode.CODE_REQUIRED);
			return;
		}
		
		consent.setCode(code);
	}
	
	private void setStatement(ConsentStatementDetail detail, ConsentStatement consent, OpenSpecimenException ose) {
		String statement = detail.getStatement();
		if (StringUtils.isBlank(statement)) {
			ose.addError(ConsentStatementErrorCode.REQUIRED);
			return;
		}
		
		consent.setStatement(statement);
	}
	
	private void setActivityStatus(ConsentStatementDetail detail, ConsentStatement consent, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}

		consent.setActivityStatus(activityStatus);
	}
}
