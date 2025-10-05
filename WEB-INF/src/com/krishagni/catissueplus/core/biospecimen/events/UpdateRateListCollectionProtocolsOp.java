package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.ArrayList;
import java.util.List;

public class UpdateRateListCollectionProtocolsOp {
	public enum Op {
		ADD,

		RM
	};

	private Long rateListId;

	private Op op;

	private List<CollectionProtocolSummary> cps = new ArrayList<>();

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

	public List<CollectionProtocolSummary> getCps() {
		return cps;
	}

	public void setCps(List<CollectionProtocolSummary> cps) {
		this.cps = cps;
	}
}
