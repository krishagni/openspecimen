package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import org.hibernate.envers.Audited;

import com.krishagni.catissueplus.core.administrative.domain.User;

@Audited
public class SpecimenListItem extends BaseEntity {
	private Specimen specimen;

	private SpecimenList list;

	private User addedBy;

	private Date addedOn;

	public Specimen getSpecimen() {
		return specimen;
	}

	public void setSpecimen(Specimen specimen) {
		this.specimen = specimen;
	}

	public SpecimenList getList() {
		return list;
	}

	public void setList(SpecimenList list) {
		this.list = list;
	}

	public User getAddedBy() {
		return addedBy;
	}

	public void setAddedBy(User addedBy) {
		this.addedBy = addedBy;
	}

	public Date getAddedOn() {
		return addedOn;
	}

	public void setAddedOn(Date addedOn) {
		this.addedOn = addedOn;
	}
}
