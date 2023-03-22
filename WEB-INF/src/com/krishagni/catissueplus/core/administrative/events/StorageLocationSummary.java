package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageLocationSummary implements Serializable {
	
	private static final long serialVersionUID = 3492284917328450439L;

	private Long id;
	
	private String name;

	private String displayName;

	private String barcode;

	private String mode;
	
	private String positionX;
	
	private String positionY;

	private Integer position;

	private String reservationId;

	private UserSummary checkoutBy;

	private Date checkoutTime;

	private String checkoutComments;

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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getMode() {
		return mode;
	}

	public void setMode(String mode) {
		this.mode = mode;
	}

	public String getPositionX() {
		return positionX;
	}

	public void setPositionX(String positionX) {
		this.positionX = positionX;
	}

	public String getPositionY() {
		return positionY;
	}

	public void setPositionY(String positionY) {
		this.positionY = positionY;
	}

	public Integer getPosition() {
		return position;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public String getReservationId() {
		return reservationId;
	}

	public void setReservationId(String reservationId) {
		this.reservationId = reservationId;
	}

	public UserSummary getCheckoutBy() {
		return checkoutBy;
	}

	public void setCheckoutBy(UserSummary checkoutBy) {
		this.checkoutBy = checkoutBy;
	}

	public Date getCheckoutTime() {
		return checkoutTime;
	}

	public void setCheckoutTime(Date checkoutTime) {
		this.checkoutTime = checkoutTime;
	}

	public String getCheckoutComments() {
		return checkoutComments;
	}

	public void setCheckoutComments(String checkoutComments) {
		this.checkoutComments = checkoutComments;
	}

	public static StorageLocationSummary from(StorageContainerPosition position) {
		if (position == null) {
			return null;
		}
		
		StorageLocationSummary storageLocation = from(position.getContainer());
		storageLocation.setPositionX(position.getPosOne());
		storageLocation.setPositionY(position.getPosTwo());
		storageLocation.setPosition(position.getPosition());
		storageLocation.setReservationId(position.getReservationId());
		storageLocation.setCheckoutBy(UserSummary.from(position.getCheckoutBy()));
		storageLocation.setCheckoutComments(position.getCheckoutComments());
		storageLocation.setCheckoutTime(position.getCheckoutTime());
		return storageLocation;
	}

	public static StorageLocationSummary from(StorageContainer container) {
		StorageLocationSummary storageLocation = new StorageLocationSummary();
		storageLocation.setId(container.getId());
		storageLocation.setName(container.getName());
		storageLocation.setBarcode(container.getBarcode());
		storageLocation.setDisplayName(container.getDisplayName());
		storageLocation.setMode(container.getPositionLabelingMode().name());
		return storageLocation;
	}

	public static List<StorageLocationSummary> from(List<StorageContainerPosition> positions) {
		return positions.stream().map(StorageLocationSummary::from).collect(Collectors.toList());
	}
}
