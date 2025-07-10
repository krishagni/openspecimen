package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.User;

@Audited
public class LabSpecimenService extends BaseEntity {
	private Specimen specimen;

	private Service service;

	private int units;

	private Date serviceDate;

	private User servicedBy;

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

	public User getServicedBy() {
		return servicedBy;
	}

	public void setServicedBy(User servicedBy) {
		this.servicedBy = servicedBy;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public void update(LabSpecimenService other) {
		setUnits(other.getUnits());
		setServiceDate(other.getServiceDate());
		setServicedBy(other.getServicedBy());
		setComments(other.getComments());
	}
}
