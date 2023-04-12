package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.List;

public class BoxDetail {
	private Long id;

	private String name;

	private String barcode;

	private String type;

	private StorageLocationSummary storageLocation;

	private List<StorageContainerPositionDetail> positions = new ArrayList<>();

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

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public StorageLocationSummary getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(StorageLocationSummary storageLocation) {
		this.storageLocation = storageLocation;
	}

	public List<StorageContainerPositionDetail> getPositions() {
		return positions;
	}

	public void setPositions(List<StorageContainerPositionDetail> positions) {
		this.positions = positions;
	}
}
