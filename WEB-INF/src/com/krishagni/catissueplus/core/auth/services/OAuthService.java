package com.krishagni.catissueplus.core.auth.services;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.krishagni.catissueplus.core.administrative.domain.User;

public interface OAuthService {
	User resolveUser(String jwt);

	String generateAppToken(HttpServletRequest httpReq, HttpServletResponse httpResp, String jwt);

	String getAuthorizeUrl(Long providerId);

	String exchangeCodeForToken(HttpServletRequest httpReq, HttpServletResponse httpResp, String state, String code);
}
