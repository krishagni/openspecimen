package com.krishagni.catissueplus.core.de.domain;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum FormErrorCode implements ErrorCode {
	NOT_FOUND,
	
	INVALID_REQ,
	
	NAME_REQUIRED,

	DUP_NAME,
	
	ENTITY_TYPE_REQUIRED,
	
	REC_NOT_FOUND,
	
	INVALID_DATA,
	
	FILE_NOT_FOUND,
	
	UPLOADED_FILE_NOT_FOUND,
	
	NO_ASSOCIATION,
			
	OP_NOT_ALLOWED,
	
	MULTIPLE_RECS_NOT_ALLOWED,
	
	INVALID_REC_ID,

	MULTI_RECS_ID_REQ,
	
	REC_ID_SPECIFIED_FOR_CREATE,
	
	SYS_FORM_UPDATE_NOT_ALLOWED,

	SYS_FORM_DEL_NOT_ALLOWED,

	SYS_REC_EDIT_NOT_ALLOWED,

	SYS_REC_DEL_NOT_ALLOWED,

	MULTIPLE_CTXS_NOT_ALLOWED,
	
	NOT_SELECT_CONTROL,

	INVALID_FORM_CTXT,

	CTXT_ID_REQ,

	OBJ_ID_REQ,

	INVALID_TOKEN,

	INV_DATA_STATUS,

	TOO_MANY_FIELDS;

	@Override
	public String code() {
		return "FORMS_" + this.name();
	}

}
