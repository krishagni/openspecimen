package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;

public class LabServiceFactoryImpl implements LabServiceFactory {

	@Override
	public LabService createService(LabServiceDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		LabService service = new LabService();
		service.setId(input.getId());
		setCode(input, service, ose);
		setDescription(input, service, ose);
		setActivityStatus(input, service, ose);
		ose.checkAndThrow();
		return service;
	}

	private void setCode(LabServiceDetail input, LabService service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getCode())) {
			ose.addError(LabServiceErrorCode.CODE_REQ);
		}

		service.setCode(input.getCode());
	}

	private void setDescription(LabServiceDetail input, LabService service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getDescription())) {
			ose.addError(LabServiceErrorCode.DESC_REQ);
		}

		service.setDescription(input.getDescription());
	}

	private void setActivityStatus(LabServiceDetail input, LabService service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			service.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (!Status.isValidActivityStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		} else {
			service.setActivityStatus(input.getActivityStatus());
		}
	}
}
