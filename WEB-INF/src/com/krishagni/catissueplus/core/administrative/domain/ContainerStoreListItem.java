package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

public class ContainerStoreListItem extends BaseEntity {
	public enum Status {
		PENDING,

		STORED,

		RETRIEVED
	}

	private ContainerStoreList storeList;

	private Specimen specimen;

	private Status status = Status.PENDING;

	private Date ackTime = Calendar.getInstance().getTime();

	private String error;

	public ContainerStoreList getStoreList() {
		return storeList;
	}

	public void setStoreList(ContainerStoreList storeList) {
		this.storeList = storeList;
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

	public Date getAckTime() {
		return ackTime;
	}

	public void setAckTime(Date ackTime) {
		this.ackTime = ackTime;
	}

	public String getError() {
		return error;
	}

	public void setError(String error) {
		this.error = error;
	}
}
