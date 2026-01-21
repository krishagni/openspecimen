package com.krishagni.catissueplus.core.auth.services.impl;

import java.util.Map;

import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.services.AuthenticationService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class NoAuthenticationServiceImpl implements AuthenticationService {

	public NoAuthenticationServiceImpl() {

	}

	public NoAuthenticationServiceImpl(Map<String, String> props) {

	}

	@Override
	public void authenticate(LoginDetail loginDetail) {
		throw OpenSpecimenException.serverError(new UnsupportedOperationException("Not supported for this implementation"));
	}
}
