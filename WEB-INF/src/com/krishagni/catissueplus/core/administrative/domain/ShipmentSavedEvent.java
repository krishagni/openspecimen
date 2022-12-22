package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class ShipmentSavedEvent extends OpenSpecimenEvent<Shipment> {

	public ShipmentSavedEvent(Shipment shipment) {
		super(null, shipment);
	}
}
