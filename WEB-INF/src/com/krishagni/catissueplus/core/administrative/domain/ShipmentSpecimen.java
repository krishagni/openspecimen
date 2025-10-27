package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentReceivedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenShipmentShippedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.SpecimenErrorCode;
import com.krishagni.catissueplus.core.biospecimen.services.SpecimenService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

@Configurable
@Audited
public class ShipmentSpecimen extends BaseEntity {
	private Shipment shipment;
	
	private Specimen specimen;

	private ShipmentContainer shipmentContainer;
	
	private PermissibleValue receivedQuality;

	@Autowired
	private SpecimenService spmnSvc;

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public ShipmentContainer getShipmentContainer() {
		return shipmentContainer;
	}

	public void setShipmentContainer(ShipmentContainer shipmentContainer) {
		this.shipmentContainer = shipmentContainer;
	}

	@Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
	public PermissibleValue getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(PermissibleValue receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public void ship() {
		if (!(shipment.isPending() || shipment.isRequested())) {
			return;
		}

		Shipment shipment = getShipment();
		if (shipment.isSpecimenShipment()) {
			moveToSite(shipment.getReceivingSite(), shipment.getShippedDate(), "Shipment: " + shipment.getName());
		}

		if (getSpecimen().isStoredInAutoFreezer()) {
			throw OpenSpecimenException.userError(SpecimenErrorCode.STORED_IN_AF_SHIP_NA, getSpecimen().getLabel(), getSpecimen().getPosition().toString());
		}

		AtomicBoolean saved = new AtomicBoolean(false);
		shipment.addOnSaveProc(
			() -> {
				if (!saved.get()) {
					addShippedEvent(this);
				}

				saved.set(true);
			}
		);
	}

	public void unship() {
		if (!getShipment().isShipped() || !getShipment().isSpecimenShipment()) {
			return;
		}

		moveToSite(getShipment().getSendingSite(), Calendar.getInstance().getTime(), "Shipment: " + shipment.getName() + " deleted.");
	}

	public void receive(ShipmentSpecimen other) {
		setReceivedQuality(other.getReceivedQuality());
		if (!shipment.isReceived()) {
			updateSpecimen(other);
			SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
		}
	}

	public void receive(PermissibleValue receivedQuality) {
		setReceivedQuality(receivedQuality);
		if (!shipment.isReceived()) {
			SpecimenShipmentReceivedEvent.createForShipmentItem(this).saveRecordEntry();
		}
	}

	public static ShipmentSpecimen createShipmentSpecimen(Shipment shipment, Specimen specimen) {
		ShipmentSpecimen shipmentSpmn = new ShipmentSpecimen();
		shipmentSpmn.setShipment(shipment);
		shipmentSpmn.setSpecimen(specimen);
		return shipmentSpmn;
	}

	private void addShippedEvent(ShipmentSpecimen item) {
		SpecimenShipmentShippedEvent.createForShipmentSpecimen(item).saveRecordEntry();
	}

	private void updateSpecimen(ShipmentSpecimen other) {
		if (!getShipment().isSpecimenShipment()) {
			return;
		}

		if (other.getReceivedQuality() != null && StringUtils.isNotBlank(other.getReceivedQuality().getValue())) {
			other.getSpecimen().setShipmentReceiveQuality(other.getReceivedQuality().getValue());
		}

		spmnSvc.updateSpecimen(getSpecimen(), other.getSpecimen());
	}

	private void moveToSite(Site site, Date time, String comments) {
		StorageContainerPosition position = new StorageContainerPosition();
		position.setContainer(site.getContainer());
		position.setOccupyingSpecimen(getSpecimen());
		position.setSupressAccessChecks(true);
		getSpecimen().updatePosition(position, null, time, comments);
	}
}
