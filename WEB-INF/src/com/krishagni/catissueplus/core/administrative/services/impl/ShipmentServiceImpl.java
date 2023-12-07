package com.krishagni.catissueplus.core.administrative.services.impl;

import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.Shipment.Status;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSavedEvent;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ShipmentContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.administrative.repository.ShipmentDao;
import com.krishagni.catissueplus.core.administrative.repository.StorageContainerListCriteria;
import com.krishagni.catissueplus.core.administrative.services.ShipmentService;
import com.krishagni.catissueplus.core.administrative.services.StorageContainerService;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.EmailService;
import com.krishagni.catissueplus.core.common.service.ObjectAccessor;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Filter;
import com.krishagni.catissueplus.core.de.domain.Filter.Op;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;
import com.krishagni.catissueplus.core.de.events.ExecuteQueryEventOp;
import com.krishagni.catissueplus.core.de.events.QueryDataExportResult;
import com.krishagni.catissueplus.core.de.services.QueryService;
import com.krishagni.catissueplus.core.de.services.SavedQueryErrorCode;
import com.krishagni.rbac.common.errors.RbacErrorCode;

import edu.common.dynamicextensions.query.WideRowMode;

public class ShipmentServiceImpl implements ShipmentService, ObjectAccessor {
	private static final String SHIPMENT_REQUESTED_EMAIL_TMPL = "shipment_requested";

	private static final String SHIPMENT_SHIPPED_EMAIL_TMPL = "shipment_shipped";
	
	private static final String SHIPMENT_RECEIVED_EMAIL_TMPL = "shipment_received";
	
	private static final String SHIPMENT_QUERY_REPORT_SETTING = "shipment_export_report";
	
	private DaoFactory daoFactory;
	
	private ShipmentFactory shipmentFactory;
	
	private EmailService emailService;
	
	private QueryService querySvc;

	private StorageContainerService containerSvc;
	
	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setShipmentFactory(ShipmentFactory shipmentFactory) {	
		this.shipmentFactory = shipmentFactory;
	}

	public void setEmailService(EmailService emailService) {
		this.emailService = emailService;
	}
	
	public void setQuerySvc(QueryService querySvc) {
		this.querySvc = querySvc;
	}

	public void setContainerSvc(StorageContainerService containerSvc) {
		this.containerSvc = containerSvc;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ShipmentDetail>> getShipments(RequestEvent<ShipmentListCriteria> req) {
		try {
			ShipmentListCriteria listCrit = addShipmentListCriteria(req.getPayload());
			List<ShipmentDetail> result = ShipmentDetail.from(getShipmentDao().getShipments(listCrit));
			if (listCrit.includeStat() && !result.isEmpty()) {
				Map<Long, ShipmentDetail> shipmentsMap = result.stream().collect(Collectors.toMap(ShipmentDetail::getId, s -> s));
				Map<Long, Integer> spmnsCount = getShipmentDao().getSpecimensCount(shipmentsMap.keySet());
				spmnsCount.forEach((shipmentId, count) -> shipmentsMap.get(shipmentId).setSpecimensCount(count));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Long> getShipmentsCount(RequestEvent<ShipmentListCriteria> req) {
		try {
			ShipmentListCriteria crit = addShipmentListCriteria(req.getPayload());
			return ResponseEvent.response(getShipmentDao().getShipmentsCount(crit));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> getShipment(RequestEvent<Long> req) {
		try {
			Shipment shipment = getShipment(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
			return ResponseEvent.response(ShipmentDetail.from(shipment));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ShipmentContainerDetail>> getShipmentContainers(RequestEvent<ShipmentItemsListCriteria> req) {
		try {
			ShipmentItemsListCriteria crit = req.getPayload();
			Shipment shipment = getShipment(crit.shipmentId(), null);
			AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
			if (shipment.isSpecimenShipment()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<ShipmentContainer> shipmentContainers = getShipmentDao().getShipmentContainers(crit);
			if (shipmentContainers.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<ShipmentContainerDetail> result = ShipmentContainerDetail.from(shipmentContainers);
			Map<Long, ShipmentContainerDetail> containersMap = result.stream()
				.collect(Collectors.toMap(sc -> sc.getContainer().getId(), sc -> sc));

			Map<Long, Integer> spmnCounts;
			if (shipment.isPending() || shipment.isRequested()) {
				spmnCounts = daoFactory.getStorageContainerDao().getSpecimensCount(containersMap.keySet());
			} else {
				spmnCounts = getShipmentDao().getSpecimensCountByContainer(shipment.getId(), containersMap.keySet());
			}

			spmnCounts.forEach((cid, count) -> containersMap.get(cid).setSpecimensCount(count));
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<ShipmentSpecimenDetail>> getShipmentSpecimens(RequestEvent<ShipmentItemsListCriteria> req) {
		try {
			ShipmentItemsListCriteria crit = req.getPayload();
			Shipment shipment = getShipment(crit.shipmentId(), null);
			AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
			return ResponseEvent.response(ShipmentSpecimenDetail.from(getShipmentDao().getShipmentSpecimens(crit)));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> createShipment(RequestEvent<ShipmentDetail> req) {
		try {
			ShipmentDetail detail = req.getPayload();
			Shipment shipment = shipmentFactory.createShipment(detail, Status.PENDING);
			AccessCtrlMgr.getInstance().ensureCreateShipmentRights(shipment);

			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureValidShipmentStatus(shipment, detail.getStatus(), ose);
			ensureUniqueConstraint(null, shipment, ose);
			ensureValidItems(null, shipment, ose);
			ensureValidNotifyUsers(shipment, ose);
			ose.checkAndThrow();

			Status status = Status.fromName(detail.getStatus());
			if (status == Status.SHIPPED) {
				createRecvSiteContainer(shipment);
				shipment.ship();
			} else if (status == Status.REQUESTED) {
				shipment.setStatus(status);
			}

			getShipmentDao().saveOrUpdate(shipment, true);
			sendEmailNotifications(shipment, null, detail.isSendMail());
			EventPublisher.getInstance().publish(new ShipmentSavedEvent(shipment));
			return ResponseEvent.response(ShipmentDetail.from(shipment));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> updateShipment(RequestEvent<ShipmentDetail> req) {
		try {
			ShipmentDetail detail = req.getPayload();
			Shipment existing = getShipment(detail.getId(), detail.getName());

			Shipment newShipment = shipmentFactory.createShipment(detail, null);
			if (existing.getType() != newShipment.getType()) {
				return ResponseEvent.userError(ShipmentErrorCode.CANNOT_CHG_TYPE);
			}

			if (newShipment.isDeleted()) {
				AccessCtrlMgr.getInstance().ensureDeleteShipmentRights(existing);
				existing.delete();
				return ResponseEvent.response(ShipmentDetail.from(existing));
			}

			AccessCtrlMgr.getInstance().ensureUpdateShipmentRights(newShipment);
			OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
			ensureUniqueConstraint(existing, newShipment, ose);
			ensureValidItems(existing, newShipment, ose);
			ensureValidNotifyUsers(newShipment, ose);
			ose.checkAndThrow();
			
			Status oldStatus = existing.getStatus();
			if (newShipment.getStatus() == Status.SHIPPED) {
				createRecvSiteContainer(newShipment);
			}

			existing.update(newShipment);
			getShipmentDao().saveOrUpdate(existing, true);
			sendEmailNotifications(newShipment, oldStatus, detail.isSendMail());
			EventPublisher.getInstance().publish(new ShipmentSavedEvent(existing));
			return ResponseEvent.response(ShipmentDetail.from(existing));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ShipmentDetail> deleteShipment(RequestEvent<Long> req) {
		try {
			Shipment shipment = getShipment(req.getPayload(), null);
			AccessCtrlMgr.getInstance().ensureDeleteShipmentRights(shipment);
			shipment.delete();
			return ResponseEvent.response(ShipmentDetail.from(shipment));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<QueryDataExportResult> exportReport(RequestEvent<Long> req) {
		Shipment shipment = getShipmentDao().getById(req.getPayload());
		if (shipment == null) {
			return ResponseEvent.userError(ShipmentErrorCode.NOT_FOUND);
		}
		
		AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
		Integer queryId = ConfigUtil.getInstance().getIntSetting("common", SHIPMENT_QUERY_REPORT_SETTING, -1);
		if (queryId == -1) {
			return ResponseEvent.userError(ShipmentErrorCode.RPT_TMPL_NOT_CONF);
		}
		
		SavedQuery query = deDaoFactory.getSavedQueryDao().getQuery(queryId.longValue());
		if (query == null) {
			return ResponseEvent.userError(SavedQueryErrorCode.NOT_FOUND, queryId);
		}
		
		return new ResponseEvent<>(exportShipmentReport(shipment, query));
	}

	@Override
	@PlusTransactional
	public List<StorageContainerSummary> getContainers(List<String> names, boolean request, String sendSiteName, String recvSiteName) {
		if (CollectionUtils.isEmpty(names)) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.CONT_NAMES_REQ);
		}

		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessContainerSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw  OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		//
		// when creating requests, users do not have access to the container stored at the sending site
		// therefore suppress the access checks
		//
		StorageContainerListCriteria crit = new StorageContainerListCriteria().siteCps(request ? null : siteCps).names(names);
		List<StorageContainer> containers = daoFactory.getStorageContainerDao().getStorageContainers(crit);
		if (containers.isEmpty()) {
			return Collections.emptyList();
		}

		ensureSpecimensAreAccessible(containers);

		if (StringUtils.isNotBlank(sendSiteName)) {
			ensureValidContainerSendingSites(containers, getSite(sendSiteName));
		}

		if (StringUtils.isNotBlank(recvSiteName)) {
			ensureValidContainerRecvSite(containers, getSite(recvSiteName));
		}

		ensureContainersAreNotShipped(containers);

		List<Long> containerIds = containers.stream().map(StorageContainer::getId).collect(Collectors.toList());
		Map<Long, Integer> spmnsCount = daoFactory.getStorageContainerDao().getSpecimensCount(containerIds);
		return containers.stream().map(
			s -> {
				StorageContainerSummary result = StorageContainerSummary.from(s);
				if (s.getAllowedCps() != null) {
					result.setAllowedCollectionProtocols(s.getAllowedCps().stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet()));
				}

				if (s.getCompAllowedCps() != null) {
					result.setCalcAllowedCollectionProtocols(s.getCompAllowedCps().stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet()));
				}

				if (s.getAllowedSpecimenClasses() != null) {
					result.setAllowedSpecimenClasses(PermissibleValue.toValueSet(s.getAllowedSpecimenClasses()));
				}

				if (s.getCompAllowedSpecimenClasses() != null) {
					result.setCalcAllowedSpecimenClasses(PermissibleValue.toValueSet(s.getCompAllowedSpecimenClasses()));
				}

				if (s.getAllowedSpecimenTypes() != null) {
					result.setAllowedSpecimenTypes(PermissibleValue.toValueSet(s.getAllowedSpecimenTypes()));
				}

				if (s.getCompAllowedSpecimenTypes() != null) {
					result.setCalcAllowedSpecimenTypes(PermissibleValue.toValueSet(s.getCompAllowedSpecimenTypes()));
				}

				result.setStoredSpecimens(spmnsCount.get(s.getId()));
				return result;
			}).collect(Collectors.toList());
	}

	@Override
	public String getObjectName() {
		return Shipment.getEntityName();
	}

	@Override
	@PlusTransactional
	public Map<String, Object> resolveUrl(String key, Object value) {
		if (key.equals("id")) {
			value = Long.valueOf(value.toString());
		}

		return daoFactory.getShipmentDao().getShipmentIds(key, value);
	}

	@Override
	public String getAuditTable() {
		return "OS_SHIPMENTS_AUD";
	}

	@Override
	public void ensureReadAllowed(Long id) {
		Shipment shipment = getShipment(id, null);
		AccessCtrlMgr.getInstance().ensureReadShipmentRights(shipment);
	}

	private ShipmentListCriteria addShipmentListCriteria(ShipmentListCriteria crit) {
		Set<SiteCpPair> sites = AccessCtrlMgr.getInstance().getReadAccessShipmentSites();
		if (sites != null && sites.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
		
		if (sites != null) {
			crit.sites(sites);
		}
		
		return crit;
	}

	private List<Specimen> getValidSpecimens(List<Long> specimenIds, OpenSpecimenException ose) {
		List<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			ose.addError(ShipmentErrorCode.INVALID_SPECIMENS);
			return null;
		}
		
		SpecimenListCriteria crit = new SpecimenListCriteria().ids(specimenIds).siteCps(siteCps);
		List<Specimen> specimens = daoFactory.getSpecimenDao().getSpecimens(crit);
		if (specimenIds.size() != specimens.size()) {
			ose.addError(ShipmentErrorCode.INVALID_SPECIMENS);
			return null;
		}
		
		return specimens;
	}
	
	private void ensureValidShipmentStatus(Shipment shipment, String shipmentStatus, OpenSpecimenException ose) {
		if (StringUtils.isBlank(shipmentStatus)) {
			return;
		}
		
		Status status = Status.fromName(shipmentStatus);
		if (status == null) {
			ose.addError(ShipmentErrorCode.INVALID_STATUS);
		} else if (shipment.isRequest() && (status != Status.PENDING && status != Status.REQUESTED)) {
			ose.addError(ShipmentErrorCode.REQ_INV_CREATE_STATUS, shipment.getName(), status.getName());
		} else if (status == Status.RECEIVED) {
			ose.addError(ShipmentErrorCode.NOT_SHIPPED_TO_RECV, shipment.getName());
		}
	}

	private void ensureUniqueConstraint(Shipment existing, Shipment newShipment, OpenSpecimenException ose) {
		if (existing != null && newShipment.getName().equals(existing.getName())) {
			return;
		}

		Shipment shipment = getShipmentDao().getShipmentByName(newShipment.getName());
		if (shipment != null) {
			ose.addError(ShipmentErrorCode.DUP_NAME, newShipment.getName());
		}
	}

	private void ensureValidItems(Shipment existing, Shipment shipment, OpenSpecimenException ose) {
		if (shipment.isSpecimenShipment()) {
			ensureValidSpecimens(existing, shipment, ose);
		} else {
			ensureValidContainers(existing, shipment, ose);
		}
	}
	
	private void ensureValidSpecimens(Shipment existing, Shipment shipment, OpenSpecimenException ose) {
		if (existing != null && !(existing.isPending() || existing.isRequested())) {
			return;
		}

		List<Long> specimenIds = Utility.collect(shipment.getShipmentSpecimens(), "specimen.id");
		List<Specimen> specimens = getValidSpecimens(specimenIds, ose);
		if (specimens == null) {
			return;
		}
		
		ensureSpecimensAreAvailable(specimens, ose);
		ensureValidSpecimenSites(specimens, shipment.getSendingSite(), shipment.getReceivingSite(), ose);
		ensureSpecimensAreNotShipped(shipment, specimenIds, ose);
	}

	private void ensureValidContainers(Shipment existing, Shipment shipment, OpenSpecimenException ose) {
		if (existing != null && !(existing.isPending() || existing.isRequested())) {
			return;
		}

		List<StorageContainer> containers = shipment.getShipmentContainers().stream()
			.map(ShipmentContainer::getContainer).collect(Collectors.toList());

		boolean accessible = ensureSpecimensAreAccessible(containers, ose);
		if (!accessible) {
			return;
		}

		ensureValidContainerSites(containers, shipment.getSendingSite(), shipment.getReceivingSite(), ose);
		ensureContainersAreNotShipped(shipment, containers, ose);
	}

	private boolean ensureSpecimensAreAccessible(List<StorageContainer> containers) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureSpecimensAreAccessible(containers, ose);
		ose.checkAndThrow();
		return true;
	}

	private boolean ensureSpecimensAreAccessible(List<StorageContainer> containers, OpenSpecimenException ose) {
		List<SiteCpPair> siteCpPairs = AccessCtrlMgr.getInstance().getReadAccessSpecimenSiteCps();
		if (siteCpPairs != null && siteCpPairs.isEmpty()) {
			ose.addError(SpecimenErrorCode.ACCESS_DENIED, null, 0);
			return false;
		}

		List<Long> containerIds = containers.stream().map(StorageContainer::getId).collect(Collectors.toList());
		boolean useMrnSites = AccessCtrlMgr.getInstance().isAccessRestrictedBasedOnMrn();
		Map<String, List<String>> invalidSpmns = daoFactory.getStorageContainerDao()
			.getInaccessibleSpecimens(containerIds, siteCpPairs, useMrnSites, 5);
		if (!invalidSpmns.isEmpty()) {
			Map.Entry<String, List<String>> contSpmns = invalidSpmns.entrySet().iterator().next();
			String errorLabels = StringUtils.join(contSpmns.getValue(), ", ") + " (" + contSpmns.getKey() + ")";
			ose.addError(SpecimenErrorCode.ACCESS_DENIED, errorLabels, 1);
			return false;
		}

		return true;
	}

	private void ensureSpecimensAreAvailable(List<Specimen> specimens, OpenSpecimenException ose) {
		List<Specimen> closedSpecimens = new ArrayList<>();
		List<Specimen> unavailableSpecimens = new ArrayList<>();
		for (Specimen specimen : specimens) {
			if (specimen.isClosed()) {
				closedSpecimens.add(specimen);
			} else if (!specimen.isAvailable()) {
				unavailableSpecimens.add(specimen);
			}
		}
		
		if (!closedSpecimens.isEmpty()) {
			String labels = closedSpecimens.stream().map(Specimen::getLabel).collect(Collectors.joining(", "));
			ose.addError(ShipmentErrorCode.CLOSED_SPECIMENS, labels);
		}
		
		if (!unavailableSpecimens.isEmpty()) {
			String labels = unavailableSpecimens.stream().map(Specimen::getLabel).collect(Collectors.joining(", "));
			ose.addError(ShipmentErrorCode.UNAVAILABLE_SPECIMENS, labels);
		}
	}

	private void ensureValidSpecimenSites(List<Specimen> specimens, Site sendingSite, Site receivingSite, OpenSpecimenException ose) {
		Map<Long, Specimen> specimenMap = specimens.stream().collect(Collectors.toMap(Specimen::getId, spmn -> spmn));
		ensureValidSpecimenSendingSites(specimenMap, sendingSite, ose);
		ensureValidSpecimenRecvSites(specimenMap, receivingSite, ose);
	}

	private void ensureValidContainerSites(List<StorageContainer> containers, Site sendingSite, Site receivingSite, OpenSpecimenException ose) {
		ensureValidContainerSendingSites(containers, sendingSite, ose);
		ensureValidContainerRecvSite(containers, receivingSite, ose);
	}

	private void ensureValidSpecimenSendingSites(Map<Long, Specimen> specimenMap, Site sendingSite, OpenSpecimenException ose) {
		Map<Long, Long> spmnStorageSites = daoFactory.getSpecimenDao().getSpecimenStorageSite(specimenMap.keySet());

		String invalidSpmnLabels = specimenMap.values().stream()
			.filter(spmn -> {
				Long spmnSiteId = spmnStorageSites.get(spmn.getId());
				return spmnSiteId != null && !spmnSiteId.equals(sendingSite.getId());
			})
			.map(Specimen::getLabel)
			.collect(Collectors.joining(", "));

		if (StringUtils.isNotBlank(invalidSpmnLabels)) {
			ose.addError(ShipmentErrorCode.SPMN_NOT_STORED_AT_SEND_SITE, invalidSpmnLabels, sendingSite.getName());
		}
	}

	private void ensureValidContainerSendingSites(List<StorageContainer> containers, Site sendingSite) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidContainerSendingSites(containers, sendingSite, ose);
		ose.checkAndThrow();
	}

	private void ensureValidContainerSendingSites(List<StorageContainer> containers, Site sendingSite, OpenSpecimenException ose) {
		List<String> invalidContainers = containers.stream()
			.filter(c -> !c.getSite().equals(sendingSite))
			.limit(5)
			.map(StorageContainer::getName)
			.collect(Collectors.toList());

		if (!invalidContainers.isEmpty()) {
			ose.addError(ShipmentErrorCode.CONTS_NOT_AT_SEND_SITE, sendingSite.getName(), StringUtils.join(invalidContainers, ", "), invalidContainers.size());
		}
	}

	private void ensureValidSpecimenRecvSites(Map<Long, Specimen> specimenMap, Site receivingSite, OpenSpecimenException ose) {
		Map<Long, Set<SiteCpPair>> spmnSites = daoFactory.getSpecimenDao().getSpecimenSites(specimenMap.keySet());

		SiteCpPair recvInstituteSite = SiteCpPair.make(receivingSite.getInstitute().getId(), receivingSite.getId(), null);
		String invalidSpmnLabels = spmnSites.entrySet().stream()
			.filter(spmnSite -> !SiteCpPair.contains(spmnSite.getValue(), recvInstituteSite))
			.map(spmnSite -> specimenMap.get(spmnSite.getKey()).getLabel())
			.collect(Collectors.joining(", "));

		if (StringUtils.isNotBlank(invalidSpmnLabels)) {
			ose.addError(ShipmentErrorCode.CANNOT_STORE_SPMN_AT_RECV_SITE, invalidSpmnLabels, receivingSite.getName());
		}
	}

	private void ensureValidContainerRecvSite(List<StorageContainer> containers, Site receivingSite) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureValidContainerRecvSite(containers, receivingSite, ose);
		ose.checkAndThrow();
	}

	private void ensureValidContainerRecvSite(List<StorageContainer> containers, Site receivingSite, OpenSpecimenException ose) {
		List<Long> containerIds = containers.stream().map(StorageContainer::getId).collect(Collectors.toList());
		Map<String, List<String>> invalidSpmns = daoFactory.getStorageContainerDao()
			.getInvalidSpecimensForSite(containerIds, receivingSite.getId(), 5);

		if (!invalidSpmns.isEmpty()) {
			Map.Entry<String, List<String>> contSpmns = invalidSpmns.entrySet().iterator().next();
			String errorLabels = contSpmns.getKey() + " (" + StringUtils.join(contSpmns.getValue(), ", ") + ")";
			ose.addError(ShipmentErrorCode.CANNOT_STORE_CONT_AT_RECV_SITE, receivingSite.getName(), errorLabels);
		}
	}
	
	private void ensureSpecimensAreNotShipped(Shipment shipment, List<Long> specimenIds, OpenSpecimenException ose) {
		if (shipment.isReceived()) {
			return;
		}
		
		List<Specimen> shippedSpecimens = getShipmentDao().getShippedSpecimensByIds(specimenIds);
		if (CollectionUtils.isNotEmpty(shippedSpecimens)) {
			String labels = shippedSpecimens.stream().map(Specimen::getLabel).collect(Collectors.joining(", "));
			ose.addError(ShipmentErrorCode.SPECIMEN_ALREADY_SHIPPED, labels);
		}
	}

	private void ensureContainersAreNotShipped(Shipment shipment, List<StorageContainer> containers, OpenSpecimenException ose) {
		if (shipment.isReceived()) {
			return;
		}

		ensureContainersAreNotShipped(containers, ose);
	}

	private void ensureContainersAreNotShipped(List<StorageContainer> containers) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		ensureContainersAreNotShipped(containers, ose);
		ose.checkAndThrow();
	}

	private void ensureContainersAreNotShipped(List<StorageContainer> containers, OpenSpecimenException ose) {
		List<Long> containerIds = containers.stream().map(StorageContainer::getId).collect(Collectors.toList());
		List<StorageContainer> shippedContainers = daoFactory.getStorageContainerDao().getShippedContainers(containerIds);
		if (!shippedContainers.isEmpty()) {
			List<String> names = shippedContainers.stream().limit(5).map(StorageContainer::getName).collect(Collectors.toList());
			ose.addError(ShipmentErrorCode.CONTAINER_ALREADY_SHIPPED, StringUtils.join(names, ", "), names.size());
		}
	}

	private void ensureValidNotifyUsers(Shipment shipment, OpenSpecimenException ose) {
		if (shipment.isReceived()) {
			return;
		}
		
		Institute institute = shipment.getReceivingSite().getInstitute();
		shipment.getNotifyUsers().stream()
			.filter(user -> !user.getInstitute().equals(institute))
			.forEach(user -> ose.addError(ShipmentErrorCode.NOTIFY_USER_NOT_BELONG_TO_INST, user.formattedName(), institute.getName()));
	}

	private Shipment getShipment(Long id, String name) {
		Shipment shipment = null;
		if (id != null) {
			shipment = getShipmentDao().getById(id);
		} else if (StringUtils.isNotBlank(name)) {
			shipment = getShipmentDao().getShipmentByName(name);
		}

		if (shipment == null) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.NOT_FOUND);
		}
		
		return shipment;
	}

	private void createRecvSiteContainer(Shipment shipment) {
		if (shipment.getReceivingSite().getContainer() != null) {
			return;
		}

		Site recvSite = shipment.getReceivingSite();
		StorageContainer recvSiteContainer = containerSvc.createSiteContainer(recvSite.getId(), recvSite.getName());
		recvSite.setContainer(recvSiteContainer);
	}

	private Site getSite(String siteName) {
		Site site = daoFactory.getSiteDao().getSiteByName(siteName);
		if (site == null) {
			throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND, siteName);
		}

		return site;
	}

	private void sendEmailNotifications(Shipment shipment, Status oldStatus, boolean sendNotif) {
		if (!sendNotif) {
			return;
		}

		if ((oldStatus == null || oldStatus == Status.PENDING) && shipment.isRequested()) {
			sendShipmentRequestedEmail(shipment);
		} else if ((oldStatus == null || oldStatus == Status.PENDING || oldStatus == Status.REQUESTED) && shipment.isShipped()) {
			sendShipmentShippedEmail(shipment);
		} else if (oldStatus == Status.SHIPPED && shipment.isReceived()) {
			sendShipmentReceivedEmail(shipment);
		}
	}

	private void sendShipmentRequestedEmail(Shipment shipment) {
		sendShipmentEmail(shipment, shipment.getSendingSite(), SHIPMENT_REQUESTED_EMAIL_TMPL);
	}

	private void sendShipmentShippedEmail(Shipment shipment) {
		sendShipmentEmail(shipment, shipment.getReceivingSite(), SHIPMENT_SHIPPED_EMAIL_TMPL);
	}

	private void sendShipmentEmail(Shipment shipment, Site site, String emailTmpl) {
		List<Long> userIds = AccessCtrlMgr.getInstance().getUserIds(
			site.getInstitute().getId(), site.getId(),
			Resource.SHIPPING_N_TRACKING, new Operation[] { Operation.UPDATE });

		Set<User> notifyUsers = new HashSet<>();
		if (!userIds.isEmpty()) {
			notifyUsers = new HashSet<>(daoFactory.getUserDao().getByIds(userIds));
		}

		if (shipment.getNotifyUsers() != null && !shipment.getNotifyUsers().isEmpty()) {
			notifyUsers.addAll(shipment.getNotifyUsers());
		}

		if (shipment.getRequester() != null) {
			notifyUsers.add(shipment.getRequester());
		}

		if (notifyUsers.isEmpty()) {
			return;
		}

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] { shipment.getName() });
		props.put("shipment", shipment);
		for (User user : notifyUsers) {
			props.put("rcpt", user);
			emailService.sendEmail(emailTmpl, new String[] { user.getEmailAddress() }, props);
		}
	}
	
	private void sendShipmentReceivedEmail(Shipment shipment) {
		Set<User> notifyUsers = new HashSet<>(shipment.getNotifyUsers());
		notifyUsers.add(shipment.getSender());
		if (shipment.getRequester() != null) {
			notifyUsers.add(shipment.getRequester());
		}

		Map<String, Object> props = new HashMap<>();
		props.put("$subject", new String[] { shipment.getName() });
		props.put("shipment", shipment);
		for (User user : notifyUsers) {
			props.put("rcpt", user);
			emailService.sendEmail(SHIPMENT_RECEIVED_EMAIL_TMPL, new String[] { user.getEmailAddress() }, props);
		}
	}
	
	private QueryDataExportResult exportShipmentReport(final Shipment shipment, SavedQuery query) {
		Filter filter = new Filter();
		filter.setField("Specimen.specimenShipments.id");
		filter.setOp(Op.EQ);
		filter.setValues(new String[] { shipment.getId().toString() });
		
		ExecuteQueryEventOp execReportOp = new ExecuteQueryEventOp();
		execReportOp.setDrivingForm("Participant");
		execReportOp.setAql(query.getAql(new Filter[] { filter }));
		execReportOp.setWideRowMode(WideRowMode.DEEP.name());
		execReportOp.setRunType("Export");
		
		return querySvc.exportQueryData(execReportOp, new QueryService.ExportProcessor() {
			@Override
			public String filename() {
				return "shipment_" + shipment.getId() + "_" + UUID.randomUUID().toString();
			}

			@Override
			public void headers(OutputStream out) {
				@SuppressWarnings("serial")
				Map<String, String> headers = new LinkedHashMap<String, String>() {{
					put(getMessage("shipment_name"), shipment.getName());
					put(getMessage("shipment_request"), MessageUtil.getInstance().getMessage(shipment.isRequest() ? "common_yes" : "common_no"));
					if (shipment.getRequester() != null) {
						put(getMessage("shipment_requester"), shipment.getRequester().formattedName());
					}

					if (shipment.getRequestDate() != null) {
						put(getMessage("shipment_request_date"), Utility.getDateTimeString(shipment.getRequestDate()));
					}

					if (StringUtils.isNotBlank(shipment.getRequesterComments())) {
						put(getMessage("shipment_requester_comments"), shipment.getRequesterComments());
					}

					put(getMessage("shipment_courier_name"),    shipment.getCourierName());
					put(getMessage("shipment_tracking_number"), shipment.getTrackingNumber());
					put(getMessage("shipment_tracking_url"),    shipment.getTrackingUrl());
					put(getMessage("shipment_sending_site"),    shipment.getSendingSite().getName());
					if (shipment.getSender() != null) {
						put(getMessage("shipment_sender"),      shipment.getSender().formattedName());
					}

					if (shipment.getShippedDate() != null) {
						put(getMessage("shipment_shipped_date"),Utility.getDateTimeString(shipment.getShippedDate()));
					}

					put(getMessage("shipment_sender_comments"), shipment.getSenderComments());
					put(getMessage("shipment_recv_site"),       shipment.getReceivingSite().getName());
					
					if (shipment.getReceiver() != null) {
						put(getMessage("shipment_receiver"),    shipment.getReceiver().formattedName());
					}
					
					if (shipment.getReceivedDate() != null) {
						put(getMessage("shipment_received_date"), Utility.getDateTimeString(shipment.getReceivedDate()));
					}
					
					put(getMessage("shipment_receiver_comments"), shipment.getReceiverComments());
					put(getMessage("shipment_status"),            shipment.getStatus().getName());

					put("", ""); // blank line
				}};
				
				Utility.writeKeyValuesToCsv(out, headers);
			}
		});
	}
	
	private String getMessage(String code) {
		return MessageUtil.getInstance().getMessage(code);
	}
	
	private ShipmentDao getShipmentDao() {
		return daoFactory.getShipmentDao();
	}
	
}
