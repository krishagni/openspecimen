package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;

public class ShipmentContainerDetail implements Comparable<ShipmentContainerDetail>, Serializable {

	private static final long serialVersionUID = -2432816789076113355L;

	private Long id;

	private StorageContainerSummary container;

	private String receivedQuality;

	private Integer specimensCount;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public StorageContainerSummary getContainer() {
		return container;
	}

	public void setContainer(StorageContainerSummary container) {
		this.container = container;
	}

	public String getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(String receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public Integer getSpecimensCount() {
		return specimensCount;
	}

	public void setSpecimensCount(Integer specimensCount) {
		this.specimensCount = specimensCount;
	}

	@Override
	public int compareTo(ShipmentContainerDetail other) {
		return getId().compareTo(other.getId());
	}

	public static ShipmentContainerDetail from(ShipmentContainer shipmentContainer) {
		ShipmentContainerDetail detail = new ShipmentContainerDetail();
		detail.setId(shipmentContainer.getId());

		StorageContainerSummary container = StorageContainerSummary.from(shipmentContainer.getContainer());
		if (shipmentContainer.getContainer().getAllowedCps() != null) {
			container.setAllowedCollectionProtocols(shipmentContainer.getContainer().getAllowedCps().stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet()));
		}

		if (shipmentContainer.getContainer().getCompAllowedCps() != null) {
			container.setCalcAllowedCollectionProtocols(shipmentContainer.getContainer().getCompAllowedCps().stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet()));
		}

		if (shipmentContainer.getContainer().getAllowedSpecimenClasses() != null) {
			container.setAllowedSpecimenClasses(PermissibleValue.toValueSet(shipmentContainer.getContainer().getAllowedSpecimenClasses()));
		}

		if (shipmentContainer.getContainer().getCompAllowedSpecimenClasses() != null) {
			container.setCalcAllowedSpecimenClasses(PermissibleValue.toValueSet(shipmentContainer.getContainer().getCompAllowedSpecimenClasses()));
		}

		if (shipmentContainer.getContainer().getAllowedSpecimenTypes() != null) {
			container.setAllowedSpecimenTypes(PermissibleValue.toValueSet(shipmentContainer.getContainer().getAllowedSpecimenTypes()));
		}

		if (shipmentContainer.getContainer().getCompAllowedSpecimenTypes() != null) {
			container.setCalcAllowedSpecimenTypes(PermissibleValue.toValueSet(shipmentContainer.getContainer().getCompAllowedSpecimenTypes()));
		}

		detail.setContainer(container);
		detail.setReceivedQuality(PermissibleValue.getValue(shipmentContainer.getReceivedQuality()));
		return detail;
	}

	public static List<ShipmentContainerDetail> from(Collection<ShipmentContainer> shipmentContainers) {
		return shipmentContainers.stream().map(ShipmentContainerDetail::from).collect(Collectors.toList());
	}
}
