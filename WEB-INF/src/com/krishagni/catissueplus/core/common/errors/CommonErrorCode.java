package com.krishagni.catissueplus.core.common.errors;

public enum CommonErrorCode implements ErrorCode {
	SERVER_ERROR,

	INVALID_INPUT,

	INVALID_REQUEST,

	SQL_EXCEPTION,

	DB_CONN_ERROR,

	FILE_NOT_FOUND,

	FILE_SEND_ERROR,

	EXCEPTION_NOT_FOUND,

	CUSTOM_FIELD_LEVEL_REQ,

	CUSTOM_FIELD_NAME_REQ,

	INVALID_TZ,

	FORM_ERROR,

	DATE_GT_TODAY,

	INTERVAL_EXCEEDS_ALLOWED,

	DATE_PARSE_ERROR,

	EVAL_EXPR_ERROR;

	@Override
	public String code() {
		return "COMMON_" + this.name();
	}
}
