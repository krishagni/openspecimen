package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.krishagni.catissueplus.core.biospecimen.repository.CpListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServiceListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.LabServicesRateListCriteria;
import com.krishagni.catissueplus.core.biospecimen.services.RateListService;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.Tuple;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.errors.ParameterizedError;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
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
			List<LabServiceDetail> services = LabServiceDetail.from(daoFactory.getLabServiceDao().getServices(crit));

			if (crit.includeStat()) {
				Map<Long, LabServiceDetail> servicesMap = new LinkedHashMap<>();
				for (LabServiceDetail service : services) {
					servicesMap.put(service.getId(), service);
				}

				Map<Long, Long> rateListsCount = daoFactory.getLabServiceDao().getRateListsCount(servicesMap.keySet());
				for (LabServiceDetail service : services) {
					service.setRateLists(rateListsCount.get(service.getId()));
				}
			}

			return ResponseEvent.response(services);
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
			AccessCtrlMgr.getInstance().ensureUserIsAdminOrInstituteAdmin();
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
			AccessCtrlMgr.getInstance().ensureUserIsAdminOrInstituteAdmin();
			LabServiceDetail input = req.getPayload();
			LabService existing = getService(input.getId(), input.getCode());
			LabService service = serviceFactory.createService(input);
			ensureUniqueServiceCode(existing, service);
			if (Status.isDisabledStatus(service.getActivityStatus())) {
				Map<String, Long> specimensCount = daoFactory.getLabServiceDao()
					.getSpecimensCountServicedBy(Collections.singletonList(existing.getId()));
				if (!specimensCount.isEmpty()) {
					return ResponseEvent.userError(LabServiceErrorCode.IN_USE, existing.getCode(), specimensCount.get(existing.getCode()));
				}
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
			AccessCtrlMgr.getInstance().ensureUserIsAdminOrInstituteAdmin();
			EntityQueryCriteria input = req.getPayload();
			LabService existing = getService(input.getId(), input.getName());

			Map<String, Long> specimensCount = daoFactory.getLabServiceDao()
				.getSpecimensCountServicedBy(Collections.singletonList(existing.getId()));
			if (!specimensCount.isEmpty()) {
				return ResponseEvent.userError(LabServiceErrorCode.IN_USE, existing.getCode(), specimensCount.get(existing.getCode()));
			}

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
	public ResponseEvent<List<LabServicesRateListDetail>> getServiceRates(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria input = req.getPayload();
			return ResponseEvent.response(daoFactory.getLabServiceDao().getRateLists(input.getId()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<LabServicesRateListDetail>> getRateLists(RequestEvent<LabServicesRateListCriteria> req) {
		try {
			LabServicesRateListCriteria crit = addReadConstraints(req.getPayload());
			List<LabServicesRateList> rateLists = daoFactory.getLabServiceRateListDao().getRateLists(crit);
			List<LabServicesRateListDetail> results = LabServicesRateListDetail.from(rateLists);

			if (!rateLists.isEmpty() && crit.includeStat()) {
				Map<Long, LabServicesRateListDetail> rlMap = new LinkedHashMap<>();
				for (LabServicesRateListDetail rateList : results) {
					rlMap.put(rateList.getId(), rateList);
				}

				Map<Long, Pair<Long, Long>> stats = daoFactory.getLabServiceRateListDao().getRateListsStats(rlMap.keySet());
				for (Map.Entry<Long, Pair<Long, Long>> statEntry : stats.entrySet()) {
					Pair<Long, Long> stat = statEntry.getValue();
					LabServicesRateListDetail rateList = rlMap.get(statEntry.getKey());
					rateList.setServicesCount(stat.first());
					rateList.setCpsCount(stat.second());
				}
			}

			return ResponseEvent.response(results);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getRateListsCount(RequestEvent<LabServicesRateListCriteria> req) {
		try {
			LabServicesRateListCriteria crit = addReadConstraints(req.getPayload());
			return ResponseEvent.response(daoFactory.getLabServiceRateListDao().getRateListsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServicesRateListDetail> getRateList(RequestEvent<EntityQueryCriteria> req) {
		try {
			LabServicesRateList rateList = getReadAllowedRateList(req.getPayload().getId());
			return ResponseEvent.response(LabServicesRateListDetail.from(rateList));
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
	public ResponseEvent<LabServicesRateListDetail> updateRateList(RequestEvent<LabServicesRateListDetail> req) {
		try {
			LabServicesRateListDetail input = req.getPayload();
			if (input.getId() == null) {
				return ResponseEvent.userError(LabServicesRateListErrorCode.ID_REQ);
			}

			LabServicesRateList existing = getRateList(input.getId());
			raiseErrorIfUpdateNotAllowed(existing);

			LabServicesRateList rateList = rateListFactory.createRateList(input);
			existing.update(rateList);
			raiseErrorIfOverlappingServices(rateList);
			return ResponseEvent.response(LabServicesRateListDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabServicesRateListDetail> cloneRateList(RequestEvent<LabServicesRateListDetail> req) {
		try {
			LabServicesRateListDetail input = req.getPayload();
			LabServicesRateList existing = getRateList(input.getCloneOf());
			raiseErrorIfUpdateNotAllowed(existing);

			LabServicesRateList newRateList = rateListFactory.createRateList(input);
			daoFactory.getLabServiceRateListDao().saveOrUpdate(newRateList, true);
			daoFactory.getLabServiceRateListDao().cloneRateListServices(existing.getId(), newRateList.getId());
			daoFactory.getLabServiceRateListDao().cloneRateListCps(existing.getId(), newRateList.getId());
			raiseErrorIfOverlappingServices(newRateList);
			return ResponseEvent.response(LabServicesRateListDetail.from(newRateList));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<LabServiceRateDetail>> getRateListServices(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			LabServicesRateList rateList = getReadAllowedRateList(crit.getId());
			return ResponseEvent.response(LabServiceRateDetail.from(rateList.getServiceRates()));
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
			List<LabServiceRateDetail> serviceRates = op.getServiceRates();
			if (CollectionUtils.isEmpty(serviceRates)) {
				return ResponseEvent.response(0);
			}

			LabServicesRateList rateList = getRateList(op.getRateListId());
			raiseErrorIfUpdateNotAllowed(rateList);

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
	public ResponseEvent<List<CollectionProtocolSummary>> getRateListCps(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			LabServicesRateList rateList = getReadAllowedRateList(crit.getId());
			CpListCriteria cpListCrit = new CpListCriteria();
			if (crit.getParams() != null && crit.getParams().get("cpListCriteria") != null) {
				cpListCrit = (CpListCriteria) crit.getParams().get("cpListCriteria");
			}

			List<LabServiceRateListCp> rateListCps = daoFactory.getLabServiceRateListDao().getRateListCps(rateList.getId(), cpListCrit);
			return ResponseEvent.response(rateListCps.stream().map(rateListCp -> CollectionProtocolSummary.from(rateListCp.getCp())).collect(Collectors.toList()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getRateListCpsCount(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			LabServicesRateList rateList = getReadAllowedRateList(crit.getId());
			CpListCriteria cpListCrit = new CpListCriteria();
			if (crit.getParams() != null && crit.getParams().get("cpListCriteria") != null) {
				cpListCrit = (CpListCriteria) crit.getParams().get("cpListCriteria");
			}

			Long count = daoFactory.getLabServiceRateListDao().getRateListCpsCount(rateList.getId(), cpListCrit);
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

			Map<Long, CollectionProtocol> cpsMap = validateCps(cps);
			Set<Long> validatedCpIds = new LinkedHashSet<>(cpsMap.keySet());

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
		if (rateListId == null) {
			throw OpenSpecimenException.userError(LabServicesRateListErrorCode.ID_REQ);
		}

		LabServicesRateList rateList = daoFactory.getLabServiceRateListDao().getById(rateListId);
		if (rateList == null) {
			throw OpenSpecimenException.userError(LabServicesRateListErrorCode.NOT_FOUND, rateListId);
		}

		return rateList;
	}

	private LabServicesRateList getReadAllowedRateList(Long rateListId) {
		if (rateListId == null) {
			throw OpenSpecimenException.userError(LabServicesRateListErrorCode.ID_REQ);
		}

		LabServicesRateListCriteria crit = new LabServicesRateListCriteria().ids(Collections.singletonList(rateListId));
		addReadConstraints(crit);
		List<LabServicesRateList> rateLists = daoFactory.getLabServiceRateListDao().getRateLists(crit);
		if (rateLists.isEmpty()) {
			throw OpenSpecimenException.userError(LabServicesRateListErrorCode.NOT_FOUND, rateListId);
		}

		return rateLists.get(0);
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


	private void raiseErrorIfUpdateNotAllowed(LabServicesRateList rateList) {
		Collection<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.CP, Operation.UPDATE, false);
		if (siteCps == null) {
			return;
		}

		if (siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		} else {
			List<CollectionProtocol> notAllowedCps = daoFactory.getLabServiceRateListDao().notAllowedCps(rateList.getId(), siteCps, 10);
			if (!notAllowedCps.isEmpty()) {
				String cpTitles = notAllowedCps.stream().map(CollectionProtocol::getShortTitle).collect(Collectors.joining(", "));
				throw OpenSpecimenException.userError(LabServicesRateListErrorCode.UPDATE_NOT_ALLOWED, rateList.getId(), rateList.getName(), cpTitles);
			}
		}
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

		throw OpenSpecimenException.userError(LabServicesRateListErrorCode.SVCS_OVERLAP, rateList.getId(), rateList.getName(), String.join("; ", args));
	}

	private LabServicesRateListCriteria addReadConstraints(LabServicesRateListCriteria crit) {
		if (crit.cpId() != null) {
			AccessCtrlMgr.getInstance().ensureReadCpRights(crit.cpId());
		} else {
			Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.CP, Operation.READ, false);
			if (siteCps != null && siteCps.isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}

			crit.siteCps(siteCps);
		}

		return crit.creatorId(AuthUtil.getCurrentUser().getId());
	}


	private Map<Long, CollectionProtocol> validateCps(Collection<CollectionProtocolSummary> inputCps) {
		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.CP, Operation.UPDATE, false);
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		int idx = 0;
		List<Pair<Long, Integer>> cpIds = new ArrayList<>();
		List<Pair<String, Integer>> cpShortTitles = new ArrayList<>();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		for (CollectionProtocolSummary cp : inputCps) {
			++idx;
			if (cp.getId() != null) {
				cpIds.add(Pair.make(cp.getId(), idx));
			} else if (StringUtils.isNotBlank(cp.getShortTitle())) {
				cpShortTitles.add(Pair.make(cp.getShortTitle(), idx));
			} else {
				ParameterizedError error = new ParameterizedError(CpErrorCode.SHORT_TITLE_REQUIRED, null);
				addItemErrorsIfPresent(idx, Collections.singletonList(error), ose);
			}
		}

		ose.checkAndThrow();

		CpListCriteria listCrit = new CpListCriteria()
			.siteCps(siteCps)
			.ids(cpIds.stream().map(Pair::first).collect(Collectors.toList()))
			.shortTitles(cpShortTitles.stream().map(Pair::first).collect(Collectors.toList()));
		List<CollectionProtocol> cps = daoFactory.getCollectionProtocolDao().getCollectionProtocolsList(listCrit.maxResults(idx));
		Map<Long, CollectionProtocol> cpsMap = new LinkedHashMap<>();
		for (CollectionProtocol cp : cps) {
			cpsMap.put(cp.getId(), cp);
			if (!cpIds.removeIf(el -> el.first().equals(cp.getId()))) {
				cpShortTitles.removeIf(
					el -> el.first().equals(cp.getShortTitle()) || el.first().equalsIgnoreCase(cp.getShortTitle())
				);
			}
		}

		if (!cpIds.isEmpty() || !cpShortTitles.isEmpty()) {
			for (Pair<Long, Integer> cpId : cpIds) {
				ParameterizedError error = new ParameterizedError(LabServicesRateListErrorCode.CP_NF_OR_UPDATE_NA, cpId.first());
				addItemErrorsIfPresent(cpId.second(), Collections.singletonList(error), ose);
			}

			for (Pair<String, Integer> cpShortTitle: cpShortTitles) {
				ParameterizedError error = new ParameterizedError(LabServicesRateListErrorCode.CP_NF_OR_UPDATE_NA, cpShortTitle.first());
				addItemErrorsIfPresent(cpShortTitle.second(), Collections.singletonList(error), ose);
			}

			ose.checkAndThrow();
		}

		return cpsMap;
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
