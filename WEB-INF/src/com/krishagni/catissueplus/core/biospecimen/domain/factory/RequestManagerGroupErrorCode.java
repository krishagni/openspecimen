package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum RequestManagerGroupErrorCode implements ErrorCode {
	NOT_FOUND;

	@Override
	public String code() {
		return "REQUEST_MANAGER_GROUP_" + this.name();
	}
}
