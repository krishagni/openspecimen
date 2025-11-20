package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionReceiveDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class CollectionEventDetail extends SpecimenEventDetail {
	private String procedure;
	
	private String container;

	public String getProcedure() {
		return procedure;
	}

	public void setProcedure(String procedure) {
		this.procedure = procedure;
	}

	public String getContainer() {
		return container;
	}

	public void setContainer(String container) {
		this.container = container;
	}

	public static CollectionEventDetail from(Specimen specimen) {
		if (specimen == null) {
			return null;
		} else if (specimen.isAliquot() || specimen.isDerivative()) {
			return from(specimen.getCollRecvDetails());
		}

		CollectionEventDetail detail = new CollectionEventDetail();
		detail.setContainer(PermissibleValue.getValue(specimen.getCollectionContainer()));
		detail.setProcedure(PermissibleValue.getValue(specimen.getCollectionProcedure()));
		detail.setUser(UserSummary.from(specimen.getCollectionUser()));
		detail.setTime(specimen.getCollectionTime());
		detail.setComments(specimen.getCollectionComments());
		return detail;
	}

	private static CollectionEventDetail from(SpecimenCollectionReceiveDetail cre) {
		if (cre == null) {
			return null;
		}

		CollectionEventDetail ce = new CollectionEventDetail();
		ce.setId(cre.getCollEventId());
		ce.setContainer(cre.getCollContainer());
		ce.setProcedure(cre.getCollProcedure());
		ce.setTime(cre.getCollTime());
		ce.setUser(UserSummary.from(cre.getCollector()));
		ce.setComments(cre.getCollComments());
		return ce;
	}
}
