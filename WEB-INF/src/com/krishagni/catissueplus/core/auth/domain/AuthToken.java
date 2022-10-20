package com.krishagni.catissueplus.core.auth.domain;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.util.AuthUtil;

public class AuthToken {
	private String token;
	
	private User user;
	
	private String ipAddress;
	
	private LoginAuditLog loginAuditLog;

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public LoginAuditLog getLoginAuditLog() {
		return loginAuditLog;
	}

	public void setLoginAuditLog(LoginAuditLog loginAuditLog) {
		this.loginAuditLog = loginAuditLog;
	}

	public String getEncodedToken() {
		return AuthUtil.encodeToken(token);
	}
}
