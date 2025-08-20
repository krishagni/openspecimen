package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class PickListSpecimensCriteria extends AbstractListCriteria<PickListSpecimensCriteria> {

	private Long cartId;

	private String cartName;

	private Long pickListId;

	private List<SiteCpPair> siteCps;

	private Boolean picked;

	private String container;

	@Override
	public PickListSpecimensCriteria self() {
		return this;
	}

	public PickListSpecimensCriteria cartId(Long cartId) {
		this.cartId = cartId;
		return self();
	}

	public Long cartId() {
		return this.cartId;
	}

	public PickListSpecimensCriteria cartName(String cartName) {
		this.cartName = cartName;
		return self();
	}

	public String cartName() {
		return this.cartName;
	}

	public PickListSpecimensCriteria pickListId(Long pickListId) {
		this.pickListId = pickListId;
		return self();
	}

	public Long pickListId() {
		return this.pickListId;
	}

	public PickListSpecimensCriteria siteCps(List<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public List<SiteCpPair> siteCps() {
		return siteCps;
	}

	public PickListSpecimensCriteria picked(Boolean picked) {
		this.picked = picked;
		return self();
	}

	public Boolean picked() {
		return this.picked;
	}

	public PickListSpecimensCriteria container(String container) {
		this.container = container;
		return self();
	}

	public String container() {
		return this.container;
	}
}
