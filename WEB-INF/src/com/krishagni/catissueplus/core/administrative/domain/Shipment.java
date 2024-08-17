package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.factory.ShipmentErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.CollectionUpdater;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.domain.PrintItem;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
public class Shipment extends BaseEntity {
	private static final String ENTITY_NAME = "shipment";

	public enum Status {
		PENDING("Pending"),

		REQUESTED("Requested"),

		SHIPPED("Shipped"),

		RECEIVED("Received");
		
		private final String name;
		
		Status(String name) {
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		
		public static Status fromName(String name) {
			if (StringUtils.isBlank(name)) {
				return null;
			}
			
			Status result = null;
			for (Status value : Status.values()) {
				if (value.name.equalsIgnoreCase(name)) {
					result = value;
					break;
				}
			}
			
			return result;
		}
	}

	public enum Type {
		SPECIMEN, CONTAINER
	}

	private boolean request;

	private User requester;

	private Date requestDate;

	private String requesterComments;

	private String name;

	private Type type;
	
	private String  courierName;
	
	private String trackingNumber;
	
	private String trackingUrl;
	
	private Site sendingSite;
	
	private Site receivingSite;
	
	private Date shippedDate;
	
	private User sender;
	
	private String senderComments;
	
	private Date receivedDate;
	
	private User receiver;
	
	private String receiverComments;
	
	private Status status;

	private PermissibleValue requestStatus;
	
	private String activityStatus;
	
	private Set<ShipmentSpecimen> shipmentSpecimens = new HashSet<>();

	private Set<ShipmentContainer> shipmentContainers = new HashSet<>();

	private Set<User> notifyUsers = new HashSet<>();

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public boolean isRequest() {
		return request;
	}

	public void setRequest(boolean request) {
		this.request = request;
	}

	public User getRequester() {
		return requester;
	}

	public void setRequester(User requester) {
		this.requester = requester;
	}

	public Date getRequestDate() {
		return requestDate;
	}

	public void setRequestDate(Date requestDate) {
		this.requestDate = requestDate;
	}

	public String getRequesterComments() {
		return requesterComments;
	}

	public void setRequesterComments(String requesterComments) {
		this.requesterComments = requesterComments;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public String getCourierName() {
		return courierName;
	}

	public void setCourierName(String courierName) {
		this.courierName = courierName;
	}

	public String getTrackingNumber() {
		return trackingNumber;
	}

	public void setTrackingNumber(String trackingNumber) {
		this.trackingNumber = trackingNumber;
	}

	public String getTrackingUrl() {
		return trackingUrl;
	}

	public void setTrackingUrl(String trackingUrl) {
		this.trackingUrl = trackingUrl;
	}

	public Site getSendingSite() {
		return sendingSite;
	}
	
	public void setSendingSite(Site sendingSite) {
		this.sendingSite = sendingSite;
	}
	
	public Site getReceivingSite() {
		return receivingSite;
	}

	public void setReceivingSite(Site receivingSite) {
		this.receivingSite = receivingSite;
	}

	public Date getShippedDate() {
		return shippedDate;
	}

	public void setShippedDate(Date shippedDate) {
		this.shippedDate = shippedDate;
	}

	public User getSender() {
		return sender;
	}

	public void setSender(User sender) {
		this.sender = sender;
	}

	public String getSenderComments() {
		return senderComments;
	}

	public void setSenderComments(String senderComments) {
		this.senderComments = senderComments;
	}

	public Date getReceivedDate() {
		return receivedDate;
	}

	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	public User getReceiver() {
		return receiver;
	}

	public void setReceiver(User receiver) {
		this.receiver = receiver;
	}

	public String getReceiverComments() {
		return receiverComments;
	}

	public void setReceiverComments(String receiverComments) {
		this.receiverComments = receiverComments;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public PermissibleValue getRequestStatus() {
		return requestStatus;
	}

	public void setRequestStatus(PermissibleValue requestStatus) {
		this.requestStatus = requestStatus;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Set<ShipmentSpecimen> getShipmentSpecimens() {
		return shipmentSpecimens;
	}

	public void setShipmentSpecimens(Set<ShipmentSpecimen> shipmentSpecimens) {
		this.shipmentSpecimens = shipmentSpecimens;
	}

	public Set<ShipmentContainer> getShipmentContainers() {
		return shipmentContainers;
	}

	public void setShipmentContainers(Set<ShipmentContainer> shipmentContainers) {
		this.shipmentContainers = shipmentContainers;
	}

	public Set<User> getNotifyUsers() {
		return notifyUsers;
	}

	public void setNotifyUsers(Set<User> notifyUsers) {
		this.notifyUsers = notifyUsers;
	}

	public boolean isSpecimenShipment() {
		return getType() == Type.SPECIMEN;
	}

	public boolean isContainerShipment() {
		return getType() == Type.CONTAINER;
	}

	public void update(Shipment other) {
		if (isRequest() != other.isRequest()) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.REQ_CHG_NA);
		}

		if (isRequest()) {
			setRequester(other.getRequester());
			setRequestDate(other.getRequestDate());
			setRequesterComments(other.getRequesterComments());
		} else {
			setRequester(null);
			setRequestDate(null);
			setRequesterComments(null);
		}

		setName(other.getName());
		setCourierName(other.getCourierName());
		setTrackingNumber(other.getTrackingNumber());
		setTrackingUrl(other.getTrackingUrl());
		setShippedDate(other.getShippedDate());
		setSender(other.getSender());
		setSenderComments(other.getSenderComments());
		setReceivedDate(other.getReceivedDate());
		setReceiver(other.getReceiver());
		setReceiverComments(other.getReceiverComments());
		setActivityStatus(other.getActivityStatus());
		if (isPending() || isRequested()) {
			setSendingSite(other.getSendingSite());
			setReceivingSite(other.getReceivingSite());
		}

		updateShipmentSpecimens(other);
		updateShipmentContainers(other);
		updateNotifyUsers(other);
		updateStatus(other);
		if (isRequested()) {
			setRequestStatus(other.getRequestStatus());
		}
	}

	public void delete() {
		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
		setName(Utility.getDisabledValue(getName(), 255));
		setActivityStatus(com.krishagni.catissueplus.core.common.util.Status.ACTIVITY_STATUS_DISABLED.getStatus());
		daoFactory.getShipmentDao().deleteSpecimenShipmentEvents(getId());
	}

	public void ship() {
		if (isShipped()) {
			//
			// the shipment is already in 'Shipped' state.
			// no need to do any extra work
			//
			return;
		}

		if ((isRequest() && !isRequested()) || (!isRequest() && !isPending())) {
			// A request has to be in requested state to be shipped.
			// Others need to be in pending state to be shipped.
			throw OpenSpecimenException.userError(ShipmentErrorCode.CANNOT_SHIP, getStatus().getName());
		}

		//
		// if we are here - it means the shipment is in pending state
		//
		if (getSender() == null) {
			// OPSMN-6460 default the sender to the current user
			setSender(AuthUtil.getCurrentUser());
		}

		if (isSpecimenShipment()) {
			if (CollectionUtils.isEmpty(getShipmentSpecimens())) {
				throw OpenSpecimenException.userError(ShipmentErrorCode.NO_SPECIMENS_TO_SHIP);
			}

			getShipmentSpecimens().forEach(ShipmentSpecimen::ship);
		} else if (isContainerShipment()) {
			if (CollectionUtils.isEmpty(getShipmentContainers())) {
				throw OpenSpecimenException.userError(ShipmentErrorCode.NO_CONTAINERS_TO_SHIP);
			}

			getShipmentContainers().forEach(ShipmentContainer::ship);
		}

		setStatus(Status.SHIPPED);
	}
	
	public void receive(Shipment other) {
		if (!isShipped() && !isReceived()) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.CANNOT_RECEIVE, getStatus().getName());
		}

		if (isSpecimenShipment()) {
			receiveSpecimens(other);
		} else if (isContainerShipment()) {
			receiveContainers(other);
		}

		setStatus(Status.RECEIVED);
 	}

	public ShipmentSpecimen addShipmentSpecimen(Specimen spmn) {
		ShipmentSpecimen shipmentSpmn = ShipmentSpecimen.createShipmentSpecimen(this, spmn);
		getShipmentSpecimens().add(shipmentSpmn);
		return shipmentSpmn;
	}

	public boolean isPending() {
		return Status.PENDING == getStatus();
	}

	public boolean isRequested() {
		return Status.REQUESTED == getStatus();
	}

	public boolean isShipped() {
		return Status.SHIPPED == getStatus();
	}
	
	public boolean isReceived() {
		return Status.RECEIVED == getStatus();
	}

	public boolean isActive() {
		return com.krishagni.catissueplus.core.common.util.Status.isActiveStatus(getActivityStatus());
	}

	public boolean isDeleted() {
		return com.krishagni.catissueplus.core.common.util.Status.isDisabledStatus(getActivityStatus());
	}

	private Map<Specimen, ShipmentSpecimen> getShipmentSpecimensMap() {
		return getShipmentSpecimens().stream()
			.collect(Collectors.toMap(ShipmentSpecimen::getSpecimen, item -> item));
	}

	private Map<StorageContainer, ShipmentContainer> getShipmentContainersMap() {
		return getShipmentContainers().stream()
			.collect(Collectors.toMap(ShipmentContainer::getContainer, item -> item));
	}

	private void updateShipmentSpecimens(Shipment other) {
		if (!isSpecimenShipment() || !(isPending() || isRequested())) {
			return;
		}

		Map<Specimen, ShipmentSpecimen> existingItems = getShipmentSpecimensMap();
		for (ShipmentSpecimen newItem : other.getShipmentSpecimens()) {
			ShipmentSpecimen oldItem = existingItems.remove(newItem.getSpecimen());
			if (oldItem == null) {
				newItem.setShipment(this);
				getShipmentSpecimens().add(newItem);
			}
		}
		
		getShipmentSpecimens().removeAll(existingItems.values());
	}

	private void receiveSpecimens(Shipment other) {
		if (!isReceived()) {
			ensureShippedSpecimens(other);
		}

		Map<Specimen, ShipmentSpecimen> existingItems = getShipmentSpecimensMap();
		List<PrintItem<Specimen>> printItems = new ArrayList<>();
		for (ShipmentSpecimen newItem : other.getShipmentSpecimens()) {
			ShipmentSpecimen oldItem = existingItems.remove(newItem.getSpecimen());
			oldItem.receive(newItem);
			if (oldItem.getSpecimen().isPrintLabel()) {
				printItems.add(PrintItem.make(oldItem.getSpecimen(), oldItem.getSpecimen().getCopiesToPrint()));
			}
		}

		if (!printItems.isEmpty()) {
			Specimen.getLabelPrinter().print(printItems);
		}
	}

	private void ensureShippedSpecimens(Shipment other) {
		Function<ShipmentSpecimen, Long> fn = (ss) -> ss.getSpecimen().getId();
		List<Long> existingSpecimens = getShipmentSpecimens().stream().map(fn).collect(Collectors.toList());
		List<Long> newSpecimens = other.getShipmentSpecimens().stream().map(fn).collect(Collectors.toList());

		if (!CollectionUtils.isEqualCollection(existingSpecimens, newSpecimens)) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.INVALID_SHIPPED_SPECIMENS);
		}
	}

	private void updateShipmentContainers(Shipment other) {
		if (!isContainerShipment() || !(isPending() || isRequested())) {
			return;
		}

		Map<StorageContainer, ShipmentContainer> existingItems = getShipmentContainersMap();
		for (ShipmentContainer newItem : other.getShipmentContainers()) {
			ShipmentContainer oldItem = existingItems.remove(newItem.getContainer());
			if (oldItem == null) {
				newItem.setShipment(this);
				getShipmentContainers().add(newItem);
			}
		}

		getShipmentContainers().removeAll(existingItems.values());
	}

	private void receiveContainers(Shipment other) {
		if (!isReceived()) {
			ensureShippedContainers(other);
		}

		Map<StorageContainer, ShipmentContainer> existingItems = getShipmentContainersMap();
		for (ShipmentContainer newItem : other.getShipmentContainers()) {
			ShipmentContainer oldItem = existingItems.get(newItem.getContainer());
			oldItem.receive(newItem);
		}
	}

	private void ensureShippedContainers(Shipment other) {
		Function<ShipmentContainer, String> fn = (ss) -> ss.getContainer().getName();
		List<String> existingContainers = getShipmentContainers().stream().map(fn).collect(Collectors.toList());
		List<String> newContainers = other.getShipmentContainers().stream().map(fn).collect(Collectors.toList());

		if (!CollectionUtils.isEqualCollection(existingContainers, newContainers)) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.INVALID_SHIPPED_CONTAINERS);
		}
	}

	private void updateNotifyUsers(Shipment other) {
		if (!(isPending() || isRequested())) {
			return;
		}

		CollectionUpdater.update(getNotifyUsers(), other.getNotifyUsers());
	}

	private void updateStatus(Shipment other) {
		if (isRequest() && (isPending() || isRequested()) && other.isRequested()) {
			//
			// to move to requested state, the shipment should be a request and
			// should be either in pending or requested state
			//
			setStatus(Status.REQUESTED);
		} else if (((!isRequest() && isPending()) || isRequested() || isShipped()) && other.isShipped()) {
			//
			// to move to shipped state, the shipment should be in either pending (for non-requests),
			// requested or shipped state
			//
			ship();
		} else if ((isShipped() || isReceived()) && other.isReceived()) {
			receive(other);
		} else if (getStatus() != other.getStatus()) {
			throw OpenSpecimenException.userError(ShipmentErrorCode.STATUS_CHANGE_NOT_ALLOWED, getStatus().getName(), other.getStatus().getName());
		}
	}
}
