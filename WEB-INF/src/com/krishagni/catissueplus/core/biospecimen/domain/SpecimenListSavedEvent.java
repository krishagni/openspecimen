package com.krishagni.catissueplus.core.biospecimen.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class SpecimenListSavedEvent extends OpenSpecimenEvent<SpecimenList> {
	private int op; // 0: created, 1: updated, 2: deleted

	public SpecimenListSavedEvent(SpecimenList list, int op) {
		super(null, list);
		this.op = op;
	}

	public int getOp() {
		return op;
	}

	public void setOp(int op) {
		this.op = op;
	}
}
