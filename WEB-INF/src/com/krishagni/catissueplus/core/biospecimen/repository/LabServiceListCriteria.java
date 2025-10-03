package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class LabServiceListCriteria extends AbstractListCriteria<LabServiceListCriteria> {
	private List<String> codes;

	@Override
	public LabServiceListCriteria self() {
		return this;
	}

	public List<String> codes() {
		return codes;
	}

	public LabServiceListCriteria codes(List<String> codes) {
		this.codes = codes;
		return self();
	}
}
