package com.krishagni.catissueplus.core.auth.domain;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class AuthDomainSavedEvent extends OpenSpecimenEvent<AuthDomain> {

	public AuthDomainSavedEvent(AuthDomain domain) {
		super(null, domain);
	}
}
