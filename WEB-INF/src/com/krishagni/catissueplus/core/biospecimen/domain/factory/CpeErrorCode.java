package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum CpeErrorCode implements ErrorCode {
	NOT_FOUND,
	
	DUP_LABEL,
	
	DUP_CODE,
	
	LABEL_REQUIRED,
	
	INVALID_POINT,

	POINT_UNIT_REQUIRED,

	INVALID_POINT_UNIT,

	INVALID_CLINICAL_DIAGNOSIS,
	
	INVALID_CLINICAL_STATUS,	
	
	CP_REQUIRED,

	IDS_OR_LABELS_REQUIRED,

	CLOSED,

	HAS_VISITS;

	@Override
	public String code() {
		return "CPE_" + this.name();
	}

}
