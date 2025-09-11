package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSpecimen;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.administrative.events.ShipmentListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface ShipmentDao extends Dao<Shipment> {
	List<Shipment> getShipments(ShipmentListCriteria crit);

	Long getShipmentsCount(ShipmentListCriteria crit);

	Shipment getShipmentByName(String name);
	
	List<Specimen> getShippedSpecimensByIds(List<Long> specimenIds);

	Map<String, Object> getShipmentIds(String key, Object value);

	List<ShipmentContainer> getShipmentContainers(ShipmentItemsListCriteria crit);

	List<ShipmentSpecimen> getShipmentSpecimens(ShipmentItemsListCriteria crit);

	Map<Long, Integer> getSpecimensCount(Collection<Long> shipmentIds);

	Map<Long, Integer> getSpecimensCountByContainer(Long shipmentId, Collection<Long> containerIds);

	Integer deleteSpecimenShipmentEvents(Long shipmentId);

	int addSpecimensToCart(Long shipmentId, Long cartId);

	int removeSpecimensFromCart(Long shipmentId, Long cartId);

	void removeShipmentCart(Long cartId);
}
