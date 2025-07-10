package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum ServiceErrorCode implements ErrorCode {
	CODE_REQ,

	DESC_REQ,

	NOT_FOUND,

	CP_CHG_NA,

	DUP_CODE,

	IN_USE,

	RPT_ST_DT_REQ,

	RPT_INT_EXCEEDS_3M,

	RPT_ED_LT_ST_DT;

	@Override
	public String code() {
		return "CP_SERVICE_" + name();
	}
}
