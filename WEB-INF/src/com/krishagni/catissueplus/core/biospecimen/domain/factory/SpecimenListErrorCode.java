package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum SpecimenListErrorCode implements ErrorCode {
	NOT_FOUND,
	
	ACCESS_NOT_ALLOWED,
	
	NAME_REQUIRED,
	
	DUP_NAME,
	
	OWNER_REQUIRED,
	
	OWNER_NOT_FOUND,
	
	INVALID_SPECIMENS,
	
	INVALID_USERS_LIST,

	INVALID_GROUPS_LIST,

	EXCEEDS_REL_SORT_SIZE,

	INV_CART_SPECIMENS,

	PICK_LIST_ID_REQ,

	PICK_LIST_NAME_REQ,

	PICK_LIST_NOT_FOUND;

	@Override
	public String code() {
		return "SPECIMEN_LIST_" + this.name();
	}
}
