
package com.krishagni.catissueplus.core.auth.events;

import java.util.HashMap;
import java.util.Map;

public class LoginDetail {
	private String emailAddress;

	private String loginName;

	private String password;

	private String domainName;
	
	private String ipAddress;
	
	private boolean doNotGenerateToken;
	
	private String apiUrl;
	
	private String requestMethod;

	private String deviceDetails;
	
	private Map<String, String> props = new HashMap<>();

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getLoginName() {
		return loginName;
	}

	public void setLoginName(String loginName) {
		this.loginName = loginName;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public boolean isDoNotGenerateToken() {
		return doNotGenerateToken;
	}

	public void setDoNotGenerateToken(boolean doNotGenerateToken) {
		this.doNotGenerateToken = doNotGenerateToken;
	}

	public String getApiUrl() {
		return apiUrl;
	}

	public void setApiUrl(String apiUrl) {
		this.apiUrl = apiUrl;
	}

	public String getRequestMethod() {
		return requestMethod;
	}

	public void setRequestMethod(String requestMethod) {
		this.requestMethod = requestMethod;
	}

	public String getDeviceDetails() {
		return deviceDetails;
	}

	public void setDeviceDetails(String deviceDetails) {
		this.deviceDetails = deviceDetails;
	}

	public Map<String, String> getProps() {
		return props;
	}

	public void setProps(Map<String, String> props) {
		this.props = props;
	}
}
