package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class ShipmentDetail implements Mergeable<String, ShipmentDetail>, Serializable {

	private static final long serialVersionUID = -778300952449162194L;

	private Long id;
	
	private String name;

	private String type;
	
	private String  courierName;
	
	private String trackingNumber;
	
	private String trackingUrl;
	
	private String sendingSite;
	
	private String receivingInstitute;
	
	private String receivingSite;
	
	private Date shippedDate;
	
	private UserSummary sender;
	
	private String senderComments;
	
	private Date receivedDate;
	
	private UserSummary receiver;
	
	private String receiverComments;
	
	private String status;
	
	private String activityStatus;
	
	private List<ShipmentSpecimenDetail> shipmentSpmns = new ArrayList<>();

	private List<ShipmentContainerDetail> shipmentContainers = new ArrayList<>();
	
	private List<UserSummary> notifyUsers = new ArrayList<>();

	private Integer specimensCount;

	private SpecimenRequestSummary request;
	
	//
	// For BO template
	//
	private ShipmentSpecimenDetail shipmentSpecimen;

	private ShipmentContainerDetail shipmentContainer;
	
	private boolean sendMail = true;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
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
	
	public String getSendingSite() {
		return sendingSite;
	}

	public void setSendingSite(String sendingSite) {
		this.sendingSite = sendingSite;
	}

	public String getReceivingInstitute() {
		return receivingInstitute;
	}

	public void setReceivingInstitute(String receivingInstitute) {
		this.receivingInstitute = receivingInstitute;
	}

	public String getReceivingSite() {
		return receivingSite;
	}

	public void setReceivingSite(String receivingSite) {
		this.receivingSite = receivingSite;
	}

	public Date getShippedDate() {
		return shippedDate;
	}

	public void setShippedDate(Date shippedDate) {
		this.shippedDate = shippedDate;
	}

	public UserSummary getSender() {
		return sender;
	}

	public void setSender(UserSummary sender) {
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

	public UserSummary getReceiver() {
		return receiver;
	}

	public void setReceiver(UserSummary receiver) {
		this.receiver = receiver;
	}

	public String getReceiverComments() {
		return receiverComments;
	}

	public void setReceiverComments(String receiverComments) {
		this.receiverComments = receiverComments;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public List<ShipmentSpecimenDetail> getShipmentSpmns() {
		return shipmentSpmns;
	}

	public void setShipmentSpmns(List<ShipmentSpecimenDetail> shipmentSpmns) {
		this.shipmentSpmns = shipmentSpmns;
	}

	public List<ShipmentContainerDetail> getShipmentContainers() {
		return shipmentContainers;
	}

	public void setShipmentContainers(List<ShipmentContainerDetail> shipmentContainers) {
		this.shipmentContainers = shipmentContainers;
	}

	public List<UserSummary> getNotifyUsers() {
		return notifyUsers;
	}

	public void setNotifyUsers(List<UserSummary> notifyUsers) {
		this.notifyUsers = notifyUsers;
	}

	public Integer getSpecimensCount() {
		return specimensCount;
	}

	public void setSpecimensCount(Integer specimensCount) {
		this.specimensCount = specimensCount;
	}

	public SpecimenRequestSummary getRequest() {
		return request;
	}

	public void setRequest(SpecimenRequestSummary request) {
		this.request = request;
	}

	public ShipmentSpecimenDetail getShipmentSpecimen() {
		return shipmentSpecimen;
	}

	public void setShipmentSpecimen(ShipmentSpecimenDetail shipmentSpecimen) {
		this.shipmentSpecimen = shipmentSpecimen;
	}

	public ShipmentContainerDetail getShipmentContainer() {
		return shipmentContainer;
	}

	public void setShipmentContainer(ShipmentContainerDetail shipmentContainer) {
		this.shipmentContainer = shipmentContainer;
	}

	public boolean isSendMail() {
		return sendMail;
	}

	public void setSendMail(boolean sendMail) {
		this.sendMail = sendMail;
	}

	public static ShipmentDetail from(Shipment shipment) {
		ShipmentDetail detail = new ShipmentDetail();
		detail.setId(shipment.getId());
		detail.setName(shipment.getName());
		detail.setType(shipment.getType().name());
		detail.setCourierName(shipment.getCourierName());
		detail.setTrackingNumber(shipment.getTrackingNumber());
		detail.setTrackingUrl(shipment.getTrackingUrl());
		detail.setSendingSite(shipment.getSendingSite().getName());
		detail.setReceivingInstitute(shipment.getReceivingSite().getInstitute().getName());
		detail.setReceivingSite(shipment.getReceivingSite().getName());
		detail.setShippedDate(shipment.getShippedDate());
		detail.setSender(UserSummary.from(shipment.getSender()));
		detail.setSenderComments(shipment.getSenderComments());
		detail.setReceivedDate(shipment.getReceivedDate());
		detail.setReceiver(shipment.getReceiver() == null ? null : UserSummary.from(shipment.getReceiver()));
		detail.setReceiverComments(shipment.getReceiverComments());
		detail.setStatus(shipment.getStatus().getName());
		detail.setActivityStatus(shipment.getActivityStatus());
		detail.setNotifyUsers(UserSummary.from(shipment.getNotifyUsers()));
		return detail;
	}
	
	public static List<ShipmentDetail> from(Collection<Shipment> shipments) {
		return shipments.stream().map(ShipmentDetail::from).collect(Collectors.toList());
	}

	@Override
	@JsonIgnore
	public String getMergeKey() {
		return name;
	}

	@Override
	public void merge(ShipmentDetail detail) {
		if (detail.getShipmentSpecimen() != null) {
			getShipmentSpmns().add(detail.getShipmentSpecimen());
		} else if (detail.getShipmentContainer() != null) {
			getShipmentContainers().add(detail.getShipmentContainer());
		}
	}
}
