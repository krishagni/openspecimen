package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import java.util.Calendar;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServicesRateListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServicesRateListFactory;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LabServicesRateListFactoryImpl implements LabServicesRateListFactory {
	@Override
	public LabServicesRateList createRateList(LabServicesRateListDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		LabServicesRateList rateList = new LabServicesRateList();
		rateList.setId(input.getId());
		rateList.setCreator(AuthUtil.getCurrentUser());
		rateList.setCreationTime(Calendar.getInstance().getTime());

		setName(input, rateList, ose);
		setDescription(input, rateList, ose);
		setStartDate(input, rateList, ose);
		setEndDate(input, rateList, ose);
		setActivityStatus(input, rateList, ose);
		ose.checkAndThrow();

		return rateList;
	}

	private void setName(LabServicesRateListDetail input, LabServicesRateList rateList, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(LabServicesRateListErrorCode.NAME_REQ);
		}

		rateList.setName(input.getName());
	}

	private void setDescription(LabServicesRateListDetail input, LabServicesRateList rateList, OpenSpecimenException ose) {
		rateList.setDescription(input.getDescription());
	}

	private void setStartDate(LabServicesRateListDetail input, LabServicesRateList rateList, OpenSpecimenException ose) {
		if (input.getStartDate() == null) {
			ose.addError(LabServicesRateListErrorCode.START_DT_REQ);
		}

		rateList.setStartDate(input.getStartDate());
	}

	private void setEndDate(LabServicesRateListDetail input, LabServicesRateList rateList, OpenSpecimenException ose) {
		rateList.setEndDate(input.getEndDate());
		if (rateList.getStartDate() != null && rateList.getEndDate() != null && rateList.getEndDate().isBefore(rateList.getStartDate())) {
			ose.addError(LabServicesRateListErrorCode.END_DT_LT_START_DT, Utility.getDateString(rateList.getStartDate()), Utility.getDateString(rateList.getEndDate()));
		}
	}

	private void setActivityStatus(LabServicesRateListDetail input, LabServicesRateList rateList, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			rateList.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (!Status.isValidActivityStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		} else {
			rateList.setActivityStatus(input.getActivityStatus());
		}
	}

}
