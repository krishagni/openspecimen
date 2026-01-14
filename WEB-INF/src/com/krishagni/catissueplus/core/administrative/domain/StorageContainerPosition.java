package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Date;
import java.util.Objects;

import org.hibernate.envers.Audited;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.util.MessageUtil;

@Audited
public class StorageContainerPosition extends BaseEntity implements Comparable<StorageContainerPosition> {
	private Integer posOneOrdinal;
	
	private Integer posTwoOrdinal;
	
	private String posOne;
	
	private String posTwo;
	
	private StorageContainer container;
	
	private Specimen occupyingSpecimen;
	
	private StorageContainer occupyingContainer;

	private String reservationId;

	private Date reservationTime;

	private Boolean blocked;

	private Specimen checkoutSpecimen;

	private StorageContainer blockedForContainer;

	private User checkoutBy;

	private Date checkoutTime;

	private String checkoutComments;

	private transient boolean supressAccessChecks;

	public Integer getPosOneOrdinal() {
		return posOneOrdinal;
	}

	public void setPosOneOrdinal(Integer posOneOrdinal) {
		this.posOneOrdinal = posOneOrdinal;
	}

	public Integer getPosTwoOrdinal() {
		return posTwoOrdinal;
	}

	public void setPosTwoOrdinal(Integer posTwoOrdinal) {
		this.posTwoOrdinal = posTwoOrdinal;
	}

	public String getPosOne() {
		return posOne;
	}

	public void setPosOne(String posOne) {
		this.posOne = posOne;
	}

	public String getPosTwo() {
		return posTwo;
	}

	public void setPosTwo(String posTwo) {
		this.posTwo = posTwo;
	}

	public Integer getPosition() {
		if (getContainer().isDimensionless() || !isSpecified()) {
			return null;
		}

		return getContainer().getPositionAssigner().toPosition(getContainer(), getPosTwoOrdinal(), getPosOneOrdinal());
	}

	public StorageContainer getContainer() {
		return container;
	}

	public Specimen getOccupyingSpecimen() {
		return occupyingSpecimen;
	}

	public void setOccupyingSpecimen(Specimen occupyingSpecimen) {
		this.occupyingSpecimen = occupyingSpecimen;
	}

	public StorageContainer getOccupyingContainer() {
		return occupyingContainer;
	}

	public void setOccupyingContainer(StorageContainer occupyingContainer) {
		this.occupyingContainer = occupyingContainer;
	}

	public void setContainer(StorageContainer container) {
		this.container = container;
	}

	public String getReservationId() {
		return reservationId;
	}

	public void setReservationId(String reservationId) {
		this.reservationId = reservationId;
	}

	public Date getReservationTime() {
		return reservationTime;
	}

	public void setReservationTime(Date reservationTime) {
		this.reservationTime = reservationTime;
	}

	public Boolean getBlocked() {
		return blocked;
	}

	public void setBlocked(Boolean blocked) {
		this.blocked = blocked;
	}

	public Boolean isBlocked() {
		return blocked != null && blocked;
	}

	public Specimen getCheckoutSpecimen() {
		return checkoutSpecimen;
	}

	public void setCheckoutSpecimen(Specimen checkoutSpecimen) {
		this.checkoutSpecimen = checkoutSpecimen;
	}

	public StorageContainer getBlockedForContainer() {
		return blockedForContainer;
	}

	public void setBlockedForContainer(StorageContainer blockedForContainer) {
		this.blockedForContainer = blockedForContainer;
	}

	public User getCheckoutBy() {
		return checkoutBy;
	}

	public void setCheckoutBy(User checkoutBy) {
		this.checkoutBy = checkoutBy;
	}

	public Date getCheckoutTime() {
		return checkoutTime;
	}

	public void setCheckoutTime(Date checkoutTime) {
		this.checkoutTime = checkoutTime;
	}

	public String getCheckoutComments() {
		return checkoutComments;
	}

	public void setCheckoutComments(String checkoutComments) {
		this.checkoutComments = checkoutComments;
	}

	public boolean isHoldingLocation() {
		return getContainer() != null && getContainer().isDistributionContainer();
	}

	public boolean isSupressAccessChecks() {
		return supressAccessChecks;
	}

	public void setSupressAccessChecks(boolean supressAccessChecks) {
		this.supressAccessChecks = supressAccessChecks;
	}

	@Override
	public BaseEntity getRoot() {
		if (getOccupyingSpecimen() != null) {
			return getOccupyingSpecimen();
		} else if (getOccupyingContainer() != null) {
			return getOccupyingContainer();
		} else {
			return null;
		}
	}

	public void update(StorageContainerPosition other) {
		//
		// Ideally when container changes, we should first remove it from old container
		// map and then add to new container map. However this creates problem because
		// Hibernates does all inserts first, followed by updates and then delete
		// So added position in new container is deleted by remove from old container 
		// Therefore the hack is to only the new position and not remove it from old
		// container
		//
		
		boolean isContainerChanged = !getContainer().equals(other.getContainer());
		BeanUtils.copyProperties(other, this, POS_UPDATE_IGN_PROPS);
		
		if (isContainerChanged) { // The old position is not freed because hibernate deletes it 
			occupy();
		}
	}
	
	public void occupy() {
		container.addPosition(this);
	}
	
	public void vacate() {
		container.removePosition(this);
		if (getBlockedForContainer() != null) {
			setBlockedForContainer(null);
		}

		if (getCheckoutSpecimen() != null) {
			getCheckoutSpecimen().setCheckoutPosition(null);
			setCheckoutSpecimen(null);
		}
	}

	public boolean equals(String row, String column, String reservationId) {
		return row.equals(getPosTwo()) && column.equals(getPosOne()) && reservationId.equals(getReservationId());
	}

	public boolean isSpecified() {
		return getPosOneOrdinal() != null && getPosTwoOrdinal() != null &&
			getPosOneOrdinal() != 0 && getPosTwoOrdinal() != 0;
	}

	@Override
	public int compareTo(StorageContainerPosition other) {
		int cmp;
		if (getContainer().isDimensionless()) {
			cmp = getId().compareTo(other.getId());
		} else {
			cmp = getPosTwoOrdinal().compareTo(other.getPosTwoOrdinal());
			if (cmp == 0) {
				cmp = getPosOneOrdinal().compareTo(other.getPosOneOrdinal());
			}
		}

		return cmp;
	}

	public String toString() {
		StringBuilder result = new StringBuilder(getContainer().getName());
		if (getContainer().isDimensionless()) {
			return result.toString();
		}

		switch (getContainer().getPositionLabelingMode()) {
			case LINEAR -> result.append(" (").append(getPosition()).append(")");
			case TWO_D -> result.append(" (").append(getPosTwo()).append(" x ").append(getPosOne()).append(")");
		}

		return result.toString();
	}

	public static String getDisplayString(StorageContainerPosition position) {
		return position == null ? MessageUtil.getInstance().getMessage(NOT_STORED) : position.toString();
	}

	public static boolean areSame(StorageContainerPosition p1, StorageContainerPosition p2) {
		if (p1 == p2) {
			return true;
		} else if (p1 == null || p2 == null) {
			return false;
		} else if (!p1.getContainer().equals(p2.getContainer())) {
			return false;
		} else {
			return Objects.equals(p1.getPosOneOrdinal(), p2.getPosOneOrdinal()) &&
				Objects.equals(p1.getPosTwoOrdinal(), p2.getPosTwoOrdinal());
		}
	}
	
	private static final String[] POS_UPDATE_IGN_PROPS = new String[] {
		"id",
		"occupyingSpecimen",
		"occupyingContainer",
		"checkoutSpecimen"
	};

	private static final String NOT_STORED = "storage_container_not_stored";
}
