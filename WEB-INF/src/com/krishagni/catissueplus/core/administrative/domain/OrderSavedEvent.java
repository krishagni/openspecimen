package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class OrderSavedEvent extends OpenSpecimenEvent<DistributionOrder> {

	public OrderSavedEvent(DistributionOrder order) {
		super(null, order);
	}
}
