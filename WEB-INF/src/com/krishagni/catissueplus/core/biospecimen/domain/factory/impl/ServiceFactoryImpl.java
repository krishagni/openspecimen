package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.ServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.events.ServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;

public class ServiceFactoryImpl implements ServiceFactory {
	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public Service createService(ServiceDetail input) {
		Service service = new Service();

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		service.setId(input.getId());
		setCp(input, service, ose);
		setCode(input, service, ose);
		setDescription(input, service, ose);
		setActivityStatus(input, service, ose);
		ose.checkAndThrow();
		return service;
	}

	private void setCp(ServiceDetail input, Service service, OpenSpecimenException ose) {
		CollectionProtocol cp = null;
		Object key = null;

		if (input.getCpId() != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(input.getCpId());
			key = input.getCpId();
		} else if (StringUtils.isNotBlank(input.getCpShortTitle())) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(input.getCpShortTitle());
			key = input.getCpShortTitle();
		}

		if (key == null) {
			ose.addError(CpErrorCode.SHORT_TITLE_REQUIRED);
		} else if (cp == null) {
			ose.addError(CpErrorCode.NOT_FOUND, key);
		}

		service.setCp(cp);
	}

	private void setCode(ServiceDetail input, Service service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getCode())) {
			ose.addError(ServiceErrorCode.CODE_REQ);
		}

		service.setCode(input.getCode());
	}

	private void setDescription(ServiceDetail input, Service service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getDescription())) {
			ose.addError(ServiceErrorCode.DESC_REQ);
		}

		service.setDescription(input.getDescription());
	}

	private void setActivityStatus(ServiceDetail input, Service service, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input.getActivityStatus())) {
			service.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (!Status.isValidActivityStatus(input.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, input.getActivityStatus());
		}

		service.setActivityStatus(input.getActivityStatus());
	}
}
