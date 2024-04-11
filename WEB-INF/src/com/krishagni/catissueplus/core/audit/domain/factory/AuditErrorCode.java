package com.krishagni.catissueplus.core.audit.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum AuditErrorCode implements ErrorCode {

	ENTITY_NOT_FOUND,

	DATE_GT_TODAY,

	DATE_INTERVAL_GT_ALLOWED,

	FROM_DT_GT_TO_DATE,

	RECORDS_REQ,

	START_DATE_REQ,

	END_DATE_REQ;

	@Override
	public String code() {
		return "AUDIT_" + this.name();
	}
}
