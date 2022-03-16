package com.krishagni.catissueplus.core.administrative.domain;

import com.krishagni.catissueplus.core.administrative.domain.factory.SpecimenRequestErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class SpecimenRequestItem extends BaseEntity {
	public enum Status {
		PENDING,

		DISTRIBUTED,

		SHIPPED
	}

	private SpecimenRequest request;

	private Specimen specimen;

	private Status status = Status.PENDING;

	private DistributionOrder distribution;

	private Shipment shipment;

	public SpecimenRequest getRequest() {
		return request;
	}

	public void setRequest(SpecimenRequest request) {
		this.request = request;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public DistributionOrder getDistribution() {
		return distribution;
	}

	public void setDistribution(DistributionOrder distribution) {
		this.distribution = distribution;
	}

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public void distribute(DistributionOrder distribution) {
		setStatus(Status.DISTRIBUTED);
		setDistribution(distribution);
	}

	public void undoProcessing() {
		setStatus(Status.PENDING);
		setDistribution(null);
		setShipment(null);
	}

	public void ship(Shipment shipment) {
		setStatus(Status.SHIPPED);
		setShipment(shipment);
	}

	public boolean isPending() {
		return status == Status.PENDING;
	}

	public void throwErrorIfFulfilled() {
		ErrorCode error = null;
		switch (getStatus()) {
			case DISTRIBUTED:
				error = SpecimenRequestErrorCode.SPECIMEN_DISTRIBUTED;
				break;

			case SHIPPED:
				error = SpecimenRequestErrorCode.SPECIMEN_SHIPPED;
				break;

			default:
				error = null;
				break;
		}

		if (error != null) {
			throw OpenSpecimenException.userError(error, getSpecimen().getLabel());
		}
	}
}