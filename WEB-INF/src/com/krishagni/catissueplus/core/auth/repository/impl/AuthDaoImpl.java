package com.krishagni.catissueplus.core.auth.repository.impl;

import java.util.Date;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.query.Query;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.domain.AuthCredential;
import com.krishagni.catissueplus.core.auth.repository.AuthDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class AuthDaoImpl extends AbstractDao<AuthDomain> implements AuthDao {

	@Override
	public Class<AuthDomain> getType() {
		return AuthDomain.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<AuthDomain> getAuthDomains(int maxResults) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_AUTH_DOMAINS)
				.setMaxResults(maxResults)
				.list();
	}
	
	@SuppressWarnings(value = {"unchecked"})
	@Override
	public AuthDomain getAuthDomainByName(String domainName) {
		List<AuthDomain> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_DOMAIN_BY_NAME)
				.setString("domainName", domainName)
				.list();
		
		return result.isEmpty() ? null : result.get(0);
	}
	
	@SuppressWarnings(value = {"unchecked"})
	@Override
	public AuthDomain getAuthDomainByType(String authType) {
		List<AuthDomain> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_DOMAIN_BY_TYPE)
				.setString("authType", authType)
				.list();

		return result.isEmpty() ? null : result.get(0);
	}

	@SuppressWarnings(value = {"unchecked"})
	@Override
	public Boolean isUniqueAuthDomainName(String domainName) {
		List<AuthDomain> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_DOMAIN_BY_NAME)
				.setString("domainName", domainName)
				.list();
		
		return result.isEmpty();
	}
	
	@SuppressWarnings(value = {"unchecked"})
	@Override
	public AuthProvider getAuthProviderByType(String authType) {
		List<AuthProvider> result = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_PROVIDER_BY_TYPE)
				.setString("authType", authType)
				.list();
		
		return result.isEmpty() ? null : result.get(0);
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public AuthToken getAuthTokenByKey(String key) {
		List<AuthToken> tokens = sessionFactory.getCurrentSession()
				.getNamedQuery(GET_AUTH_TOKEN_BY_KEY)
				.setString("token", key)
				.list();

		return tokens.isEmpty() ? null : tokens.get(0);
	}
	
	@Override
	public void saveAuthToken(AuthToken token) {
		sessionFactory.getCurrentSession().saveOrUpdate(token);
	}
	
	@Override
	public void deleteAuthToken(AuthToken token) {
		sessionFactory.getCurrentSession().delete(token);
	}

	@Override
	public void deleteAuthTokens(Long userId, String except) {
		String sql = DELETE_USER_AUTH_TOKENS_SQL;
		if (StringUtils.isNotBlank(except)) {
			sql += " and token != :exceptToken";
		}

		Query query = getCurrentSession().createSQLQuery(sql);
		query.setParameter("userId", userId);
		if (StringUtils.isNotBlank(except)) {
			query.setParameter("exceptToken", except);
		}

		query.executeUpdate();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<LoginAuditLog> getLoginAuditLogsByUser(Long userId, int maxResults) {
		return sessionFactory.getCurrentSession()
				.getNamedQuery(GET_LOGIN_AUDIT_LOGS_BY_USER_ID)
				.setLong("userId", userId)
				.setMaxResults(maxResults)
				.list();
	}
	
	@Override
	public void deleteInactiveAuthTokens(Date latestAccessTime) {
		getCurrentSession().getNamedQuery(DELETE_INACTIVE_AUTH_TOKENS)
			.setParameter("latestCallTime", latestAccessTime)
			.executeUpdate();
	}
	
	@Override
	public int deleteAuthTokensByUser(List<Long> userIds) {
		return getCurrentSession().getNamedQuery(DELETE_AUTH_TOKENS_BY_USER_ID)
			.setParameterList("ids", userIds)
			.executeUpdate();
	}
	
	@Override
	public void saveLoginAuditLog(LoginAuditLog log) {
		sessionFactory.getCurrentSession().saveOrUpdate(log);
	}

	@Override
	public void saveCredentials(AuthCredential credential) {
		getCurrentSession().saveOrUpdate(credential);
	}

	@Override
	public AuthCredential getCredentials(String token) {
		return (AuthCredential) getCurrentSession().getNamedQuery(GET_CREDENTIAL)
			.setParameter("token", token)
			.uniqueResult();
	}

	@Override
	public void deleteCredentials(String token) {
		getCurrentSession().getNamedQuery(DELETE_CREDENTIAL)
			.setParameter("token", token)
			.executeUpdate();
	}

	@Override
	public void deleteDanglingCredentials() {
		getCurrentSession().getNamedQuery(DELETE_DANGLING_CREDS).executeUpdate();
	}

	private static final String FQN = AuthDomain.class.getName();
	
	private static final String GET_AUTH_DOMAINS = FQN + ".getAuthDomains";

	private static final String GET_DOMAIN_BY_NAME = FQN + ".getDomainByName";

	private static final String GET_DOMAIN_BY_TYPE = FQN + ".getDomainByType";

	private static final String GET_PROVIDER_BY_TYPE = AuthProvider.class.getName() + ".getProviderByType";
	
	private static final String GET_AUTH_TOKEN_BY_KEY = AuthToken.class.getName() + ".getByKey";
	
	private static final String DELETE_INACTIVE_AUTH_TOKENS = AuthToken.class.getName() + ".deleteInactiveAuthTokens";

	private static final String GET_LOGIN_AUDIT_LOGS_BY_USER_ID = LoginAuditLog.class.getName() + ".getLogsByUserId";
	
	private static final String DELETE_AUTH_TOKENS_BY_USER_ID = AuthToken.class.getName() + ".deleteAuthTokensByUserId";

	private static final String GET_CREDENTIAL = AuthCredential.class.getName() + ".getByToken";

	private static final String DELETE_CREDENTIAL = AuthCredential.class.getName() + ".deleteByToken";

	private static final String DELETE_DANGLING_CREDS = AuthCredential.class.getName() + ".deleteDanglingCredentials";

	private static final String DELETE_USER_AUTH_TOKENS_SQL = "delete from os_auth_tokens where user_id = :userId";
}
