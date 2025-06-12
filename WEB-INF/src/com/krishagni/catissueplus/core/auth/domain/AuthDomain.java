
package com.krishagni.catissueplus.core.auth.domain;

import java.util.HashMap;
import java.util.Map;


import org.apache.commons.lang3.exception.ExceptionUtils;

import com.krishagni.catissueplus.core.auth.domain.factory.AuthProviderErrorCode;
import com.krishagni.catissueplus.core.auth.services.AuthenticationService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class AuthDomain {
	private static final LogUtil logger = LogUtil.getLogger(AuthDomain.class);

	private static Map<Long, AuthenticationService> authProviderMap = new HashMap<>();

	private Long id;

	private String name;

	private AuthProvider authProvider;


	private boolean allowLogins = true;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setAuthProvider(AuthProvider authProvider) {
		this.authProvider = authProvider;
	}

	public AuthProvider getAuthProvider() {
		return authProvider;
	}

	public AuthenticationService getAuthProviderInstance() {
		return getAuthProviderInstance(getAuthProvider());
	}

	public boolean isAllowLogins() {
		return allowLogins;
	}

	public void setAllowLogins(boolean allowLogins) {
		this.allowLogins = allowLogins;
	}
	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public void update(AuthDomain domain) {
		setName(domain.getName());
		setAllowLogins(domain.isAllowLogins());

		Map<String, String> newProps = domain.getAuthProvider().getProps();
		Map<String, String> oldProps = authProvider.getProps();

		oldProps.clear();
		oldProps.putAll(newProps);

		if (Status.isDisabledStatus(domain.getActivityStatus())) {
			delete();
		}

		//
		// Removing updated domain's auth provider implementation instance from cached
		// instances so that new instance with new properties can be created
		//
		authProviderMap.remove(getAuthProvider().getId());

		//
		// re-init the cache
		//
		getAuthProviderInstance();
	}

	public void delete() {
		setName(Utility.getDisabledValue(getName(), 64));
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
		getAuthProvider().setAuthType(Utility.getDisabledValue(getAuthProvider().getAuthType(), 64));
		authProviderMap.remove(getAuthProvider().getId());
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private AuthenticationService getAuthProviderInstance(AuthProvider authProvider) {
		try {
			AuthenticationService authService = authProviderMap.get(authProvider.getId());
			if (authService == null) {
				Class authImplClass = (Class) Class.forName(authProvider.getImplClass());
				authService = (AuthenticationService) authImplClass
							.getConstructor(Map.class)
							.newInstance(authProvider.getProps());
				authProviderMap.put(authProvider.getId(), authService);
			}
			
			return authService;
		} catch (Exception e) {
			String cause = ExceptionUtils.getRootCauseMessage(e);
			logger.error("Error obtaining an instance of auth provider - " + cause, e);
			throw OpenSpecimenException.userError(AuthProviderErrorCode.INVALID_AUTH_IMPL, cause);
		}
	}
}
