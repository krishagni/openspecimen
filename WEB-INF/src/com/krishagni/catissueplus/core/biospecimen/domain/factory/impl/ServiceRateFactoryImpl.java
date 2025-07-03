package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.biospecimen.domain.ServiceRate;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceRateErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceRateFactory;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ServiceRateFactoryImpl implements ServiceRateFactory {

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public ServiceRate createServiceRate(ServiceRateDetail input) {
		Service service = getService(input.getServiceId(), input.getCpId(), input.getCpShortTitle(), input.getServiceCode());

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		ServiceRate rate = new ServiceRate();
		rate.setId(input.getId());
		rate.setService(service);
		setStartDate(input, rate, ose);
		setEndDate(input, rate, ose);
		setRate(input, rate, ose);
		setActivityStatus(input, rate, ose);
		ose.checkAndThrow();

		return rate;
	}

	private void setStartDate(ServiceRateDetail input, ServiceRate rate, OpenSpecimenException ose) {
		if (input.getStartDate() == null) {
			ose.addError(ServiceRateErrorCode.START_DT_REQ);
		}

		rate.setStartDate(input.getStartDate());
	}

	private void setEndDate(ServiceRateDetail input, ServiceRate rate, OpenSpecimenException ose) {
		if (input.getStartDate() != null && input.getEndDate() != null && input.getEndDate().before(input.getStartDate())) {
			ose.addError(ServiceRateErrorCode.END_DT_LT_START_DT, Utility.getDateString(input.getStartDate()), Utility.getDateString(input.getEndDate()));
		}

		rate.setEndDate(input.getEndDate());
	}

	private void setRate(ServiceRateDetail input, ServiceRate rate, OpenSpecimenException ose) {
		if (input.getRate() == null) {
			ose.addError(ServiceRateErrorCode.REQ);
		}

		rate.setRate(input.getRate());
	}

	private void setActivityStatus(ServiceRateDetail input, ServiceRate service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			service.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (!Status.isValidActivityStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		} else {
			service.setActivityStatus(input.getActivityStatus());
		}
	}


	private Service getService(Long serviceId, Long cpId, String cpShortTitle, String serviceCode) {
		Service service = null;
		Object key = null;

		if (serviceId != null) {
			key = serviceId;
			service = daoFactory.getServiceDao().getById(serviceId);
		} else if (StringUtils.isNotBlank(serviceCode)) {
			if (cpId != null) {
				key = cpId + " / " + serviceCode;
				service = daoFactory.getServiceDao().getService(cpId, serviceCode);
			} else if (StringUtils.isNotBlank(cpShortTitle)) {
				key = cpShortTitle + " / " + serviceCode;
				service = daoFactory.getServiceDao().getService(cpShortTitle, serviceCode);
			}

			if (key == null) {
				throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
			}
		}

		if (key == null) {
			throw OpenSpecimenException.userError(ServiceErrorCode.CODE_REQ);
		} else if (service == null) {
			throw OpenSpecimenException.userError(ServiceErrorCode.NOT_FOUND, key);
		}

		return service;
	}

}
