package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum LabServicesRateListErrorCode implements ErrorCode {
	ID_REQ,

	NAME_REQ,

	START_DT_REQ,

	END_DT_LT_START_DT,

	CURRENCY_REQ,

	CURRENCY_MAX_LEN,

	NOT_FOUND,

	SVC_RATE_REQ,

	INV_SVC_RATE,

	SVC_RATE_NF,

	SVC_CODE_REQ,

	ITEM_ERROR,

	SVCS_OVERLAP,

	UPDATE_NOT_ALLOWED,

	CP_NF_OR_UPDATE_NA;

	@Override
	public String code() {
		return "LAB_SVC_RL_" + name();
	}
}
