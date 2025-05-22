package com.krishagni.catissueplus.core.common.errors;

public enum PrintRuleConfigErrorCode implements ErrorCode{
	NOT_FOUND,

	ID_REQ,

	OBJECT_TYPE_REQ,

	INVALID_OBJECT_TYPE,

	RULES_REQ,

	CMD_FILES_DIR_REQ,

	LABEL_TOKEN_NOT_FOUND,

	LABEL_TOKENS_REQ,

	INVALID_IP_RANGE,

	INVALID_CMD_FILE_FMT,

	INVALID_CMD_FILES_DIR,

	INVALID_USERS,

	INVALID_USER_GROUPS,

	DESC_REQ,

	INV_SORT_ORDER;

	@Override
	public String code() {
		return "PRINT_RULE_CFG_" + this.name();
	}
}
