package com.krishagni.catissueplus.core.auth.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.auth.domain.AuthCredential;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.domain.LoginOtp;
import com.krishagni.catissueplus.core.auth.repository.AuthDao;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Query;

public class AuthDaoImpl extends AbstractDao<AuthDomain> implements AuthDao {

	@Override
	public Class<AuthDomain> getType() {
		return AuthDomain.class;
	}

	@Override
	public List<AuthDomain> getAuthDomains(int maxResults) {
		return createNamedQuery(GET_AUTH_DOMAINS, AuthDomain.class)
			.setMaxResults(maxResults)
			.list();
	}

	@Override
	public AuthDomain getAuthDomainByName(String domainName) {
		List<AuthDomain> result = createNamedQuery(GET_DOMAIN_BY_NAME, AuthDomain.class)
			.setParameter("domainName", domainName)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public AuthDomain getAuthDomainByType(String authType) {
		List<AuthDomain> result = createNamedQuery(GET_DOMAIN_BY_TYPE, AuthDomain.class)
			.setParameter("authType", authType)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public AuthDomain getLegacySamlAuthDomain() {
		Criteria<AuthDomain> query = createCriteria(AuthDomain.class, "d")
			.join("d.authProvider", "ap");
		return query.add(query.eq("ap.authType", "saml"))
			.add(query.eq("d.legacySaml", Boolean.TRUE))
			.uniqueResult();
	}

	@Override
	public Boolean isUniqueAuthDomainName(String domainName) {
		List<AuthDomain> result = createNamedQuery(GET_DOMAIN_BY_NAME, AuthDomain.class)
			.setParameter("domainName", domainName)
			.list();
		return result.isEmpty();
	}

	@Override
	public AuthProvider getAuthProviderByType(String authType) {
		List<AuthProvider> result = createNamedQuery(GET_PROVIDER_BY_TYPE, AuthProvider.class)
			.setParameter("authType", authType)
			.list();
		return result.isEmpty() ? null : result.get(0);
	}

	@Override
	public AuthToken getAuthTokenByKey(String key) {
		List<AuthToken> tokens = createNamedQuery(GET_AUTH_TOKEN_BY_KEY, AuthToken.class)
			.setParameter("token", key)
			.list();
		return tokens.isEmpty() ? null : tokens.get(0);
	}

	@Override
	public List<AuthToken> getAuthTokensByKey(List<String> keys) {
		Criteria<AuthToken> query = createCriteria(AuthToken.class, "t");
		return query.createAlias("t.loginAuditLog", "l")
			.add(query.in("t.token", keys))
			.list();
	}

	@Override
	public void saveAuthToken(AuthToken token) {
		getCurrentSession().saveOrUpdate(token);
	}
	
	@Override
	public void deleteAuthToken(AuthToken token) {
		super.delete(token);
	}

	@Override
	public int deleteAuthTokens(Long userId, String except) {
		String sql = DELETE_USER_AUTH_TOKENS_SQL;
		if (StringUtils.isNotBlank(except)) {
			sql += " and token != :exceptToken";
		}

		Query<?> query = createNativeQuery(sql);
		query.setParameter("userId", userId);
		if (StringUtils.isNotBlank(except)) {
			query.setParameter("exceptToken", except);
		}

		return query.executeUpdate();
	}

	@Override
	public int deleteAuthTokens(List<String> tokens) {
		return createNamedQuery(DELETE_AUTH_TOKENS)
			.setParameter("tokens", tokens)
			.executeUpdate();
	}

	@Override
	public List<Pair<String, Date>> getUserTokens(Long userId) {
		List<Object[]> rows = createNamedQuery(GET_USER_TOKENS, Object[].class)
			.setParameter("userId", userId)
			.list();

		List<Pair<String, Date>> tokens = new ArrayList<>();
		for (Object[] row : rows) {
			String token = (String) row[0];
			Date lastActiveTime = (Date) row[1];
			if (lastActiveTime == null) {
				lastActiveTime = Calendar.getInstance().getTime();
			}

			tokens.add(Pair.make(token, lastActiveTime));
		}

		return tokens.stream().sorted(Comparator.comparing(Pair::second)).collect(Collectors.toList());
	}

	@Override
	public List<LoginAuditLog> getLoginAuditLogsByUser(Long userId, int maxResults) {
		return createNamedQuery(GET_LOGIN_AUDIT_LOGS_BY_USER_ID, LoginAuditLog.class)
			.setParameter("userId", userId)
			.setMaxResults(maxResults)
			.list();
	}
	
	@Override
	public void deleteInactiveAuthTokens(Date latestAccessTime) {
		createNamedQuery(DELETE_INACTIVE_AUTH_TOKENS)
			.setParameter("latestCallTime", latestAccessTime)
			.executeUpdate();
	}
	
	@Override
	public int deleteAuthTokensByUser(List<Long> userIds) {
		return createNamedQuery(DELETE_AUTH_TOKENS_BY_USER_ID)
			.setParameterList("ids", userIds)
			.executeUpdate();
	}
	
	@Override
	public void saveLoginAuditLog(LoginAuditLog log) {
		getCurrentSession().saveOrUpdate(log);
	}

	@Override
	public void saveCredentials(AuthCredential credential) {
		getCurrentSession().saveOrUpdate(credential);
	}

	@Override
	public AuthCredential getCredentials(String token) {
		return createNamedQuery(GET_CREDENTIAL, AuthCredential.class)
			.setParameter("token", token)
			.uniqueResult();
	}

	@Override
	public void deleteCredentials(String token) {
		createNamedQuery(DELETE_CREDENTIAL)
			.setParameter("token", token)
			.executeUpdate();
	}

	@Override
	public void deleteDanglingCredentials() {
		createNamedQuery(DELETE_DANGLING_CREDS).executeUpdate();
	}

	@Override
	public LoginOtp getLoginOtp(String emailAddress, String otp) {
		Criteria<LoginOtp> query = createCriteria(LoginOtp.class, "loginOtp")
			.join("loginOtp.user", "user");
		return query.add(query.eq("loginOtp.otp", otp))
			.add(query.eq("user.emailAddress", emailAddress))
			.uniqueResult();
	}

	@Override
	public void saveOrUpdate(LoginOtp otp) {
		getCurrentSession().saveOrUpdate(otp);
	}

	@Override
	public void deleteLoginOtps(Long userId) {
		createNamedQuery(DELETE_USER_LOGIN_OTPS).setParameter("userId", userId).executeUpdate();
	}

	@Override
	public void deleteLoginOtp(LoginOtp otp) {
		getCurrentSession().delete(otp);
	}

	@Override
	public void deleteExpiredLoginOtps() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -5);
		createNamedQuery(DELETE_EXPIRED_USER_LOGIN_OTPS).setParameter("expiryTime", cal.getTime()).executeUpdate();
	}


	private static final String FQN = AuthDomain.class.getName();
	
	private static final String GET_AUTH_DOMAINS = FQN + ".getAuthDomains";

	private static final String GET_DOMAIN_BY_NAME = FQN + ".getDomainByName";

	private static final String GET_DOMAIN_BY_TYPE = FQN + ".getDomainByType";

	private static final String GET_PROVIDER_BY_TYPE = AuthProvider.class.getName() + ".getProviderByType";
	
	private static final String GET_AUTH_TOKEN_BY_KEY = AuthToken.class.getName() + ".getByKey";
	
	private static final String DELETE_INACTIVE_AUTH_TOKENS = AuthToken.class.getName() + ".deleteInactiveAuthTokens";

	private static final String GET_USER_TOKENS = AuthToken.class.getName() + ".getUserTokens";

	private static final String GET_LOGIN_AUDIT_LOGS_BY_USER_ID = LoginAuditLog.class.getName() + ".getLogsByUserId";
	
	private static final String DELETE_AUTH_TOKENS_BY_USER_ID = AuthToken.class.getName() + ".deleteAuthTokensByUserId";

	private static final String DELETE_AUTH_TOKENS = AuthToken.class.getName() + ".deleteAuthTokens";

	private static final String GET_CREDENTIAL = AuthCredential.class.getName() + ".getByToken";

	private static final String DELETE_CREDENTIAL = AuthCredential.class.getName() + ".deleteByToken";

	private static final String DELETE_DANGLING_CREDS = AuthCredential.class.getName() + ".deleteDanglingCredentials";

	private static final String DELETE_USER_AUTH_TOKENS_SQL = "delete from os_auth_tokens where user_id = :userId";

	private static final String DELETE_USER_LOGIN_OTPS = LoginOtp.class.getName() + ".deleteUserOtps";

	private static final String DELETE_EXPIRED_USER_LOGIN_OTPS = LoginOtp.class.getName() + ".deleteExpiredUserOtps";
}
