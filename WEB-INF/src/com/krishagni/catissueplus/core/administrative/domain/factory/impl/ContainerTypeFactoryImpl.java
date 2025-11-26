package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ContainerTypeDetail;
import com.krishagni.catissueplus.core.administrative.events.ContainerTypeSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;
import com.krishagni.catissueplus.core.common.util.Status;

public class ContainerTypeFactoryImpl implements ContainerTypeFactory {
	private DaoFactory daoFactory;

	private LabelGenerator nameGenerator;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setNameGenerator(LabelGenerator nameGenerator) {
		this.nameGenerator = nameGenerator;
	}

	@Override
	public ContainerType createContainerType(ContainerTypeDetail detail) {
		return createContainerType(detail, null);
	}

	@Override
	public ContainerType createContainerType(ContainerTypeDetail detail, ContainerType existing) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		ContainerType containerType = new ContainerType();
		if (existing != null) {
			containerType.setId(existing.getId());
		} else {
			containerType.setId(detail.getId());
		}

		setName(detail, existing, containerType, ose);
		setNameFormat(detail, existing, containerType, ose);
		setTemperature(detail, existing, containerType, ose);
		setDimension(detail, existing, containerType, ose);
		setPositionLabelingMode(detail, existing, containerType, ose);
		setPositionAssignment(detail, existing, containerType, ose);
		setLabelingSchemes(detail, existing, containerType, ose);
		setStoreSpecimenEnabled(detail, existing, containerType, ose);
		setCanHold(detail, existing, containerType, ose);
		setActivityStatus(detail, existing, containerType, ose);

		ose.checkAndThrow();
		return containerType;
	}

	private void setName(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		String name = detail.getName();
		if (StringUtils.isBlank(name)) {
			ose.addError(ContainerTypeErrorCode.NAME_REQUIRED);
			return;
		}
		
		containerType.setName(name);
	}

	private void setName(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("name") || existing == null) {
			setName(detail, containerType, ose);
		} else {
			containerType.setName(existing.getName());
		}
	}

	private void setNameFormat(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		String nameFormat = detail.getNameFormat();
		if (StringUtils.isBlank(nameFormat)) {
			return;
		}

		if (!nameGenerator.isValidLabelTmpl(nameFormat)) {
			ose.addError(ContainerTypeErrorCode.INVALID_NAME_FORMAT, nameFormat);
			return;
		}

		containerType.setNameFormat(nameFormat);
	}

	private void setNameFormat(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("nameFormat") || existing == null) {
			setNameFormat(detail, containerType, ose);
		} else {
			containerType.setNameFormat(existing.getNameFormat());
		}
	}

	private void setTemperature(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("temperature") || existing == null) {
			containerType.setTemperature(detail.getTemperature());
		} else {
			containerType.setTemperature(existing.getTemperature());
		}
	}

	private void setDimension(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		setNoOfColumns(detail, existing, containerType, ose);
		setNoOfRows(detail, existing, containerType, ose);

		boolean rowDimLess = (containerType.getNoOfRows() == null);
		boolean colDimLess = (containerType.getNoOfColumns() == null);
		if ((!rowDimLess || !colDimLess) && (rowDimLess || colDimLess)) {
			ose.addError(ContainerTypeErrorCode.INVALID_CAPACITY);
		}

		try {
			containerType.ensureCapacityWithinLimits();
		} catch (OpenSpecimenException e) {
			ose.addErrors(e.getErrors());
		}
	}
	
	private void setNoOfColumns(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		Integer noOfCols = detail.getNoOfColumns();
		if (noOfCols != null && noOfCols <= 0) {
			ose.addError(ContainerTypeErrorCode.INVALID_CAPACITY, noOfCols);
		}
		
		containerType.setNoOfColumns(noOfCols);	
	}

	private void setNoOfColumns(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("noOfColumns") || existing == null) {
			setNoOfColumns(detail, containerType, ose);
		} else {
			containerType.setNoOfColumns(existing.getNoOfColumns());
		}
	}

	private void setNoOfRows(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		Integer noOfRows = detail.getNoOfRows();
		if (noOfRows != null && noOfRows <= 0) {
			ose.addError(ContainerTypeErrorCode.INVALID_CAPACITY, noOfRows);
		}
				
		containerType.setNoOfRows(noOfRows);		
	}

	private void setNoOfRows(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("noOfRows") || existing == null) {
			setNoOfRows(detail, containerType, ose);
		} else {
			containerType.setNoOfRows(existing.getNoOfRows());
		}
	}

	private void setPositionLabelingMode(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		try {
			if (StringUtils.isNotBlank(detail.getPositionLabelingMode())) {
				containerType.setPositionLabelingMode(StorageContainer.PositionLabelingMode.valueOf(detail.getPositionLabelingMode()));
			}
		} catch (Exception e) {
			ose.addError(StorageContainerErrorCode.INVALID_POSITION_LABELING_MODE, detail.getPositionLabelingMode());
		}
	}

	private void setPositionLabelingMode(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (containerType.isDimensionless()) {
			containerType.setPositionLabelingMode(StorageContainer.PositionLabelingMode.NONE);
			return;
		}

		if (detail.isAttrModified("positionLabelingMode") || existing == null) {
			setPositionLabelingMode(detail, containerType, ose);
		} else {
			containerType.setPositionLabelingMode(existing.getPositionLabelingMode());
		}
	}

	private void setPositionAssignment(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		try {
			String assignment = detail.getPositionAssignment();
			if (StringUtils.isNotBlank(assignment)) {
				containerType.setPositionAssignment(StorageContainer.PositionAssignment.valueOf(assignment.toUpperCase()));
			}
		} catch (Exception e) {
			ose.addError(StorageContainerErrorCode.INVALID_POSITION_ASSIGNMENT, detail.getPositionAssignment());
		}
	}

	private void setPositionAssignment(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (containerType.isDimensionless()) {
			containerType.setPositionAssignment(StorageContainer.PositionAssignment.HZ_TOP_DOWN_LEFT_RIGHT);
			return;
		}

		if (detail.isAttrModified("positionAssignment") || existing == null) {
			setPositionAssignment(detail, containerType, ose);
		} else {
			containerType.setPositionAssignment(existing.getPositionAssignment());
		}
	}

	private void setLabelingSchemes(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (containerType.getPositionLabelingMode() != StorageContainer.PositionLabelingMode.TWO_D) {
			containerType.setColumnLabelingScheme(StorageContainer.NUMBER_LABELING_SCHEME);
			containerType.setRowLabelingScheme(StorageContainer.NUMBER_LABELING_SCHEME);
			return;
		}

		setColumnLabelingScheme(detail, existing, containerType, ose);
		setRowLabelingScheme(detail, existing, containerType, ose);
	}
	
	private void setColumnLabelingScheme(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		String columnLabelingScheme = detail.getColumnLabelingScheme();
		if (StringUtils.isBlank(columnLabelingScheme)) {
			columnLabelingScheme = StorageContainer.NUMBER_LABELING_SCHEME;
		}
		
		if (!StorageContainer.isValidScheme(columnLabelingScheme)) {
			ose.addError(ContainerTypeErrorCode.INVALID_LABELING_SCHEME, columnLabelingScheme);
		}
		
		containerType.setColumnLabelingScheme(columnLabelingScheme);		
	}

	private void setColumnLabelingScheme(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("columnLabelingScheme") || existing == null) {
			setColumnLabelingScheme(detail, containerType, ose);
		} else {
			containerType.setColumnLabelingScheme(existing.getColumnLabelingScheme());
		}
	}
	
	private void setRowLabelingScheme(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		String rowLabelingScheme = detail.getRowLabelingScheme();
		if (StringUtils.isBlank(rowLabelingScheme)) {
			rowLabelingScheme = containerType.getColumnLabelingScheme();
		}
		
		if (!StorageContainer.isValidScheme(rowLabelingScheme)) {
			ose.addError(ContainerTypeErrorCode.INVALID_LABELING_SCHEME, rowLabelingScheme);
		}
		
		containerType.setRowLabelingScheme(rowLabelingScheme);		
	}

	private void setRowLabelingScheme(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("rowLabelingScheme") || existing == null) {
			setRowLabelingScheme(detail, containerType, ose);
		} else {
			containerType.setRowLabelingScheme(existing.getRowLabelingScheme());
		}
	}

	private void setStoreSpecimenEnabled(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("storeSpecimenEnabled") || existing == null) {
			containerType.setStoreSpecimenEnabled(detail.isStoreSpecimenEnabled());
		} else {
			containerType.setStoreSpecimenEnabled(existing.isStoreSpecimenEnabled());
		}
	}

	private void setCanHold(ContainerTypeDetail detail, ContainerType containerType, OpenSpecimenException ose) {
		ContainerTypeSummary typeDetail = detail.getCanHold();
		if (typeDetail == null) {
			return;
		}

		if (containerType.isStoreSpecimenEnabled()) {
			ose.addError(ContainerTypeErrorCode.CANNOT_HOLD_CONTAINER, containerType.getName());
			return;
		}
		
		Object key = null;
		ContainerType canHold = null;
		if (typeDetail.getId() != null) {
			canHold = daoFactory.getContainerTypeDao().getById(typeDetail.getId());
			key = typeDetail.getId();
		} else if (StringUtils.isNotBlank(typeDetail.getName())) {
			canHold = daoFactory.getContainerTypeDao().getByName(typeDetail.getName());
			key = typeDetail.getName();
		}
		
		if (canHold == null) {
			ose.addError(ContainerTypeErrorCode.NOT_FOUND, key);
			return;
		}
		
		containerType.setCanHold(canHold);
	}

	private void setCanHold(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("canHold") || existing == null) {
			setCanHold(detail, containerType, ose);
		} else {
			containerType.setCanHold(existing.getCanHold());
		}
	}
	
	private void setActivityStatus(ContainerTypeSummary detail, ContainerType containerType, OpenSpecimenException ose) {
		String status = detail.getActivityStatus();
		
		if (StringUtils.isBlank(status)) {
			containerType.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		} else if (Status.isValidActivityStatus(status)) {
			containerType.setActivityStatus(status);
		} else {
			ose.addError(ActivityStatusErrorCode.INVALID);
		}
	}

	private void setActivityStatus(ContainerTypeDetail detail, ContainerType existing, ContainerType containerType, OpenSpecimenException ose) {
		if (detail.isAttrModified("activityStatus") || existing == null) {
			setActivityStatus(detail, containerType, ose);
		} else {
			containerType.setActivityStatus(existing.getActivityStatus());
		}
	}
}
