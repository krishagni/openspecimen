package com.krishagni.catissueplus.core.biospecimen.repository;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class PickListsCriteria extends AbstractListCriteria<PickListsCriteria> {
	private Long cartId;

	private String cartName;

	@Override
	public PickListsCriteria self() {
		return this;
	}

	public Long cartId() {
		return cartId;
	}

	public PickListsCriteria cartId(Long cartId) {
		this.cartId = cartId;
		return self();
	}

	public String cartName() {
		return cartName;
	}

	public PickListsCriteria cartName(String cartName) {
		this.cartName = cartName;
		return self();
	}
}
