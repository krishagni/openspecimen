package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.InstituteFactory;
import com.krishagni.catissueplus.core.administrative.events.InstituteDetail;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;

public class InstituteFactoryImpl implements InstituteFactory {

	@Override
	public Institute createInstitute(InstituteDetail details) {
		Institute institute = new Institute();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		setInstituteName(details, institute, ose);
		setActivityStatus(details, institute, ose);
		
		ose.checkAndThrow();
		return institute;
	}

	private void setInstituteName(InstituteDetail detail, Institute institute, OpenSpecimenException ose) {
		String name = detail.getName();
		if (StringUtils.isBlank(name)) {
			ose.addError(InstituteErrorCode.NAME_REQUIRED);
			return;
		}
		
		institute.setName(name);
	}

	private void setActivityStatus(InstituteDetail detail, Institute institute, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}

		institute.setActivityStatus(activityStatus);
	}
}