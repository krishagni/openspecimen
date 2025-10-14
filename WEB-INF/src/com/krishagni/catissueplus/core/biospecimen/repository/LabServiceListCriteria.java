package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class LabServiceListCriteria extends AbstractListCriteria<LabServiceListCriteria> {
	private List<String> codes;

	private Long cpId;

	private String cpShortTitle;

	private Long notInRateListId;

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

	public Long cpId() {
		return cpId;
	}

	public LabServiceListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public String cpShortTitle() {
		return cpShortTitle;
	}

	public LabServiceListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public Long notInRateListId() {
		return notInRateListId;
	}

	public LabServiceListCriteria notInRateListId(Long notInRateListId) {
		this.notInRateListId = notInRateListId;
		return self();
	}
}
