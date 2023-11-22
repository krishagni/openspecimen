package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.filter.GenericFilterBean;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.audit.domain.UserApiCallLog;
import com.krishagni.catissueplus.core.audit.services.AuditService;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.domain.LoginAuditLog;
import com.krishagni.catissueplus.core.auth.domain.UserRequestData;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.auth.services.UserRequestDataProvider;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.ConfigChangeListener;
import com.krishagni.catissueplus.core.common.service.ConfigurationService;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.rest.RestErrorController;

public class AuthTokenFilter extends GenericFilterBean implements InitializingBean {
	private static final String OS_CLIENT_HDR = "X-OS-API-CLIENT";

	private static final String BASIC_AUTH = "Basic ";
	
	private Set<String> allowedOrigins;
	
	private UserAuthenticationService authService;
	
	private Map<String, List<String>> excludeUrls = new HashMap<>();

	private AuditService auditService;

	private ConfigurationService cfgSvc;

	private List<UserRequestDataProvider> userRequestDataProviders = new ArrayList<>();

	public void setAuthService(UserAuthenticationService authService) {
		this.authService = authService;
	}

	public void setCfgSvc(ConfigurationService cfgSvc) {
		this.cfgSvc = cfgSvc;
	}

	public void setExcludeUrls(Map<String, List<String>> excludeUrls) {
		this.excludeUrls = excludeUrls;
	}

	public void addExcludeUrl(String method, String resourceUrl) {
		addUrl(excludeUrls, method, resourceUrl);
	}

	public void setAuditService(AuditService auditService) {
		this.auditService = auditService;
	}

	public void addUserRequestDataProvider(UserRequestDataProvider provider) {
		userRequestDataProviders.add(provider);
	}

	public void doFilter(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException {
		if (!(req instanceof HttpServletRequest)) {
			throw new IllegalAccessError("Unknown protocol request");
		}

		try {
			doFilter0(req, resp, chain);
		} catch (Exception e) {
			sendError((HttpServletRequest) req, (HttpServletResponse) resp, e);
		}
	}

	@Override
	public void afterPropertiesSet()
	throws ServletException {
		super.afterPropertiesSet();
		cfgSvc.registerChangeListener("common", new ConfigChangeListener() {
			@Override
			public void onConfigChange(String name, String value) {
				if (!"allowed_req_origins".equals(name)) {
					return;
				}

				allowedOrigins = getAllowedOrigins(value);
			}
		});
	}

	private void doFilter0(ServletRequest req, ServletResponse resp, FilterChain chain)
	throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest)req;
		HttpServletResponse httpResp = (HttpServletResponse)resp;

		String origin = Utility.getHeader(httpReq, "Origin");
		if (!isOriginAllowed(origin)) {
			httpResp.sendError(
				HttpServletResponse.SC_METHOD_NOT_ALLOWED,
				"Requests from the origin server "  + Utility.escapeXss(origin) + " not allowed");
			return;
		}

		if (StringUtils.isNotBlank(origin)) {
			httpResp.setHeader("Access-Control-Allow-Origin", origin);
		}

		httpResp.setHeader("Access-Control-Allow-Credentials", "true");
		httpResp.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE, PATCH, OPTIONS");
		httpResp.setHeader("Access-Control-Allow-Headers", "Origin, Accept, Content-Type, X-OS-API-TOKEN, X-OS-API-CLIENT, X-OS-IMPERSONATE-USER, X-OS-CLIENT-TZ, X-OS-SURVEY-TOKEN");
		httpResp.setHeader("Access-Control-Expose-Headers", "Content-Disposition, Content-Length, Content-Type");

		httpResp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
		httpResp.setHeader("Pragma", "no-cache");
		httpResp.setDateHeader("Expires", 0);

		if (httpReq.getMethod().equalsIgnoreCase("options")) {
			httpResp.setStatus(HttpServletResponse.SC_OK);
			return;
		}

		User user = null;
		String authToken = AuthUtil.getAuthTokenFromHeader(httpReq);
		if (authToken == null) {
			logger.info("No auth token in the header. Probing the cookie. URL = " + httpReq.getRequestURI());
			authToken = AuthUtil.getTokenFromCookie(httpReq);
			if (authToken != null) {
				logger.info("Found the auth token in the cookie. URL = " + httpReq.getRequestURI());
			}
		}
		
		LoginAuditLog loginAuditLog = null;
		if (authToken != null) {
			TokenDetail tokenDetail = new TokenDetail();
			tokenDetail.setToken(authToken);
			tokenDetail.setIpAddress(Utility.getRemoteAddress(httpReq));

			ResponseEvent<AuthToken> atResp = authService.validateToken(new RequestEvent<>(tokenDetail));
			if (atResp.isSuccessful()) {
				user = atResp.getPayload().getUser();
				loginAuditLog = atResp.getPayload().getLoginAuditLog();
			} else {
				logger.info("The auth token received in request is invalid. URL = " + httpReq.getRequestURI());
				setUnauthorizedResp(httpReq, httpResp, chain, true);
			}
		} else if (httpReq.getHeader(HttpHeaders.AUTHORIZATION) != null) {
			logger.info("No auth token in the request. Probing the authorization header for username/password details. URL = " + httpReq.getRequestURI());
			AuthToken token = doBasicAuthentication(httpReq, httpResp);
			if (token != null) {
				user = token.getUser();
				loginAuditLog = token.getLoginAuditLog();
			} else {
				logger.info("The credentials received in authorization header are invalid. URL = " + httpReq.getRequestURI());
				setUnauthorizedResp(httpReq, httpResp, chain, true);
			}
		}

		httpReq.setAttribute("user", user);
		setupReqDataProviders(httpReq, httpResp);
		if (user == null) {
			user = UserRequestData.getInstance().getUser();
		}

		if (user == null) {
			logger.info("No user details. URL = " + httpReq.getRequestURI());
			String clientHdr = httpReq.getHeader(OS_CLIENT_HDR);
			if (clientHdr != null && clientHdr.equals("webui")) {
				setUnauthorizedResp(httpReq, httpResp, chain, false);
			} else {
				setRequireAuthResp(httpReq, httpResp, chain);
			}

			teardownReqDataProviders(httpReq, httpResp);
			return;
		}

		if (user.isContact() || user.isClosed()) {
			logger.info("Contact or archived users not allowed to access the resource. Stopping the request processing: " + httpReq.getRequestURI());

			AuthUtil.clearTokenCookie(httpReq, httpResp);
			httpResp.sendError(
				HttpServletResponse.SC_UNAUTHORIZED,
				String.format(
					"%s (%d) is not allowed to sign-in. Type = %s, Status = %s",
					user.formattedName(), user.getId(), user.getType().name(), user.getActivityStatus()
				)
			);


			teardownReqDataProviders(httpReq, httpResp);
			return;
		}

		User impersonatedUser = null;
		if (user.isAdmin() && !user.isSysUser()) {
			String impUserToken = AuthUtil.getImpersonateUser(httpReq);
			if (StringUtils.isNotBlank(impUserToken)) {
				logger.info("Validating the credentials of the impersonate request by: " + user.formattedName() + ", URL = " + httpReq.getRequestURI());
				TokenDetail tokenDetail = new TokenDetail();
				tokenDetail.setToken(impUserToken);
				tokenDetail.setIpAddress(Utility.getRemoteAddress(httpReq));

				try {
					ResponseEvent<AuthToken> atResp = authService.validateToken(new RequestEvent<>(tokenDetail));
					atResp.throwErrorIfUnsuccessful();

					authToken = impUserToken;
					impersonatedUser = atResp.getPayload().getUser();
					loginAuditLog = atResp.getPayload().getLoginAuditLog();
				} catch (OpenSpecimenException ose) {
					logger.error("Credentials of the impersonate request are either invalid or processing failed. URL = " + httpReq.getRequestURI(), ose);
					if (ose.containsError(AuthErrorCode.IMP_TOKEN_EXP) || ose.containsError(AuthErrorCode.INVALID_TOKEN)) {
						AuthUtil.clearTokenCookie(httpReq, httpResp);
						httpResp.sendError(HttpServletResponse.SC_UNAUTHORIZED, ose.getMessage());
						teardownReqDataProviders(httpReq, httpResp);
						return;
					}

					throw ose;
				}

				impersonatedUser.setImpersonated(true);
			}
		}

		AuthUtil.setCurrentUser(impersonatedUser != null ? impersonatedUser : user, authToken, httpReq, impersonatedUser != null);
		Date callStartTime = Calendar.getInstance().getTime();
		chain.doFilter(req, resp);
		AuthUtil.clearCurrentUser();
		teardownReqDataProviders(httpReq, httpResp);

		if (isRecordableApi(httpReq)) {
			UserApiCallLog userAuditLog = new UserApiCallLog();
			userAuditLog.setUser(user);
			userAuditLog.setImpersonatedUser(impersonatedUser);
			userAuditLog.setUrl(httpReq.getRequestURI());
			userAuditLog.setMethod(httpReq.getMethod());
			userAuditLog.setCallStartTime(callStartTime);
			userAuditLog.setCallEndTime(Calendar.getInstance().getTime());
			userAuditLog.setResponseCode(Integer.toString(httpResp.getStatus()));
			userAuditLog.setLoginAuditLog(loginAuditLog);
			auditService.insertApiCallLog(userAuditLog);
		}
	}

	private AuthToken doBasicAuthentication(HttpServletRequest httpReq, HttpServletResponse httpResp) throws UnsupportedEncodingException {
		String header = httpReq.getHeader(HttpHeaders.AUTHORIZATION);
		if (header == null || !header.startsWith(BASIC_AUTH)) {
			return null;
		}

		String basicAuth = new String(Base64.getDecoder().decode(header.substring(BASIC_AUTH.length())));
		String[] parts = basicAuth.split(":");
		if (parts.length != 2) {
			return null;
		}

		LoginDetail detail = new LoginDetail();
		detail.setLoginName(parts[0]);
		detail.setPassword(parts[1]);
		detail.setDomainName(User.DEFAULT_AUTH_DOMAIN);
		detail.setDoNotGenerateToken(true);
		detail.setIpAddress(Utility.getRemoteAddress(httpReq));

		ResponseEvent<Map<String, Object>> resp = authService.authenticateUser(RequestEvent.wrap(detail));
		if (resp.isSuccessful()) {
			return (AuthToken) resp.getPayload().get("tokenObj");
		}

		return null;
	}

	private void setRequireAuthResp(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
	throws IOException, ServletException {
		if (requiresSignIn(req)) {
			resp.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"OpenSpecimen\"");
			setUnauthorizedResp(req, resp, chain, true);
		} else {
			chain.doFilter(req, resp);
		}
	}

	private void setUnauthorizedResp(HttpServletRequest req, HttpServletResponse resp, FilterChain chain, boolean reqSignIn)
	throws IOException, ServletException {
		if (reqSignIn || requiresSignIn(req)) {
			logger.info("The requested resource requires a valid signed-in session. Sending HTTP 401 to authenticate the user. URL = " + req.getRequestURI());
			AuthUtil.clearTokenCookie(req, resp);
			resp.sendError(HttpServletResponse.SC_UNAUTHORIZED,
				"You must supply valid credentials to access the OpenSpecimen REST API");
		} else {
			chain.doFilter(req, resp);
		}
	}

	private void addUrl(Map<String, List<String>> urlsMap, String method, String resourceUrl) {
		List<String> urls = urlsMap.computeIfAbsent(method, (key) -> new ArrayList<>());
		if (!urls.contains(resourceUrl)) {
			urls.add(resourceUrl);
		}
	}

	private boolean requiresSignIn(ServletRequest req) {
		return !matchesUrl(req, excludeUrls);
	}

	private boolean matchesUrl(ServletRequest req, Map<String, List<String>> inputUrls) {
		HttpServletRequest httpReq = (HttpServletRequest)req;

		List<String> urls = inputUrls.get(httpReq.getMethod());
		if (urls == null) {
			urls = Collections.emptyList();
		}

		boolean result = false;
		for (String url : urls) {
			if (matches(httpReq, url)) {
				result = true;
				break;
			}
		}

		return result;
	}

	private boolean isRecordableApi(HttpServletRequest httpReq) {
		return !matches(httpReq, "/user-notifications/unread-count/**") &&
			!matches(httpReq, "/label-print-jobs/items/**");
	}

	private boolean matches(HttpServletRequest httpReq, String url) {
		return new AntPathRequestMatcher(getUrlPattern(url), httpReq.getMethod(), true).matches(httpReq);
	}

	private String getUrlPattern(String url) {
		if (!url.startsWith("/**")) {
			String prefix = "/**";
			if (!url.startsWith("/")) {
				prefix += "/";
			}

			url = prefix + url;
		}

		return url;
	}

	private void sendError(HttpServletRequest httpReq, HttpServletResponse httpResp, Exception e)
	throws IOException {
		ResponseEntity<Object> resp = new RestErrorController().handleOtherException(e, new ServletWebRequest(httpReq));

		httpResp.setContentType("application/json");
		httpResp.setStatus(resp.getStatusCodeValue());
		httpResp.getOutputStream().write(toJsonString(resp.getBody()).getBytes());
		httpResp.getOutputStream().flush();
		httpResp.getOutputStream().close();
	}

	private String toJsonString(Object body) {
		String unknownError = "[{\"message\": \"Unknown error\"}]";
		if (body == null) {
			return unknownError;
		}

		try {
			return new ObjectMapper().writeValueAsString(body);
		} catch (Exception e) {
			e.printStackTrace();
			return unknownError;
		}
	}

	private User getUser(User user, String userIpAddress, String userString) {
		String result = new String(Base64.getDecoder().decode(userString));
		result = Utility.decrypt(result);
		String[] parts = result.split("/");

		Long userId = Long.parseLong(parts[0]);
		Long impUserId = Long.parseLong(parts[1]);
		Date validUntil = new Date(Long.parseLong(parts[2]));
		String ipAddress = parts[3];
		if (validUntil.getTime() <= Calendar.getInstance().getTimeInMillis()) {
			throw OpenSpecimenException.userError(AuthErrorCode.IMP_TOKEN_EXP);
		}

		if (!user.getId().equals(userId) || !StringUtils.equals(userIpAddress, ipAddress)) {
			throw OpenSpecimenException.userError(AuthErrorCode.IMP_TOKEN_INV);
		}

		return authService.getUser(impUserId);
	}

	private boolean isOriginAllowed(String origin) {
		if (StringUtils.isBlank(origin)) {
			return true;
		}

		return getAllowedOrigins().isEmpty() || getAllowedOrigins().contains("*") || getAllowedOrigins().contains(origin.trim());
	}

	private Set<String> getAllowedOrigins() {
		if (allowedOrigins == null) {
			String setting = ConfigUtil.getInstance().getStrSetting("common", "allowed_req_origins", "");
			allowedOrigins = getAllowedOrigins(setting);
		}

		return allowedOrigins;
	}

	private Set<String> getAllowedOrigins(String setting) {
		if (StringUtils.isBlank(setting)) {
			return Collections.emptySet();
		}

		return Stream.of(setting.split(",")).map(String::trim).collect(Collectors.toSet());
	}

	private void setupReqDataProviders(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		for (UserRequestDataProvider provider : userRequestDataProviders) {
			provider.setup(httpReq, httpResp);
		}
	}

	private void teardownReqDataProviders(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		for (UserRequestDataProvider provider : userRequestDataProviders) {
			provider.teardown(httpReq, httpResp);
		}
	}
}