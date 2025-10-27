package com.krishagni.catissueplus.core.administrative.domain;

import java.util.List;

import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import com.krishagni.catissueplus.core.administrative.domain.factory.StorageContainerErrorCode;
import com.krishagni.catissueplus.core.administrative.events.ShipmentItemsListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.repository.SpecimenListCriteria;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

@Audited
public class ShipmentContainer extends BaseEntity {
	private Shipment shipment;

	private StorageContainer container;

	private PermissibleValue receivedQuality;

	public Shipment getShipment() {
		return shipment;
	}

	public void setShipment(Shipment shipment) {
		this.shipment = shipment;
	}

	public StorageContainer getContainer() {
		return container;
	}

	public void setContainer(StorageContainer container) {
		this.container = container;
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

		getContainer().moveTo(getShipment().getReceivingSite().getContainer());
		shipSpecimens();
	}

	public void unship() {
		if (!getShipment().isShipped()) {
			return;
		}

		getContainer().moveTo(getShipment().getSendingSite().getContainer());
	}

	public void receive(ShipmentContainer other) {
		receiveSpecimens(other.getReceivedQuality());
		setReceivedQuality(other.getReceivedQuality());
		if (!shipment.isReceived()) {
			updatePosition(other);
		}
	}

	private void shipSpecimens() {
		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");

		int startAt = 0, maxSpmns = 50;
		SpecimenListCriteria crit = new SpecimenListCriteria()
			.ancestorContainerId(getContainer().getId())
			.limitItems(true)
			.maxResults(maxSpmns);

		boolean endOfSpmns = false;
		while (!endOfSpmns) {
			List<Specimen> spmns = daoFactory.getSpecimenDao().getSpecimens(crit.startAt(startAt));
			for (Specimen spmn : spmns) {
				ShipmentSpecimen shipmentSpmn = getShipment().addShipmentSpecimen(spmn);
				shipmentSpmn.setShipmentContainer(this);
				shipmentSpmn.ship();
			}

			startAt += spmns.size();
			endOfSpmns = (spmns.size() < maxSpmns);
			spmns.clear();
		}
	}

	private void updatePosition(ShipmentContainer other) {
		StorageContainer parentContainer = null;
		StorageContainerPosition position = other.getContainer().getPosition();
		if (position != null) {
			parentContainer = position.getContainer();
			if (parentContainer.isPositionOccupied(position.getPosOne(), position.getPosTwo())) {
				throw OpenSpecimenException.userError(StorageContainerErrorCode.NO_FREE_SPACE, parentContainer.getName());
			}
		}

		getContainer().moveTo(other.getContainer().getSite(), parentContainer, position);
	}

	private void receiveSpecimens(PermissibleValue receivedQuality) {
		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");

		int startAt = 0, maxSpmns = 50;
		ShipmentItemsListCriteria crit = new ShipmentItemsListCriteria()
			.shipmentId(getShipment().getId())
			.containerId(getContainer().getId());

		boolean endOfSpmns = false;
		while (!endOfSpmns) {
			List<ShipmentSpecimen> spmns = daoFactory.getShipmentDao().getShipmentSpecimens(crit.startAt(startAt));
			for (ShipmentSpecimen spmn : spmns) {
				spmn.receive(receivedQuality);
			}

			startAt += spmns.size();
			endOfSpmns = (spmns.size() < maxSpmns);
			spmns.clear();
		}
	}
}
