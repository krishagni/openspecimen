package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class ShipmentSpecimensRetrieveDetail {
	private Long shipmentId;

	private String shipmentName;

	private UserSummary transferUser;

	private Date transferTime;

	private String comments;

	private Boolean checkout;

	public Long getShipmentId() {
		return shipmentId;
	}

	public void setShipmentId(Long shipmentId) {
		this.shipmentId = shipmentId;
	}

	public String getShipmentName() {
		return shipmentName;
	}

	public void setShipmentName(String shipmentName) {
		this.shipmentName = shipmentName;
	}

	public UserSummary getTransferUser() {
		return transferUser;
	}

	public void setTransferUser(UserSummary transferUser) {
		this.transferUser = transferUser;
	}

	public Date getTransferTime() {
		return transferTime;
	}

	public void setTransferTime(Date transferTime) {
		this.transferTime = transferTime;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Boolean getCheckout() {
		return checkout;
	}

	public void setCheckout(Boolean checkout) {
		this.checkout = checkout;
	}
}
