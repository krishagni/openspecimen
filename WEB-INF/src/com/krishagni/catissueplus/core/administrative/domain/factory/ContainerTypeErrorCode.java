
package com.krishagni.catissueplus.core.administrative.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ContainerTypeErrorCode implements ErrorCode {
	NOT_FOUND,
	
	NAME_REQUIRED,
	
	ID_OR_NAME_REQ,
	
	DUP_NAME,
	
	INVALID_CAPACITY,

	INVALID_POSITION_LABELING_MODE,

	INVALID_POSITION_ASSIGNMENT,
	
	INVALID_LABELING_SCHEME, 
	
	REF_ENTITY_FOUND,

	CYCLES_NOT_ALLOWED,

	NAME_FORMAT_REQUIRED,

	CANNOT_HOLD_CONTAINER,

	INVALID_NAME_FORMAT,

	EXCEED_CAPACITY_LMT;

	@Override
	public String code() {
		return "CONTAINER_TYPE_" + this.name();
	}
}
