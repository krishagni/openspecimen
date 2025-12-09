package com.krishagni.catissueplus.core.administrative.domain;

import java.math.BigDecimal;
import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.beans.factory.annotation.Qualifier;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.service.LabelGenerator;

@Configurable
@Audited
public class DistributionOrderItem extends BaseEntity {
	public enum Status {
		DISTRIBUTED,
		
		DISTRIBUTED_AND_CLOSED,

		RETURNED
	}

	private static final String ENTITY_NAME = "distribution_order_item";
	
	private DistributionOrder order;
	
	private BigDecimal quantity;
	
	private Specimen specimen;

	private String specimenLocation;
	
	private Status status;

	private BigDecimal returnedQuantity;

	private StorageContainer returningContainer;

	private String returningRow;

	private String returningColumn;

	private User returnedBy;

	private Date returnDate;

	private Integer freezeThawIncrOnReturn;

	private String returnComments;

	private BigDecimal cost;

	private String label;
	private transient SpecimenRequestItem requestItem;

	private transient boolean printLabel;

	@Autowired
	@Qualifier("distributionLabelGenerator")
	private LabelGenerator labelGenerator;

	public DistributionOrder getOrder() {
		return order;
	}

	public void setOrder(DistributionOrder order) {
		this.order = order;
	}

	public BigDecimal getQuantity() {
		return quantity;
	}

	public void setQuantity(BigDecimal quantity) {
		this.quantity = quantity;
	}

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public String getSpecimenLocation() {
		return specimenLocation;
	}

	public void setSpecimenLocation(String specimenLocation) {
		this.specimenLocation = specimenLocation;
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public BigDecimal getReturnedQuantity() {
		return returnedQuantity;
	}

	public void setReturnedQuantity(BigDecimal returnedQuantity) {
		this.returnedQuantity = returnedQuantity;
	}

	public StorageContainer getReturningContainer() {
		return returningContainer;
	}

	public void setReturningContainer(StorageContainer returningContainer) {
		this.returningContainer = returningContainer;
	}

	public String getReturningRow() {
		return returningRow;
	}

	public void setReturningRow(String returningRow) {
		this.returningRow = returningRow;
	}

	public String getReturningColumn() {
		return returningColumn;
	}

	public void setReturningColumn(String returningColumn) {
		this.returningColumn = returningColumn;
	}

	public User getReturnedBy() {
		return returnedBy;
	}

	public void setReturnedBy(User returnedBy) {
		this.returnedBy = returnedBy;
	}

	public Date getReturnDate() {
		return returnDate;
	}

	public void setReturnDate(Date returnDate) {
		this.returnDate = returnDate;
	}

	public Integer getFreezeThawIncrOnReturn() {
		return freezeThawIncrOnReturn;
	}

	public void setFreezeThawIncrOnReturn(Integer freezeThawIncrOnReturn) {
		this.freezeThawIncrOnReturn = freezeThawIncrOnReturn;
	}

	public String getReturnComments() {
		return returnComments;
	}

	public void setReturnComments(String returnComments) {
		this.returnComments = returnComments;
	}

	public BigDecimal getCost() {
		return cost;
	}

	public void setCost(BigDecimal cost) {
		this.cost = cost;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public boolean isDistributedAndClosed() {
		return getStatus() == Status.DISTRIBUTED_AND_CLOSED;
	}

	@NotAudited
	public SpecimenRequestItem getRequestItem() {
		return requestItem;
	}

	public void setRequestItem(SpecimenRequestItem requestItem) {
		this.requestItem = requestItem;
	}

	public boolean isPrintLabel() {
		return printLabel;
	}

	public void setPrintLabel(boolean printLabel) {
		this.printLabel = printLabel;
	}

	public boolean isReturned() { return getStatus() == Status.RETURNED; }

	public void distribute() {
		if (requestItem != null) {
			requestItem.throwErrorIfFulfilled();
		}

		if (specimen.getPosition() != null) {
			setSpecimenLocation(specimen.getPosition().toString());
		}

		specimen.distribute(this);

		if (requestItem != null) {
			requestItem.distribute(getOrder());
		}

		String labelFmt = getOrder().getDistributionProtocol().getOrderItemLabelFormat();
		if (StringUtils.isNotBlank(labelFmt)) {
			setLabel(labelGenerator.generateLabel(labelFmt, this));
		}
	}

	public void undoDistribution() {
		if (requestItem != null) {
			requestItem.undoProcessing();
		}

		specimen.undoDistribution(this);
	}

	public void returnSpecimen() {
		specimen.returnSpecimen(this);
		setStatus(Status.RETURNED);
	}

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public static boolean isValidDistributionStatus(String status) {
		return status.equals(Status.DISTRIBUTED.name()) ||
			status.equals(Status.DISTRIBUTED_AND_CLOSED.name());
	}

	public static DistributionOrderItem createOrderItem(DistributionOrder order, Specimen specimen) {
		DistributionOrderItem item = new DistributionOrderItem();
		item.setOrder(order);
		item.setSpecimen(specimen);
		item.setQuantity(specimen.getAvailableQuantity());
		item.setStatus(Status.DISTRIBUTED_AND_CLOSED);

		return item;
	}
}
