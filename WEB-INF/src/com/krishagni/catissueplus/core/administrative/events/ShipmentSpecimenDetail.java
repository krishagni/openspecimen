package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.ShipmentSpecimen;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;

public class ShipmentSpecimenDetail implements Comparable<ShipmentSpecimenDetail>, Serializable {
	
	private static final long serialVersionUID = -2432813388976113244L;

	private Long id;
	
	private SpecimenInfo specimen;
	
	private String receivedQuality;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public SpecimenInfo getSpecimen() {
		return specimen;
	}

	public void setSpecimen(SpecimenInfo specimen) {
		this.specimen = specimen;
	}

	public String getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(String receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	@Override
	public int compareTo(ShipmentSpecimenDetail other) {
		return getId().compareTo(other.getId());
	}

	public static ShipmentSpecimenDetail from(ShipmentSpecimen shipmentSpecimen) {
		return from(shipmentSpecimen, true);
	}

	public static ShipmentSpecimenDetail from(ShipmentSpecimen shipmentSpecimen, boolean incSpmn) {
		ShipmentSpecimenDetail itemDetail = new ShipmentSpecimenDetail();
		itemDetail.setId(shipmentSpecimen.getId());
		itemDetail.setReceivedQuality(PermissibleValue.getValue(shipmentSpecimen.getReceivedQuality()));
		if (incSpmn) {
			itemDetail.setSpecimen(SpecimenInfo.from(shipmentSpecimen.getSpecimen()));
		}
		return itemDetail;
	}
	
	public static List<ShipmentSpecimenDetail> from(Collection<ShipmentSpecimen> shipmentSpmns) {
		return shipmentSpmns.stream().map(ShipmentSpecimenDetail::from).collect(Collectors.toList());
	}
}
