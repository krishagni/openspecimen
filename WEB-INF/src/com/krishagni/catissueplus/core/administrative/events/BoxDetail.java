package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
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

	// DISPOSE, NOT_STORED, DISTRIBUTED
	private String removeSpecimensReason;

	// COMMENTs
	private String removeSpecimensComments;

	//
	// internal APIs
	//
	private StorageContainer box;

	private Consumer<StorageContainerPosition> onSpecimenStore;

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

	public String getRemoveSpecimensReason() {
		return removeSpecimensReason;
	}

	public void setRemoveSpecimensReason(String removeSpecimensReason) {
		this.removeSpecimensReason = removeSpecimensReason;
	}

	public String getRemoveSpecimensComments() {
		return removeSpecimensComments;
	}

	public void setRemoveSpecimensComments(String removeSpecimensComments) {
		this.removeSpecimensComments = removeSpecimensComments;
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

	public StorageContainer getBox() {
		return box;
	}

	public void setBox(StorageContainer box) {
		this.box = box;
	}

	public Consumer<StorageContainerPosition> getOnSpecimenStore() {
		return onSpecimenStore;
	}

	public void setOnSpecimenStore(Consumer<StorageContainerPosition> onSpecimenStore) {
		this.onSpecimenStore = onSpecimenStore;
	}
}
