
package com.krishagni.catissueplus.core.auth.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.domain.AuthCredential;
import com.krishagni.catissueplus.core.auth.domain.LoginOtp;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface AuthDao extends Dao<AuthDomain> {

	List<AuthDomain> getAuthDomains(int maxResults);
	
	AuthDomain getAuthDomainByName(String domainName);

	//
	// Gets the first "configured/created" auth domain of the request type.
	//
	AuthDomain getAuthDomainByType(String authType);

	AuthDomain getLegacySamlAuthDomain();

	Boolean isUniqueAuthDomainName(String domainName);

	AuthProvider getAuthProviderByType(String authType);
	
	AuthToken getAuthTokenByKey(String key);

	List<AuthToken> getAuthTokensByKey(List<String> keys);
	
	void saveAuthToken(AuthToken token);
	
	void deleteInactiveAuthTokens(Date expiresOn);
	
	void deleteAuthToken(AuthToken token);

	int deleteAuthTokens(Long userId, String except);

	int deleteAuthTokens(List<String> tokens);

	// {last_active_time: token}
	List<Pair<String, Date>> getUserTokens(Long userId);

	List<LoginAuditLog> getLoginAuditLogsByUser(Long userId, int maxResults);
	
	void saveLoginAuditLog(LoginAuditLog log);
	
	int deleteAuthTokensByUser(List<Long> userIds);

	void saveCredentials(AuthCredential credential);

	AuthCredential getCredentials(String token);

	void deleteCredentials(String token);

	void deleteDanglingCredentials();

	// login OTPs
	LoginOtp getLoginOtp(String emailAddress, String otp);

	void saveOrUpdate(LoginOtp otp);

	void deleteLoginOtps(Long userId);

	void deleteLoginOtp(LoginOtp otp);

	void deleteExpiredLoginOtps();
}
