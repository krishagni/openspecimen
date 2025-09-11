package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;

public class ShipmentCartDetail {
	private Long shipmentId;

	private String name;

	private String description;

	private String pickListName;

	private List<UserGroupSummary> sharedWith;

	public Long getShipmentId() {
		return shipmentId;
	}

	public void setShipmentId(Long shipmentId) {
		this.shipmentId = shipmentId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getPickListName() {
		return pickListName;
	}

	public void setPickListName(String pickListName) {
		this.pickListName = pickListName;
	}

	public List<UserGroupSummary> getSharedWith() {
		return sharedWith;
	}

	public void setSharedWith(List<UserGroupSummary> sharedWith) {
		this.sharedWith = sharedWith;
	}
}
