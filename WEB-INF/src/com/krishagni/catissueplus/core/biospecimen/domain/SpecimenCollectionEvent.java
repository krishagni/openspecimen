package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.User;

//
// Added purely for backward compatibility w.r.t labels generation and printing
//
public class SpecimenCollectionEvent {
	private Specimen specimen;

	public SpecimenCollectionEvent(Specimen specimen) {
		this.specimen = specimen;
	}

	public PermissibleValue getProcedure() {
		return specimen.getCollectionProcedure();
	}

	public PermissibleValue getContainer() {
		return specimen.getCollectionContainer();
	}

	public User getUser() {
		return specimen.getCollectionUser();
	}

	public Date getTime() {
		return specimen.getCollectionTime();
	}

	public String getComments() {
		return specimen.getCollectionComments();
	}
}
