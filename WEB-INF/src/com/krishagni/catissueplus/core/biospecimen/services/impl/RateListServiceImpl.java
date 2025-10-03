package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServicesRateListFactory;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class RateListServiceImpl implements RateListService {
	private LabServiceFactory serviceFactory;

	private LabServicesRateListFactory rateListFactory;

	private DaoFactory daoFactory;

	public void setServiceFactory(LabServiceFactory serviceFactory) {
		this.serviceFactory = serviceFactory;
	}

	public void setRateListFactory(LabServicesRateListFactory rateListFactory) {
		this.rateListFactory = rateListFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<LabServiceDetail>> getServices(RequestEvent<LabServiceListCriteria> req) {
		try {
			LabServiceListCriteria crit = req.getPayload();
			crit.maxResults(crit.maxResults() > 500 ? 500 : crit.maxResults());
			return ResponseEvent.response(LabServiceDetail.from(daoFactory.getLabServiceDao().getServices(crit)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getServicesCount(RequestEvent<LabServiceListCriteria> req) {
		try {
			return ResponseEvent.response(daoFactory.getLabServiceDao().getServicesCount(req.getPayload()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServiceDetail> getService(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria input = req.getPayload();
			LabService service = getService(input.getId(), input.getName());
			return ResponseEvent.response(LabServiceDetail.from(service));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
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

	@Override
	@PlusTransactional
	public ResponseEvent<LabServiceDetail> deleteService(RequestEvent<EntityQueryCriteria> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			EntityQueryCriteria input = req.getPayload();
			LabService existing = getService(input.getId(), input.getName());
			// TODO:
			// ensureServiceIsNotInUse(service);
			//

			existing.delete();
			return ResponseEvent.response(LabServiceDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServicesRateListDetail> createRateList(RequestEvent<LabServicesRateListDetail> req) {
		try {
			if (!AccessCtrlMgr.getInstance().hasUpdateCpRights()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			LabServicesRateListDetail input = req.getPayload();
			LabServicesRateList rateList = rateListFactory.createRateList(input);
			daoFactory.getLabServiceRateListDao().saveOrUpdate(rateList);
			return ResponseEvent.response(LabServicesRateListDetail.from(rateList));
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
