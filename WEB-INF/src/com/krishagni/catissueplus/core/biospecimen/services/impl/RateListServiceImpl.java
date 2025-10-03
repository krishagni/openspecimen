package com.krishagni.catissueplus.core.biospecimen.services.impl;

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

	private void ensureUniqueServiceCode(LabService existing, LabService service) {
		if (existing != null && existing.getCode().equalsIgnoreCase(service.getCode())) {
			return;
		}

		LabService dbService = daoFactory.getLabServiceDao().getByCode(service.getCode());
		if (dbService != null) {
			throw OpenSpecimenException.userError(LabServiceErrorCode.DUP_CODE, service.getCode());
		}
	}
}
