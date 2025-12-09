package com.krishagni.catissueplus.core.auth.domain.factory;

import com.krishagni.catissueplus.core.common.errors.ErrorCode;


public enum AuthProviderErrorCode implements ErrorCode {
	TYPE_NOT_SPECIFIED,
	
	INVALID_TYPE,
	
	LDAP_NOT_FOUND,
	
	NOT_FOUND,
	
	DOMAIN_NOT_FOUND,
	
	DOMAIN_NOT_SPECIFIED,
	
	DUP_DOMAIN_NAME,
	
	IMPL_NOT_SPECIFIED,
	
	INVALID_AUTH_IMPL,

	NOT_SAML,

	IDP_METADATA_NOT_SPECIFIED;
	
	@Override
	public String code() {
		return "AUTH_PROVIDER_" + this.name();
	}
}
