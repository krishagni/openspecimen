package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.List;

public class PickListSpecimensOp {
	public enum Op {
		PICK,
		UNPICK
	}

	private Long cartId;

	private String cartName;

	private Long pickListId;

	private Op op;

	private List<SpecimenInfo> specimens;

	private String boxName;

	public Long getCartId() {
		return cartId;
	}

	public void setCartId(Long cartId) {
		this.cartId = cartId;
	}

	public String getCartName() {
		return cartName;
	}

	public void setCartName(String cartName) {
		this.cartName = cartName;
	}

	public Long getPickListId() {
		return pickListId;
	}

	public void setPickListId(Long pickListId) {
		this.pickListId = pickListId;
	}

	public Op getOp() {
		return op;
	}

	public void setOp(Op op) {
		this.op = op;
	}

	public List<SpecimenInfo> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(List<SpecimenInfo> specimens) {
		this.specimens = specimens;
	}

	public String getBoxName() {
		return boxName;
	}

	public void setBoxName(String boxName) {
		this.boxName = boxName;
	}
}
