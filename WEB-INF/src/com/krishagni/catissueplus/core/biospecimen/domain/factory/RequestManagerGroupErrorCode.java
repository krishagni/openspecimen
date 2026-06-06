package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum RequestManagerGroupErrorCode implements ErrorCode {
	NOT_FOUND,

	NAME_REQ,

	DUP_NAME;

	@Override
	public String code() {
		return "RMG_" + this.name();
	}
}
