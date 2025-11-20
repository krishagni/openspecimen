package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenCollectionReceiveDetail;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class ReceivedEventDetail extends SpecimenEventDetail {
	private String receivedQuality;

	private String newLabel;

	public String getReceivedQuality() {
		return receivedQuality;
	}

	public void setReceivedQuality(String receivedQuality) {
		this.receivedQuality = receivedQuality;
	}

	public String getNewLabel() {
		return newLabel;
	}

	public void setNewLabel(String newLabel) {
		this.newLabel = newLabel;
	}

	public static ReceivedEventDetail from(Specimen specimen) {
		if (specimen == null) {
			return null;
		} else if (specimen.isDerivative() || specimen.isAliquot()) {
			return from(specimen.getCollRecvDetails());
		}

		ReceivedEventDetail detail = new ReceivedEventDetail();
		detail.setReceivedQuality(PermissibleValue.getValue(specimen.getReceivedQuality()));
		detail.setUser(UserSummary.from(specimen.getReceivedUser()));
		detail.setTime(specimen.getReceivedTime());
		detail.setComments(specimen.getReceivedComments());
		return detail;
	}

	private static ReceivedEventDetail from(SpecimenCollectionReceiveDetail cre) {
		if (cre == null) {
			return null;
		}

		ReceivedEventDetail re = new ReceivedEventDetail();
		re.setId(cre.getRecvEventId());
		re.setReceivedQuality(cre.getRecvQuality());
		re.setTime(cre.getRecvTime());
		re.setUser(UserSummary.from(cre.getReceiver()));
		re.setComments(cre.getRecvComments());
		return re;
	}
}