
package com.krishagni.catissueplus.core.auth.services.impl;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.krishagni.catissueplus.core.administrative.domain.ForgotPasswordToken;
import com.krishagni.catissueplus.core.administrative.domain.Password;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.UserEvent;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.auth.AuthConfig;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.domain.LoginOtp;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.auth.services.AuthenticationService;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConcurrentLruCache;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class UserAuthenticationServiceImpl implements UserAuthenticationService {
	private static final String ACCOUNT_LOCKED_NOTIF_TMPL = "account_locked_notification";

	private static final String FAILED_LOGIN_USER_NOTIF_TMPL = "failed_login_user";

	private static final String FAILED_LOGIN_USER_ADMIN_TMPL = "failed_login_admins";

	private static final String IMPERSONATE_USER_TMPL = "impersonate_user";

	private static final String KILLED_SESSIONS_TMPL = "killed_sessions_notif";

	private static final String USER_OTP_TMPL = "user_login_otp";

	private static final long ONE_HOUR_IN_MS = 60 * 60 * 1000;

	private DaoFactory daoFactory;

	private com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory;
	
	private AuditService auditService;

	private ConcurrentLruCache<String, Date> tokenAccessTimes = new ConcurrentLruCache<>(100);

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setDeDaoFactory(com.krishagni.catissueplus.core.de.repository.DaoFactory deDaoFactory) {
		this.deDaoFactory = deDaoFactory;
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> authenticateUser(RequestEvent<LoginDetail> req) {
		LoginDetail loginDetail = req.getPayload();
		User user = null;
		try {
			user = daoFactory.getUserDao().getUser(loginDetail.getLoginName(), loginDetail.getDomainName());
			if (user == null) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}

			if (!user.getAuthDomain().isAllowLogins()) {
				return ResponseEvent.userError(AuthErrorCode.DOMAIN_LOGIN_DISABLED, user.getAuthDomain().getName());
			}

			if (!user.isAllowedAccessFrom(loginDetail.getIpAddress())) {
				throw OpenSpecimenException.userError(AuthErrorCode.NA_IP_ADDRESS, loginDetail.getIpAddress());
			}

			if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			}

			if (user.getActivityStatus().equals(Status.ACTIVITY_STATUS_LOCKED.getStatus())) {
				throw OpenSpecimenException.userError(AuthErrorCode.USER_LOCKED, loginDetail.getLoginName());
			}
			
			if (user.getActivityStatus().equals(Status.ACTIVITY_STATUS_EXPIRED.getStatus())) {
				throw OpenSpecimenException.userError(AuthErrorCode.PASSWD_EXPIRED);
			}
			
			if (!user.isEnabled()) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}
			
			AuthenticationService authService = user.getAuthDomain().getAuthProviderInstance();
			authService.authenticate(loginDetail);

			//
			// publish auth data for anyone to do extra checks
			//
			Map<String, Object> authEventData = new HashMap<>();
			authEventData.put("loginDetail", loginDetail);
			authEventData.put("user", user);
			EventPublisher.getInstance().publish(UserEvent.XTRA_AUTH, authEventData);

			if (user.isOpenSpecimenUser() && user.isForcePasswordReset()) {
				//
				// OpenSpecimen domain user
				// doing sign-in for the first time
				// force him/her to reset the password
				//
				ForgotPasswordToken fpToken = daoFactory.getUserDao().getFpTokenByUser(user.getId());
				if (fpToken != null) {
					daoFactory.getUserDao().deleteFpToken(fpToken);
				}

				fpToken = new ForgotPasswordToken(user);
				daoFactory.getUserDao().saveFpToken(fpToken);
				return ResponseEvent.response(Collections.singletonMap("resetPasswordToken", fpToken.getToken()));
			}

			Map<String, Object> authDetail = new HashMap<>();
			authDetail.put("user", user);

			AuthToken token = createToken(user, loginDetail, null);
			authDetail.put("token", AuthUtil.encodeToken(token.getToken()));
			authDetail.put("tokenObj", token);

			return ResponseEvent.response(authDetail);
		} catch (OpenSpecimenException ose) {
			if (user != null && user.isEnabled()) {
				insertLoginAudit(user, loginDetail.getIpAddress(), null, false);
				checkFailedLoginAttempt(user);
				if (ose.containsError(AuthErrorCode.INVALID_CREDENTIALS)) {
					notifyFailedLogin(loginDetail, user);
				}
			}
			return ResponseEvent.error(ose, true);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@PlusTransactional
	public ResponseEvent<AuthToken> validateToken(RequestEvent<TokenDetail> req) {
		try {
			TokenDetail tokenDetail = req.getPayload();
			String token = AuthUtil.decodeToken(tokenDetail.getToken());

			AuthToken authToken = daoFactory.getAuthDao().getAuthTokenByKey(token);
			if (authToken == null) {
				throw OpenSpecimenException.userError(AuthErrorCode.INVALID_TOKEN);
			}

			User user = authToken.getUser();
			if (!user.isAllowedAccessFrom(tokenDetail.getIpAddress())) {
				throw OpenSpecimenException.userError(AuthErrorCode.NA_IP_ADDRESS, tokenDetail.getIpAddress());
			}

			if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			}

			long now = System.currentTimeMillis();
			LoginAuditLog loginAuditLog = authToken.getLoginAuditLog();
			if (loginAuditLog.getImpersonatedBy() != null) {
				long endTime = loginAuditLog.getLoginTime().getTime() + ONE_HOUR_IN_MS;
				if (endTime < now) {
					throw OpenSpecimenException.userError(AuthErrorCode.IMP_TOKEN_EXP);
				}
			}

			boolean accessTimeFromDb = false;
			Date lastAccess = tokenAccessTimes.get(token);
			if (lastAccess == null) {
				lastAccess = daoFactory.getAuditDao().getLatestApiCallTime(user.getId(), token);
				tokenAccessTimes.put(token, lastAccess);
				accessTimeFromDb = true;
			}

			long timeSinceLastApiCall = TimeUnit.MILLISECONDS.toMinutes(now - lastAccess.getTime());
			int tokenInactiveInterval = AuthConfig.getInstance().getTokenInactiveIntervalInMinutes();
			if (timeSinceLastApiCall > tokenInactiveInterval) {
				boolean expired = true;
				if (!accessTimeFromDb) {
					lastAccess = daoFactory.getAuditDao().getLatestApiCallTime(user.getId(), token);
					tokenAccessTimes.put(token, lastAccess);

					timeSinceLastApiCall = TimeUnit.MILLISECONDS.toMinutes(now - lastAccess.getTime());
					expired = timeSinceLastApiCall > tokenInactiveInterval;
				}

				if (expired) {
					daoFactory.getAuthDao().deleteAuthToken(authToken);
					tokenAccessTimes.remove(token);
					throw OpenSpecimenException.userError(AuthErrorCode.TOKEN_EXPIRED);
				}
			}

			if (AuthConfig.getInstance().isTokenIpVerified()) {
				if (!tokenDetail.getIpAddress().equals(authToken.getIpAddress())) {
					throw OpenSpecimenException.userError(AuthErrorCode.IP_ADDRESS_CHANGED);
				}
			}

			if (!Hibernate.isInitialized(user)) {
				Hibernate.initialize(user);
			}
			
			return ResponseEvent.response(authToken);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	public Date touchToken(String token, Date lastAccess) {
		if (StringUtils.isBlank(token)) {
			return null;
		}

		return tokenAccessTimes.touch(AuthUtil.decodeToken(token), lastAccess);
	}

	@PlusTransactional
	public ResponseEvent<UserSummary> getCurrentLoggedInUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		User user = (User)auth.getPrincipal();

		UserSummary result = UserSummary.from(user);
		result.setDaysBeforePasswordExpiry(-1);
		if (user.isOpenSpecimenUser()) {
			Password password = daoFactory.getUserDao().getLatestPassword(user.getId());
			result.setDaysBeforePasswordExpiry(password != null ? password.daysBeforeExpiry() : 0);
		}

		return ResponseEvent.response(result);
	}

	@PlusTransactional
	public ResponseEvent<String> removeToken(RequestEvent<String> req) {
		String userToken = AuthUtil.decodeToken(req.getPayload());
		try {
			AuthToken token = daoFactory.getAuthDao().getAuthTokenByKey(userToken);
			LoginAuditLog loginAuditLog = token.getLoginAuditLog();
			loginAuditLog.setLogoutTime(Calendar.getInstance().getTime());
			
			daoFactory.getAuthDao().deleteAuthToken(token);
			daoFactory.getAuthDao().deleteCredentials(token.getToken());
			tokenAccessTimes.remove(userToken);
			return ResponseEvent.response("Success");
		} catch (Exception e) {	
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public User getUser(String domainName, String loginName) {
		return daoFactory.getUserDao().getUser(loginName, domainName);
	}

	@Override
	@PlusTransactional
	public User getUser(Long userId) {
		return daoFactory.getUserDao().getById(userId);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<AuthToken> impersonate(RequestEvent<LoginDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();

			LoginDetail input = req.getPayload();
			String domain = input.getDomainName();
			if (StringUtils.isBlank(domain)) {
				domain = User.DEFAULT_AUTH_DOMAIN;
			}

			if (StringUtils.isBlank(input.getLoginName())) {
				return ResponseEvent.userError(UserErrorCode.LOGIN_NAME_REQUIRED);
			}

			User user = daoFactory.getUserDao().getUser(input.getLoginName(), domain);
			if (user == null) {
				return ResponseEvent.userError(UserErrorCode.NOT_FOUND, domain + "/" + input.getLoginName());
			}

			Date startTime = Calendar.getInstance().getTime();
			Date endTime = new Date(startTime.getTime() + 60 * 60 * 1000);

			AuthToken token = createToken(user, input, AuthUtil.getCurrentUser());
			Map<String, Object> props = new HashMap<>();
			props.put("$subject", new String[] { AuthUtil.getCurrentUser().formattedName() });
			props.put("user", AuthUtil.getCurrentUser());
			props.put("impUser", user);
			props.put("impStartTime", Utility.getDateTimeString(startTime));
			props.put("impEndTime", Utility.getDateTimeString(endTime));
			props.put("loginIpAddress", AuthUtil.getRemoteAddr());
			EmailUtil.getInstance().sendEmail(
				IMPERSONATE_USER_TMPL,
				new String[] { user.getEmailAddress(), AuthUtil.getCurrentUser().getEmailAddress() },
				null,
				props
			);

			return ResponseEvent.response(token);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> generateOtp(RequestEvent<LoginDetail> req) {
		try {
			LoginDetail loginDetail = req.getPayload();
			if (StringUtils.isBlank(loginDetail.getEmailAddress())) {
				return ResponseEvent.userError(UserErrorCode.EMAIL_REQUIRED);
			}

			User user = daoFactory.getUserDao().getUserByEmailAddress(loginDetail.getEmailAddress());
			if (user == null) {
				return ResponseEvent.response(true);
			}

			if (!user.isAllowedAccessFrom(loginDetail.getIpAddress())) {
				throw OpenSpecimenException.userError(AuthErrorCode.NA_IP_ADDRESS, loginDetail.getIpAddress());
			}

			if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			}

			if (user.getActivityStatus().equals(Status.ACTIVITY_STATUS_LOCKED.getStatus())) {
				throw OpenSpecimenException.userError(AuthErrorCode.USER_LOCKED, loginDetail.getEmailAddress());
			}

			daoFactory.getAuthDao().deleteLoginOtps(user.getId());

			LoginOtp loginOtp = new LoginOtp();
			loginOtp.setUser(user);
			loginOtp.setTime(Calendar.getInstance().getTime());
			loginOtp.setIpAddress(loginDetail.getIpAddress());
			loginOtp.setOtp(generateOtp());
			daoFactory.getAuthDao().saveOrUpdate(loginOtp);

			Map<String, Object> props = new HashMap<>();
			props.put("user", loginOtp.getUser());
			props.put("otp", loginOtp);
			props.put("ccAdmin", false);
			EmailUtil.getInstance().sendEmail(
				USER_OTP_TMPL,
				new String[] { user.getEmailAddress() },
				null,
				props);
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Object>> verifyOtp(RequestEvent<LoginDetail> req) {
		try {
			LoginDetail loginDetail = req.getPayload();
			Map<String, String> props = loginDetail.getProps();
			if (props == null) {
				props = Collections.emptyMap();
			}

			String otp = props.get("otp");
			if (StringUtils.isBlank(otp)) {
				return ResponseEvent.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}

			LoginOtp loginOtp = daoFactory.getAuthDao().getLoginOtp(loginDetail.getEmailAddress(), otp);
			if (loginOtp == null) {
				return ResponseEvent.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}

			if (!StringUtils.equals(loginOtp.getIpAddress(), loginDetail.getIpAddress())) {
				return ResponseEvent.userError(AuthErrorCode.INVALID_CREDENTIALS);
			}

			Calendar cal = Calendar.getInstance();
			cal.add(Calendar.MINUTE, -5);
			if (cal.getTime().after(loginOtp.getTime())) {
				daoFactory.getAuthDao().deleteLoginOtp(loginOtp);
				return ResponseEvent.userError(AuthErrorCode.OTP_EXPIRED);
			}

			//
			// touch to initialise the user object proxy
			//
			loginOtp.getUser().getFirstName();

			Map<String, Object> authDetail = new HashMap<>();
			authDetail.put("user", loginOtp.getUser());

			AuthToken token = createToken(loginOtp.getUser(), loginDetail, null);
			authDetail.put("token", AuthUtil.encodeToken(token.getToken()));
			authDetail.put("tokenObj", token);
			daoFactory.getAuthDao().deleteLoginOtp(loginOtp);
			return ResponseEvent.response(authDetail);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Scheduled(cron="0 0 1 ? * *")
	@PlusTransactional
	public void deleteInactiveAuthTokens() {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -AuthConfig.getInstance().getTokenInactiveIntervalInMinutes());
		daoFactory.getAuthDao().deleteInactiveAuthTokens(cal.getTime());
		daoFactory.getAuthDao().deleteDanglingCredentials();
		daoFactory.getAuthDao().deleteExpiredLoginOtps(); // deletes all login OTPs that are older than 5 minutes

		Date cutOffTime = cal.getTime();
		tokenAccessTimes.removeIf(d -> d.before(cutOffTime));
	}
	
	public String generateToken(User user, LoginDetail loginDetail) {
		AuthToken token = createToken(user, loginDetail, null);
		return token != null ? AuthUtil.encodeToken(token.getToken()) : null;
	}

	private AuthToken createToken(User user, LoginDetail loginDetail, User impersonatedBy) {
		int maxSessions = AuthConfig.getInstance().maxConcurrentLoginSessions();
		if (maxSessions > 0) {
			deleteOlderSessions(user, maxSessions);
		}

		LoginAuditLog loginAuditLog = insertLoginAudit(user, loginDetail.getIpAddress(), impersonatedBy, true);

		AuthToken authToken = new AuthToken();
		authToken.setIpAddress(loginDetail.getIpAddress());
		authToken.setUser(user);
		authToken.setLoginAuditLog(loginAuditLog);

		if (loginDetail.isDoNotGenerateToken()) {
			return authToken;
		}

		String token = UUID.randomUUID().toString();
		authToken.setToken(token);
		daoFactory.getAuthDao().saveAuthToken(authToken);
		insertApiCallLog(loginDetail, user, loginAuditLog);
		return authToken;
	}

	private int deleteOlderSessions(User user, int maxSessions) {
		List<Pair<String, Date>> tokens = daoFactory.getAuthDao().getUserTokens(user.getId());

		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.MINUTE, -AuthConfig.getInstance().getTokenInactiveIntervalInMinutes());
		Date cutOffTime = cal.getTime();
		List<String> toDelete = new ArrayList<>();
		for (Iterator<Pair<String, Date>> iter = tokens.iterator(); iter.hasNext(); ) {
			Pair<String, Date> token = iter.next();
			if (token.second().before(cutOffTime)) {
				toDelete.add(token.first());
				iter.remove();
			}
		}

		Iterator<Pair<String, Date>> iter = tokens.iterator();
		List<String> killedTokens = new ArrayList<>();
		int killCount = tokens.size() - maxSessions;
		for (int i = 0; i <= killCount; ++i) {
			if (iter.hasNext()) {
				String token = iter.next().first();
				killedTokens.add(token);
				toDelete.add(token);
			}
		}

		int deleted = 0;
		List<AuthToken> killedSessions = null;
		if (!toDelete.isEmpty()) {
			if (!killedTokens.isEmpty()) {
				killedSessions = daoFactory.getAuthDao().getAuthTokensByKey(killedTokens);
			}

			deleted = daoFactory.getAuthDao().deleteAuthTokens(toDelete);
		}

		if (!killedTokens.isEmpty()) {
			Map<String, Object> props = new HashMap<>();
			props.put("$subject", new Object[] { killedTokens.size(), killedSessions.size() == 1 ? 1 : 2 });
			props.put("killedSessions", killedSessions);
			props.put("rcpt", user);
			props.put("maxLoginSessions", maxSessions);
			EmailUtil.getInstance().sendEmail(KILLED_SESSIONS_TMPL, new String[] { user.getEmailAddress() }, null, props);
		}

		toDelete.forEach(token -> tokenAccessTimes.remove(token));
		return deleted;
	}

	private LoginAuditLog insertLoginAudit(User user, String ipAddress, User impersonatedBy, boolean loginSuccessful) {
		LoginAuditLog loginAuditLog = new LoginAuditLog();
		loginAuditLog.setUser(user);
		loginAuditLog.setImpersonatedBy(impersonatedBy);
		loginAuditLog.setIpAddress(ipAddress);
		loginAuditLog.setLoginTime(Calendar.getInstance().getTime());
		loginAuditLog.setLogoutTime(null);
		loginAuditLog.setLoginSuccessful(loginSuccessful); 
		
		daoFactory.getAuthDao().saveLoginAuditLog(loginAuditLog);
		return loginAuditLog;
	}
	
	private void checkFailedLoginAttempt(User user) {
		int failedLoginAttempts = AuthConfig.getInstance().getAllowedFailedLoginAttempts();
		List<LoginAuditLog> logs = daoFactory.getAuthDao()
			.getLoginAuditLogsByUser(user.getId(), failedLoginAttempts);
		
		if (logs.size() < failedLoginAttempts) {
			return;
		}
		
		for (LoginAuditLog log: logs) {
			if (log.isLoginSuccessful()) {
				return;
			}
		}
		
		user.setActivityStatus(Status.ACTIVITY_STATUS_LOCKED.getStatus());
		notifyUserAccountLocked(user, failedLoginAttempts);
	}

	private void notifyFailedLogin(LoginDetail loginDetail, User user) {
		if (!AuthConfig.getInstance().isFailedLoginNotifEnabled()) {
			return;
		}

		String[] subjParams = { user.formattedName(), user.getLoginName() };
		Map<String, Object> props = new HashMap<>();
		props.put("user", user);
		props.put("$subject", subjParams);
		props.put("ccAdmin", false);
		props.put("ignoreDnd", true);
		props.put("ipAddress", loginDetail.getIpAddress());
		props.put("dateTime", Utility.getDateTimeString(Calendar.getInstance().getTime()));
		EmailUtil.getInstance().sendEmail(FAILED_LOGIN_USER_NOTIF_TMPL, new String[] { user.getEmailAddress() }, null, props);

		List<User> admins = daoFactory.getUserDao().getSuperAndInstituteAdmins(user.getInstitute().getName());
		for (User admin : admins) {
			props.put("admin", admin);
			EmailUtil.getInstance().sendEmail(FAILED_LOGIN_USER_ADMIN_TMPL, new String[] { admin.getEmailAddress() }, null, props);
		}

		Notification notif = new Notification();
		notif.setEntityId(user.getId());
		notif.setEntityType(User.getEntityName());
		notif.setCreatedBy(daoFactory.getUserDao().getSystemUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setOperation("UPDATE");
		notif.setMessage(MessageUtil.getInstance().getMessage(FAILED_LOGIN_USER_ADMIN_TMPL + "_subj", subjParams));
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("user-overview", admins));
	}
	
	private void insertApiCallLog(LoginDetail loginDetail, User user, LoginAuditLog loginAuditLog) {
		UserApiCallLog userAuditLog = new UserApiCallLog();
		userAuditLog.setUser(user);
		userAuditLog.setUrl(loginDetail.getApiUrl());
		userAuditLog.setMethod(loginDetail.getRequestMethod());
		userAuditLog.setResponseCode(Integer.toString(HttpStatus.OK.value()));
		userAuditLog.setCallStartTime(Calendar.getInstance().getTime());
		userAuditLog.setCallEndTime(Calendar.getInstance().getTime());
		userAuditLog.setLoginAuditLog(loginAuditLog);
		auditService.saveOrUpdateApiLog(userAuditLog);
	}

	private boolean isSystemLockedDown() {
		return ConfigUtil.getInstance().getBoolSetting("administrative", "system_lockdown", false);
	}

	private void notifyUserAccountLocked(User lockedUser, int failedLoginAttempts) {
		String[] subjParams = {lockedUser.getFirstName(), lockedUser.getLastName()};

		Map<String, Object> emailProps = new HashMap<>();
		emailProps.put("lockedUser", lockedUser);
		emailProps.put("failedLoginAttempts", failedLoginAttempts);
		emailProps.put("$subject", subjParams);
		emailProps.put("ccAdmin", false);
		emailProps.put("ignoreDnd", true);

		List<User> rcpts = daoFactory.getUserDao().getSuperAndInstituteAdmins(lockedUser.getInstitute().getName());
		if (!rcpts.contains(lockedUser)) {
			rcpts.add(lockedUser);
		}

		for (User rcpt : rcpts) {
			emailProps.put("user", rcpt);
			EmailUtil.getInstance().sendEmail(ACCOUNT_LOCKED_NOTIF_TMPL, new String[] {rcpt.getEmailAddress()}, null, emailProps);
		}


		//
		// remove the user who is locked as they can't see the UI notification
		//
		rcpts.remove(lockedUser);

		Notification notif = new Notification();
		notif.setEntityId(lockedUser.getId());
		notif.setEntityType(User.getEntityName());
		notif.setCreatedBy(daoFactory.getUserDao().getSystemUser());
		notif.setCreationTime(Calendar.getInstance().getTime());
		notif.setOperation("UPDATE");
		notif.setMessage(MessageUtil.getInstance().getMessage(ACCOUNT_LOCKED_NOTIF_TMPL + "_subj", subjParams));
		NotifUtil.getInstance().notify(notif, Collections.singletonMap("user-overview", rcpts));
	}

	private String generateOtp() {
		return String.format("%06d", new SecureRandom().nextInt(999999));
	}
}
