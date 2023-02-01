package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@ListenAttributeChanges
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StorageContainerSummary extends AttributeModifiedSupport {
	private Long id;

	private String name;

	private String barcode;

	private String displayName;

	private Long typeId;
	
	private String typeName;

	private String usedFor;

	private String activityStatus;

	private String siteName;

	private StorageLocationSummary storageLocation;

	private UserSummary createdBy;
	
	private Integer noOfColumns;
	
	private Integer noOfRows;

	private String positionLabelingMode;

	private String positionAssignment;

	private String columnLabelingScheme;

	private String rowLabelingScheme;

	private Integer freePositions;

	private Integer usedPositions;
	
	private Boolean storeSpecimensEnabled;

	private Integer capacity;

	private Integer storedSpecimens;

	private Boolean automated;

	private String autoFreezerProvider;

	private Set<String> allowedCollectionProtocols = new HashSet<>();

	private Set<String> calcAllowedCollectionProtocols = new HashSet<>();

	private List<StorageContainerSummary> childContainers;

	private Boolean starred;

	private String status;

	private StorageLocationSummary blockedLocation;

	private Long freezerId;

	private String freezerName;

	private String freezerBarcode;

	private String freezerDisplayName;

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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getTypeId() {
		return typeId;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public String getTypeName() {
		return typeName;
	}

	public void setTypeName(String typeName) {
		this.typeName = typeName;
	}

	public String getUsedFor() {
		return usedFor;
	}

	public void setUsedFor(String usedFor) {
		this.usedFor = usedFor;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}
	
	public StorageLocationSummary getStorageLocation() {
		return storageLocation;
	}

	public void setStorageLocation(StorageLocationSummary storageLocation) {
		this.storageLocation = storageLocation;
	}

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}
	
	public Integer getNoOfColumns() {
		return noOfColumns;
	}

	public void setNoOfColumns(Integer noOfColumns) {
		this.noOfColumns = noOfColumns;
	}

	public Integer getNoOfRows() {
		return noOfRows;
	}

	public void setNoOfRows(Integer noOfRows) {
		this.noOfRows = noOfRows;
	}

	public String getPositionLabelingMode() {
		return positionLabelingMode;
	}

	public void setPositionLabelingMode(String positionLabelingMode) {
		this.positionLabelingMode = positionLabelingMode;
	}

	public String getPositionAssignment() {
		return positionAssignment;
	}

	public void setPositionAssignment(String positionAssignment) {
		this.positionAssignment = positionAssignment;
	}

	public String getColumnLabelingScheme() {
		return columnLabelingScheme;
	}

	public void setColumnLabelingScheme(String columnLabelingScheme) {
		this.columnLabelingScheme = columnLabelingScheme;
	}

	public String getRowLabelingScheme() {
		return rowLabelingScheme;
	}

	public void setRowLabelingScheme(String rowLabelingScheme) {
		this.rowLabelingScheme = rowLabelingScheme;
	}

	public Integer getFreePositions() {
		return freePositions;
	}

	public void setFreePositions(Integer freePositions) {
		this.freePositions = freePositions;
	}

	public Integer getUsedPositions() {
		return usedPositions;
	}

	public void setUsedPositions(Integer usedPositions) {
		this.usedPositions = usedPositions;
	}

	@JsonProperty
	public boolean isStoreSpecimensEnabled() {
		return storeSpecimensEnabled != null ? storeSpecimensEnabled : false;
	}
	
	@JsonIgnore
	public Boolean getStoreSpecimensEnabled() {
		return storeSpecimensEnabled;
	}

	@JsonProperty
	public void setStoreSpecimensEnabled(Boolean storeSpecimensEnabled) {
		this.storeSpecimensEnabled = storeSpecimensEnabled;
	}

	public Integer getCapacity() {
		return capacity;
	}

	public void setCapacity(Integer capacity) {
		this.capacity = capacity;
	}

	public Integer getStoredSpecimens() {
		return storedSpecimens;
	}

	public void setStoredSpecimens(Integer storedSpecimens) {
		this.storedSpecimens = storedSpecimens;
	}

	public Boolean getAutomated() {
		return automated;
	}

	public void setAutomated(Boolean automated) {
		this.automated = automated;
	}

	public String getAutoFreezerProvider() {
		return autoFreezerProvider;
	}

	public void setAutoFreezerProvider(String autoFreezerProvider) {
		this.autoFreezerProvider = autoFreezerProvider;
	}

	public Set<String> getAllowedCollectionProtocols() {
		return allowedCollectionProtocols;
	}

	public void setAllowedCollectionProtocols(Set<String> allowedCollectionProtocols) {
		this.allowedCollectionProtocols = allowedCollectionProtocols;
	}

	public Set<String> getCalcAllowedCollectionProtocols() {
		return calcAllowedCollectionProtocols;
	}

	public void setCalcAllowedCollectionProtocols(Set<String> calcAllowedCollectionProtocols) {
		this.calcAllowedCollectionProtocols = calcAllowedCollectionProtocols;
	}

	public List<StorageContainerSummary> getChildContainers() {
		return childContainers;
	}

	public void setChildContainers(List<StorageContainerSummary> childContainers) {
		this.childContainers = childContainers;
	}

	public Boolean getStarred() {
		return starred;
	}

	public void setStarred(Boolean starred) {
		this.starred = starred;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public StorageLocationSummary getBlockedLocation() {
		return blockedLocation;
	}

	public void setBlockedLocation(StorageLocationSummary blockedLocation) {
		this.blockedLocation = blockedLocation;
	}

	public Long getFreezerId() {
		return freezerId;
	}

	public void setFreezerId(Long freezerId) {
		this.freezerId = freezerId;
	}

	public String getFreezerName() {
		return freezerName;
	}

	public void setFreezerName(String freezerName) {
		this.freezerName = freezerName;
	}

	public String getFreezerBarcode() {
		return freezerBarcode;
	}

	public void setFreezerBarcode(String freezerBarcode) {
		this.freezerBarcode = freezerBarcode;
	}

	public String getFreezerDisplayName() {
		return freezerDisplayName;
	}

	public void setFreezerDisplayName(String freezerDisplayName) {
		this.freezerDisplayName = freezerDisplayName;
	}

	protected static void transform(StorageContainer container, StorageContainerSummary result) {
		transform(container, result, true);
	}

	protected static void transform(StorageContainer container, StorageContainerSummary result, boolean hydrated) {
		result.setId(container.getId());
		result.setName(container.getName());
		result.setBarcode(container.getBarcode());
		result.setDisplayName(container.getDisplayName());
		result.setUsedFor(container.getUsedFor().name());
		result.setActivityStatus(container.getActivityStatus());
		result.setCreatedBy(UserSummary.from(container.getCreatedBy()));
		
		result.setSiteName(container.getSite().getName());
		result.setStorageLocation(StorageLocationSummary.from(container.getPosition()));
		if (result.getStorageLocation() == null && container.getParentContainer() != null) {
			result.setStorageLocation(StorageLocationSummary.from(container.getParentContainer()));
		}
		
		result.setNoOfColumns(container.getNoOfColumns());
		result.setNoOfRows(container.getNoOfRows());
		result.setCapacity(container.getCapacity());
		result.setPositionLabelingMode(container.getPositionLabelingMode().name());
		result.setPositionAssignment(container.getPositionAssignment().name());
		result.setColumnLabelingScheme(container.getColumnLabelingScheme());
		result.setRowLabelingScheme(container.getRowLabelingScheme());
		result.setStoreSpecimensEnabled(container.isStoreSpecimenEnabled());
		result.setAutomated(container.isAutomated());
		result.setStatus(container.getStatus() != null ? container.getStatus().name() : null);
		if (hydrated) {
			result.setFreePositions(container.freePositionsCount());
			result.setBlockedLocation(StorageLocationSummary.from(container.getBlockedPosition()));
		}

		if (container.getAutoFreezerProvider() != null) {
			result.setAutoFreezerProvider(container.getAutoFreezerProvider().getName());
		}
		
		if (container.getType() != null) {
			result.setTypeId(container.getType().getId());
			result.setTypeName(container.getType().getName());
		}
	}
	
	public static StorageContainerSummary from(StorageContainer container) {
		return from(container, false);
	}
	
	public static StorageContainerSummary from(StorageContainer container, boolean includeChildren) {
		StorageContainerSummary result = new StorageContainerSummary();
		transform(container, result);
		if (includeChildren) {
			result.setChildContainers(from(container.getChildContainers(), true));
		}
		return result;
	}
	
	public static List<StorageContainerSummary> from(Collection<StorageContainer> containers) {
		return from(containers, false);
	}
	
	public static List<StorageContainerSummary> from(Collection<StorageContainer> containers, boolean includeChildren) {
		if (CollectionUtils.isEmpty(containers)) {
			return Collections.emptyList();
		}

		return containers.stream().map(c -> from(c, includeChildren)).collect(Collectors.toList());
	}
}
