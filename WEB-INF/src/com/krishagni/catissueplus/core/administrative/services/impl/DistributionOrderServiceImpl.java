package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.File;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.mail.internet.MimeUtility;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder.Status;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.OrderSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.administrative.domain.SpecimenReservedEvent;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionProtocolErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SpecimenRequestErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemDetail;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.PrintDistributionLabelDetail;
import com.krishagni.catissueplus.core.administrative.events.ReserveSpecimensDetail;
import com.krishagni.catissueplus.core.administrative.events.RetrieveSpecimensOp;
import com.krishagni.catissueplus.core.administrative.events.ReturnedSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.administrative.services.DistributionOrderService;
import com.krishagni.catissueplus.core.administrative.services.DistributionValidator;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenListErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.EntityCrudListener;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.domain.LabelPrintJob;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.LabelPrintJobSummary;
import com.krishagni.catissueplus.core.common.events.LabelTokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.LabelPrinter;
import com.krishagni.catissueplus.core.common.service.ManifestGenerator;
import com.krishagni.catissueplus.core.common.service.ManifestGeneratorFactory;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.SessionUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.Filter.Op;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.query.Column;
import com.krishagni.catissueplus.core.query.ListConfig;
import com.krishagni.catissueplus.core.query.ListService;
import com.krishagni.catissueplus.core.query.ListUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;

public class DistributionOrderServiceImpl implements DistributionOrderService, ObjectAccessor, InitializingBean {
	private static final LogUtil logger = LogUtil.getLogger(DistributionOrderServiceImpl.class);

	private static final long ASYNC_CALL_TIMEOUT = 5000L;

	private static final String ORDER_MANIFEST = "order_manifest";

	private DaoFactory daoFactory;

	private DistributionOrderFactory distributionFactory;
	
	private QueryService querySvc;
	
	private EmailService emailService;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;

	private AsyncTaskExecutor taskExecutor;

	private Map<String, DistributionValidator> validators = new LinkedHashMap<>();

	private List<EntityCrudListener<DistributionOrderDetail, DistributionOrder>> listeners = new ArrayList<>();

	private LabelPrinter<DistributionOrderItem> labelPrinter;

	private ListService listSvc;

	private ManifestGeneratorFactory manifestGeneratorFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDistributionFactory(DistributionOrderFactory distributionFactory) {
		this.distributionFactory = distributionFactory;
	}
	
	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}
	
	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setTaskExecutor(AsyncTaskExecutor taskExecutor) {
		this.taskExecutor = taskExecutor;
	}

	public void setValidators(List<DistributionValidator> validators) {
		for (DistributionValidator validator : validators) {
			this.validators.put(validator.getName(), validator);
		}
	}

	@Override
	public void addValidator(DistributionValidator validator) {
		this.validators.put(validator.getName(), validator);
	}

	@Override
	public void removeValidator(String name) {
		this.validators.remove(name);
	}

	public void setLabelPrinter(LabelPrinter<DistributionOrderItem> labelPrinter) {
		this.labelPrinter = labelPrinter;
	}

	public void setListSvc(ListService listSvc) {
		this.listSvc = listSvc;
	}

	public void setManifestGeneratorFactory(ManifestGeneratorFactory manifestGeneratorFactory) {
		this.manifestGeneratorFactory = manifestGeneratorFactory;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderSummary>> getOrders(RequestEvent<DistributionOrderListCriteria> req) {
		try {
			DistributionOrderListCriteria crit = addOrderListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getDistributionOrderDao().getOrders(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getOrdersCount(RequestEvent<DistributionOrderListCriteria> req) {
		try {
			DistributionOrderListCriteria crit = addOrderListCriteria(req.getPayload());
			return ResponseEvent.response(daoFactory.getDistributionOrderDao().getOrdersCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionOrderDetail> getOrder(RequestEvent<Long> req) {
		try {
			DistributionOrder order = getOrder(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			DistributionOrderDetail output = DistributionOrderDetail.from(order);
			listeners.forEach(listener -> listener.onRead(output, order));
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<DistributionOrderDetail> createOrder(RequestEvent<DistributionOrderDetail> req) {
		return saveOrUpdateOrder(this::createOrder, req.getPayload());
	}
	
	@Override
	public ResponseEvent<DistributionOrderDetail> updateOrder(RequestEvent<DistributionOrderDetail> req) {
		return saveOrUpdateOrder(this::updateOrder, req.getPayload());
	}

	@Override
	@PlusTransactional
	public ResponseEvent<DistributionOrderDetail> deleteOrder(RequestEvent<Long> req) {
		try {
			User currentUser = AuthUtil.getCurrentUser();
			Future<ResponseEvent<DistributionOrderDetail>> result = taskExecutor.submit(
				() -> {
					long t1 = System.currentTimeMillis();
					try {
						AuthUtil.setCurrentUser(currentUser);
						ResponseEvent<DistributionOrderDetail> resp = deleteOrder(req.getPayload());
						if (!resp.isSuccessful()) {
							notifyFailedOrderDelete(req.getPayload(), resp.getError().getMessage());
						}

						return resp;
					} finally {
						AuthUtil.clearCurrentUser();
					}
				}
			);

			return unwrap(result, ASYNC_CALL_TIMEOUT);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req) {
		try {
			Long orderId = req.getPayload();
			DistributionOrder order = daoFactory.getDistributionOrderDao().getById(orderId);
			if (order == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.NOT_FOUND);
			}
			
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			SavedQuery query = getReportQuery(order);
			if (query == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.RPT_TMPL_NOT_CONFIGURED, order.getDistributionProtocol().getShortTitle());
			}
			
			return new ResponseEvent<>(exportReport(order, query, false));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderItemDetail>> getOrderItems(RequestEvent<DistributionOrderItemListCriteria> req) {
		try {
			DistributionOrderItemListCriteria crit = req.getPayload();

			DistributionOrder order = getOrder(crit.orderId(), null);
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getOrderItems(crit);
			return ResponseEvent.response(DistributionOrderItemDetail.from(items));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> deleteOrderItems(Long orderId, String orderName, List<Long> itemIds) {
		try {
			DistributionOrder order = getOrder(orderId, orderName);
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(order);
			if (CollectionUtils.isEmpty(itemIds)) {
				return ResponseEvent.response(0);
			}

			List<DistributionOrderItem> deletedItems = order.deleteItems(itemIds);
			notifyOrderItemsDeleted(order, deletedItems);
			return ResponseEvent.response(deletedItems.size());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<DistributionOrderItemDetail>> getDistributedSpecimens(RequestEvent<SpecimenListCriteria> req) {
		try {
			List<Specimen> specimens = getReadAccessSpecimens(req.getPayload());
			if (CollectionUtils.isEmpty(specimens)) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<Long> ids = specimens.stream().map(Specimen::getId).collect(Collectors.toList());
			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getDistributedOrderItems(ids);
			Set<DistributionOrder> accessAllowed = new HashSet<>();
			for (DistributionOrderItem item : items) {
				if (accessAllowed.contains(item.getOrder())) {
					continue;
				}

				AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(item.getOrder());
				accessAllowed.add(item.getOrder());
			}

			return ResponseEvent.response(DistributionOrderItemDetail.from(items));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> returnSpecimens(RequestEvent<List<ReturnedSpecimenDetail>> req) {
		try {
			Map<String, DistributionOrder> ordersMap = new HashMap<>();
			Map<String, StorageContainer> containersMap = new HashMap<>();

			List<Specimen> result = new ArrayList<>();
			for (ReturnedSpecimenDetail returnedSpmn : req.getPayload()) {
				result.add(returnSpecimen(returnedSpmn, ordersMap, containersMap));
			}

			return ResponseEvent.response(SpecimenInfo.from(result));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SpecimenInfo>> getReservedSpecimens(RequestEvent<SpecimenListCriteria> req) {
		try {
			SpecimenListCriteria criteria = getReservedSpecimensCriteria(req.getPayload());
			List<Specimen> spmns = daoFactory.getSpecimenDao().getSpecimens(criteria);
			return ResponseEvent.response(SpecimenInfo.from(spmns));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> getReservedSpecimensCount(RequestEvent<SpecimenListCriteria> req) {
		try {
			SpecimenListCriteria criteria = getReservedSpecimensCriteria(req.getPayload());
			Integer count = daoFactory.getSpecimenDao().getSpecimensCount(criteria);
			return ResponseEvent.response(count);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> reserveSpecimens(RequestEvent<ReserveSpecimensDetail> req) {
		try {
			ReserveSpecimensDetail detail = req.getPayload();

			//
			// Step 1: Check whether the DP exists. If exists, retrieve it
			//
			DistributionProtocol dp = getDp(detail.getDpId(), detail.getDpShortTitle());

			//
			// Step 2: Ensure user can distribute specimens to the DP
			//
			AccessCtrlMgr.getInstance().ensureCreateDistributionOrderRights(dp);

			//
			// Step 3: Ensure user has specimen read rights
			//
			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			User user = null;
			if (detail.getUser() != null) {
				user = getUser(detail.getUser().getId(), detail.getUser().getEmailAddress());
			}

			if (user == null) {
				user = AuthUtil.getCurrentUser();
			}

			Date time = detail.getTime();
			if (time == null) {
				time = Calendar.getInstance().getTime();
			}

			//
			// Step 4: Retrieve all requested specimens from DB that are to be reserved for the DP
			//
			List<Specimen> specimens = getSpecimens(detail.getSpecimens());

			//
			// Step 5: Reserve/cancel reservation after ensuring the accessibility and validity of specimens for the DP
			//
			int count;
			if (detail.getCancelOp() == null || detail.getCancelOp().equals(Boolean.FALSE)) {
				count = reserveSpecimens(specimens, dp, user, time, detail.getComments(), siteCps);
			} else {
				count = cancelSpecimenReservation(specimens, dp, user, time, detail.getComments(), siteCps);
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
	public ResponseEvent<Integer> retrieveSpecimens(RequestEvent<RetrieveSpecimensOp> req) {
		try {
			RetrieveSpecimensOp detail = req.getPayload();
			DistributionOrder order = daoFactory.getDistributionOrderDao().getById(detail.getListId());
			if (order == null) {
				return ResponseEvent.userError(DistributionOrderErrorCode.NOT_FOUND, detail.getListId());
			}

			ensureUserCanRetrieveSpecimens(order);

			User retrievedBy = null;
			if (detail.getUser() != null) {
				retrievedBy = getUser(detail.getUser().getId(), detail.getUser().getEmailAddress());
			}

			if (retrievedBy == null) {
				retrievedBy = AuthUtil.getCurrentUser();
			}

			Date retrieveDate = detail.getTime();
			if (retrieveDate == null) {
				retrieveDate = Calendar.getInstance().getTime();
			}

			int retrievedSpmnsCount = 0;
			DistributionOrderItemListCriteria itemsCrit = new DistributionOrderItemListCriteria()
				.orderId(order.getId())
				.storedInContainers(true);
			boolean endOfItems = false;
			while (!endOfItems) {
				// startAt is not set. The idea is the previously retrieved specimens won't appear in next chunk.
				List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getOrderItems(itemsCrit);
				endOfItems = (items.size() < itemsCrit.maxResults());

				for (DistributionOrderItem item : items) {
					item.getSpecimen().updatePosition(null, retrievedBy, retrieveDate, detail.getComments());
					item.getSpecimen().initCollections(); // HSEARCH-1350
				}

				retrievedSpmnsCount += items.size();
				items.clear();
				SessionUtil.getInstance().clearSession();
			}

			return ResponseEvent.response(retrievedSpmnsCount);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<LabelPrintJobSummary> printDistributionLabels(RequestEvent<PrintDistributionLabelDetail> req) {
		try {
			PrintDistributionLabelDetail input = req.getPayload();

			DistributionOrder order = getOrder(input.getOrderId(), input.getOrderName());
			AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

			List<LabelPrintJob> printJobs = new ArrayList<>();
			boolean endOfItems = false;
			List<Long> itemIds = input.getItemIds();
			int startAt = 0;
			while (!endOfItems) {
				DistributionOrderItemListCriteria crit = new DistributionOrderItemListCriteria().orderId(order.getId());
				if (CollectionUtils.isNotEmpty(itemIds)) {
					if (itemIds.size() > crit.maxResults()) {
						crit.ids(itemIds.subList(0, crit.maxResults()));
						itemIds = itemIds.subList(crit.maxResults(), itemIds.size());
					} else {
						crit.ids(itemIds);
						itemIds = null;
					}
				} else {
					crit.startAt(startAt);
				}

				List<DistributionOrderItem> orderItems = daoFactory.getDistributionOrderDao().getOrderItems(crit);
				LabelPrintJob job = printDistributionLabels(orderItems, input.getCopies());
				if (job != null) {
					printJobs.add(job);
				}

				startAt += orderItems.size();
				endOfItems = CollectionUtils.isEmpty(itemIds) && orderItems.size() < crit.maxResults();
			}

			if (printJobs.isEmpty()) {
				return ResponseEvent.userError(DistributionOrderErrorCode.NO_ITEMS_PRINTED);
			}

			if (printJobs.size() == 1) {
				LabelPrintJob job = printJobs.iterator().next();
				job.generateLabelsDataFile();
				return ResponseEvent.response(LabelPrintJobSummary.from(job));
			}

			return ResponseEvent.response(null);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public ResponseEvent<List<LabelTokenDetail>> getPrintLabelTokens() {
		return ResponseEvent.response(LabelTokenDetail.from("print_", labelPrinter.getTokens()));
	}

	@Override
	public void addListener(EntityCrudListener<DistributionOrderDetail, DistributionOrder> listener) {
		listeners.add(listener);
	}

	@Override
	@PlusTransactional
	public void validateSpecimens(DistributionProtocol dp, List<Specimen> specimens) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidSpecimens(specimens, dp, siteCps, ose);
		ose.checkAndThrow();
	}

	@Override
	public String getObjectName() {
		return DistributionOrder.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getDistributionOrderDao().getOrderIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "OS_ORDERS_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		DistributionOrder order = getOrder(id, null);
		AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		listSvc.registerListConfigurator("order-specimens-list-view", this::getOrderSpecimensConfig);
	}

	private DistributionOrderListCriteria addOrderListCriteria(DistributionOrderListCriteria crit) {
		Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessDistributionOrderSites();
		if (sites != null && sites.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (sites != null) {
			crit.sites(sites);
		}

		return crit;
	}

	@PlusTransactional
	private ResponseEvent<DistributionOrderDetail> createOrder(DistributionOrderDetail input) {
		long t1 = System.currentTimeMillis();
		try {
			DistributionOrder order = distributionFactory.createDistributionOrder(input, Status.PENDING);

			AccessCtrlMgr.getInstance().ensureCreateDistributionOrderRights(order);

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraints(null, order, ose);
			ensureValidSpecimenList(order, ose);

			Status inputStatus = null;
			try {
				inputStatus = Status.valueOf(input.getStatus());
			} catch (IllegalArgumentException iae) {
				ose.addError(DistributionOrderErrorCode.INVALID_STATUS, input.getStatus());
			}

			ose.checkAndThrow();

			SpecimenRequest request = order.getRequest();
			if (request != null && request.isClosed()) {
				return ResponseEvent.userError(SpecimenRequestErrorCode.CLOSED, request.getId());
			}

			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.userError(RbacErrorCode.ACCESS_DENIED);
			}

			daoFactory.getDistributionOrderDao().saveOrUpdate(order, true);

			ensureValidSpecimens(order, siteCps, ose);
			ose.checkAndThrow();

			order = daoFactory.getDistributionOrderDao().getById(order.getId());
			addRates(order, order.getOrderItems());
			distributeOrder(order, siteCps, inputStatus);

			DistributionOrder savedOrder = daoFactory.getDistributionOrderDao().getById(order.getId());
			savedOrder.addOrUpdateExtension();

			EventPublisher.getInstance().publish(new OrderSavedEvent(savedOrder));
			DistributionOrderDetail output = DistributionOrderDetail.from(savedOrder);
			listeners.forEach(listener -> listener.onSave(input, output, savedOrder));
			notifySaveOrUpdateOrder(savedOrder, null, t1);
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			logger.info("Time taken to save distribution order is " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	@PlusTransactional
	private ResponseEvent<DistributionOrderDetail> updateOrder(DistributionOrderDetail input) {
		long t1 = System.currentTimeMillis();
		try {
			DistributionOrder existingOrder = getOrder(input.getId(), input.getName());
			input.setId(existingOrder.getId());

			boolean isDistributed = existingOrder.isOrderExecuted();
			if (isDistributed) {
				input.setCopyItemsFromExistingOrder(true);
				input.setSpecimenList(null);
			}

			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(existingOrder);
			DistributionOrder newOrder = distributionFactory.createDistributionOrder(input, null);
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(newOrder);

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraints(existingOrder, newOrder, ose);
			ensureValidSpecimenList(newOrder, ose);
			ose.checkAndThrow();

			List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}

			if (isDistributed) {
				newOrder.setDistributionProtocol(existingOrder.getDistributionProtocol());
			} else if (input.isCopyItemsFromExistingOrder()) {
				newOrder.setOrderItems(existingOrder.getOrderItems());
			}

			Status oldStatus = existingOrder.getStatus();
			existingOrder.update(newOrder);
			daoFactory.getDistributionOrderDao().saveOrUpdate(existingOrder, true);
			existingOrder.addOrUpdateExtension();

			if (!isDistributed) {
				ensureValidSpecimens(existingOrder, siteCps, ose);
				ose.checkAndThrow();

				existingOrder = daoFactory.getDistributionOrderDao().getById(existingOrder.getId());
				addRates(existingOrder, existingOrder.getOrderItems());
				distributeOrder(existingOrder, siteCps, newOrder.getStatus());
				existingOrder = daoFactory.getDistributionOrderDao().getById(existingOrder.getId());
			}

			EventPublisher.getInstance().publish(new OrderSavedEvent(existingOrder));
			DistributionOrderDetail output = DistributionOrderDetail.from(existingOrder);
			notifyOrderListeners(input, output, existingOrder);
			notifySaveOrUpdateOrder(existingOrder, oldStatus, t1);
			return ResponseEvent.response(output);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		} finally {
			logger.info("Time taken to save distribution order is " + (System.currentTimeMillis() - t1) + " ms");
		}
	}

	private ResponseEvent<DistributionOrderDetail> saveOrUpdateOrder(Function<DistributionOrderDetail, ResponseEvent<DistributionOrderDetail>> workFn, DistributionOrderDetail input) {
		long timeout = input.isAsync() ? ASYNC_CALL_TIMEOUT : 0L;
		User currentUser = AuthUtil.getCurrentUser();
		Future<ResponseEvent<DistributionOrderDetail>> result = taskExecutor.submit(
				() -> {
					try {
						long t1 = System.currentTimeMillis();
						AuthUtil.setCurrentUser(currentUser);

						ResponseEvent<DistributionOrderDetail> resp = workFn.apply(input);
						if (!resp.isSuccessful() && timeout > 0 && (System.currentTimeMillis() - t1) > (timeout - 500)) {
							//
							// if the request was async and it took round about same time (+/- 500 ms) as the wait time
							// then notify users about the errors via email notifications.
							//
							notifyFailedOrder(resp);
						}

						return resp;
					} finally {
						AuthUtil.clearCurrentUser();
					}
				}
		);

		return unwrap(result, timeout);
	}

	@PlusTransactional
	private ResponseEvent<DistributionOrderDetail> deleteOrder(Long orderId) {
		try {
			DistributionOrder order = getOrder(orderId, null);
			AccessCtrlMgr.getInstance().ensureDeleteDistributionOrderRights(order);

			String name = order.getName();
			order.delete();

			DistributionOrderDetail result = DistributionOrderDetail.from(order);
			notifyOrderDeleted(name, order);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void notifyOrderListeners(DistributionOrderDetail input, DistributionOrderDetail output, DistributionOrder order) {
		listeners.forEach(listener -> listener.onSave(input, output, order));
	}

	private ResponseEvent<DistributionOrderDetail> unwrap(Future<ResponseEvent<DistributionOrderDetail>> result, long timeout) {
		try {
			if (timeout > 0) {
				return result.get(timeout, TimeUnit.MILLISECONDS);
			} else {
				return result.get();
			}
		} catch (TimeoutException e) {
			DistributionOrderDetail output = new DistributionOrderDetail();
			output.setCompleted(false);
			return ResponseEvent.response(output);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureUniqueConstraints(DistributionOrder existingOrder, DistributionOrder newOrder, OpenSpecimenException ose) {
		if (existingOrder == null || !newOrder.getName().equals(existingOrder.getName())) {
			DistributionOrder order = daoFactory.getDistributionOrderDao().getOrder(newOrder.getName());
			if (order != null) {
				ose.addError(DistributionOrderErrorCode.DUP_NAME, newOrder.getName());
			}
		}
	}

	private void ensureValidSpecimenList(DistributionOrder order, OpenSpecimenException ose) {
		SpecimenList specimenList = order.getSpecimenList();
		if (specimenList == null) {
			return;
		}

		if (!AuthUtil.isAdmin() && !specimenList.canUserAccess(AuthUtil.getCurrentUser().getId())) {
			ose.addError(SpecimenListErrorCode.ACCESS_NOT_ALLOWED);
		}
	}

	private List<Specimen> getReadAccessSpecimens(SpecimenListCriteria crit) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			return null;
		}

		return daoFactory.getSpecimenDao().getSpecimens(crit.siteCps(siteCps));
	}

	private void ensureValidSpecimens(DistributionOrder order, List<SiteCpPair> siteCps, OpenSpecimenException ose) {
		if (order.getSpecimenList() != null || order.isForAllReservedSpecimens()) {
			int maxSpmns = 100;
			Long lastId = -1L;
			Long orderId = order.getId();
			Function<Long, List<Specimen>> getSpecimens = getSpecimensFn(order, siteCps, maxSpmns);

			boolean endOfSpecimens = false;
			while (!endOfSpecimens) {
				if (order == null) {
					order = daoFactory.getDistributionOrderDao().getById(orderId);
					getSpecimens = getSpecimensFn(order, siteCps, maxSpmns);
				}

				List<Specimen> specimens = getSpecimens.apply(lastId);
				if (specimens.isEmpty() && lastId == null) {
					if (order.getSpecimenList() != null) {
						ose.addError(DistributionOrderErrorCode.NO_SPMNS_IN_LIST, order.getSpecimenList().getName());
					} else {
						ose.addError(DistributionOrderErrorCode.NO_SPMNS_RESV_FOR_DP, order.getDistributionProtocol().getShortTitle());
					}
				}

				ensureValidSpecimens(specimens, order.getDistributionProtocol(), siteCps, ose);

				endOfSpecimens = (specimens.size() < maxSpmns);
				if (!specimens.isEmpty()) {
					lastId = specimens.get(specimens.size() - 1).getId();
				}

				specimens.clear();
				order = null;
				SessionUtil.getInstance().clearSession();
			}
		} else {
			List<Specimen> specimens = Utility.collect(order.getOrderItems(), "specimen");
			ensureValidSpecimens(specimens, order.getDistributionProtocol(), siteCps, ose);
		}
	}

	private Function<Long, List<Specimen>> getSpecimensFn(DistributionOrder order, List<SiteCpPair> siteCps, int maxSpmns) {
		Long specimenListId = order.getSpecimenList() != null ? order.getSpecimenList().getId() : null;
		Long reservedForDp  = specimenListId != null ? null : order.getDistributionProtocol().getId();

		return (lastId) -> daoFactory.getSpecimenDao().getSpecimens(new SpecimenListCriteria()
			.specimenListId(specimenListId)
			.reservedForDp(reservedForDp)
			.siteCps(siteCps)
			.lastId(lastId)
			.startAt(0).maxResults(maxSpmns)
			.limitItems(true)
		);
	}

	private void ensureValidSpecimens(List<Specimen> inputSpmns, DistributionProtocol dp, List<SiteCpPair> siteCps, OpenSpecimenException ose) {
		if (CollectionUtils.isEmpty(inputSpmns)) {
			return;
		}

		List<Long> specimenIds = inputSpmns.stream().map(Specimen::getId).collect(Collectors.toList());
		ensureSpecimensAccessibility(specimenIds, siteCps, ose);

		List<String> closedSpmns = inputSpmns.stream()
			.filter(spmn -> !spmn.isActive()).map(Specimen::getLabel)
			.limit(10)
			.toList();
		if (!closedSpmns.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.CLOSED_SPECIMENS, closedSpmns.size(), StringUtils.join(closedSpmns, ", "));
			return;
		}

		ensureDpValidity(inputSpmns, dp, ose);

		Map<String, Object> ctxt = Collections.singletonMap("siteCpPairs", siteCps);
		for (DistributionValidator validator : validators.values()) {
			try {
				validator.validate(dp, inputSpmns, ctxt);
			} catch (OpenSpecimenException ve) {
				if (ve.getException() == null) {
					ose.addErrors(ve.getErrors());
				} else {
					throw ve;
				}

				break;
			}
		}
	}

	private void ensureSpecimensAccessibility(List<Long> specimenIds, List<SiteCpPair> siteCps, OpenSpecimenException ose) {
		SpecimenListCriteria crit = new SpecimenListCriteria().ids(specimenIds).siteCps(siteCps);
		String nonCompliantSpmnLabels = daoFactory.getSpecimenDao().getNonCompliantSpecimens(crit)
			.stream().limit(10).collect(Collectors.joining(", "));
		if (!nonCompliantSpmnLabels.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.SPECIMEN_DOES_NOT_EXIST, nonCompliantSpmnLabels);
		}
	}

	private void ensureDpValidity(List<Specimen> specimens, DistributionProtocol dp, OpenSpecimenException ose) {
		List<Specimen> spmnWithDps = specimens.stream()
			.filter(spmn -> spmn.getReservedEvent() != null || !spmn.getDistributionProtocols().isEmpty())
			.collect(Collectors.toList());

		List<String> resvForOthDps = spmnWithDps.stream()
			.filter(spmn -> isSpecimenReservedForOtherDp(spmn, dp))
			.map(Specimen::getLabel)
			.collect(Collectors.toList());
		if (!resvForOthDps.isEmpty()) {
			ose.addError(DistributionOrderErrorCode.SPMN_RESV_FOR_OTH_DPS,
				resvForOthDps.stream().limit(10).collect(Collectors.joining(", ")),
				resvForOthDps.size());
		}

		List<Specimen> spmnWithoutDps = specimens.stream()
			.filter(spmn -> !spmnWithDps.contains(spmn))
			.collect(Collectors.toList());
		if (spmnWithoutDps.isEmpty()) {
			return;
		}

		//
		// We'll validate only those specimens that are not yet associated / assigned to DP
		// This implicitly means specimens with DPs have been pre-validated
		//
		Map<Long, Specimen> specimenMap = spmnWithoutDps.stream().collect(Collectors.toMap(Specimen::getId, Function.identity()));
		Map<Long, Set<SiteCpPair>> spmnSites = daoFactory.getSpecimenDao().getSpecimenSites(spmnWithoutDps.stream().map(Specimen::getId).collect(Collectors.toSet()));

		String errorLabels = notAllowedSpecimenLabels(specimenMap, spmnSites, dp.getAllowedDistributingSites(null));
		if (StringUtils.isNotBlank(errorLabels)) {
			ose.addError(DistributionOrderErrorCode.INVALID_SPECIMENS_FOR_DP, errorLabels, dp.getShortTitle());
			return;
		}

		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> allowedSites = AccessCtrlMgr.getInstance().getDistributionOrderAllowedSites(dp);
		allowedSites.forEach(s -> s.setResource(null));
		errorLabels = notAllowedSpecimenLabels(specimenMap, spmnSites, allowedSites);
		if (StringUtils.isNotBlank(errorLabels)) {
			ose.addError(DistributionOrderErrorCode.SPMNS_DENIED, errorLabels);
		}
	}

	private String notAllowedSpecimenLabels(Map<Long, Specimen> specimenMap, Map<Long, Set<SiteCpPair>> spmnSites, Set<SiteCpPair> allowedSites) {
		return spmnSites.entrySet().stream()
			.filter(spmnSite -> !SiteCpPair.contains(allowedSites, spmnSite.getValue()))
			.map(spmnSite -> specimenMap.get(spmnSite.getKey()).getLabel())
			.limit(10)
			.collect(Collectors.joining(", "));
	}

	private boolean isSpecimenReservedForOtherDp(Specimen specimen, DistributionProtocol dp) {
		if (specimen.getReservedEvent() != null && !specimen.getReservedEvent().getDp().equals(dp)) {
			return true;
		}

		return !specimen.getDistributionProtocols().isEmpty() && !specimen.getDistributionProtocols().contains(dp);
	}

	private DistributionProtocol getDp(Long dpId, String dpShortTitle) {
		DistributionProtocol dp = null;
		Object key = null;

		if (dpId != null) {
			dp = daoFactory.getDistributionProtocolDao().getById(dpId);
			key = dpId;
		} else if (StringUtils.isNotBlank(dpShortTitle)) {
			dp = daoFactory.getDistributionProtocolDao().getByShortTitle(dpShortTitle);
			key = dpShortTitle;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.DP_REQUIRED);
		} else if (dp == null) {
			throw OpenSpecimenException.userError(DistributionProtocolErrorCode.NOT_FOUND, key);
		}

		return dp;
	}

	private User getUser(Long userId, String userEmailId) {
		User user = null;
		Object key = null;

		if (userId != null) {
			user = daoFactory.getUserDao().getById(userId);
			key = userId;
		} else if (StringUtils.isNotBlank(userEmailId)) {
			user = daoFactory.getUserDao().getUserByEmailAddress(userEmailId);
			key = userEmailId;
		}

		if (key != null && user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, key);
		}

		return user;
	}

	private List<Specimen> getSpecimens(List<SpecimenInfo> input) {
		Set<Long> specimenIds = new HashSet<>();
		Set<Pair<String, String>> specimenLabels = new HashSet<>();
		Set<Pair<String, String>> specimenBarcodes = new HashSet<>();

		for (SpecimenInfo inputSpmn : input) {
			if (inputSpmn.getId() != null) {
				specimenIds.add(inputSpmn.getId());
			} else if (StringUtils.isNotBlank(inputSpmn.getCpShortTitle())) {
				if (StringUtils.isNotBlank(inputSpmn.getLabel())) {
					specimenLabels.add(Pair.make(inputSpmn.getCpShortTitle(), inputSpmn.getLabel()));
				} else if (StringUtils.isNotBlank(inputSpmn.getBarcode())) {
					specimenBarcodes.add(Pair.make(inputSpmn.getCpShortTitle(), inputSpmn.getBarcode()));
				}
			}
		}

		Map<String, Specimen> dbSpmns = new HashMap<>();
		if (!specimenIds.isEmpty()) {
			List<Specimen> spmns = daoFactory.getSpecimenDao().getByIds(specimenIds);
			if (spmns.size() != specimenIds.size()) {
				spmns.forEach(spmn -> specimenIds.remove(spmn.getId()));
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, specimenIds.stream().limit(10).map(id -> id.toString()).collect(Collectors.joining(", ")));
			}

			dbSpmns.putAll(spmns.stream().collect(Collectors.toMap(s -> s.getId().toString(), Function.identity())));
		}

		if (!specimenLabels.isEmpty()) {
			List<Specimen> spmns = daoFactory.getSpecimenDao().getByLabels(specimenLabels);
			if (spmns.size() != specimenLabels.size()) {
				spmns.forEach(spmn -> specimenLabels.remove(Pair.make(spmn.getCpShortTitle(), spmn.getLabel())));
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, specimenLabels.stream().limit(10).map(Pair::second).collect(Collectors.joining(", ")));
			}

			dbSpmns.putAll(spmns.stream().collect(Collectors.toMap(s -> "l#" + s.getCpShortTitle() + "#" + s.getLabel(), Function.identity())));
		}

		if (!specimenBarcodes.isEmpty()) {
			List<Specimen> spmns = daoFactory.getSpecimenDao().getByBarcodes(specimenBarcodes);
			if (spmns.size() != specimenBarcodes.size()) {
				spmns.forEach(spmn -> specimenBarcodes.remove(Pair.make(spmn.getCpShortTitle(), spmn.getBarcode())));
				throw OpenSpecimenException.userError(SpecimenErrorCode.NOT_FOUND, specimenBarcodes.stream().limit(10).map(Pair::second).collect(Collectors.joining(", ")));
			}

			dbSpmns.putAll(spmns.stream().collect(Collectors.toMap(s -> "b#" + s.getCpShortTitle() + "#" + s.getBarcode(), Function.identity())));
		}

		List<Specimen> result = new ArrayList<>();
		for (SpecimenInfo spmn : input) {
			if (spmn.getId() != null) {
				result.add(dbSpmns.get(spmn.getId().toString()));
			} else if (StringUtils.isNotBlank(spmn.getCpShortTitle())) {
				if (StringUtils.isNotBlank(spmn.getLabel())) {
					result.add(dbSpmns.get("l#" + spmn.getCpShortTitle() + "#" + spmn.getLabel()));
				} else if (StringUtils.isNotBlank(spmn.getBarcode())) {
					result.add(dbSpmns.get("b#" + spmn.getCpShortTitle() + "#" + spmn.getBarcode()));
				}
			}
		}

		return result;
	}

	private SpecimenListCriteria getReservedSpecimensCriteria(SpecimenListCriteria criteria) {
		DistributionProtocol dp = getDp(criteria.reservedForDp(), null);
		AccessCtrlMgr.getInstance().ensureReadDpRights(dp);

		//
		// Ensure user has specimen read rights
		//
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		return criteria.siteCps(siteCps);
	}

	private int reserveSpecimens(Collection<Specimen> specimens, DistributionProtocol dp, User user, Date time, String comments, List<SiteCpPair> siteCps) {
		//
		// Filter out the specimens that have been already reserved for the DP
		//
		List<Specimen> notReservedForDp = specimens.stream()
			.filter(spmn -> spmn.getReservedEvent() == null || !dp.equals(spmn.getReservedEvent().getDp()))
			.collect(Collectors.toList());

		//
		// Ensure the left out specimens are accessible to the user and can be distributed to the DP
		//
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidSpecimens(notReservedForDp, dp, siteCps, ose);
		ose.checkAndThrow();

		List<SpecimenReservedEvent> events = notReservedForDp.stream().map(spmn -> {
			SpecimenReservedEvent event = new SpecimenReservedEvent();
			event.setSpecimen(spmn);
			event.setDp(dp);
			event.setUser(user);
			event.setTime(time);
			event.setComments(comments);

			spmn.setReservedEvent(event);
			spmn.setAvailabilityStatus(Specimen.RESERVED);
			return event;
		}).collect(Collectors.toList());
		daoFactory.getDistributionProtocolDao().saveReservedEvents(events);

		Long formCtxtId = DeObject.getFormContextId(true, "SpecimenEvent", -1L, "SpecimenReservedEvent");
		if (formCtxtId != null) {
			events.forEach(event -> DeObject.saveRecord(formCtxtId, event.getSpecimen().getId(), event.getId()));
		}

		return events.size();
	}

	private int cancelSpecimenReservation(Collection<Specimen> specimens, DistributionProtocol dp, User user, Date time, String comments, List<SiteCpPair> siteCps) {
		//
		// Ensure specimens are read accessible
		//
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		List<Long> spmnIds = specimens.stream().map(Specimen::getId).collect(Collectors.toList());
		ensureSpecimensAccessibility(spmnIds, siteCps, ose);
		ose.checkAndThrow();

		//
		// Report if specimens are reserved for another DP
		//
		List<String> resvForOthDps = specimens.stream()
			.filter(spmn -> spmn.getReservedEvent() != null && !spmn.getReservedEvent().getDp().equals(dp))
			.map(Specimen::getLabel)
			.collect(Collectors.toList());
		if (!resvForOthDps.isEmpty()) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.SPMN_RESV_FOR_OTH_DPS,
				resvForOthDps.stream().limit(10).collect(Collectors.joining(", ")),
				resvForOthDps.size());
		}

		List<SpecimenReservedEvent> events = new ArrayList<>();
		for (Specimen specimen : specimens) {
			if (specimen.getReservedEvent() == null) {
				continue;
			}

			SpecimenReservedEvent event = new SpecimenReservedEvent();
			event.setSpecimen(specimen);
			event.setDp(dp);
			event.setCancelledEvent(specimen.getReservedEvent());
			event.setUser(user);
			event.setTime(time);
			event.setComments(comments);
			events.add(event);

			specimen.cancelReservation();
		}

		daoFactory.getDistributionProtocolDao().saveReservedEvents(events);

		Long formCtxtId = DeObject.getFormContextId(true, "SpecimenEvent", -1L, "SpecimenReservationCancelledEvent");
		if (formCtxtId != null) {
			events.forEach(event -> DeObject.saveRecord(formCtxtId, event.getSpecimen().getId(), event.getId()));
		}

		return events.size();
	}

	private void addRates(DistributionOrder order, Collection<DistributionOrderItem> items) {
		if (order.getSpecimenList() == null && !order.isForAllReservedSpecimens()) {
			items.forEach(item -> addRate(order, item));
		}
	}

	private DistributionOrderItem addRate(DistributionOrder order, DistributionOrderItem item) {
		if (item.getCost() == null) {
			item.setCost(order.getDistributionProtocol().getCost(item.getSpecimen()));
		}

		return item;
	}

	private void distributeOrder(DistributionOrder order, List<SiteCpPair> siteCps, Status status) {
		if (!Status.EXECUTED.equals(status)) {
			return;
		}

		order.distribute();
		daoFactory.getDistributionOrderDao().saveOrUpdate(order);
		if (order.getSpecimenList() == null && !order.isForAllReservedSpecimens()) {
			printDistributionLabels(order.getOrderItems());
			clearList(order);
			return;
		}

		boolean endOfSpecimens = false;
		int maxSpmns = 100;
		Long lastId = -1L;
		Function<Long, List<Specimen>> getSpecimens = getSpecimensFn(order, siteCps, maxSpmns);
		List<Specimen> specimens;
		while (!endOfSpecimens) {
			specimens = getSpecimens.apply(lastId);

			List<DistributionOrderItem> orderItems = new ArrayList<>();
			for (Specimen specimen : specimens) {
				orderItems.add(distributeSpecimen(order, specimen));
			}

			printDistributionLabels(orderItems);

			endOfSpecimens = (specimens.size() < maxSpmns);
			if (!specimens.isEmpty()) {
				lastId = specimens.get(specimens.size() - 1).getId();
			}

			specimens.clear();
			SessionUtil.getInstance().clearSession();
		}

		clearList(order);
	}

	private DistributionOrderItem distributeSpecimen(DistributionOrder order, Specimen specimen) {
		DistributionOrderItem item = addRate(order, DistributionOrderItem.createOrderItem(order, specimen));
		daoFactory.getDistributionOrderDao().saveOrUpdateOrderItem(item);
		item.distribute();
		return item;
	}

	private LabelPrintJob printDistributionLabels(Collection<DistributionOrderItem> orderItems) {
		List<DistributionOrderItem> toPrintItems = orderItems.stream()
			.filter(DistributionOrderItem::isPrintLabel)
			.sorted(Comparator.comparing(DistributionOrderItem::getId))
			.collect(Collectors.toList());
		return printDistributionLabels(toPrintItems, 1);
	}

	private LabelPrintJob printDistributionLabels(List<DistributionOrderItem> orderItems, int copies) {
		if (CollectionUtils.isEmpty(orderItems)) {
			return null;
		}

		return labelPrinter.print(PrintItem.make(orderItems, copies));
	}

	private void clearList(DistributionOrder order) {
		if (order.getClearListId() == null ||
			order.getClearListMode() == null ||
			order.getClearListMode() == DistributionOrder.ClearListMode.NONE) {
			return;
		}

		if (order.getClearListMode() == DistributionOrder.ClearListMode.ALL) {
			daoFactory.getSpecimenListDao().clearList(order.getClearListId());
		} else if (order.getClearListMode() == DistributionOrder.ClearListMode.DISTRIBUTED) {
			List<Long> specimenIds = new ArrayList<>();
			for (DistributionOrderItem item : order.getOrderItems()) {
				specimenIds.add(item.getSpecimen().getId());
				if (specimenIds.size() == 963) {
					clearList(order.getClearListId(), specimenIds);
				}
			}

			clearList(order.getClearListId(), specimenIds);
		}
	}

	private void clearList(Long listId, List<Long> specimenIds) {
		if (specimenIds.isEmpty()) {
			return;
		}

		daoFactory.getSpecimenListDao().deleteListItems(listId, specimenIds);
		specimenIds.clear();
	}

	private SavedQuery getReportQuery(DistributionOrder order) {
		SavedQuery query = order.getDistributionProtocol().getReport();
		if (query != null) {
			return query;
		}

		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", "distribution_report_query", -1);
		if (queryId == -1) {
			return null;
		}

		return deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
	}

	private QueryDataExportResult exportReport(final DistributionOrder order, SavedQuery report, boolean waitForCompletion) {
		Filter filter = new Filter();
		filter.setField("Specimen.specimenOrders.id");
		filter.setOp(Op.EQ);
		filter.setValues(new String[] { order.getId().toString() });
		
		ExecuteQueryEventOp queryOp = new ExecuteQueryEventOp();
		queryOp.setDrivingForm("Participant");
		queryOp.setAql(report.getAql(new Filter[] { filter }));
		queryOp.setWideRowMode(WideRowMode.DEEP.name());
		queryOp.setRunType("Export");
		queryOp.setSynchronous(waitForCompletion);
		queryOp.setSavedQueryId(report.getId());
		queryOp.setReportName(MessageUtil.getInstance().getMessage("dist_order_report", new String[] { order.getName() }));
		return querySvc.exportQueryData(queryOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "order_" + order.getId() + "_" + UUID.randomUUID();
			}

			@Override
			public void headers(OutputStream out) {
				@SuppressWarnings("serial")
				Map<String, String> headers = new LinkedHashMap<String, String>() {{
					String notSpecified = msg("common_not_specified");
					DistributionProtocol dp = order.getDistributionProtocol();

					put(msg("dist_order_name"),     order.getName());
					put(msg("dist_dp_title"),       dp.getTitle());
					put(msg("dist_dp_short_title"), dp.getShortTitle());
					put(msg("dist_requestor_name"), order.getRequester().formattedName());
					put(msg("dist_requested_date"), Utility.getDateTimeString(order.getExecutionDate()));
					put(msg("dist_receiving_site"), order.getSite() == null ? notSpecified : order.getSite().getName());
					put(msg("dist_tracking_url"),   StringUtils.isBlank(order.getTrackingUrl()) ? notSpecified : order.getTrackingUrl());
					put(msg("dist_checkout_specimens"), Boolean.TRUE.equals(order.getCheckoutSpecimens()) ? msg("common_yes") : msg("common_no"));
					put(msg("dist_comments"),       StringUtils.isBlank(order.getComments()) ? notSpecified : order.getComments());
					put(msg("dp_irb_id"),           StringUtils.isBlank(dp.getIrbId()) ? notSpecified : dp.getIrbId());
					put(msg("dist_exported_by"),    Objects.requireNonNull(AuthUtil.getCurrentUser()).formattedName());
					put(msg("dist_exported_on"),    Utility.getDateTimeString(Calendar.getInstance().getTime()));

					User pi = dp.getPrincipalInvestigator();
					put(msg("dist_dp_pi_inst"),        pi.getInstitute().getName());
					put(msg("dist_dp_pi_email_addr"),  pi.getEmailAddress());
					put(msg("dist_dp_pi_cont_num"),    StringUtils.isBlank(pi.getPhoneNumber()) ? notSpecified : pi.getPhoneNumber());
					put(msg("dist_dp_pi_addr"),        StringUtils.isBlank(pi.getAddress()) ? notSpecified : pi.getAddress());

					DeObject extension = order.getDistributionProtocol().getExtension();
					if (extension != null) {
						putAll(extension.getLabelValueMap());
					}

					if (order.getExtension() != null) {
						putAll(order.getExtension().getLabelValueMap());
					}

					put("", ""); // blank line
				}};

				Utility.writeKeyValuesToCsv(out, headers);
				if (order.getExtension() != null) {
					order.getExtension().writeSubForms(out);
				}
			}
		});
	}

	private String msg(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}
	
	private void notifySaveOrUpdateOrder(DistributionOrder order, Status oldStatus, Long startTime)
	throws UnsupportedEncodingException {
		long timeTaken = (System.currentTimeMillis() - startTime) + 500;

		Status newStatus = order.getStatus();
		if (timeTaken < ASYNC_CALL_TIMEOUT && (!newStatus.equals(Status.EXECUTED) || newStatus.equals(oldStatus))) {
			return;
		}

		Set<User> rcpts = new HashSet<>();
		rcpts.add(AuthUtil.getCurrentUser());
		if (newStatus.equals(Status.EXECUTED)) {
			rcpts.add(order.getDistributor());
			rcpts.add(order.getRequester());
			rcpts.add(order.getDistributionProtocol().getPrincipalInvestigator());
			rcpts.addAll(order.getDistributionProtocol().getCoordinators());

			if (order.getSite() != null && CollectionUtils.isNotEmpty(order.getSite().getCoordinators())) {
				rcpts.addAll(order.getSite().getCoordinators());
			}
		}

		Object[] subjectParams = {order.getId(), order.getName(), newStatus.equals(Status.EXECUTED) ? 1 : 2};
		if (!order.getDistributionProtocol().areEmailNotifsDisabled()) {
			Map<String, Object> emailProps = new HashMap<>();
			emailProps.put("$subject", subjectParams);
			emailProps.put("order", order);

			Pair<String, File> attachment = null;
			if (newStatus.equals(Status.EXECUTED)) {
				attachment = getOrderReportAttachment(order);

				String name = (attachment != null) ? attachment.second().getName() : null;
				String extn = (attachment != null) ? "." + attachment.first() : "";
				emailProps.put("$attachments", Collections.singletonMap(name, Utility.sanitizeFilename(order.getName()) + extn));
			}

			//
			// Send email notification
			//
			File[] attachments = attachment != null ? new File[] { attachment.second() } : null;
			for (User rcpt : rcpts) {
				emailProps.put("rcpt", rcpt);
				emailService.sendEmail(ORDER_DISTRIBUTED_EMAIL_TMPL, new String[] { rcpt.getEmailAddress() }, attachments, emailProps);
			}
		}

		// UI notification
		String msg = MessageUtil.getInstance().getMessage(ORDER_DISTRIBUTED_EMAIL_TMPL + "_subj", subjectParams);
		Notification notif = new Notification();
		notif.setEntityType(DistributionOrder.getEntityName());
		notif.setEntityId(order.getId());
		notif.setOperation(oldStatus == null ? "CREATE" : "UPDATE");
		notif.setCreatedBy(AuthUtil.getCurrentUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setMessage(msg);
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("order-overview", rcpts));
	}

	private Pair<String, File> getOrderReportAttachment(DistributionOrder order)
	throws UnsupportedEncodingException {
		DistributionProtocol.NotifAttachmentType attachType = order.getDistributionProtocol().getAttachmentType();
		if (attachType == null) {
			String attachTypeStr = ConfigUtil.getInstance().getStrSetting("administrative", "order_attachment_type", "none");
			attachType = DistributionProtocol.NotifAttachmentType.valueOf(attachTypeStr.toUpperCase());
		}

		if (attachType == DistributionProtocol.NotifAttachmentType.NONE) {
			return null;
		}

		String filename = Utility.sanitizeFilename(order.getName());
		switch (attachType) {
			case CSV_REPORT -> {
				File dataFile = getCsvReport(order);
				if (dataFile == null) {
					return null;
				}

				return Pair.make("zip", dataFile);
			}
			case MANIFEST -> {
				File manifest = getManifest(order);
				if (manifest == null) {
					return null;
				}
				if (isFileLargerThan1MB(manifest)) {
					String name = MimeUtility.encodeText(filename + ".pdf");
					return Pair.make("zip", zipFile(name, manifest));
				} else {
					return Pair.make("pdf", manifest);
				}
			}
			case BOTH -> {
				List<Pair<String, String>> inputFiles = new ArrayList<>();
				String zipPath = null;
				File dataFile = getCsvReport(order);
				if (dataFile != null) {
					String name = MimeUtility.encodeText(filename + ".zip");
					inputFiles.add(Pair.make(dataFile.getAbsolutePath(), name));
					zipPath = dataFile.getParent();
				}
				File pdfFile = getManifest(order);
				if (pdfFile != null) {
					String name = MimeUtility.encodeText(filename + ".pdf");
					inputFiles.add(Pair.make(pdfFile.getAbsolutePath(), name));
					zipPath = pdfFile.getParent();
				}
				if (!inputFiles.isEmpty()) {
					zipPath = new File(zipPath, filename + ".zip").getAbsolutePath();
					return Pair.make("zip", Utility.zipFilesWithNames(inputFiles, zipPath));
				}
			}
		}

		return null;
	}

	private File getCsvReport(DistributionOrder order) {
		SavedQuery query = getReportQuery(order);
		if (query == null) {
			return null;
		}

		QueryDataExportResult result = exportReport(order, query, true);
		if (!result.isCompleted() || StringUtils.isBlank(result.getDataFile())) {
			return null;
		}

		return result.getDataFileHandle();
	}

	private File getManifest(DistributionOrder order) {
		ManifestGenerator<DistributionOrder> generator = manifestGeneratorFactory.getGenerator(ORDER_MANIFEST);
		if (generator == null) {
			return null;
		}

		return generator.generateManifest(order);
	}

	private boolean isFileLargerThan1MB(File file) {
		return file.length() > 1024 * 1024;
	}

	private File zipFile(String name, File dataFile) {
		String zipPath = new File(dataFile.getParentFile(), dataFile.getName() + ".zip").getAbsolutePath();
		return Utility.zipFilesWithNames(Collections.singletonList(Pair.make(dataFile.getAbsolutePath(), name)), zipPath);
	}

	private void notifyFailedOrder(ResponseEvent<?> resp) {
		if (resp.isSuccessful()) {
			return;
		}

		logger.error("Error saving/updating the order: " + resp.getError().getMessage());
		User currentUser = AuthUtil.getCurrentUser();
		if (currentUser == null || StringUtils.isBlank(currentUser.getEmailAddress())) {
			return;
		}

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[0]);
		emailProps.put("errorMsg", resp.getError().getMessage());
		emailProps.put("rcpt", currentUser.formattedName());
		emailService.sendEmail(ORDER_FAILED_EMAIL_TMPL, new String[] { currentUser.getEmailAddress() }, null, emailProps);
	}

	private void notifyOrderDeleted(String name, DistributionOrder order) {
		if (order.getDistributionProtocol().areEmailNotifsDisabled()) {
			return;
		}

		Set<User> rcpts = new HashSet<>();
		rcpts.add(AuthUtil.getCurrentUser());
		rcpts.add(order.getDistributor());
		rcpts.add(order.getRequester());
		rcpts.add(order.getDistributionProtocol().getPrincipalInvestigator());
		rcpts.addAll(order.getDistributionProtocol().getCoordinators());
		if (order.getSite() != null && CollectionUtils.isNotEmpty(order.getSite().getCoordinators())) {
			rcpts.addAll(order.getSite().getCoordinators());
		}

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[] { order.getId(), name });
		emailProps.put("order", order);
		emailProps.put("orderName", name);
		emailProps.put("deletedBy", AuthUtil.getCurrentUser());
		for (User rcpt : rcpts) {
			emailProps.put("rcpt", rcpt);
			emailService.sendEmail(ORDER_DELETED_EMAIL_TMPL, new String[] { rcpt.getEmailAddress() }, null, emailProps);
		}
	}

	private void notifyFailedOrderDelete(Long orderId, String errorMessage) {
		User currentUser = AuthUtil.getCurrentUser();
		if (currentUser == null || StringUtils.isBlank(currentUser.getEmailAddress())) {
			return;
		}

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[] { orderId });
		emailProps.put("errorMsg", errorMessage);
		emailProps.put("orderId", orderId);
		emailProps.put("rcpt", currentUser);
		emailService.sendEmail(ORDER_DELETE_FAILED_EMAIL_TMPL, new String[] { currentUser.getEmailAddress() }, null, emailProps);
	}

	private void notifyOrderItemsDeleted(DistributionOrder order, List<DistributionOrderItem> deletedItems) {
		if (order.getDistributionProtocol().areEmailNotifsDisabled()) {
			return;
		}

		Set<User> rcpts = new HashSet<>();
		rcpts.add(AuthUtil.getCurrentUser());
		rcpts.add(order.getDistributor());
		rcpts.add(order.getRequester());
		rcpts.add(order.getDistributionProtocol().getPrincipalInvestigator());
		rcpts.addAll(order.getDistributionProtocol().getCoordinators());
		if (order.getSite() != null && CollectionUtils.isNotEmpty(order.getSite().getCoordinators())) {
			rcpts.addAll(order.getSite().getCoordinators());
		}

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("$subject", new Object[] { order.getId(), order.getName() });
		emailProps.put("order", order);
		emailProps.put("deletedItems", deletedItems);
		emailProps.put("deletedBy", AuthUtil.getCurrentUser());
		for (User rcpt : rcpts) {
			emailProps.put("rcpt", rcpt);
			emailService.sendEmail(ORDER_ITEMS_DELETED_EMAIL_TMPL, new String[] { rcpt.getEmailAddress() }, null, emailProps);
		}
	}

	private DistributionOrder getOrder(Long orderId, String orderName) {
		DistributionOrder order = null;
		Object key = null;

		if (orderId != null) {
			order = daoFactory.getDistributionOrderDao().getById(orderId);
			key = orderId;
		} else if (StringUtils.isNotBlank(orderName)) {
			order = daoFactory.getDistributionOrderDao().getOrder(orderName);
			key = orderName;
		}

		if (order == null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.NOT_FOUND, key);
		}

		return order;
	}

	private Specimen returnSpecimen(
			ReturnedSpecimenDetail detail,
			Map<String, DistributionOrder> ordersMap,
			Map<String, StorageContainer> containersMap) {

		String orderName = detail.getOrderName();
		if (StringUtils.isBlank(orderName)) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.NAME_REQUIRED);
		}

		String label = detail.getSpecimenLabel();
		if (StringUtils.isBlank(label)) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.LABEL_REQUIRED);
		}

		DistributionOrder order = ordersMap.get(orderName);
		if (order == null) {
			order = getOrder(null, detail.getOrderName());
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(order);
			if (!order.isOrderExecuted()) {
				throw OpenSpecimenException.userError(DistributionOrderErrorCode.NOT_DISTRIBUTED, order.getName());
			}

			ordersMap.put(order.getName(), order);
		}

		DistributionOrderItem item = daoFactory.getDistributionOrderDao().getOrderItem(order.getId(), label);
		if (item == null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.SPMN_NOT_FOUND, label, orderName);
		}

		returnSpecimen(detail, item, containersMap);
		return item.getSpecimen();
	}


	private void returnSpecimen(ReturnedSpecimenDetail detail, DistributionOrderItem item, Map<String, StorageContainer> containersMap) {
		ensureItemNotReturned(item);
		setItemReturnedQty(item, detail.getQuantity());
		setItemReturnDate(item, detail.getTime());
		setItemReturnedBy(item, detail.getUser());
		setItemReturningPosition(item, detail.getLocation(), containersMap);
		setItemFreezeThawIncrOnReturn(item, detail.getIncrFreezeThaw());
		item.setReturnComments(detail.getComments());
		item.returnSpecimen();
	}

	private void ensureItemNotReturned(DistributionOrderItem item) {
		if (item.isReturned()) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.SPECIMEN_ALREADY_RETURNED, item.getSpecimen().getLabel());
		}
	}

	private void setItemReturnedQty(DistributionOrderItem item, BigDecimal returnQty) {
		BigDecimal distQty = item.getQuantity();
		if (returnQty == null && distQty != null) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.RETURN_QTY_REQ, item.getSpecimen().getLabel());
		}

		if (NumUtil.lessThanZero(returnQty) || // returnQty < 0
			NumUtil.lessThan(distQty, returnQty) || // distQty < returnQty
			(NumUtil.greaterThanZero(distQty) && NumUtil.isZero(returnQty))) { // distQty > 0 && returnQty == 0
			raiseError(DistributionOrderErrorCode.INVALID_RETURN_QUANTITY, item.getSpecimen().getLabel(), returnQty);
		}

		item.setReturnedQuantity(returnQty);
	}

	private void setItemReturnDate(DistributionOrderItem item, Date returnDate) {
		if (returnDate == null) {
			raiseError(DistributionOrderErrorCode.RETURN_DATE_REQ, item.getSpecimen().getLabel());
		}

		if (item.getOrder().getExecutionDate().after(returnDate)) {
			raiseError(DistributionOrderErrorCode.INVALID_RETURN_DATE, item.getSpecimen().getLabel(), returnDate);
		}

		item.setReturnDate(returnDate);
	}

	private void setItemReturningPosition(DistributionOrderItem item, StorageLocationSummary location, Map<String, StorageContainer> containersMap) {
		if (location == null || StringUtils.isBlank(location.getName())) {
			return;
		}

		StorageContainer container = containersMap.get(location.getName());
		if (container == null) {
			Object key = null;
			if (location.getId() != null) {
				container = daoFactory.getStorageContainerDao().getById(location.getId());
				key = location.getId();
			} else {
				container = daoFactory.getStorageContainerDao().getByName(location.getName());
				key = location.getName();
			}

			if (container == null) {
				raiseError(StorageContainerErrorCode.NOT_FOUND, key, 1);
			}

			containersMap.put(location.getName(), container);
		}


		Specimen specimen = item.getSpecimen();
		if (!container.canContain(specimen)) {
			raiseError(StorageContainerErrorCode.CANNOT_HOLD_SPECIMEN, container.getName(), specimen.getLabelOrDesc());
		}

		//
		// TODO: This is duplicate code. Need to consolidate this with specimen/container objects
		//
		String row = null, column = null;
		if (!container.isDimensionless()) {
			row = location.getPositionY();
			column = location.getPositionX();
			if (container.usesLinearLabelingMode() && location.getPosition() != null && location.getPosition() != 0) {
				Pair<Integer, Integer> coord = container.getPositionAssigner().fromPosition(container, location.getPosition());
				row = coord.first().toString();
				column = coord.second().toString();
			}
		}

		StorageContainerPosition position = null;
		if (StringUtils.isNotBlank(row) && StringUtils.isNotBlank(column)) {
			if (container.canSpecimenOccupyPosition(specimen.getId(), column, row)) {
				position = container.createPosition(column, row);
				container.setLastAssignedPos(position);
			} else {
				raiseError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}
		} else {
			position = container.nextAvailablePosition(true);
			if (position == null) {
				raiseError(StorageContainerErrorCode.NO_FREE_SPACE, container.getName());
			}
		}

		item.setReturningContainer(container);
		item.setReturningRow(position.getPosTwo());
		item.setReturningColumn(position.getPosOne());
	}

	private void setItemFreezeThawIncrOnReturn(DistributionOrderItem item, Integer incrFreezeThaw) {
		item.getSpecimen().incrementFreezeThaw(incrFreezeThaw);
		item.setFreezeThawIncrOnReturn(incrFreezeThaw);
	}

	private void setItemReturnedBy(DistributionOrderItem item, UserSummary userDetail) {
		if (userDetail == null || (userDetail.getId() == null && StringUtils.isBlank(userDetail.getEmailAddress()))) {
			raiseError(DistributionOrderErrorCode.RETURNED_BY_REQ, item.getSpecimen().getLabel());
		}

		Object key = null;
		User user = null;
		if (userDetail.getId() != null) {
			key = userDetail.getId();
			user = daoFactory.getUserDao().getById(userDetail.getId());
		} else {
			key = userDetail.getEmailAddress();
			user = daoFactory.getUserDao().getUserByEmailAddress(userDetail.getEmailAddress());
		}

		if (user == null) {
			raiseError(UserErrorCode.NOT_FOUND, key);
		}

		item.setReturnedBy(user);
	}

	private void ensureUserCanRetrieveSpecimens(DistributionOrder order) {
		try {
			AccessCtrlMgr.getInstance().ensureUpdateDistributionOrderRights(order);
		} catch (OpenSpecimenException ose) {
			if (ose.getErrors().size() > 1 || !ose.containsError(RbacErrorCode.ACCESS_DENIED)) {
				throw ose;
			}

			User currentUser = AuthUtil.getCurrentUser();
			DistributionProtocol dp = order.getDistributionProtocol();
			if (!order.getRequester().equals(currentUser) &&
				!dp.getPrincipalInvestigator().equals(currentUser) &&
				!dp.getCoordinators().contains(currentUser)) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}
		}
	}

	private ListConfig getOrderSpecimensConfig(Map<String, Object> listReq) {
		Number orderId = (Number) listReq.get("orderId");
		if (orderId == null) {
			orderId = (Number) listReq.get("objectId");
		}

		if (orderId == null) {
			return null;
		}

		DistributionOrder order = getOrder(orderId.longValue(), null);
		AccessCtrlMgr.getInstance().ensureReadDistributionOrderRights(order);

		ListConfig cfg = ListUtil.getSpecimensListConfig("order-specimens-list-view", false);
		ListUtil.addHiddenFieldsOfSpecimen(cfg);
		if (cfg == null) {
			return null;
		}

		Column itemId = new Column();
		itemId.setExpr("Specimen.specimenOrders.itemId");
		itemId.setCaption("orderItemId_");
		cfg.getHiddenColumns().add(itemId);

		String restriction = "Specimen.specimenOrders.id = " + order.getId();
		cfg.setDrivingForm("Specimen");
		cfg.setRestriction(restriction);
		cfg.setDistinct(true);

		if (CollectionUtils.isEmpty(cfg.getOrderBy())) {
			Column orderBy = new Column();
			orderBy.setExpr("Specimen.specimenOrders.id");
			orderBy.setDirection("asc");
			cfg.setOrderBy(Collections.singletonList(orderBy));
		}

		return ListUtil.setListLimit(cfg, listReq);
	}

	private void raiseError(ErrorCode error, Object ... args) {
		throw OpenSpecimenException.userError(error, args);
	}

	private static final String ORDER_DISTRIBUTED_EMAIL_TMPL = "order_distributed";

	private static final String ORDER_FAILED_EMAIL_TMPL = "order_failed";

	private static final String ORDER_DELETED_EMAIL_TMPL = "order_deleted";

	private static final String ORDER_ITEMS_DELETED_EMAIL_TMPL = "order_items_deleted";

	private static final String ORDER_DELETE_FAILED_EMAIL_TMPL = "order_delete_failed";
}
