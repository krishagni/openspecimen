package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.List;

public class UpdateFolderCartsOp {
	public enum Operation {
		ADD,
		REMOVE
	};

	private Long folderId;

	private List<Long> cartIds;

	private Operation op;

	public Long getFolderId() {
		return folderId;
	}

	public void setFolderId(Long folderId) {
		this.folderId = folderId;
	}

	public List<Long> getCartIds() {
		return cartIds;
	}

	public void setCartIds(List<Long> cartIds) {
		this.cartIds = cartIds;
	}

	public Operation getOp() {
		return op;
	}

	public void setOp(Operation op) {
		this.op = op;
	}
}
