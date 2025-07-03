package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ServiceRateErrorCode implements ErrorCode {
	ID_REQ,

	NOT_FOUND,

	START_DT_REQ,

	END_DT_LT_START_DT,

	REQ,

	INT_OVERLAP,

	SERVICE_CHG_NA;

	@Override
	public String code() {
		return "CP_SERVICE_RATE_" + name();
	}
}
