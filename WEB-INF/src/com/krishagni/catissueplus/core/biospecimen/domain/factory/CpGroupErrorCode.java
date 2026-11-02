package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum CpGroupErrorCode implements ErrorCode {
	NOT_FOUND,

	NAME_REQ,

	DUP_NAME,

	CP_REQ,

	CP_NOT_FOUND,

	CP_IN_OTH_GRPS,

	DUP_FORM;

	@Override
	public String code() {
		return "CP_GROUP_" + name();
	}
}
