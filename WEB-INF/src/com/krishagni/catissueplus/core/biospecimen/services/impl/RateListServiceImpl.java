package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRate;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServiceRateListCp;
import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServiceFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServicesRateListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.LabServicesRateListFactory;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServiceRateDetail;
import com.krishagni.catissueplus.core.biospecimen.events.LabServicesRateListDetail;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListCollectionProtocolsOp;
import com.krishagni.catissueplus.core.biospecimen.events.UpdateRateListServicesOp;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.ParameterizedError;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
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

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> updateRateListServices(RequestEvent<UpdateRateListServicesOp> req) {
		try {
			UpdateRateListServicesOp op = req.getPayload();
			LabServicesRateList rateList = getRateList(op.getRateListId());

			//
			// TODO: Access check
			// TODO: Check whether rate list can be updated by the user
			//

			List<LabServiceRateDetail> serviceRates = op.getServiceRates();
			if (CollectionUtils.isEmpty(serviceRates)) {
				return ResponseEvent.response(0);
			}

			int count = 0;
			if (op.getOp() == null || op.getOp() == UpdateRateListServicesOp.Op.UPSERT) {
				count = upsertServiceRates(rateList, serviceRates);
				raiseErrorIfOverlappingServices(rateList);
			} else if (op.getOp() == UpdateRateListServicesOp.Op.DELETE) {
				count = deleteServiceRates(rateList, serviceRates);
			}

			return ResponseEvent.response(count);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> updateRateListCps(RequestEvent<UpdateRateListCollectionProtocolsOp> req) {
		try {
			UpdateRateListCollectionProtocolsOp op = req.getPayload();
			LabServicesRateList rateList = getRateList(op.getRateListId());

			List<CollectionProtocolSummary> cps = op.getCps();
			if (CollectionUtils.isEmpty(cps)) {
				return ResponseEvent.response(0);
			}

			int idx = 0;
			Set<Long> validatedCpIds = new LinkedHashSet<>();
			Map<Long, CollectionProtocol> cpsMap = new HashMap<>();
			OpenSpecimenException userErrors = new OpenSpecimenException(ErrorType.USER_ERROR);
			for (CollectionProtocolSummary inputCp : cps) {
				++idx;
				List<ParameterizedError> errors = new ArrayList<>();
				try {
					CollectionProtocol cp = getCollectionProtocol(inputCp);
					cpsMap.put(cp.getId(), cp);

					if (AccessCtrlMgr.getInstance().hasUpdateCpRights(cp)) {
						validatedCpIds.add(cp.getId());
					} else {
						errors.add(new ParameterizedError(RbacErrorCode.ACCESS_DENIED, null));
					}
				} catch (OpenSpecimenException e) {
					errors.addAll(e.getErrors());
				}

				addItemErrorsIfPresent(idx, errors, userErrors);
			}

			userErrors.checkAndThrow();

			int count = 0;
			if (op.getOp() == null || op.getOp() == UpdateRateListCollectionProtocolsOp.Op.ADD) {
				List<Long> preAssociatedCpIds = getRateListAssociatedCps(rateList, validatedCpIds);
				preAssociatedCpIds.forEach(validatedCpIds::remove);
				count = associateRateListCps(cpsMap, rateList, validatedCpIds);
				raiseErrorIfOverlappingServices(rateList);
			} else if (op.getOp() == UpdateRateListCollectionProtocolsOp.Op.RM) {
				count = removeRateListCpAssociations(rateList, validatedCpIds);
			}

			return ResponseEvent.response(count);
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

	private LabServicesRateList getRateList(Long rateListId) {
		LabServicesRateList rateList = daoFactory.getLabServiceRateListDao().getById(rateListId);
		if (rateList == null) {
			throw OpenSpecimenException.userError(LabServicesRateListErrorCode.NOT_FOUND, rateListId);
		}

		return rateList;
	}

	private int upsertServiceRates(LabServicesRateList rateList, List<LabServiceRateDetail> serviceRates) {
		Tuple maps = rateList.getServiceRateLookupMaps();
		Map<Long, LabServiceRate> serviceRatesByIdMap = maps.element(0);
		Map<Long, LabServiceRate> serviceRatesBySvcIdMap = maps.element(1);
		Map<String, LabServiceRate> serviceRatesBySvcCodeMap = maps.element(2);

		int idx = 0;
		OpenSpecimenException userErrors = new OpenSpecimenException(ErrorType.USER_ERROR);
		for (LabServiceRateDetail inputSvcRate : serviceRates) {
			++idx;

			List<ParameterizedError> errors = new ArrayList<>();
			try {
				if (inputSvcRate.getRate() == null) {
					errors.add(new ParameterizedError(LabServicesRateListErrorCode.SVC_RATE_REQ, null));
				} else if (NumUtil.lessThanZero(inputSvcRate.getRate())) {
					errors.add(new ParameterizedError(LabServicesRateListErrorCode.INV_SVC_RATE, inputSvcRate.getRate()));
				}

				if (inputSvcRate.getId() != null) {
					LabServiceRate existing = serviceRatesByIdMap.get(inputSvcRate.getId());
					if (existing == null) {
						errors.add(new ParameterizedError(LabServicesRateListErrorCode.SVC_RATE_NF, inputSvcRate.getId()));
					} else {
						existing.setRate(inputSvcRate.getRate());
					}
				} else if (inputSvcRate.getServiceId() != null || StringUtils.isNotBlank(inputSvcRate.getServiceCode())) {
					LabServiceRate existing = null;
					if (inputSvcRate.getServiceId() != null) {
						existing = serviceRatesBySvcIdMap.get(inputSvcRate.getServiceId());
					} else {
						existing = serviceRatesBySvcCodeMap.get(inputSvcRate.getServiceCode());
					}

					if (existing == null) {
						LabService service = getService(inputSvcRate.getServiceId(), inputSvcRate.getServiceCode());
						existing = new LabServiceRate();
						existing.setService(service);
						existing.setRate(inputSvcRate.getRate());
						existing.setRateList(rateList);
						rateList.getServiceRates().add(existing);
						serviceRatesBySvcIdMap.put(service.getId(), existing);
						serviceRatesBySvcCodeMap.put(service.getCode(), existing);
					} else {
						existing.setRate(inputSvcRate.getRate());
					}
				} else {
					errors.add(new ParameterizedError(LabServicesRateListErrorCode.SVC_CODE_REQ, null));
				}
			} catch (OpenSpecimenException ex) {
				errors.addAll(ex.getErrors());
			}

			addItemErrorsIfPresent(idx, errors, userErrors);
		}

		userErrors.checkAndThrow();
		return idx;
	}

	private int deleteServiceRates(LabServicesRateList rateList, List<LabServiceRateDetail> serviceRates) {
		Tuple lookupMaps = rateList.getServiceRateLookupMaps();
		Map<Long, LabServiceRate> serviceRatesByIdMap = lookupMaps.element(0);
		Map<Long, LabServiceRate> serviceRatesBySvcIdMap = lookupMaps.element(1);
		Map<String, LabServiceRate> serviceRatesBySvcCodeMap = lookupMaps.element(2);

		int idx = 0;
		OpenSpecimenException userErrors = new OpenSpecimenException(ErrorType.USER_ERROR);
		for (LabServiceRateDetail inputSvcRate : serviceRates) {
			++idx;

			List<ParameterizedError> errors = new ArrayList<>();
			try {
				Object key = null;
				LabServiceRate existing = null;
				if (inputSvcRate.getId() != null) {
					existing = serviceRatesByIdMap.get(inputSvcRate.getId());
					key = inputSvcRate.getId();
				} else if (inputSvcRate.getServiceId() != null) {
					existing = serviceRatesBySvcIdMap.get(inputSvcRate.getServiceId());
					key = inputSvcRate.getServiceId();
				} else if (StringUtils.isNotBlank(inputSvcRate.getServiceCode())) {
					existing = serviceRatesBySvcCodeMap.get(inputSvcRate.getServiceCode());
					key = inputSvcRate.getServiceCode();
				}

				if (key == null) {
					errors.add(new ParameterizedError(LabServicesRateListErrorCode.SVC_CODE_REQ, null));
				} else if (existing == null) {
					errors.add(new ParameterizedError(LabServicesRateListErrorCode.SVC_RATE_NF, key));
				} else {
					rateList.getServiceRates().remove(existing);
				}
			} catch (OpenSpecimenException ex) {
				errors.addAll(ex.getErrors());
			}

			addItemErrorsIfPresent(idx, errors, userErrors);
		}

		userErrors.checkAndThrow();
		return idx;
	}

	private void raiseErrorIfOverlappingServices(LabServicesRateList rateList) {
		daoFactory.getLabServiceRateListDao().flush();

		List<Object[]> overlappingServiceRates = daoFactory.getLabServiceRateListDao().getOverlappingServiceRates(rateList);
		if (overlappingServiceRates.isEmpty()) {
			return;
		}

		Map<String, List<String>> services = new LinkedHashMap<>();
		for (Object[] serviceRate : overlappingServiceRates) {
			String rateListCp = "#" + serviceRate[0] + " " + serviceRate[1] + ", " + serviceRate[2];
			services.computeIfAbsent(rateListCp, (k) -> new ArrayList<>()).add((String) serviceRate[3]);
		}

		List<String> args = new ArrayList<>();
		for (Map.Entry<String, List<String>> service : services.entrySet()) {
			args.add(service.getKey() + ": " + String.join(" / ", service.getValue()));
		}

		throw OpenSpecimenException.userError(LabServicesRateListErrorCode.SVCS_OVERLAP, String.join("; ", args));
	}

	private CollectionProtocol getCollectionProtocol(CollectionProtocolSummary input) {
		CollectionProtocol cp = null;
		Object key = null;
		if (input.getId() != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(input.getId());
			key = input.getId();
		} else if (StringUtils.isNotBlank(input.getShortTitle())) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(input.getShortTitle());
			key = input.getShortTitle();
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CpErrorCode.SHORT_TITLE_REQUIRED);
		} else if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND, key);
		}

		return cp;
	}

	private List<Long> getRateListAssociatedCps(LabServicesRateList rateList, Collection<Long> cpIds) {
		return daoFactory.getLabServiceRateListDao().getAssociatedCpIds(rateList.getId(), cpIds);
	}

	private int associateRateListCps(Map<Long, CollectionProtocol> cpsMap, LabServicesRateList rateList, Collection<Long> cpIds) {
		for (Long cpId : cpIds) {
			LabServiceRateListCp rateListCp = new LabServiceRateListCp();
			rateListCp.setRateList(rateList);
			rateListCp.setCp(cpsMap.get(cpId));
			daoFactory.getLabServiceRateListDao().saveRateListCp(rateListCp);
		}

		return cpIds.size();
	}

	private int removeRateListCpAssociations(LabServicesRateList rateList, Collection<Long> cpIds) {
		List<LabServiceRateListCp> rateListCps = daoFactory.getLabServiceRateListDao().getRateListCps(rateList.getId(), cpIds);
		for (LabServiceRateListCp rateListCp : rateListCps) {
			daoFactory.getLabServiceRateListDao().deleteRateListCp(rateListCp);
		}

		return rateListCps.size();
	}

	private void addItemErrorsIfPresent(int idx, List<ParameterizedError> errors, OpenSpecimenException userErrors) {
		if (errors.isEmpty()) {
			return;
		}

		StringBuilder errorMsg = new StringBuilder();
		for (ParameterizedError pe : errors) {
			if (!errorMsg.isEmpty()) {
				errorMsg.append(", ");
			}

			errorMsg.append(MessageUtil.getInstance().getMessage(pe.error().code().toLowerCase(), pe.params()));
		}

		userErrors.addError(LabServicesRateListErrorCode.ITEM_ERROR, idx, errorMsg.toString());
	}
}
