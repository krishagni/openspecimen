package com.krishagni.catissueplus.core.auth.domain;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;

public enum AuthErrorCode implements ErrorCode {
	INVALID_CREDENTIALS,
	
	USER_LOCKED,
	
	INVALID_TOKEN,
	
	TOKEN_EXPIRED,
	
	IP_ADDRESS_CHANGED,

	PASSWD_EXPIRED,

	SYSTEM_LOCKDOWN,

	NA_IP_ADDRESS,

	IMP_TOKEN_INV,

	IMP_TOKEN_EXP,

	OTP_EXPIRED,

	DOMAIN_LOGIN_DISABLED,

	JWT_PARSE_ERROR,

	JWT_UNKNOWN_ISSUER,

	JWT_EXPIRED,

	JWT_INV_AUD;

	@Override
	public String code() {
		return "AUTH_" + this.name();
	}

}
