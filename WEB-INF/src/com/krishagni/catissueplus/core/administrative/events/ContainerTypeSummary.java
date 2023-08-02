package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;

@ListenAttributeChanges
public class ContainerTypeSummary extends AttributeModifiedSupport {
	private Long id;
	
	private String name;

	private String nameFormat;
	
	private Integer noOfColumns;
	
	private Integer noOfRows;

	private String positionLabelingMode;

	private String positionAssignment;

	private String columnLabelingScheme;

	private String rowLabelingScheme;

	private Double temperature;

	private boolean storeSpecimenEnabled;

	private String activityStatus;

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

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static <T extends ContainerTypeSummary> void copy(ContainerType containerType, T detail) {
		detail.setId(containerType.getId());
		detail.setName(containerType.getName());
		detail.setNameFormat(containerType.getNameFormat());
		detail.setNoOfColumns(containerType.getNoOfColumns());
		detail.setNoOfRows(containerType.getNoOfRows());
		detail.setPositionLabelingMode(containerType.getPositionLabelingMode().name());
		detail.setPositionAssignment(containerType.getPositionAssignment().name());
		detail.setColumnLabelingScheme(containerType.getColumnLabelingScheme());
		detail.setRowLabelingScheme(containerType.getRowLabelingScheme());
		detail.setTemperature(containerType.getTemperature());
		detail.setStoreSpecimenEnabled(containerType.isStoreSpecimenEnabled());
		detail.setActivityStatus(containerType.getActivityStatus());
	}
	
	public static ContainerTypeSummary from(ContainerType containerType) {
		if (containerType == null) {
			return null;
		}
		
		ContainerTypeSummary result = new ContainerTypeSummary();
		copy(containerType, result);
		return result;
	}
	
	public static List<ContainerTypeSummary> from(List<ContainerType> containerTypes) {
		return containerTypes.stream().map(ContainerTypeSummary::from).collect(Collectors.toList());
	}
}
