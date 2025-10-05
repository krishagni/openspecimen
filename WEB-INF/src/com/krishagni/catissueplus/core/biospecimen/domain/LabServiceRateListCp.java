package com.krishagni.catissueplus.core.biospecimen.domain;

import org.hibernate.envers.Audited;

@Audited
public class LabServiceRateListCp extends BaseEntity {
	private LabServicesRateList rateList;

	private CollectionProtocol cp;

	public LabServicesRateList getRateList() {
		return rateList;
	}

	public void setRateList(LabServicesRateList rateList) {
		this.rateList = rateList;
	}

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}
}
