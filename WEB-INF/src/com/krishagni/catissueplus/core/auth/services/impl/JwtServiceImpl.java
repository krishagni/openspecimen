package com.krishagni.catissueplus.core.auth.services.impl;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Hibernate;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationListener;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import com.nimbusds.jwt.SignedJWT;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthDomainSavedEvent;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.services.JwtService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Status;

public class JwtServiceImpl implements JwtService, InitializingBean, ApplicationListener<AuthDomainSavedEvent> {

	private static final LogUtil logger = LogUtil.getLogger(JwtServiceImpl.class);

	private static final int ALLOWED_SKEW = 60000; // 60 seconds

	private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

	private final Map<Long, String> issuerUrls = new ConcurrentHashMap<>();

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public User resolveUser(String token) {
		try {
			return resolveUser(validateToken(getIssuer(token), token));
		} catch (OpenSpecimenException ose) {
			throw ose;
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public void afterPropertiesSet()
	throws Exception {
		List<AuthDomain> domains = daoFactory.getAuthDao().getAuthDomainsByType("oauth");
		domains.forEach(this::addOrUpdateDecoder);
	}

	@Override
	public void onApplicationEvent(AuthDomainSavedEvent event) {
		AuthDomain domain = event.getEventData();
		if (!domain.getAuthProvider().getAuthType().equals("oauth")) {
			return;
		}

		if (domain.isAllowLogins() && Status.isActiveStatus(domain.getActivityStatus())) {
			addOrUpdateDecoder(domain);
		} else {
			removeDecoder(domain);
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

		String issuer = props.get("issuer");
		if (StringUtils.isBlank(issuer)) {
			logger.warn("OAuth token issuer URL is empty. Skipping the registration of " + domain.getName());
			return;
		}

		String jwksUrl = props.get("jwksUrl");
		if (StringUtils.isBlank(jwksUrl)) {
			if (!issuer.endsWith("/")) {
				issuer += "/";
			}

			jwksUrl = issuer + ".well-known/jwks.json";
			logger.info("OAuth issuer JWK set URL is empty. Defaulting to " + jwksUrl);
		}

		issuerUrls.put(domain.getId(), issuer);
		decoders.put(issuer, NimbusJwtDecoder.withJwkSetUri(jwksUrl).build());
	}

	private void removeDecoder(AuthDomain domain) {
		String issuer = issuerUrls.remove(domain.getId());
		if (StringUtils.isNotBlank(issuer)) {
			decoders.remove(issuer);
		}
	}

	//
	// Parse the token without validating it.
	// Extract only the issuer from unvalidated token.
	//
	private String getIssuer(String token) {
		try {
			SignedJWT signedJwt = SignedJWT.parse(token);
			return signedJwt.getJWTClaimsSet().getIssuer();
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

		if (jwt.getAudience() == null || !jwt.getAudience().contains(ConfigUtil.getInstance().getAppUrl())) {
			String receivedAud = jwt.getAudience() == null ? "none" : String.join(", ", jwt.getAudience());
			throw OpenSpecimenException.userError(AuthErrorCode.JWT_INV_AUD, receivedAud);
		}

		return jwt;
	}

	@PlusTransactional
	private User resolveUser(Jwt jwt) {
		String issuer  = jwt.getIssuer().toString();
		String subject = jwt.getSubject();

		Long domainId = issuerUrls.entrySet().stream()
			.filter(issuerEntry -> issuerEntry.getValue().equals(issuer))
			.map(Map.Entry::getKey)
			.findFirst().orElse(null);

		User user = daoFactory.getUserDao().getUser(subject, domainId);
		if (user == null) {
			throw OpenSpecimenException.userError(UserErrorCode.NOT_FOUND, subject);
		}

		Hibernate.initialize(user);
		return user;
	}
}
