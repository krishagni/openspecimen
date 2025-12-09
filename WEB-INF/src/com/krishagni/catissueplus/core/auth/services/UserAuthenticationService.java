package com.krishagni.catissueplus.core.auth.services;

import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public interface UserAuthenticationService {	
	ResponseEvent<Map<String, Object>> authenticateUser(RequestEvent<LoginDetail> req);
	
	ResponseEvent<AuthToken> validateToken(RequestEvent<TokenDetail> req);
	
	ResponseEvent<UserSummary> getCurrentLoggedInUser();
	
	ResponseEvent<String> removeToken(RequestEvent<String> req);

	User getUser(String domainName, String loginName);

	User getUser(Long userId);

	ResponseEvent<AuthToken> impersonate(RequestEvent<LoginDetail> req);

	ResponseEvent<Boolean> generateOtp(RequestEvent<LoginDetail> req);

	ResponseEvent<Map<String, Object>> verifyOtp(RequestEvent<LoginDetail> req);

	String generateToken(User user, LoginDetail loginDetail);
}