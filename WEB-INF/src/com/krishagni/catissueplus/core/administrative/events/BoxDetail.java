package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;

@ListenAttributeChanges
public class BoxDetail extends AttributeModifiedSupport {
	private Long id;

	private String name;

	private String barcode;

	private String type;

	private StorageLocationSummary storageLocation;

	private Set<String> allowedSpecimenClasses;

	private Set<String> allowedSpecimenTypes;

	private Set<String> allowedCollectionProtocols;

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

	public Set<String> getAllowedSpecimenClasses() {
		return allowedSpecimenClasses;
	}

	public void setAllowedSpecimenClasses(Set<String> allowedSpecimenClasses) {
		this.allowedSpecimenClasses = allowedSpecimenClasses;
	}

	public Set<String> getAllowedSpecimenTypes() {
		return allowedSpecimenTypes;
	}

	public void setAllowedSpecimenTypes(Set<String> allowedSpecimenTypes) {
		this.allowedSpecimenTypes = allowedSpecimenTypes;
	}

	public Set<String> getAllowedCollectionProtocols() {
		return allowedCollectionProtocols;
	}

	public void setAllowedCollectionProtocols(Set<String> allowedCollectionProtocols) {
		this.allowedCollectionProtocols = allowedCollectionProtocols;
	}
}
