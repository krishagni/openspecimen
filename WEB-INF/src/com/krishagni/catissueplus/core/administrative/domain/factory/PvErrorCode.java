package com.krishagni.catissueplus.core.administrative.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum PvErrorCode implements ErrorCode {
	ATTR_NAME_REQUIRED,

	ATTR_CAPTION_REQUIRED,

	FORM_ID_REQUIRED,

	SOURCE_ATTR_REQUIRED,

	FORM_ATTR_REQUIRED,

	FORM_ATTR_FORM_MISMATCH,
	
	VALUE_REQUIRED,

	DUP_ATTR,

	DUP_VALUE,
	
	PARENT_ATTR_NOT_FOUND,
	
	NOT_FOUND,

	IN_USE;

	@Override
	public String code() {		
		return "PV_" + this.name();
	}	
}
