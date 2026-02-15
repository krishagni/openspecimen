package com.krishagni.catissueplus.core.auth.services.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.SignedJWT;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.administrative.services.ScheduledTaskManager;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthDomainSavedEvent;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.OAuthState;
import com.krishagni.catissueplus.core.auth.domain.factory.AuthProviderErrorCode;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.services.OAuthService;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;

public class OAuthServiceImpl implements OAuthService, InitializingBean, ApplicationListener<AuthDomainSavedEvent> {

	private static final LogUtil logger = LogUtil.getLogger(OAuthServiceImpl.class);

	private static final int ALLOWED_SKEW = 60000; // 60 seconds

	private static final int OLD_STATE_MINS = 60;

	private static final int OLD_STATE_CLEAN_UP_FREQ_MINS = 30;

	private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

	private final Map<Long, String> issuerUrls = new ConcurrentHashMap<>();

	private final Map<String, AuthDomain> clientIds = new ConcurrentHashMap<>();

	private DaoFactory daoFactory;

	private UserAuthenticationService userAuthSvc;

	private ScheduledTaskManager taskManager;

	private RestTemplate restTmpl = null;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setUserAuthSvc(UserAuthenticationService userAuthSvc) {
		this.userAuthSvc = userAuthSvc;
	}

	public void setTaskManager(ScheduledTaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public OAuthServiceImpl() {
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(10 * 1000); // max wait of 10 seconds for connection
		requestFactory.setReadTimeout(60 * 1000); // max wait of 60 seconds for read
		restTmpl = new RestTemplate(requestFactory);
	}

	@Override
	public User resolveUser(String jwt) {
		try {
			return resolveUser(validateToken(getIssuer(jwt), jwt));
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	public String generateAppToken(HttpServletRequest httpReq, HttpServletResponse httpResp, String jwt) {
		try {
			User user = resolveUser(jwt);
			String ipAddress = Utility.getRemoteAddress(httpReq);

			if (!user.getAuthDomain().isAllowLogins()) {
				throw OpenSpecimenException.userError(AuthErrorCode.DOMAIN_LOGIN_DISABLED, user.getAuthDomain().getName());
			} else if (!user.isAllowedAccessFrom(ipAddress)) {
				throw OpenSpecimenException.userError(AuthErrorCode.NA_IP_ADDRESS, ipAddress);
			} else if (!user.isAdmin() && isSystemLockedDown()) {
				throw OpenSpecimenException.userError(AuthErrorCode.SYSTEM_LOCKDOWN);
			} else if (user.isLocked()) {
				throw OpenSpecimenException.userError(AuthErrorCode.USER_LOCKED, user.formattedName());
			} else if (!user.isActive()) {
				throw OpenSpecimenException.userError(UserErrorCode.INACTIVE, user.formattedName());
			}

			String encodedToken = generateAppToken(httpReq, httpResp, user);
			AuthUtil.setTokenCookie(httpReq, httpResp, encodedToken);
			return encodedToken;
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public String getAuthorizeUrl(Long providerId) {
		AuthDomain domain = daoFactory.getAuthDao().getById(providerId);
		if (domain == null) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.DOMAIN_NOT_FOUND, providerId);
		}

		AuthProvider provider = domain.getAuthProvider();
		if (!provider.getAuthType().equals("oauth")) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.NOT_OAUTH, domain.getName());
		}

		OAuthState state = new OAuthState();
		try {
			state.setDomain(domain);
			state.setState(UUID.randomUUID().toString());
			state.setVerifier(generateVerifier());
			state.setChallenge(generateChallenge(state.getVerifier()));
			state.setTime(Instant.now());
			daoFactory.getOAuthStateDao().saveOrUpdate(state);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}


		Map<String, String> props = provider.getProps();
		String scope = StringUtils.isBlank(props.get("scope")) ? "openid" : props.get("scope");
		return UriComponentsBuilder.fromHttpUrl(props.get("authorizeUrl"))
			.queryParam("response_type", "code")
			.queryParam("client_id", props.get("clientId"))
			.queryParam("audience", props.get("audience"))
			.queryParam("redirect_uri", getRedirectUri())
			.queryParam("scope", scope)
			.queryParam("code_challenge", state.getChallenge())
			.queryParam("code_challenge_method", "S256")
			.queryParam("state", state.getState())
			.toUriString();
	}

	@Override
	@PlusTransactional
	public String exchangeCodeForToken(HttpServletRequest httpReq, HttpServletResponse httpResp, String state, String code) {
		OAuthState savedState = getSavedState(state);
		AuthDomain domain = savedState.getDomain();
		AuthProvider provider = domain.getAuthProvider();
		Map<String, String> props = provider.getProps();

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("grant_type", "authorization_code");
		map.add("code", code);
		map.add("redirect_uri", getRedirectUri());
		map.add("code_verifier", savedState.getVerifier());
		map.add("client_id", props.get("clientId"));
		if (StringUtils.isNotBlank(props.get("clientSecret"))) {
			map.add("client_secret", props.get("clientSecret"));
		}

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);
		String tokenUrl = props.get("tokenUrl");
		Map<String, Object> response = restTmpl.postForObject(tokenUrl, request, Map.class);
		return (String) response.get("id_token");
	}

	@Override
	@PlusTransactional
	public void afterPropertiesSet()
	throws Exception {
		List<AuthDomain> domains = daoFactory.getAuthDao().getAuthDomainsByType("oauth");
		domains.forEach(this::addOrUpdateDecoder);

		taskManager.scheduleWithFixedDelay(getOldStatesCleanerThread(), OLD_STATE_CLEAN_UP_FREQ_MINS);
	}

	@Override
	public void onApplicationEvent(AuthDomainSavedEvent event) {
		AuthDomain domain = event.getEventData();
		if (!domain.getAuthProvider().getAuthType().equals("oauth")) {
			return;
		}

		if (domain.isAllowLogins() && Status.isActiveStatus(domain.getActivityStatus())) {
			ensureRequiredAttrsSpecified(domain);
			addOrUpdateDecoder(domain);
		} else {
			removeDecoder(domain);
		}
	}

	private void ensureRequiredAttrsSpecified(AuthDomain domain) {
		AuthProvider provider = domain.getAuthProvider();

		Map<String, String> props = provider.getProps();
		if (StringUtils.isBlank(props.get("issuer"))) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.ISSUER_URL_REQ);
		}

		if (StringUtils.isBlank(props.get("authorizeUrl"))) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.AUTHORIZE_URL_REQ);
		}

		if (StringUtils.isBlank(props.get("tokenUrl"))) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.TOKEN_URL_REQ);
		}

		if (StringUtils.isBlank(props.get("clientId"))) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.CLIENT_ID_REQ);
		}
	}

	private void addOrUpdateDecoder(AuthDomain domain) {
		removeDecoder(domain);
		if (domain.isAllowLogins()) {
			addDecoder(domain);
		}
	}

	private void addDecoder(AuthDomain domain) {
		AuthProvider provider = domain.getAuthProvider();
		Map<String, String> props = provider.getProps();
		if (props == null || props.isEmpty()) {
			logger.warn("The OAuth domain is not configured with the issuer or client ID. Skipping the registration of " + domain.getName());
			return;
		}

		String issuer = normaliseUrl(props.get("issuer"));
		if (StringUtils.isBlank(issuer)) {
			logger.warn("OAuth token issuer URL is empty. Skipping the registration of " + domain.getName());
			return;
		}

		String clientId = props.get("clientId");
		if (StringUtils.isBlank(clientId)) {
			logger.warn("OAuth client ID is empty. Skipping the registration of " + domain.getName());
			return;
		}

		String jwksUrl = props.get("jwksUrl");
		if (StringUtils.isBlank(jwksUrl)) {
			jwksUrl = issuer;
			if (!jwksUrl.endsWith("/")) {
				jwksUrl += "/";
			}

			jwksUrl += ".well-known/jwks.json";
			logger.info("OAuth issuer JWK set URL is empty. Defaulting to " + jwksUrl);
		}

		issuerUrls.put(domain.getId(), issuer);
		clientIds.put(clientId, domain);
		decoders.put(issuer, NimbusJwtDecoder.withJwkSetUri(jwksUrl).build());
	}

	private void removeDecoder(AuthDomain domain) {
		String issuer = issuerUrls.remove(domain.getId());
		if (StringUtils.isNotBlank(issuer)) {
			decoders.remove(issuer);

			AuthProvider provider = domain.getAuthProvider();
			Map<String, String> props = provider.getProps();
			String clientId = props.get("clientId");
			if (clientId != null) {
				clientIds.remove(clientId);
			}
		}
	}

	//
	// Parse the token without validating it.
	// Extract only the issuer from the token.
	//
	private String getIssuer(String token) {
		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			return normaliseUrl(signedJwt.getJWTClaimsSet().getIssuer());
		} catch (Exception e) {
			throw OpenSpecimenException.userError(AuthErrorCode.JWT_PARSE_ERROR, e.getLocalizedMessage());
		}
	}

	//
	// Validate the token
	// 1. recognises the issuer
	// 2. verify signature (decoder.decode)
	// 3. verify the timestamps (decoder.decode)
	// 4. token was intended for us
	//
	private Jwt validateToken(String issuer, String token) {
		JwtDecoder decoder = decoders.get(issuer);
		if (decoder == null) {
			throw OpenSpecimenException.userError(AuthErrorCode.JWT_UNKNOWN_ISSUER, issuer);
		}

		Jwt jwt = decoder.decode(token);
		Instant now = Instant.now(Clock.systemUTC());
		if (jwt.getExpiresAt() != null && jwt.getExpiresAt().plusMillis(ALLOWED_SKEW).isBefore(now)) {
			throw OpenSpecimenException.userError(AuthErrorCode.JWT_EXPIRED, jwt.getExpiresAt().toString(), now.toString());
		}

		boolean invAud = true, invIssuerForAud = true;
		List<String> audiences = jwt.getAudience();
		if (audiences != null) {
			for (String audience : audiences) {
				AuthDomain domain = clientIds.get(audience);
				if (domain != null) {
					invAud = false;

					String registeredIssuer = issuerUrls.get(domain.getId());
					if (registeredIssuer.equals(issuer)) {
						invIssuerForAud = false;
						break;
					}
				} else if (getAppUrl().equals(normaliseUrl(audience))) {
					invAud = invIssuerForAud = false;
					break;
				}
			}
		}

		if (invAud || invIssuerForAud) {
			String receivedAud = audiences == null ? "none" : String.join(", ", audiences);
			throw OpenSpecimenException.userError(
				invAud ? AuthErrorCode.JWT_INV_AUD : AuthErrorCode.JWT_INV_AUD_BY_ISSUER,
				receivedAud,
				issuer
			);
		}

		return jwt;
	}

	//
	// Input to this is a validated token
	//
	@PlusTransactional
	private User resolveUser(Jwt jwt) {
		String subject = jwt.getSubject();
		List<AuthDomain> domains = new ArrayList<>();
		for (String audience : jwt.getAudience()) {
			AuthDomain domain = clientIds.get(audience);
			if (domain != null) {
				domains.add(domain);
				break;
			}
		}

		if (domains.isEmpty()) {
			String issuer = normaliseUrl(jwt.getIssuer().toString());
			for (Map.Entry<Long, String> registeredIssuer : issuerUrls.entrySet()) {
				if (!registeredIssuer.getValue().equals(issuer)) {
					continue;
				}

				Long domainId = registeredIssuer.getKey();
				for (AuthDomain domain : clientIds.values()) {
					if (domain.getId().equals(domainId)) {
						domains.add(domain);
						break;
					}
				}
			}
		}

		if (domains.isEmpty()) {
			throw OpenSpecimenException.userError(AuthErrorCode.JWT_INV_AUD, String.join(", ", jwt.getAudience()));
		}

		User user = null;
		for (AuthDomain domain : domains) {
			user = daoFactory.getUserDao().getUser(subject, domain.getName());
			if (user != null) {
				break;
			}
		}

		if (user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, subject);
		}

		Hibernate.initialize(user);
		return user;
	}

	@PlusTransactional
	private String generateAppToken(HttpServletRequest httpReq, HttpServletResponse httpResp, User user) {
		LoginDetail loginDetail = new LoginDetail();
		loginDetail.setIpAddress(Utility.getRemoteAddress(httpReq));
		loginDetail.setApiUrl(httpReq.getRequestURI());
		loginDetail.setRequestMethod(RequestMethod.POST.name());
		return userAuthSvc.generateToken(user, loginDetail);
	}

	private boolean isSystemLockedDown() {
		return ConfigUtil.getInstance().getBoolSetting("administrative", "system_lockdown", false);
	}

	private String generateVerifier() {
		byte[] code = new byte[32];
		new SecureRandom().nextBytes(code);
		return Base64URL.encode(code).toString();
	}

	private String generateChallenge(String verifier)
	throws NoSuchAlgorithmException {
		byte[] bytes = verifier.getBytes(StandardCharsets.US_ASCII);
		byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
		return Base64URL.encode(digest).toString();
	}

	private String getRedirectUri() {
		return getAppUrl() + "rest/ng/oauth/callback";
	}

	@PlusTransactional
	private OAuthState getSavedState(String state) {
		OAuthState savedState = daoFactory.getOAuthStateDao().getByState(state);
		if (savedState == null) {
			throw OpenSpecimenException.userError(AuthErrorCode.INV_STATE);
		}

		// touch the properties for later use
		AuthDomain domain = savedState.getDomain();
		AuthProvider provider = domain.getAuthProvider();
		Map<String, String> props = provider.getProps();
		return savedState;
	}

	private String normaliseUrl(String url) {
		if (StringUtils.isBlank(url)) {
			return url;
		}

		if (!url.endsWith("/")) {
			url += "/";
		}

		return url;
	}

	private String getAppUrl() {
		return normaliseUrl(ConfigUtil.getInstance().getAppUrl());
	}


	public Runnable getOldStatesCleanerThread() {
		return new Runnable() {
			@Override
			public void run() {
				try {
					run0();
				} catch (Throwable e) {
					logger.error("Error cleaning up of OAuth state table", e);
				}
			}

			@PlusTransactional
			private void run0() {
				Instant olderThan = Instant.now().minus(OLD_STATE_MINS, ChronoUnit.MINUTES);
				int count = daoFactory.getOAuthStateDao().deleteStatesOlderThan(olderThan);
				if (count > 0) {
					logger.info(String.format("Cleaned up %d stale OAuth states", count));
				}
			}
		};
	}
}
