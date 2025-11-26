package com.krishagni.catissueplus.core.administrative.domain;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.DependentEntityDetail;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
@Configurable
public class ContainerType extends BaseEntity {
	private static final String ENTITY_NAME = "container_type";
	
	private String name;

	private String nameFormat;

	private Integer noOfColumns;
	
	private Integer noOfRows;

	private StorageContainer.PositionLabelingMode positionLabelingMode = StorageContainer.PositionLabelingMode.TWO_D;

	private StorageContainer.PositionAssignment positionAssignment = StorageContainer.PositionAssignment.HZ_TOP_DOWN_LEFT_RIGHT;
	
	private String columnLabelingScheme;
	
	private String rowLabelingScheme;
	
	private Double temperature;
	
	private boolean storeSpecimenEnabled;
	
	private ContainerType canHold;
	
	private String activityStatus;
	
	private Set<StorageContainer> containers = new HashSet<>();

	private Set<ContainerType> canBeStoredIn = new HashSet<>();

	@Autowired
	private DaoFactory daoFactory;

	public static String getEntityName() {
		return ENTITY_NAME;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getNameFormat() {
		return nameFormat;
	}

	public void setNameFormat(String nameFormat) {
		this.nameFormat = nameFormat;
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

	public boolean isDimensionless() {
		return noOfRows == null && noOfColumns == null;
	}

	public StorageContainer.PositionLabelingMode getPositionLabelingMode() {
		return positionLabelingMode;
	}

	public void setPositionLabelingMode(StorageContainer.PositionLabelingMode positionLabelingMode) {
		this.positionLabelingMode = positionLabelingMode;
	}

	public StorageContainer.PositionAssignment getPositionAssignment() {
		return positionAssignment;
	}

	public void setPositionAssignment(StorageContainer.PositionAssignment positionAssignment) {
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

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}
	
	public boolean isStoreSpecimenEnabled() {
		return storeSpecimenEnabled;
	}

	public void setStoreSpecimenEnabled(boolean storeSpecimenEnabled) {
		this.storeSpecimenEnabled = storeSpecimenEnabled;
	}

	public ContainerType getCanHold() {
		return canHold;
	}

	public void setCanHold(ContainerType canHold) {
		this.canHold = canHold;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	@NotAudited
	public Set<StorageContainer> getContainers() {
		return containers;
	}

	public void setContainers(Set<StorageContainer> containers) {
		this.containers = containers;
	}

	@NotAudited
	public Set<ContainerType> getCanBeStoredIn() {
		return canBeStoredIn;
	}

	public void setCanBeStoredIn(Set<ContainerType> canBeStoredIn) {
		this.canBeStoredIn = canBeStoredIn;
	}


	public void update(ContainerType containerType) {
		setName(containerType.getName());
		setNameFormat(containerType.getNameFormat());
		setNoOfColumns(containerType.getNoOfColumns());
		setNoOfRows(containerType.getNoOfRows());
		setPositionLabelingMode(containerType.getPositionLabelingMode());
		setPositionAssignment(containerType.getPositionAssignment());
		setColumnLabelingScheme(containerType.getColumnLabelingScheme());
		setRowLabelingScheme(containerType.getRowLabelingScheme());
		setTemperature(containerType.getTemperature());
		setStoreSpecimenEnabled(containerType.isStoreSpecimenEnabled());
		setActivityStatus(containerType.getActivityStatus());
		updateCanHold(containerType.getCanHold());

		ensureCapacityWithinLimits();
	}
	
	public List<DependentEntityDetail> getDependentEntities() {
		return daoFactory.getContainerTypeDao().getDependentEntities(getId());
	}
	
	public void delete() {
		List<DependentEntityDetail> dependentEntities = getDependentEntities();
		if (!dependentEntities.isEmpty()) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.REF_ENTITY_FOUND, getName());
		}

		setName(Utility.getDisabledValue(getName(), 64));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public void ensureCapacityWithinLimits() {
		int maxAllowedSize = ConfigUtil.getInstance().getIntSetting("administrative", "max_container_capacity", 225);
		if (isDimensionless() || getNoOfRows() == null || getNoOfColumns() == null) {
			return;
		}

		int currentCapacity = getNoOfRows() * getNoOfColumns();
		if (currentCapacity > maxAllowedSize) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.EXCEED_CAPACITY_LMT, currentCapacity, maxAllowedSize);
		}
	}

	private void updateCanHold(ContainerType canHold) {
		if (cycleExistsInHierarchy(canHold)) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.CYCLES_NOT_ALLOWED, getName(), canHold.getName());
		}
		
		setCanHold(canHold);
	}

	private boolean cycleExistsInHierarchy(ContainerType canHold) {
		ContainerType other = canHold;
		while (other != null) {
			if (this.equals(other)) {
				return true;
			}

			other = other.getCanHold();
		}
		
		return false;
	}
}
