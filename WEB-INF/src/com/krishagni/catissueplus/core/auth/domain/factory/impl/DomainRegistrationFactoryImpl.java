
package com.krishagni.catissueplus.core.auth.domain.factory.impl;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.factory.AuthProviderErrorCode;
import com.krishagni.catissueplus.core.auth.domain.factory.DomainRegistrationFactory;
import com.krishagni.catissueplus.core.auth.events.AuthDomainDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.errors.ActivityStatusErrorCode;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class DomainRegistrationFactoryImpl implements DomainRegistrationFactory {

	private static final LogUtil logger = LogUtil.getLogger(DomainRegistrationFactoryImpl.class);

	@Autowired
	private DaoFactory daoFactory;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	@Override
	public AuthDomain createDomain(AuthDomainDetail input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		AuthDomain domain = new AuthDomain();
		setDomainName(input, domain, ose);
		setAllowLogins(input, domain, ose);
		setAuthProvider(input, domain, ose);
		setActivityStatus(input, domain, ose);

		ose.checkAndThrow();
		return domain;
	}
	
	private void setDomainName(AuthDomainDetail input, AuthDomain domain, OpenSpecimenException ose) {
		String domainName = input.getName();
		if (StringUtils.isBlank(domainName)) {
			ose.addError(AuthProviderErrorCode.DOMAIN_NOT_SPECIFIED);
		}
		
		domain.setName(domainName);
	}

	private void setAllowLogins(AuthDomainDetail input, AuthDomain domain, OpenSpecimenException ose) {
		domain.setAllowLogins(input.isAllowLogins());
	}
	
	private void setAuthProvider(AuthDomainDetail detail, AuthDomain authDomain, OpenSpecimenException ose) {
		String authType = detail.getAuthType();
		if (StringUtils.isBlank(authType)) {
			ose.addError(AuthProviderErrorCode.TYPE_NOT_SPECIFIED);
			return;
		}

		authDomain.setAuthProvider(getNewAuthProvider(detail, ose));
	}

	private AuthProvider getNewAuthProvider(AuthDomainDetail detail, OpenSpecimenException ose) {
		String implClass = detail.getImplClass();
		if (StringUtils.isBlank(implClass)) {
			ose.addError(AuthProviderErrorCode.IMPL_NOT_SPECIFIED);
		}

		if (!isValidImplClass(implClass)) {
			ose.addError(AuthProviderErrorCode.INVALID_AUTH_IMPL, "Invalid class: " + implClass);
			return null;
		}
		
		AuthProvider authProvider = new AuthProvider();
		authProvider.setAuthType(detail.getAuthType());
		authProvider.setImplClass(implClass);
		authProvider.setProps(detail.getAuthProviderProps());
		return authProvider;
	}

	private boolean isValidImplClass(String implClass) {
		try {
			Class.forName(implClass).getDeclaredConstructor().newInstance();
			return true;
		} catch (Exception e) {
			logger.error("Error validating the  authentication provider: " + implClass, e);
			return false;
		}
	}

	private void setActivityStatus(AuthDomainDetail detail, AuthDomain authDomain, OpenSpecimenException ose) {
		if (StringUtils.isBlank(detail.getActivityStatus())) {
			authDomain.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
			return;
		}

		if (!Status.isValidActivityStatus(detail.getActivityStatus())) {
			ose.addError(ActivityStatusErrorCode.INVALID, detail.getActivityStatus());
		}

		authDomain.setActivityStatus(detail.getActivityStatus());
	}
}
