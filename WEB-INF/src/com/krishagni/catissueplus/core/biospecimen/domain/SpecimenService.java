package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.User;

public class SpecimenService extends BaseEntity {
	private Specimen specimen;

	private Service service;

	private int units;

	private Date serviceDate;

	private User serviceBy;

	private String comments;

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public Service getService() {
		return service;
	}

	public void setService(Service service) {
		this.service = service;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public Date getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Date serviceDate) {
		this.serviceDate = serviceDate;
	}

	public User getServiceBy() {
		return serviceBy;
	}

	public void setServiceBy(User serviceBy) {
		this.serviceBy = serviceBy;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}
}
