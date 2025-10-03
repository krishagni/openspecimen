package com.krishagni.catissueplus.core.biospecimen.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum LabServicesRateListErrorCode implements ErrorCode {
	NAME_REQ,

	START_DT_REQ,

	END_DT_LT_START_DT;

	@Override
	public String code() {
		return "LAB_SVC_RL_" + name();
	}
}
