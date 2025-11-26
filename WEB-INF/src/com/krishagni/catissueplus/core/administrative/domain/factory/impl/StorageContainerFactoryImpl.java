package com.krishagni.catissueplus.core.administrative.domain.factory.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.AutoFreezerProvider;
import com.krishagni.catissueplus.core.administrative.domain.ContainerType;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.ContainerTypeErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerFactory;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.events.AutoFreezerProviderErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ContainerHierarchyDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageContainerDetail;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.de.domain.DeObject;

import static com.krishagni.catissueplus.core.common.PvAttributes.SPECIMEN_CLASS;

public class StorageContainerFactoryImpl implements StorageContainerFactory {
	private DaoFactory daoFactory;
	
	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public StorageContainer createStorageContainer(StorageContainerDetail detail) {
		return createStorageContainer(null, detail);
	}

	@Override
	public StorageContainer createStorageContainer(StorageContainer existing, StorageContainerDetail detail) {
		StorageContainer container = new StorageContainer();
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		if (existing != null) {
			container.setId(existing.getId());
		} else {
			container.setId(detail.getId());
		}

		setStatus(detail, existing, container, ose);
		setType(detail, existing, container, ose);
		setName(detail, existing, container, ose);
		setBarcode(detail, existing, container, ose);
		setDisplayName(detail, existing, container, ose);
		setUsageMode(detail, existing, container, ose);
		setTemperature(detail, existing, container, ose);
		setCapacity(detail, existing, container, ose);
		setPositionLabelingMode(detail, existing, container, ose);
		setPositionAssignment(detail, existing, container, ose);
		setLabelingSchemes(detail, existing, container, ose);
		setSiteAndParentContainer(detail, existing, container, ose);
		setPosition(detail, existing, container, ose);
		setCreatedBy(detail, existing, container, ose);
		setActivityStatus(detail, existing, container, ose);
		setComments(detail, existing, container, ose);
		setStoreSpecimenEnabled(detail, existing, container, ose);
		setAutomated(detail, existing, container, ose);
		setAutoFreezerProvider(detail, existing, container, ose);
		setCellDisplayProp(detail, existing, container, ose);
		setExtension(detail, existing, container, ose);
		setTransferDetails(detail, existing, container, ose);

		if (!container.isDistributionContainer()) {
			setAllowedSpecimenClasses(detail, existing, container, ose);
			setAllowedSpecimenTypes(detail, existing, container, ose);
			setAllowedCps(detail, existing, container, ose);
		} else {
			setAllowedDps(detail, existing, container, ose);
		}

		setComputedRestrictions(container);
		ose.checkAndThrow();
		return container;
	}

	@Override
	public StorageContainer createStorageContainer(String name, ContainerHierarchyDetail input) {
		ContainerType type = getType(input.getTypeId(), input.getTypeName());
		StorageContainerDetail detail = getStorageContainerDetail(type);
		detail.setName(name);
		detail.setUsedFor(input.getUsedFor());
		detail.setSiteName(input.getSiteName());
		detail.setStorageLocation(input.getStorageLocation());
		detail.setCellDisplayProp(input.getCellDisplayProp());
		detail.setAllowedSpecimenClasses(input.getAllowedSpecimenClasses());
		detail.setAllowedSpecimenTypes(input.getAllowedSpecimenTypes());
		detail.setAllowedCollectionProtocols(input.getAllowedCollectionProtocols());
		detail.setAllowedDistributionProtocols(input.getAllowedDistributionProtocols());
		
		if (input.getNoOfColumns() != null && input.getNoOfColumns() > 0) {
			detail.setNoOfColumns(input.getNoOfColumns());
		}

		if (input.getNoOfRows() != null && input.getNoOfRows() > 0) {
			detail.setNoOfRows(input.getNoOfRows());
		}

		if (StringUtils.isNotBlank(input.getColumnLabelingScheme())) {
			detail.setColumnLabelingScheme(input.getColumnLabelingScheme());
		}

		if (StringUtils.isNotBlank(input.getRowLabelingScheme())) {
			detail.setRowLabelingScheme(input.getRowLabelingScheme());
		}

		if (input.getTemperature() != null) {
			detail.setTemperature(input.getTemperature());
		}

		if (input.getStoreSpecimensEnabled() != null) {
			detail.setStoreSpecimensEnabled(input.getStoreSpecimensEnabled());
		}

		if (input.getExtensionDetail() != null) {
			detail.setExtensionDetail(input.getExtensionDetail());
		}
		
		StorageContainer container = createStorageContainer(detail);
		container.setType(type);
		return container;
	}
	
	@Override
	public StorageContainer createStorageContainer(String name, ContainerType type, StorageContainer parent) {
		StorageContainerDetail detail = getStorageContainerDetail(type);
		detail.setName(name);
		detail.setUsedFor(parent.getUsedFor().name());
		detail.setSiteName(parent.getSite().getName());
		detail.setCellDisplayProp(parent.getCellDisplayProp() != null ? parent.getCellDisplayProp().name() : null);

		StorageContainer container = createStorageContainer(detail);
		container.setParentContainer(parent);

		StorageContainerPosition position = parent.nextAvailablePosition();
		position.setOccupyingContainer(container);
		container.setPosition(position);
		setComputedRestrictions(container);
		container.setType(type);
		return container;
	}

	private void setStatus(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("status") || existing == null) {
			try {
				container.setStatus(detail.getStatus() == null ? null : StorageContainer.Status.valueOf(detail.getStatus()));
			} catch (Exception e) {
				ose.addError(CommonErrorCode.INVALID_INPUT, "Invalid status: " + detail.getStatus());
			}
		} else {
			container.setStatus(existing.getStatus());
		}
	}
	
	private void setName(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		String name = detail.getName();
		if (StringUtils.isBlank(name)) {
			if (container.getType() == null || StringUtils.isBlank(container.getType().getNameFormat())) {
				ose.addError(StorageContainerErrorCode.NAME_REQUIRED);
			}

			return;
		}
		
		container.setName(name);
	}
	
	private void setName(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("name") || existing == null) {
			setName(detail, container, ose);
		} else {
			container.setName(existing.getName());
		}
	}
	
	private void setBarcode(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getBarcode())) {
			return;
		}

		container.setBarcode(detail.getBarcode());
	}
	
	private void setBarcode(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("barcode") || existing == null) {
			setBarcode(detail, container, ose);
		} else {
			container.setBarcode(existing.getBarcode());
		}
	}

	private void setDisplayName(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getDisplayName())) {
			return;
		}

		container.setDisplayName(detail.getDisplayName());
	}

	private void setDisplayName(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("displayName") || existing == null) {
			setDisplayName(detail, container, ose);
		} else {
			container.setDisplayName(existing.getDisplayName());
		}
	}

	private void setUsageMode(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getUsedFor())) {
			container.setUsedFor(StorageContainer.UsageMode.STORAGE);
			return;
		}

		try {
			container.setUsedFor(StorageContainer.UsageMode.valueOf(detail.getUsedFor()));
		} catch (Exception e) {
			ose.addError(StorageContainerErrorCode.INV_USAGE_MODE, detail.getUsedFor());
		}
	}

	private void setUsageMode(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (existing == null) {
			setUsageMode(detail, container, ose);
		} else {
			container.setUsedFor(existing.getUsedFor());
		}
	}
	
	private void setType(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (detail.getTypeId() == null && StringUtils.isBlank(detail.getTypeName())) {
			return;
		}
		
		container.setType(getType(detail.getTypeId(), detail.getTypeName()));
	}
	
	private void setType(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("typeId") || detail.isAttrModified("typeName") || existing == null) {
			setType(detail, container, ose);
		} else {
			container.setType(existing.getType());
		}
	}	
	
	private void setTemperature(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (detail.getTemperature() != null || detail.isAttrModified("temperature")) {
			//
			// Either user has explicitly blanked out or specified temperature value
			//
			container.setTemperature(detail.getTemperature());
		} else if (container.getType() != null) {
			//
			// User has not specified any value for temperature; therefore pick it from
			// container type
			//
			container.setTemperature(container.getType().getTemperature());
		} else {
			//
			// fall through case - when nothing above, just set it to null
			//
			container.setTemperature(null);
		}
	}
	
	private void setTemperature(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("temperature") || existing == null) {
			setTemperature(detail, container, ose);
		} else {
			container.setTemperature(existing.getTemperature());
		}
	}
		
	private void setCapacity(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("noOfColumns") || existing == null) {
			setNoOfColumns(detail, container, ose);
		} else {
			container.setNoOfColumns(existing.getNoOfColumns());
		}
		
		if (detail.isAttrModified("noOfRows") || existing == null) {
			setNoOfRows(detail, container, ose);
		} else {
			container.setNoOfRows(existing.getNoOfRows());
		}

		try {
			container.ensureWithinCapacityLimits();
		} catch (OpenSpecimenException ce) {
			ose.addErrors(ce.getErrors());
		}

		boolean rowDimLess = (container.getNoOfRows() == null);
		boolean colDimLess = (container.getNoOfColumns() == null);
		if ((!rowDimLess || !colDimLess) && (rowDimLess || colDimLess)) {
			ose.addError(StorageContainerErrorCode.INVALID_DIMENSION_CAPACITY);
		}

		if (detail.isAttrModified("capacity") || existing == null) {
			setApproxCapacity(detail, container, ose);
		} else {
			container.setCapacity(existing.getCapacity());
		}
	}
	
	private void setNoOfColumns(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Integer noOfCols = null;
		if (detail.getNoOfColumns() != null || detail.isAttrModified("noOfColumns")) {
			noOfCols = detail.getNoOfColumns();
		} else if (container.getType() != null) {
			noOfCols = container.getType().getNoOfColumns();
		} else {
			noOfCols = null;
		}

		if (noOfCols != null && noOfCols <= 0) {
			ose.addError(StorageContainerErrorCode.INVALID_DIMENSION_CAPACITY);
		}

		container.setNoOfColumns(noOfCols);
	}
	
	private void setNoOfRows(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Integer noOfRows = null;
		if (detail.getNoOfRows() != null || detail.isAttrModified("noOfRows")) {
			noOfRows = detail.getNoOfRows();
		} else if (container.getType() != null) {
			noOfRows = container.getType().getNoOfRows();
		} else {
			noOfRows = null;
		}

		if (noOfRows != null && noOfRows <= 0) {
			ose.addError(StorageContainerErrorCode.INVALID_DIMENSION_CAPACITY);
		}

		container.setNoOfRows(noOfRows);
	}

	private void setApproxCapacity(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Integer capacity = detail.getCapacity();
		if (capacity == null) {
			return;
		}

		if (capacity <= 0) {
			ose.addError(StorageContainerErrorCode.INVALID_CAPACITY, capacity);
			return;
		}

		container.setCapacity(capacity);
	}

	private void setPositionLabelingMode(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		try {
			String mode = detail.getPositionLabelingMode();
			if (StringUtils.isNotBlank(mode)) {
				mode = mode.toUpperCase();
				container.setPositionLabelingMode(StorageContainer.PositionLabelingMode.valueOf(mode));
			} else if (container.getType() != null) {
				container.setPositionLabelingMode(container.getType().getPositionLabelingMode());
			}

			if (container.getPositionLabelingMode() == StorageContainer.PositionLabelingMode.NONE) {
				ose.addError(StorageContainerErrorCode.INVALID_POSITION_LABELING_MODE, detail.getPositionLabelingMode());
			}
		} catch (Exception e) {
			ose.addError(StorageContainerErrorCode.INVALID_POSITION_LABELING_MODE, detail.getPositionLabelingMode());
		}
	}

	private void setPositionLabelingMode(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (container.isDimensionless()) {
			container.setPositionLabelingMode(StorageContainer.PositionLabelingMode.NONE);
			return;
		}

		if (detail.isAttrModified("positionLabelingMode") || existing == null) {
			setPositionLabelingMode(detail, container, ose);
		} else {
			container.setPositionLabelingMode(existing.getPositionLabelingMode());
		}
	}

	private void setPositionAssignment(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		try {
			String assignment = detail.getPositionAssignment();
			if (StringUtils.isNotBlank(assignment)) {
				assignment = assignment.toUpperCase();
				container.setPositionAssignment(StorageContainer.PositionAssignment.valueOf(assignment));
			} else if (container.getType() != null) {
				container.setPositionAssignment(container.getType().getPositionAssignment());
			}
		} catch (Exception e) {
			ose.addError(StorageContainerErrorCode.INVALID_POSITION_ASSIGNMENT, detail.getPositionAssignment());
		}
	}

	private void setPositionAssignment(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (container.isDimensionless()) {
			container.setPositionAssignment(StorageContainer.PositionAssignment.HZ_TOP_DOWN_LEFT_RIGHT);
			return;
		}

		if (detail.isAttrModified("positionAssignment") || existing == null) {
			setPositionAssignment(detail, container, ose);
		} else {
			container.setPositionAssignment(existing.getPositionAssignment());
		}
	}

	private void setLabelingSchemes(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (container.getPositionLabelingMode() != StorageContainer.PositionLabelingMode.TWO_D) {
			container.setRowLabelingScheme(StorageContainer.NUMBER_LABELING_SCHEME);
			container.setColumnLabelingScheme(StorageContainer.NUMBER_LABELING_SCHEME);
			return;
		}

		if (detail.isAttrModified("columnLabelingScheme") || existing == null) {
			setColumnLabelingScheme(detail, container, ose);
		} else {
			container.setColumnLabelingScheme(existing.getColumnLabelingScheme());
		}
		
		if (detail.isAttrModified("rowLabelingScheme") || existing == null) {
			setRowLabelingScheme(detail, container, ose);
		} else {
			container.setRowLabelingScheme(existing.getRowLabelingScheme());
		}
	}
	
	private void setColumnLabelingScheme(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		String columnLabelingScheme = detail.getColumnLabelingScheme();

		if (StringUtils.isBlank(columnLabelingScheme)) {
			ContainerType type = container.getType();
			columnLabelingScheme = type != null ? type.getColumnLabelingScheme() : StorageContainer.NUMBER_LABELING_SCHEME;
		}
		
		if (!StorageContainer.isValidScheme(columnLabelingScheme)) {
			ose.addError(StorageContainerErrorCode.INVALID_DIMENSION_LABELING_SCHEME);
		}
		
		container.setColumnLabelingScheme(columnLabelingScheme);		
	}
	
	private void setRowLabelingScheme(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		String rowLabelingScheme = detail.getRowLabelingScheme();
		if (StringUtils.isBlank(rowLabelingScheme)) {
			ContainerType type = container.getType();
			rowLabelingScheme = type != null ? type.getRowLabelingScheme() : container.getColumnLabelingScheme();
		}
		
		if (!StorageContainer.isValidScheme(rowLabelingScheme)) {
			ose.addError(StorageContainerErrorCode.INVALID_DIMENSION_LABELING_SCHEME);
		}
		
		container.setRowLabelingScheme(rowLabelingScheme);		
	}
		
	private void setSiteAndParentContainer(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Site site = setSite(detail, container, ose);
		StorageContainer parentContainer = setParentContainer(detail, container, ose);
		
		if (site == null && parentContainer == null) {
			ose.addError(StorageContainerErrorCode.REQUIRED_SITE_OR_PARENT_CONT);
			return;
		}
		
		if (site == null) {
			container.setSite(parentContainer.getSite());
		} else if (parentContainer != null && !parentContainer.getSite().equals(site)) {
			ose.addError(StorageContainerErrorCode.INVALID_SITE_AND_PARENT_CONT);
		}

		if (container.isSiteContainer()) {
			if (!container.isDimensionless() || parentContainer != null) {
				ose.addError(StorageContainerErrorCode.SITE_CONT_VIOLATED);
			} else {
				container.addOnSaveProc(() -> container.getSite().setContainer(container));
			}
		}
	}
	
	private void setSiteAndParentContainer(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("siteName") || detail.isAttrModified("storageLocation") || existing == null) {
			setSiteAndParentContainer(detail, container, ose);
		} else {
			container.setSite(existing.getSite());
			container.setParentContainer(existing.getParentContainer());
		}
	}
	
	private Site setSite(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		String siteName = detail.getSiteName();
		if (StringUtils.isBlank(siteName)) {
			return null;
		}
				
		Site site = daoFactory.getSiteDao().getSiteByName(siteName);
		if (site == null) {
			ose.addError(SiteErrorCode.NOT_FOUND);			
		}
			
		container.setSite(site);
		return site;		
	}
	
	private StorageContainer setParentContainer(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		StorageContainer parentContainer = null;
		Object key = null;
		
		StorageLocationSummary storageLocation = detail.getStorageLocation();
		if (storageLocation == null) {
			return null;
		}
		
		if (storageLocation.getId() != null) {
			parentContainer = daoFactory.getStorageContainerDao().getById(storageLocation.getId());
			key = storageLocation.getId();
		} else if (StringUtils.isNotBlank(storageLocation.getName())) {
			parentContainer = daoFactory.getStorageContainerDao().getByName(storageLocation.getName());
			key = storageLocation.getName();
		}

		if (parentContainer == null) {
			if (key != null) {
				ose.addError(StorageContainerErrorCode.PARENT_CONT_NOT_FOUND, key);
			}
		} else {
			if (container.getUsedFor() != parentContainer.getUsedFor()) {
				ose.addError(StorageContainerErrorCode.USAGE_DIFFER, parentContainer.getUsedFor(), container.getUsedFor());
			}

			container.setParentContainer(parentContainer);
		}

		return parentContainer;
	}

	private void setCellDisplayProp(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (container.getParentContainer() != null) {
			container.setCellDisplayProp(container.getParentContainer().getCellDisplayProp());
			return;
		}

		StorageContainer.CellDisplayProp prop = StorageContainer.CellDisplayProp.SPECIMEN_LABEL;
		if (StringUtils.isNotBlank(detail.getCellDisplayProp())) {
			try {
				prop = StorageContainer.CellDisplayProp.valueOf(detail.getCellDisplayProp());
			} catch (IllegalArgumentException iae) {
				ose.addError(StorageContainerErrorCode.INVALID_CELL_DISP_PROP, detail.getCellDisplayProp());
				return;
			}
		}

		container.setCellDisplayProp(prop);
	}

	private void setCellDisplayProp(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("cellDisplayProp") || existing == null) {
			setCellDisplayProp(detail, container, ose);
		} else {
			container.setCellDisplayProp(existing.getCellDisplayProp());
		}
	}

	private void setExtension(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		DeObject extension = DeObject.createExtension(detail.getExtensionDetail(), container);
		container.setExtension(extension);
	}

	private void setExtension(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("extensionDetail") || existing == null) {
			setExtension(detail, container, ose);
		} else {
			container.setExtension(existing.getExtension());
		}
	}

	private void setTransferDetails(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		container.setTransferDate(detail.getTransferDate());
		container.setOpComments(detail.getTransferComments());
		container.setCheckOut(detail.getCheckOut());

		UserSummary transferredBy = detail.getTransferredBy();
		if (transferredBy == null) {
			return;
		}

		Object key = null;
		User user = null;
		if (transferredBy.getId() != null) {
			user = daoFactory.getUserDao().getById(transferredBy.getId());
			key = transferredBy.getId();
		} else if (StringUtils.isNotBlank(transferredBy.getEmailAddress())) {
			user = daoFactory.getUserDao().getUserByEmailAddress(transferredBy.getEmailAddress());
			key = transferredBy.getEmailAddress();
		}

		if (key != null && user == null) {
			ose.addError(UserErrorCode.NOT_FOUND, key);
		}

		container.setTransferredBy(user);
	}
	
	private void setPosition(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		StorageContainer parentContainer = container.getParentContainer();
		StorageLocationSummary location = detail.getStorageLocation();
		if (parentContainer == null || location == null || container.isCheckedOut()) { // top-level container or checked out; therefore no position
			return;
		}

		String posOne = location.getPositionX(), posTwo = location.getPositionY();
		if (!parentContainer.isDimensionless() && parentContainer.usesLinearLabelingMode() && location.getPosition() != null && location.getPosition() != 0) {
			Pair<Integer, Integer> coord = parentContainer.getPositionAssigner().fromPosition(parentContainer, location.getPosition());
			posTwo = coord.first().toString();
			posOne = coord.second().toString();
		}

		StorageContainerPosition position = null;
		if (!parentContainer.isDimensionless() && StringUtils.isNotBlank(posOne) && StringUtils.isNotBlank(posTwo)) {
			if (parentContainer.canContainerOccupyPosition(container.getId(), posOne, posTwo)) {
				position = parentContainer.createPosition(posOne, posTwo);
				parentContainer.setLastAssignedPos(position);
			} else {
				ose.addError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
			}
		} else {
			position = parentContainer.nextAvailablePosition();
			if (position == null) {
				ose.addError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
			}
		} 
		
		if (position != null) {
			position.setOccupyingContainer(container);
			container.setPosition(position);			
		}
	}
	
	private void setPosition(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("storageLocation") || existing == null) {
			setPosition(detail, container, ose);
		} else {
			container.setPosition(existing.getPosition());
		}
	}
	
	private void setCreatedBy(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Long userId = null;
		if (detail.getCreatedBy() == null) {
			userId = AuthUtil.getCurrentUser().getId();
		} else {
			userId = detail.getCreatedBy().getId();
		}

		User user = daoFactory.getUserDao().getById(userId);
		if (user == null) {
			ose.addError(UserErrorCode.NOT_FOUND);
			return;
		}
		
		container.setCreatedBy(user);
	}
	
	private void setCreatedBy(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (container.getId() != null) {
			return;
		}

		//
		// OPSMN-4458: Created by is set only when a new container is being created which has no ID
		//
		setCreatedBy(detail, container, ose);
	}
	
	private void setActivityStatus(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		String activityStatus = detail.getActivityStatus();
		if (activityStatus == null) {
			activityStatus = Status.ACTIVITY_STATUS_ACTIVE.getStatus();
		}
		
		if (!Status.isValidActivityStatus(activityStatus)) {
			ose.addError(ActivityStatusErrorCode.INVALID);
			return;
		}
		
		container.setActivityStatus(activityStatus);
	}
	
	private void setActivityStatus(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("activityStatus") || existing == null) {
			setActivityStatus(detail, container, ose);
		} else {
			container.setActivityStatus(existing.getActivityStatus());
		}
	}
	
	private void setComments(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		container.setComments(detail.getComments());
	}	
	
	private void setComments(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("comments") || existing == null) {
			setComments(detail, container, ose);
		} else {
			container.setComments(existing.getComments());
		}
	}

	private void setStoreSpecimenEnabled(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		boolean storeSpecimensEnabled = detail.isStoreSpecimensEnabled();
		if (detail.getStoreSpecimensEnabled() == null && container.getType() != null) {
			storeSpecimensEnabled = container.getType().isStoreSpecimenEnabled();
		}

		container.setStoreSpecimenEnabled(storeSpecimensEnabled);
	}

	private void setStoreSpecimenEnabled(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("storeSpecimensEnabled") || existing == null) {
			setStoreSpecimenEnabled(detail, container, ose);
		} else {
			container.setStoreSpecimenEnabled(existing.isStoreSpecimenEnabled());
		}
	}

	private void setAutomated(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		container.setAutomated(BooleanUtils.isTrue(detail.getAutomated()));
	}

	private void setAutomated(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("automated") || existing == null) {
			setAutomated(detail, container, ose);
		} else {
			container.setAutomated(existing.isAutomated());
		}

		if (container.isAutomated() && !container.isDimensionless()) {
			ose.addError(StorageContainerErrorCode.AUTOMATED_NOT_DIMENSIONLESS, detail.getName());
		}
	}

	private void setAutoFreezerProvider(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getAutoFreezerProvider()) || !container.isAutomated()) {
			return;
		}

		AutoFreezerProvider provider = daoFactory.getAutoFreezerProviderDao().getByName(detail.getAutoFreezerProvider());
		if (provider == null) {
			ose.addError(AutoFreezerProviderErrorCode.NOT_FOUND, detail.getAutoFreezerProvider());
		}

		container.setAutoFreezerProvider(provider);
	}

	private void setAutoFreezerProvider(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (!container.isAutomated()) {
			return;
		}

		if (detail.isAttrModified("autoFreezerProvider") || existing == null) {
			setAutoFreezerProvider(detail, container, ose);
		} else {
			container.setAutoFreezerProvider(existing.getAutoFreezerProvider());
		}
	}

	private void setAllowedSpecimenClasses(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Set<String> allowedSpecimenClasses = detail.getAllowedSpecimenClasses();
		if (CollectionUtils.isEmpty(allowedSpecimenClasses)) {
			return;
		}

		List<PermissibleValue> classPvs = daoFactory.getPermissibleValueDao()
			.getPvs(SPECIMEN_CLASS, allowedSpecimenClasses);
		if (classPvs.size() != allowedSpecimenClasses.size()) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_CLASS, String.join(", ", allowedSpecimenClasses));
			return;
		}

		container.setAllowedSpecimenClasses(new HashSet<>(classPvs));
	}
	
	private void setAllowedSpecimenClasses(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("allowedSpecimenClasses") || existing == null) {
			setAllowedSpecimenClasses(detail, container, ose);
		} else {
			container.setAllowedSpecimenClasses(existing.getAllowedSpecimenClasses());
		}
	}
	
	private void setAllowedSpecimenTypes(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Set<String> allowedSpecimenTypes = detail.getAllowedSpecimenTypes();
		if (CollectionUtils.isEmpty(allowedSpecimenTypes)) {
			return;
		}

		List<PermissibleValue> typePvs = daoFactory.getPermissibleValueDao()
			.getPvs(SPECIMEN_CLASS, allowedSpecimenTypes);
		if (typePvs.size() != allowedSpecimenTypes.size()) {
			ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, String.join(", ", allowedSpecimenTypes));
			return;
		}

		for (PermissibleValue typePv : typePvs) {
			if (typePv.getParent() == null) {
				ose.addError(SpecimenErrorCode.INVALID_SPECIMEN_TYPE, typePv.getValue());
				return;
			}
		}

		container.setAllowedSpecimenTypes(new HashSet<>(typePvs));
	}
	
	private void setAllowedSpecimenTypes(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("allowedSpecimenTypes") || existing == null) {
			setAllowedSpecimenTypes(detail, container, ose);
		} else {
			container.setAllowedSpecimenTypes(existing.getAllowedSpecimenTypes());
		}
	}
	
	private void setAllowedCps(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Set<String> allowedCps = detail.getAllowedCollectionProtocols();
		
		List<CollectionProtocol> cps = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(allowedCps) && container.getSite() != null) {
			cps = daoFactory.getCollectionProtocolDao().getCpsByShortTitle(allowedCps, container.getSite().getName());
			if (cps.size() != allowedCps.size()) {
				ose.addError(StorageContainerErrorCode.INVALID_CPS);
				return;
			}			
		}

		container.setAllowedCps(new HashSet<>(cps));
	}
	
	private void setAllowedCps(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("allowedCollectionProtocols") || existing == null) {
			setAllowedCps(detail, container, ose);
		} else {
			container.setAllowedCps(existing.getAllowedCps());
		}		
	}

	private void setAllowedDps(StorageContainerDetail detail, StorageContainer container, OpenSpecimenException ose) {
		Set<String> allowedDps = detail.getAllowedDistributionProtocols();

		List<DistributionProtocol> dps = new ArrayList<>();
		if (CollectionUtils.isNotEmpty(allowedDps)) {
			dps = daoFactory.getDistributionProtocolDao().getDistributionProtocols(allowedDps);
			if (dps.size() != allowedDps.size()) {
				ose.addError(StorageContainerErrorCode.INVALID_DPS);
				return;
			}
		}

		container.setAllowedDps(new HashSet<>(dps));
	}

	private void setAllowedDps(StorageContainerDetail detail, StorageContainer existing, StorageContainer container, OpenSpecimenException ose) {
		if (detail.isAttrModified("allowedDistributionProtocols") || existing == null) {
			setAllowedDps(detail, container, ose);
		} else {
			container.setAllowedDps(existing.getAllowedDps());
		}
	}

	private void setComputedRestrictions(StorageContainer container) {
		if (container.isDistributionContainer()) {
			container.setCompAllowedDps(container.computeAllowedDps());
		} else {
			container.setCompAllowedSpecimenClasses(container.computeAllowedSpecimenClasses());
			container.setCompAllowedSpecimenTypes(container.computeAllowedSpecimenTypes());
			container.setCompAllowedCps(container.computeAllowedCps());
		}
	}
	
	private ContainerType getType(Long id, String name) {
		ContainerType type = null;
		Object key = null;

		if (id != null) {
			type = daoFactory.getContainerTypeDao().getById(id);
			key = id;
		} else if (StringUtils.isNotBlank(name)) {
			type = daoFactory.getContainerTypeDao().getByName(name);
			key = name;
		} else {
			throw OpenSpecimenException.userError(StorageContainerErrorCode.TYPE_REQUIRED);
		}
		
		if (type == null) {
			throw OpenSpecimenException.userError(ContainerTypeErrorCode.NOT_FOUND, key);
		}

		return type;
	}
	

	private StorageContainerDetail getStorageContainerDetail(ContainerType type) {
		StorageContainerDetail detail = new StorageContainerDetail();
		detail.setNoOfColumns(type.getNoOfColumns());
		detail.setNoOfRows(type.getNoOfRows());
		detail.setPositionLabelingMode(type.getPositionLabelingMode().name());
		detail.setColumnLabelingScheme(type.getColumnLabelingScheme());
		detail.setRowLabelingScheme(type.getRowLabelingScheme());
		detail.setPositionAssignment(type.getPositionAssignment().name());
		detail.setTemperature(type.getTemperature());
		detail.setStoreSpecimensEnabled(type.isStoreSpecimenEnabled());
		return detail;
	}
}