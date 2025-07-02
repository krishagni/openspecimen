package com.krishagni.catissueplus.core.biospecimen.domain.factory.impl;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ServiceErrorCode implements ErrorCode {
	CODE_REQ,

	DESC_REQ,

	NOT_FOUND,

	CP_CHG_NA,

	DUP_CODE,

	IN_USE;

	@Override
	public String code() {
		return "CP_SERVICE_" + name();
	}
}
