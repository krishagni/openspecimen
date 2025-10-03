package com.krishagni.catissueplus.core.biospecimen.services.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;

public class RateListServiceImpl implements RateListService {
	private LabServiceFactory serviceFactory;

	private DaoFactory daoFactory;

	public void setServiceFactory(LabServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServiceDetail> createService(RequestEvent<LabServiceDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			LabService service = serviceFactory.createService(req.getPayload());
			if (!service.isActive()) {
				return ResponseEvent.userError(ActivityStatusErrorCode.INVALID, service.getActivityStatus());
			}

			ensureUniqueServiceCode(null, service);
			daoFactory.getLabServiceDao().saveOrUpdate(service);
			return ResponseEvent.response(LabServiceDetail.from(service));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServiceDetail> updateService(RequestEvent<LabServiceDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			LabServiceDetail input = req.getPayload();
			LabService existing = getService(input.getId(), input.getCode());
			LabService service = serviceFactory.createService(input);
			ensureUniqueServiceCode(existing, service);
			if (Status.isDisabledStatus(service.getActivityStatus())) {
				// TODO:
				// ensureServiceIsNotInUse(service);
				//
			}

			existing.update(service);
			return ResponseEvent.response(LabServiceDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureUniqueServiceCode(LabService existing, LabService service) {
		if (existing != null && existing.getCode().equalsIgnoreCase(service.getCode())) {
			return;
		}

		LabService dbService = daoFactory.getLabServiceDao().getByCode(service.getCode());
		if (dbService != null) {
			throw OpenSpecimenException.userError(LabServiceErrorCode.DUP_CODE, service.getCode());
		}
	}

	private LabService getService(Long id, String code) {
		Object key = null;
		LabService service = null;

		if (id != null) {
			service = daoFactory.getLabServiceDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(code)) {
			service = daoFactory.getLabServiceDao().getByCode(code);
			key = code;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(LabServiceErrorCode.CODE_REQ);
		} else if (service == null) {
			throw OpenSpecimenException.userError(LabServiceErrorCode.NOT_FOUND, key);
		}

		return service;
	}
}
