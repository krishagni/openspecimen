package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.List;

public class UpdateRateListServicesOp {
	public enum Op {
		UPSERT,

		DELETE
	}

	private Long rateListId;

	private Op op;

	private List<LabServiceRateDetail> serviceRates;

	public Long getRateListId() {
		return rateListId;
	}

	public void setRateListId(Long rateListId) {
		this.rateListId = rateListId;
	}

	public Op getOp() {
		return op;
	}

	public void setOp(Op op) {
		this.op = op;
	}

	public List<LabServiceRateDetail> getServiceRates() {
		return serviceRates;
	}

	public void setServiceRates(List<LabServiceRateDetail> serviceRates) {
		this.serviceRates = serviceRates;
	}
}
