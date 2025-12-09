package com.krishagni.catissueplus.core.auth.services.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.core.xml.XMLObject;
import org.opensaml.core.xml.config.XMLObjectProviderRegistrySupport;
import org.opensaml.core.xml.io.MarshallingException;
import org.opensaml.core.xml.util.XMLObjectSupport;
import org.opensaml.saml.common.SAMLVersion;
import org.opensaml.saml.saml2.core.Issuer;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.NameID;
import org.opensaml.saml.saml2.core.NameIDType;
import org.opensaml.saml.saml2.core.SessionIndex;
import org.opensaml.security.x509.BasicX509Credential;
import org.opensaml.xmlsec.signature.Signature;
import org.opensaml.xmlsec.signature.support.SignatureConstants;
import org.opensaml.xmlsec.signature.support.SignatureException;
import org.opensaml.xmlsec.signature.support.Signer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.authentication.Saml2AuthenticatedPrincipal;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthCredential;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthErrorCode;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.ErrorCode;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

import net.shibboleth.utilities.java.support.xml.SerializeSupport;

//
// Callback handler when user authentication is done by SAML based IdP.
// Additionally, responsible for constructing the SAMLLogout request.
//
public class SamlAuthenticationHandler implements AuthenticationSuccessHandler, AuthenticationFailureHandler {
	private static final LogUtil logger = LogUtil.getLogger(SamlAuthenticationHandler.class);

	private DaoFactory daoFactory;

	private UserAuthenticationService userAuthSvc;

	private RelyingPartyRegistrationRepository relyingPartyRepository;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setUserAuthSvc(UserAuthenticationService userAuthSvc) {
		this.userAuthSvc = userAuthSvc;
	}

	public void setRelyingPartyRepository(RelyingPartyRegistrationRepository relyingPartyRepository) {
		this.relyingPartyRepository = relyingPartyRepository;
	}

	@Override
	public void onAuthenticationFailure(HttpServletRequest httpReq, HttpServletResponse httpResp, AuthenticationException error)
	throws IOException {
		logger.error("Error authenticating the user. Error = " + error.getMessage(), error);
		httpResp.sendRedirect(errorUrl(error.getMessage()));
	}

	@Override
	@PlusTransactional
	public void onAuthenticationSuccess(HttpServletRequest httpReq, HttpServletResponse httpResp, Authentication auth)
	throws IOException {
		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) auth.getPrincipal();

		String idpId = principal.getRelyingPartyRegistrationId();
		AuthDomain domain = daoFactory.getAuthDao().getAuthDomainByName(idpId);
		if (domain == null) {
			httpResp.sendRedirect(errorUrl(UserErrorCode.DOMAIN_NOT_FOUND, idpId));
			return;
		}

		User user = getUser(httpResp, principal, domain);
		if (user == null) {
			return;
		}

		String encodedToken = generateAuthToken(httpReq, httpResp, auth, user);
		AuthCredential credential = new AuthCredential();
		credential.setToken(AuthUtil.decodeToken(encodedToken));
		credential.setCredential(auth.getPrincipal());
		daoFactory.getAuthDao().saveCredentials(credential);
		httpResp.sendRedirect(homeUrl());
	}

	@PlusTransactional
	public Map<String, String> getSamlLogoutUrl()
	throws Exception {
		User user = AuthUtil.getCurrentUser();
		if (user == null || user.getAuthDomain() == null) {
			return Collections.emptyMap();
		}

		AuthDomain domain = user.getAuthDomain();
		AuthProvider provider = daoFactory.getAuthDao().getAuthProvider(domain.getAuthProvider().getId());
		if (!"saml".equals(provider.getAuthType())) {
			return Collections.emptyMap();
		}

		RelyingPartyRegistration rp = relyingPartyRepository.findByRegistrationId(user.getAuthDomain().getName());
		if (rp == null) {
			return Collections.emptyMap();
		}

		String idpSloUrl = rp.getAssertingPartyDetails().getSingleLogoutServiceLocation();
		if (StringUtils.isBlank(idpSloUrl)) {
			return Collections.emptyMap();
		}

		String token = AuthUtil.getAuthToken();
		AuthCredential credential = daoFactory.getAuthDao().getCredentials(token);
		if (credential == null) {
			return Collections.emptyMap();
		}

		Saml2AuthenticatedPrincipal principal = (Saml2AuthenticatedPrincipal) credential.getCredential();
		if (principal == null) {
			return Collections.emptyMap();
		}

		String encodedReq = samlEncode(buildLogoutRequest(rp, principal));
		String redirectUrl = UriComponentsBuilder.fromUriString(idpSloUrl)
			.queryParam("SAMLRequest", encodedReq)
			.build(true)
			.toUriString();
		return Collections.singletonMap("url", redirectUrl);
	}

	private User getUser(HttpServletResponse httpResp, Saml2AuthenticatedPrincipal principal, AuthDomain domain)
	throws IOException {
		Map<String, String> props = domain.getAuthProvider().getProps();
		String loginNameAttr      = props.get("loginNameAttr");
		String emailAttr          = props.get("emailAddressAttr");

		String key = null;
		User user = null;
		if (StringUtils.isNotBlank(loginNameAttr)) {
			String loginName = principal.getFirstAttribute(loginNameAttr);
			user = daoFactory.getUserDao().getUser(loginName, domain.getName());
			key = loginName;
		} else if (StringUtils.isNotBlank(emailAttr)) {
			String emailId = principal.getFirstAttribute(emailAttr);
			user = daoFactory.getUserDao().getUserByEmailAddress(emailId);
			key = emailId;
		}

		if (user == null) {
			httpResp.sendRedirect(errorUrl(UserErrorCode.ONE_OR_MORE_NOT_FOUND, key));
		} else if (user.isLocked()) {
			httpResp.sendRedirect(errorUrl(AuthErrorCode.USER_LOCKED, key));
		} else if (!user.isActive()) {
			httpResp.sendRedirect(errorUrl(UserErrorCode.INACTIVE, key));
		} else {
			return user;
		}

		return null;
	}

	private String generateAuthToken(HttpServletRequest httpReq, HttpServletResponse httpResp, Authentication auth, User user) {
		LoginDetail loginDetail = new LoginDetail();
		loginDetail.setIpAddress(Utility.getRemoteAddress(httpReq));
		loginDetail.setApiUrl(httpReq.getRequestURI());
		loginDetail.setRequestMethod(RequestMethod.POST.name());
		SecurityContextHolder.getContext().setAuthentication(
			new Authentication() {
				@Override
				public Collection<? extends GrantedAuthority> getAuthorities() {
					return null;
				}

				@Override
				public Object getCredentials() {
					return auth.getCredentials();
				}

				@Override
				public Object getDetails() {
					return null;
				}

				@Override
				public Object getPrincipal() {
					return null;
				}

				@Override
				public boolean isAuthenticated() {
					return false;
				}

				@Override
				public void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException {

				}

				@Override
				public String getName() {
					return null;
				}
			}
		);

		String encodedToken = userAuthSvc.generateToken(user, loginDetail);
		AuthUtil.setTokenCookie(httpReq, httpResp, encodedToken);
		return encodedToken;
	}

	private LogoutRequest buildLogoutRequest(RelyingPartyRegistration rp, Saml2AuthenticatedPrincipal principal)
	throws MarshallingException, SignatureException {
		String nameIdValue = principal.getName();
		String sessionIdxValue = null;
		if (principal.getSessionIndexes() != null && !principal.getSessionIndexes().isEmpty()) {
			sessionIdxValue = principal.getSessionIndexes().iterator().next();
		}

		LogoutRequest logoutReq = (LogoutRequest) XMLObjectSupport.buildXMLObject(LogoutRequest.DEFAULT_ELEMENT_NAME);
		logoutReq.setID("_" + UUID.randomUUID());
		logoutReq.setIssueInstant(Instant.now());
		logoutReq.setVersion(SAMLVersion.VERSION_20);
		logoutReq.setDestination(rp.getAssertingPartyDetails().getSingleLogoutServiceLocation());

		// Issuer - We the service provider - SP
		Issuer issuer = (Issuer) XMLObjectSupport.buildXMLObject(Issuer.DEFAULT_ELEMENT_NAME);
		issuer.setValue(rp.getEntityId());
		issuer.setFormat(Issuer.ENTITY);
		logoutReq.setIssuer(issuer);

		// NameID
		NameID nameId = (NameID) XMLObjectSupport.buildXMLObject(NameID.DEFAULT_ELEMENT_NAME);
		nameId.setValue(nameIdValue);
		nameId.setFormat(NameIDType.PERSISTENT);
		logoutReq.setNameID(nameId);

		// SessionIndex
		if (sessionIdxValue != null) {
			SessionIndex si = (SessionIndex) XMLObjectSupport.buildXMLObject(SessionIndex.DEFAULT_ELEMENT_NAME);
			si.setValue(sessionIdxValue);
			logoutReq.getSessionIndexes().add(si);
		}

		// Add signature to the request
		Signature signature = getSignature(rp);
		logoutReq.setSignature(signature);

		XMLObjectProviderRegistrySupport.getMarshallerFactory()
			.getMarshaller(logoutReq)
			.marshall(logoutReq);

		Signer.signObject(signature);
		return logoutReq;
	}

	private Signature getSignature(RelyingPartyRegistration rp) {
		Saml2X509Credential signing = rp.getSigningX509Credentials().stream()
			.findFirst()
			.orElseThrow(() -> new IllegalStateException("No signing credential"));

		BasicX509Credential credential = new BasicX509Credential(signing.getCertificate(), signing.getPrivateKey());
		Signature signature = (Signature) XMLObjectSupport.buildXMLObject(Signature.DEFAULT_ELEMENT_NAME);
		signature.setSigningCredential(credential);
		signature.setSignatureAlgorithm(SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256);
		signature.setCanonicalizationAlgorithm(SignatureConstants.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		return signature;
	}

	private String samlEncode(XMLObject object) throws Exception {
		Element node = XMLObjectSupport.marshall(object);
		String xml = SerializeSupport.nodeToString(node);
		byte[] deflated = deflate(xml);
		String base64Encoded = Base64.getEncoder().encodeToString(deflated);
		return URLEncoder.encode(base64Encoded, StandardCharsets.UTF_8);
	}

	private byte[] deflate(String inputString) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		Deflater deflater = new Deflater(Deflater.DEFLATED, true);

		DeflaterOutputStream deflaterStream = new DeflaterOutputStream(byteStream, deflater);
		deflaterStream.write(inputString.getBytes(StandardCharsets.UTF_8));
		deflaterStream.close();
		return byteStream.toByteArray();
	}

	private String errorUrl(ErrorCode code, Object... args) {
		return errorUrl(MessageUtil.getInstance().getMessage(code.code().toLowerCase(), args));
	}

	private String errorUrl(String message) {
		return appUiUrl() + "login-error?message=" + message;
	}

	private String homeUrl() {
		return appUiUrl() + "home";
	}

	private String appUiUrl() {
		String url = ConfigUtil.getInstance().getAppUrl();
		if (!url.endsWith("/")) {
			url += "/";
		}

		url += "ui-app/#/";
		return url;
	}
}
