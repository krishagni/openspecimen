package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenEvent;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;

public class SpecimenReturnEvent extends SpecimenEvent {
	private BigDecimal quantity;

	private StorageContainer container;

	@Autowired
	private DaoFactory daoFactory;

	public SpecimenReturnEvent(Specimen specimen) {
		super(specimen);
	}

	public BigDecimal getQuantity() {
		loadRecordIfNotLoaded();
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		loadRecordIfNotLoaded();
		this.quantity = quantity;
	}

	public StorageContainer getContainer() {
		loadRecordIfNotLoaded();
		return container;
	}

	public void setContainer(StorageContainer container) {
		loadRecordIfNotLoaded();
		this.container = container;
	}
	@Override
	public String getFormName() {
		return "SpecimenReturnEvent";
	}

	@Override
	protected Map<String, Object> getEventAttrs() {
		Map<String, Object> attrs = new HashMap<>();
		attrs.put("quantity", quantity);
		attrs.put("container", container.getId());
		return attrs;
	}

	@Override
	protected void setEventAttrs(Map<String, Object> attrValues) {
		this.quantity = new BigDecimal(attrValues.get("quantity").toString());
		Long containerId = Long.parseLong(attrValues.get("container").toString());
		this.container = daoFactory.getStorageContainerDao().getById(containerId);
	}

	public static SpecimenReturnEvent createForDistributionOrderItem(DistributionOrderItem item) {
		SpecimenReturnEvent event = new SpecimenReturnEvent(item.getSpecimen());
		event.setId(item.getId());
		return event;
	}
}
