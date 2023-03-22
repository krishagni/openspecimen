package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainerPosition;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

@Configurable
public class SpecimenTransferEvent extends SpecimenEvent {
	private StorageContainer fromContainer;
	
	private Integer fromDimensionOne;
	
	private Integer fromDimensionTwo;

	private Integer fromPosition;

	private String fromRow;

	private String fromCol;

	private StorageContainer toContainer;
	
	private Integer toDimensionOne;
	
	private Integer toDimensionTwo;

	private Integer toPosition;

	private String toRow;

	private String toCol;

	private String status;

	@Autowired
	private DaoFactory daoFactory;

	public SpecimenTransferEvent(Specimen specimen) {
		super(specimen);
	}

	public StorageContainer getFromContainer() {
		loadRecordIfNotLoaded();
		return fromContainer;
	}

	public void setFromContainer(StorageContainer fromContainer) {
		this.fromContainer = fromContainer;
	}

	public Integer getFromDimensionOne() {
		loadRecordIfNotLoaded();
		return fromDimensionOne;
	}

	public void setFromDimensionOne(Integer fromDimensionOne) {
		this.fromDimensionOne = fromDimensionOne;
	}

	public Integer getFromDimensionTwo() {
		loadRecordIfNotLoaded();
		return fromDimensionTwo;
	}

	public void setFromDimensionTwo(Integer fromDimensionTwo) {
		this.fromDimensionTwo = fromDimensionTwo;
	}

	public Integer getFromPosition() {
		return fromPosition;
	}

	public void setFromPosition(Integer fromPosition) {
		this.fromPosition = fromPosition;
	}

	public String getFromRow() {
		return fromRow;
	}

	public void setFromRow(String fromRow) {
		this.fromRow = fromRow;
	}

	public String getFromCol() {
		return fromCol;
	}

	public void setFromCol(String fromCol) {
		this.fromCol = fromCol;
	}

	public void setFromLocation(StorageContainerPosition from) {
		setFromContainer(from.getContainer());
		setFromDimensionOne(from.getPosOneOrdinal());
		setFromDimensionTwo(from.getPosTwoOrdinal());
		setFromRow(from.getPosTwo());
		setFromCol(from.getPosOne());
	}

	public StorageContainer getToContainer() {
		loadRecordIfNotLoaded();
		return toContainer;
	}

	public void setToContainer(StorageContainer toContainer) {
		this.toContainer = toContainer;
	}

	public Integer getToDimensionOne() {
		loadRecordIfNotLoaded();
		return toDimensionOne;
	}

	public void setToDimensionOne(Integer toDimensionOne) {
		this.toDimensionOne = toDimensionOne;
	}

	public Integer getToDimensionTwo() {
		loadRecordIfNotLoaded();
		return toDimensionTwo;
	}

	public void setToDimensionTwo(Integer toDimensionTwo) {
		this.toDimensionTwo = toDimensionTwo;
	}

	public Integer getToPosition() {
		return toPosition;
	}

	public void setToPosition(Integer toPosition) {
		this.toPosition = toPosition;
	}

	public String getToRow() {
		return toRow;
	}

	public void setToRow(String toRow) {
		this.toRow = toRow;
	}

	public String getToCol() {
		return toCol;
	}

	public void setToCol(String toCol) {
		this.toCol = toCol;
	}

	public void setToLocation(StorageContainerPosition to) {
		setToContainer(to.getContainer());
		setToDimensionOne(to.getPosOneOrdinal());
		setToDimensionTwo(to.getPosTwoOrdinal());
		setToRow(to.getPosTwo());
		setToCol(to.getPosOne());
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	@Override
	public Map<String, Object> getEventAttrs() {
		Map<String, Object> eventAttrs = new HashMap<>();
		if (fromContainer != null) {
			eventAttrs.put("fromContainer", fromContainer.getId());
			eventAttrs.put("fromDimensionOne", fromDimensionOne);
			eventAttrs.put("fromDimensionTwo", fromDimensionTwo);
			eventAttrs.put("fromRow", fromRow);
			eventAttrs.put("fromCol", fromCol);
			eventAttrs.put("fromPosition", getPosition(fromContainer, fromDimensionOne, fromDimensionTwo));
		}
		
		if (toContainer != null) {
			eventAttrs.put("toContainer", toContainer.getId());
			eventAttrs.put("toDimensionOne", toDimensionOne);
			eventAttrs.put("toDimensionTwo", toDimensionTwo);
			eventAttrs.put("toRow", toRow);
			eventAttrs.put("toCol", toCol);
			eventAttrs.put("toPosition", getPosition(toContainer, toDimensionOne, toDimensionTwo));
		}

		eventAttrs.put("status", status);
		return eventAttrs;
	}

	@Override
	public void setEventAttrs(Map<String, Object> attrValues) {
		Number fromContainerId = (Number)attrValues.get("fromContainer");
		if (fromContainerId != null) {
			setFromContainer(getContainer(fromContainerId));
			setFromDimensionOne(getInt(attrValues.get("fromDimensionOne")));
			setFromDimensionTwo(getInt(attrValues.get("fromDimensionTwo")));
			setFromPosition(getInt(attrValues.get("fromPosition")));
			setFromRow(getStr(attrValues.get("fromRow")));
			setFromCol(getStr(attrValues.get("fromCol")));
		}
		
		Number toContainerId = (Number)attrValues.get("toContainer");
		if (toContainerId != null) {
			setToContainer(getContainer(toContainerId));
			setToDimensionOne(getInt(attrValues.get("toDimensionOne"))); 
			setToDimensionTwo(getInt(attrValues.get("toDimensionTwo")));
			setToPosition(getInt(attrValues.get("toPosition")));
			setToRow(getStr(attrValues.get("toRow")));
			setToCol(getStr(attrValues.get("toCol")));
		}

		setStatus(getStr(attrValues.get("status")));
	}

	@Override
	public String getFormName() {
		return "SpecimenTransferEvent";
	}
			
	public static List<SpecimenTransferEvent> getFor(Specimen specimen) {
		List<Long> recIds = new SpecimenTransferEvent(specimen).getRecordIds();		
		if (CollectionUtils.isEmpty(recIds)) {
			return Collections.emptyList();
		}
		
		List<SpecimenTransferEvent> events = new ArrayList<>();
		for (Long recId : recIds) {
			SpecimenTransferEvent event = new SpecimenTransferEvent(specimen);
			event.setId(recId);
			events.add(event);
		}
		
		return events;		
	}
	
	private StorageContainer getContainer(Number containerId) {
		return daoFactory.getStorageContainerDao().getById(containerId.longValue());
	}
	
	private Integer getInt(Object number) {
		return number instanceof Number ? ((Number) number).intValue() : null;
	}

	private String getStr(Object str) {
		return str != null ? str.toString() : null;
	}

	private Integer getPosition(StorageContainer container, Integer dimOne, Integer dimTwo) {
		if (container.isDimensionless() || !isSpecified(dimOne, dimTwo)) {
			return null;
		}

		return container.getPositionAssigner().toPosition(container, dimTwo, dimOne);
	}

	private boolean isSpecified(Integer dimOne, Integer dimTwo) {
		return dimOne != null && dimTwo != null && dimOne != 0 && dimTwo != 0;
	}
}
