package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum LabServiceErrorCode implements ErrorCode {
	CODE_REQ,

	DESC_REQ,

	DUP_CODE,

	NOT_FOUND,

	IN_USE;

	@Override
	public String code() {
		return "LAB_SVC_" + name();
	}
}
