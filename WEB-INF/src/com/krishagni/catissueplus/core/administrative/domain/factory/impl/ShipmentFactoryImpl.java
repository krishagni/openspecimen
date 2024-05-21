package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSpecimen;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ShipmentContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentDetail;
import com.krishagni.catissueplus.core.administrative.events.ShipmentSpecimenDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenFactory;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenDetail;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenResolver;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.NumUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.importer.services.impl.ImporterContextHolder;

public class ShipmentFactoryImpl implements ShipmentFactory {
	private static final String RECV_QUALITY = "shipment_item_received_quality";

	private DaoFactory daoFactory;
	
	private SpecimenFactory specimenFactory;

	private SpecimenResolver specimenResolver;

	private StorageContainerFactory containerFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setSpecimenFactory(SpecimenFactory specimenFactory) {
		this.specimenFactory = specimenFactory;
	}

	public void setSpecimenResolver(SpecimenResolver specimenResolver) {
		this.specimenResolver = specimenResolver;
	}

	public void setContainerFactory(StorageContainerFactory containerFactory) {
		this.containerFactory = containerFactory;
	}

	public Shipment createShipment(ShipmentDetail detail, Shipment.Status status) {
		Shipment shipment = new Shipment();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);
		
		shipment.setId(detail.getId());
		shipment.setRequest(detail.isRequest());
		setRequester(detail, shipment, ose);
		setRequestDate(detail, shipment, ose);
		setRequesterComments(detail, shipment, ose);
		setName(detail, shipment, ose);
		setType(detail, shipment, ose);
		setCourierName(detail, shipment, ose);
		setTrackingNumber(detail, shipment, ose);
		setTrackingUrl(detail, shipment, ose);
		setSendingSite(detail, shipment, ose);
		setReceivingSite(detail, shipment, ose);
		setStatus(detail, status, shipment, ose);
		setShippedDate(detail, shipment, ose);
		setSender(detail, shipment, ose);
		setSenderComments(detail, shipment, ose);
		setReceivedDate(detail, shipment, ose);
		setReceiver(detail, shipment, ose);
		setReceiverComments(detail, shipment, ose);
		setActivityStatus(detail, shipment, ose);
		setShipmentSpecimens(detail, shipment, ose);
		setShipmentContainers(detail, shipment, ose);
		setNotifyUser(detail, shipment, ose);
		
		ose.checkAndThrow();
		return shipment;
	}

	private void setRequester(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!detail.isRequest()) {
			shipment.setRequester(null);
		} else {
			shipment.setRequester(getUser(detail.getRequester(), AuthUtil.getCurrentUser(), ose));
		}
	}

	private void setRequestDate(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!detail.isRequest()) {
			shipment.setRequestDate(null);
		} else {
			shipment.setRequestDate(detail.getRequestDate());
			if (shipment.getRequestDate() == null) {
				shipment.setRequestDate(Calendar.getInstance().getTime());
			}
		}
	}

	private void setRequesterComments(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!detail.isRequest()) {
			shipment.setRequesterComments(null);
		} else {
			shipment.setRequesterComments(detail.getRequesterComments());
		}
	}
	
	private void setName(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		String name = detail.getName();
		if (StringUtils.isBlank(name)) {
			ose.addError(ShipmentErrorCode.NAME_REQUIRED);
			return;
		}
		
		shipment.setName(name);
	}

	private void setType(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		Shipment.Type type = Shipment.Type.SPECIMEN;
		if (StringUtils.isNotBlank(detail.getType())) {
			try {
				type = Shipment.Type.valueOf(detail.getType());
			} catch (Exception e) {
				ose.addError(ShipmentErrorCode.INVALID_TYPE, detail.getType());
			}
		}

		shipment.setType(type);
	}

	private void setCourierName(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		shipment.setCourierName(detail.getCourierName());
	}
	
	private void setTrackingNumber(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		shipment.setTrackingNumber(detail.getTrackingNumber());
	}
	
	private void setTrackingUrl(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		shipment.setTrackingUrl(detail.getTrackingUrl());
	}
	
	private void setSendingSite(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		String siteName = detail.getSendingSite();
		if (StringUtils.isBlank(siteName)) {
			ose.addError(ShipmentErrorCode.SEND_SITE_REQUIRED);
			return;
		}
		
		Site site = daoFactory.getSiteDao().getSiteByName(siteName);
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);
			return;
		}

		shipment.setSendingSite(site);
	}
	
	private void setReceivingSite(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		String siteName = detail.getReceivingSite();
		if (StringUtils.isBlank(siteName)) {
			ose.addError(ShipmentErrorCode.REC_SITE_REQUIRED);
			return;
		}
		
		Site site = daoFactory.getSiteDao().getSiteByName(siteName);
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);
			return;
		}

		shipment.setReceivingSite(site);
	}
	
	private void setStatus(ShipmentDetail detail, Shipment.Status initialStatus, Shipment shipment, OpenSpecimenException ose) {
		if (initialStatus != null) {
			shipment.setStatus(initialStatus);
			return;
		}
		
		if (StringUtils.isBlank(detail.getStatus())) {
			ose.addError(ShipmentErrorCode.STATUS_REQUIRED);
			return;
		}
		
		Shipment.Status status = Shipment.Status.fromName(detail.getStatus());
		if (status == null) {
			ose.addError(ShipmentErrorCode.INVALID_STATUS);
		}
		
		shipment.setStatus(status);
	}
	
	private void setShippedDate(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (shipment.isRequest()) {
			if (shipment.isPending() || shipment.isRequested()) {
				return;
			}
		}

		Date requestDate = shipment.getRequestDate();
		Date shippedDate = detail.getShippedDate();
		Date todayDate = Calendar.getInstance().getTime();
		if (shippedDate == null) {
			shippedDate = requestDate == null || todayDate.after(requestDate) ? todayDate : requestDate;
		} else if (requestDate != null && shippedDate.before(requestDate)) {
			ose.addError(ShipmentErrorCode.SHIP_DT_BEFORE_REQ_DT, Utility.getDateTimeString(shippedDate), Utility.getDateTimeString(requestDate));
		}

		shipment.setShippedDate(shippedDate);
	}
	
	private void setSender(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		shipment.setSender(getUser(detail.getSender(), null, ose));
	}
	
	private void setSenderComments(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		shipment.setSenderComments(detail.getSenderComments());
	}
	
	private void setReceivedDate(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!shipment.isReceived()) {
			return;
		}
		
		Date receivedDate = detail.getReceivedDate();
		Date todayDate = Calendar.getInstance().getTime();
		if (receivedDate == null) {
			receivedDate = todayDate.after(shipment.getShippedDate()) ? todayDate : shipment.getShippedDate();
		} else if (receivedDate.before(shipment.getShippedDate())) {
			ose.addError(ShipmentErrorCode.RECV_DT_BEFORE_SHIP_DT, Utility.getDateTimeString(receivedDate), Utility.getDateTimeString(shipment.getShippedDate()));
		}

		shipment.setReceivedDate(receivedDate);
	}
	
	private void setReceiver(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!shipment.isReceived()) {
			return;
		}

		shipment.setReceiver(getUser(detail.getReceiver(), AuthUtil.getCurrentUser(), ose));
	}
	
	private void setReceiverComments(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!shipment.isReceived()) {
			return;
		}
		
		shipment.setReceiverComments(detail.getReceiverComments());
	}
	
	private void setActivityStatus(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (StringUtils.isBlank(activityStatus)) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		shipment.setActivityStatus(activityStatus);
	}
	
	
	private void setShipmentSpecimens(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!shipment.isSpecimenShipment()) {
			return;
		}

		if (CollectionUtils.isEmpty(detail.getShipmentSpmns())) {
			if (shipment.isActive()) {
				ose.addError(ShipmentErrorCode.NO_SPECIMENS_TO_SHIP);
			}

			return;
		}

		Map<Long, ShipmentSpecimen> shipmentSpmns = new LinkedHashMap<>();
		for (ShipmentSpecimenDetail item : detail.getShipmentSpmns()) {
			ShipmentSpecimen shipmentSpmn = getShipmentSpmn(item, shipment, ose);
			if (shipmentSpmn == null) {
				return;
			}
			
			if (shipmentSpmns.containsKey(shipmentSpmn.getSpecimen().getId())) {
				continue;
			}
			
			shipmentSpmns.put(shipmentSpmn.getSpecimen().getId(), shipmentSpmn);
		}
		
		shipment.setShipmentSpecimens(new LinkedHashSet<>(shipmentSpmns.values()));
	}

	private void setShipmentContainers(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (!shipment.isContainerShipment()) {
			return;
		}

		if (CollectionUtils.isEmpty(detail.getShipmentContainers())) {
			if (shipment.isActive()) {
				ose.addError(ShipmentErrorCode.NO_CONTAINERS_TO_SHIP);
			}

			return;
		}

		Map<Long, ShipmentContainer> shipmentContainers = new LinkedHashMap<>();
		for (ShipmentContainerDetail item : detail.getShipmentContainers()) {
			ShipmentContainer shipmentContainer = getShipmentContainer(item, shipment, ose);
			if (shipmentContainer == null) {
				return;
			}

			if (shipmentContainers.containsKey(shipmentContainer.getContainer().getId())) {
				continue;
			}

			shipmentContainers.put(shipmentContainer.getContainer().getId(), shipmentContainer);
		}

		//
		// remove descendant containers whose ancestors are also part of the shipment
		//
		Map<Long, List<Long>> descendantIds = daoFactory.getStorageContainerDao()
			.getDescendantContainerIds(shipmentContainers.keySet());
		descendantIds.values().stream().flatMap(List::stream).forEach(shipmentContainers::remove);

		shipment.setShipmentContainers(new LinkedHashSet<>(shipmentContainers.values()));
	}
	
	private void setNotifyUser(ShipmentDetail detail, Shipment shipment, OpenSpecimenException ose) {
		if (shipment.isReceived()) {
			return;
		}
		
		if (CollectionUtils.isEmpty(detail.getNotifyUsers())) {
			return;
		}
		
		Set<User> result = new HashSet<>();
		for (UserSummary userSummary : detail.getNotifyUsers()) {
			User user = getUser(userSummary, null, ose);
			if (user != null) {
				result.add(user);
			}
		}
		
		shipment.setNotifyUsers(result);
	}
	
	private User getUser(UserSummary userSummary, User defaultUser, OpenSpecimenException ose) {
		if (userSummary == null) {
			return defaultUser;
		}

		Object key = null;
		User user = defaultUser;
		if (userSummary.getId() != null) {
			key = userSummary.getId();
			user = daoFactory.getUserDao().getById(userSummary.getId());
		} else if (StringUtils.isNotBlank(userSummary.getEmailAddress())) {
			key = userSummary.getEmailAddress();
			user = daoFactory.getUserDao().getUserByEmailAddress(userSummary.getEmailAddress());
		} else if (StringUtils.isNotBlank(userSummary.getLoginName()) && StringUtils.isNotBlank(userSummary.getDomain())) {
			key = userSummary.getDomain() + ": " + userSummary.getLoginName();
			user = daoFactory.getUserDao().getUser(userSummary.getLoginName(), userSummary.getDomain());
		}

		if (key == null) {
			return defaultUser;
		} else if (user == null) {
			if (ose != null) {
				ose.addError(UserErrorCode.NOT_FOUND, key);
			}
		}
		
		return user;
	}
	
	private ShipmentSpecimen getShipmentSpmn(ShipmentSpecimenDetail detail, Shipment shipment, OpenSpecimenException ose) {
		PermissibleValue receivedQuality = null;
		if (shipment.isReceived()) {
			receivedQuality = getReceivedQuality(detail.getReceivedQuality(), ose);
		}
		
		Specimen specimen = getSpecimen(shipment, detail.getSpecimen(), ose);
		if (specimen == null) {
			return null;
		}

		initSpecimen(shipment, specimen);

		ShipmentSpecimen shipmentSpecimen = new ShipmentSpecimen();
		shipmentSpecimen.setShipment(shipment);
		shipmentSpecimen.setSpecimen(specimen);
		shipmentSpecimen.setReceivedQuality(receivedQuality);
		return shipmentSpecimen;
	}

	private ShipmentContainer getShipmentContainer(ShipmentContainerDetail detail, Shipment shipment, OpenSpecimenException ose) {
		PermissibleValue receivedQuality = null;
		if (shipment.isReceived()) {
			receivedQuality = getReceivedQuality(detail.getReceivedQuality(), ose);
		}

		StorageContainer container = getContainer(shipment, detail.getContainer(), ose);
		if (container == null) {
			return null;
		}

		ShipmentContainer shipmentContainer = new ShipmentContainer();
		shipmentContainer.setShipment(shipment);
		shipmentContainer.setContainer(container);
		shipmentContainer.setReceivedQuality(receivedQuality);
		return shipmentContainer;
	}

	private PermissibleValue getReceivedQuality(String input, OpenSpecimenException ose) {
		if (StringUtils.isBlank(input)) {
			ose.addError(ShipmentErrorCode.RECV_QUALITY_REQ);
			return null;
		}

		PermissibleValue recvQualityPv = daoFactory.getPermissibleValueDao().getPv(RECV_QUALITY, input);
		if (recvQualityPv == null) {
			ose.addError(ShipmentErrorCode.INV_RECEIVED_QUALITY, input);
		}

		return recvQualityPv;
	}

	private Specimen getSpecimen(Shipment shipment, SpecimenInfo info, OpenSpecimenException ose) {
		Specimen existing = specimenResolver.getSpecimen(info.getId(), info.getCpShortTitle(), info.getLabel(), info.getBarcode(), ose);
		if (existing == null || !shipment.isReceived()) {
			return existing;
		}

		//
		// Assign location only if received quality is acceptable
		//
		SpecimenDetail detail = new SpecimenDetail();
		detail.setId(info.getId());
		detail.setStorageLocation(info.getStorageLocation());
		detail.setTransferTime(shipment.getReceivedDate());
		detail.setPrintLabel(info.isPrintLabel());

		if (info.getLabel() != null && isSpmnRelabelingAllowed()) {
			detail.setLabel(info.getLabel()); // relabeling at time of receiving shipment
		}

		if (info.getAvailableQty() != null) {
			detail.setAvailableQty(info.getAvailableQty());

			if (NumUtil.greaterThan(info.getAvailableQty(), existing.getInitialQuantity())) {
				detail.setInitialQty(info.getAvailableQty());
			}
		}

		if (info.getExternalIds() != null) {
			detail.setExternalIds(info.getExternalIds());
		}

		return specimenFactory.createSpecimen(existing, detail, null);
	}

	private boolean isSpmnRelabelingAllowed() {
		return ConfigUtil.getInstance().getBoolSetting(ADMIN_MODULE, ALLOW_SPMN_RELABELING, false);
	}

	private StorageContainer getContainer(Shipment shipment, StorageContainerSummary info, OpenSpecimenException ose) {
		Object key = null;
		StorageContainer existing = null;
		if (info.getId() != null) {
			existing = daoFactory.getStorageContainerDao().getById(info.getId());
			key = info.getId();
		} else if (StringUtils.isNotBlank(info.getName())) {
			existing = daoFactory.getStorageContainerDao().getByName(info.getName());
			key = info.getName();
		}

		if (key == null) {
			ose.addError(StorageContainerErrorCode.NAME_REQUIRED);
		} else if (existing == null) {
			ose.addError(StorageContainerErrorCode.NOT_FOUND, key, 1);
		}

		if (existing == null || !shipment.isReceived()) {
			return existing;
		}

		StorageContainerDetail detail = new StorageContainerDetail();
		detail.setId(existing.getId());
		detail.setSiteName(shipment.getReceivingSite().getName());
		detail.setStorageLocation(info.getStorageLocation());
		StorageContainer container = containerFactory.createStorageContainer(existing, detail);
		container.validateRestrictions();
		return container;
	}

	//
	// HSEARCH-1350: https://hibernate.atlassian.net/browse/HSEARCH-1350
	//
	private void initSpecimen(Shipment shipment, Specimen specimen) {
		if (ImporterContextHolder.getInstance().isImportOp()) {
			specimen.initCollections();
		}
	}

	private static final String ADMIN_MODULE = "administrative";

	private static final String ALLOW_SPMN_RELABELING = "allow_spmn_relabeling";

}
