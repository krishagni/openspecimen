package com.krishagni.catissueplus.core.auth;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;

import javax.servlet.Filter;
import javax.servlet.ServletException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.app.VelocityEngine;
import org.opensaml.Configuration;
import org.opensaml.PaosBootstrap;
import org.opensaml.saml2.metadata.provider.AbstractMetadataProvider;
import org.opensaml.saml2.metadata.provider.HTTPMetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProvider;
import org.opensaml.saml2.metadata.provider.MetadataProviderException;
import org.opensaml.saml2.metadata.provider.ResourceBackedMetadataProvider;
import org.opensaml.util.resource.FilesystemResource;
import org.opensaml.util.resource.ResourceException;
import org.opensaml.xml.parse.ParserPool;
import org.opensaml.xml.parse.StaticBasicParserPool;
import org.opensaml.xml.parse.XMLParserException;
import org.opensaml.xml.security.BasicSecurityConfiguration;
import org.opensaml.xml.security.keyinfo.NamedKeyInfoGeneratorManager;
import org.opensaml.xml.security.x509.X509KeyInfoGeneratorFactory;
import org.opensaml.xml.signature.SignatureConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.saml.SAMLAuthenticationProvider;
import org.springframework.security.saml.SAMLAuthenticationToken;
import org.springframework.security.saml.SAMLConstants;
import org.springframework.security.saml.SAMLDiscovery;
import org.springframework.security.saml.SAMLEntryPoint;
import org.springframework.security.saml.SAMLLogoutFilter;
import org.springframework.security.saml.SAMLLogoutProcessingFilter;
import org.springframework.security.saml.SAMLProcessingFilter;
import org.springframework.security.saml.context.SAMLContextProvider;
import org.springframework.security.saml.context.SAMLContextProviderImpl;
import org.springframework.security.saml.context.SAMLContextProviderLB;
import org.springframework.security.saml.key.JKSKeyManager;
import org.springframework.security.saml.key.KeyManager;
import org.springframework.security.saml.log.SAMLDefaultLogger;
import org.springframework.security.saml.metadata.CachingMetadataManager;
import org.springframework.security.saml.metadata.ExtendedMetadata;
import org.springframework.security.saml.metadata.ExtendedMetadataDelegate;
import org.springframework.security.saml.metadata.MetadataDisplayFilter;
import org.springframework.security.saml.metadata.MetadataGenerator;
import org.springframework.security.saml.metadata.MetadataGeneratorFilter;
import org.springframework.security.saml.processor.HTTPArtifactBinding;
import org.springframework.security.saml.processor.HTTPPAOS11Binding;
import org.springframework.security.saml.processor.HTTPPostBinding;
import org.springframework.security.saml.processor.HTTPRedirectDeflateBinding;
import org.springframework.security.saml.processor.HTTPSOAP11Binding;
import org.springframework.security.saml.processor.SAMLBinding;
import org.springframework.security.saml.processor.SAMLProcessorImpl;
import org.springframework.security.saml.storage.EmptyStorageFactory;
import org.springframework.security.saml.trust.httpclient.TLSProtocolConfigurer;
import org.springframework.security.saml.userdetails.SAMLUserDetailsService;
import org.springframework.security.saml.util.VelocityFactory;
import org.springframework.security.saml.websso.ArtifactResolutionProfileImpl;
import org.springframework.security.saml.websso.SingleLogoutProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfile;
import org.springframework.security.saml.websso.WebSSOProfileConsumer;
import org.springframework.security.saml.websso.WebSSOProfileConsumerHoKImpl;
import org.springframework.security.saml.websso.WebSSOProfileConsumerImpl;
import org.springframework.security.saml.websso.WebSSOProfileECPImpl;
import org.springframework.security.saml.websso.WebSSOProfileImpl;
import org.springframework.security.saml.websso.WebSSOProfileOptions;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;

import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.rest.filter.SamlFilter;

@Configurable
public class SamlBootstrap {
	private static final LogUtil logger = LogUtil.getLogger(SamlBootstrap.class);

	private static final String LOGIN_URL = "/saml/login/**";

	private static final String SSO_URL = "/saml/SSO/**";

	private static final String METADATA_URL = "/saml/metadata/**";

	private static final String LOGOUT_URL = "/saml/logout/**";

	private static final String SINGLE_LOGOUT_URL = "/saml/SingleLogout/**";

	private static final String DEFAULT_TARGET_URL = "/#/home";

	@Autowired
	private SAMLUserDetailsService userSvc;

	@Autowired
	private ProviderManager authManager;

	@Autowired
	private SamlFilter samlFilter;

	private SimpleUrlAuthenticationSuccessHandler successRedirectHandler;

	private Map<String, String> samlProps;

	private SAMLDefaultLogger samlLogger;

	public SamlBootstrap(SimpleUrlAuthenticationSuccessHandler successRedirectHandler, Map<String, String> props) {
		this.successRedirectHandler = successRedirectHandler;
		samlProps = props;
	}

	public void initialize() {
		try {
			PaosBootstrap.bootstrap();
			setSignatureMethod();
			setDigestMethod();
			setMetadataKeyInfoGenerator();
			setupSamlFilterChain();

			authManager.getProviders().removeIf(provider -> provider.supports(SAMLAuthenticationToken.class));
			authManager.getProviders().add(getSamlAuthenticationProvider());
		} catch (Exception e) {
			logger.error("Error configuring SAML authentication provider - " + e.getMessage(), e);
			throw new RuntimeException("Error configuring SAML authentication provider - " + e.getMessage(), e);
		}
	}

	private void setSignatureMethod() {
		String signature = samlProps.get("signature");
		if (StringUtils.isBlank(signature)) {
			return;
		}

		Pair<String, String> method = SIGNATURE_ALGOS.get(signature.toLowerCase());
		if (method == null) {
			throw new RuntimeException("Unknown signature method - " + signature.toLowerCase());
		}

		BasicSecurityConfiguration config = (BasicSecurityConfiguration) Configuration.getGlobalSecurityConfiguration();
		config.registerSignatureAlgorithmURI(method.first(), method.second());
	}

	private void setDigestMethod() {
		String digest = samlProps.get("digest");
		if (StringUtils.isBlank(digest)) {
			return;
		}

		String method = DIGEST_ALGOS.get(digest.toLowerCase());
		if (method == null) {
			throw new RuntimeException("Unknown digest method - " + digest.toLowerCase());
		}

		BasicSecurityConfiguration config = (BasicSecurityConfiguration) Configuration.getGlobalSecurityConfiguration();
		config.setSignatureReferenceDigestMethod(method);
	}

	private void setMetadataKeyInfoGenerator() {
		NamedKeyInfoGeneratorManager manager = Configuration.getGlobalSecurityConfiguration().getKeyInfoGeneratorManager();
		X509KeyInfoGeneratorFactory generator = new X509KeyInfoGeneratorFactory();
		generator.setEmitEntityCertificate(true);
		generator.setEmitEntityCertificateChain(true);
		manager.registerFactory(SAMLConstants.SAML_METADATA_KEY_INFO_GENERATOR, generator);
	}

	/**
	 * Define the security filter chain in order to support SSO Auth by using SAML 2.0
	 *
	 * @return Filter chain proxy
	 * @throws Exception
	 */
	private void setupSamlFilterChain() throws Exception {
		ParserPool parserPool = getParserPool();
		KeyManager keyManager = getKeyManager();
		CachingMetadataManager metadata = getMetadata(parserPool, keyManager);
		SAMLContextProviderImpl contextProvider = getContextProvider(metadata, keyManager);
		SAMLProcessorImpl processor = getProcessor(parserPool);
		SAMLEntryPoint samlEntryPoint = getSamlEntryPoint(contextProvider, processor, metadata);
		SAMLProcessingFilter samlWebSSOProcessingFilter = getSamlWebSSOProcessingFilter(contextProvider, processor);
		MetadataDisplayFilter metadataDisplayFilter = getMetadataDisplayFilter(contextProvider, metadata, keyManager);
		MetadataGenerator metadataGenerator = getMetadataGenerator(keyManager, samlEntryPoint, samlWebSSOProcessingFilter);
		MetadataGeneratorFilter metadataGenFilter = getMetadataGenFilter(metadataGenerator, metadata, metadataDisplayFilter);
		SAMLLogoutFilter logoutFilter = getLogoutFilter(contextProvider, processor, metadata);
		SAMLLogoutProcessingFilter logoutProcFilter = getLogoutProcFilter(contextProvider, processor, metadata);

		Map<String, Filter> filters = new HashMap<>();
		filters.put(LOGIN_URL, samlEntryPoint);
		filters.put(SSO_URL, samlWebSSOProcessingFilter);
		filters.put(METADATA_URL, metadataDisplayFilter);
		filters.put(LOGOUT_URL, logoutFilter);
		filters.put(SINGLE_LOGOUT_URL, logoutProcFilter);
		samlFilter.setFilterChain(metadataGenFilter, filters);
	}
	
	/*
	 * Create metadata for service provider on first request
	 */
	private MetadataGeneratorFilter getMetadataGenFilter(MetadataGenerator metadataGenerator, CachingMetadataManager metadata, 
			MetadataDisplayFilter metadataDisplayFilter) throws Exception {
		MetadataGeneratorFilter metadataGeneratorFilter = new MetadataGeneratorFilter(metadataGenerator);
		metadataGeneratorFilter.setManager(metadata);
		metadataGeneratorFilter.setDisplayFilter(metadataDisplayFilter);

		metadataGeneratorFilter.afterPropertiesSet();
		return metadataGeneratorFilter;
	}

	private MetadataGenerator getMetadataGenerator(KeyManager keyManager, SAMLEntryPoint samlEntryPoint, SAMLProcessingFilter samlWebSSOProcessingFilter) throws Exception {
		MetadataGenerator metadataGenerator = new MetadataGenerator();
		metadataGenerator.setEntityId(samlProps.get("entityId"));
		metadataGenerator.setEntityBaseURL(getAppUrl());
		metadataGenerator.setKeyManager(keyManager);
		metadataGenerator.setSamlEntryPoint(samlEntryPoint);
		metadataGenerator.setSamlWebSSOFilter(samlWebSSOProcessingFilter);
		metadataGenerator.setExtendedMetadata(getExtendedMetadata());

		return metadataGenerator;
	}

	/*
	 * SAML Authentication Provider responsible for validating of received SAML messages
	 */
	private SAMLAuthenticationProvider getSamlAuthenticationProvider() throws ServletException {
		SAMLAuthenticationProvider samlAuthenticationProvider = new SAMLAuthenticationProvider();
		samlAuthenticationProvider.setUserDetails(userSvc);
		samlAuthenticationProvider.setForcePrincipalAsString(false);
		samlAuthenticationProvider.setConsumer(getWebSSOprofileConsumer());
		samlAuthenticationProvider.setHokConsumer(getHokWebSSOprofileConsumer());
		samlAuthenticationProvider.setSamlLogger(getSamlLogger());
		samlAuthenticationProvider.afterPropertiesSet();
		return samlAuthenticationProvider;
	}

	/*
	 * Entry point to initialize authentication, default values taken from properties file
	 */
	private SAMLEntryPoint getSamlEntryPoint(SAMLContextProviderImpl contextProvider, SAMLProcessorImpl processor, CachingMetadataManager metadata) throws Exception {
		SAMLEntryPoint samlEntryPoint = new SAMLEntryPoint();
		samlEntryPoint.setMetadata(metadata);
		samlEntryPoint.setDefaultProfileOptions(getDefaultWebSSOProfileOptions());
		samlEntryPoint.setWebSSOprofile(getWebSSOprofile(processor, metadata));
		samlEntryPoint.setWebSSOprofileECP(getEcpprofile(processor, metadata));
		samlEntryPoint.setSamlLogger(getSamlLogger());
		samlEntryPoint.setContextProvider(contextProvider);
		samlEntryPoint.setSamlDiscovery(getSamlIDPDiscovery());

		samlEntryPoint.afterPropertiesSet();
		return samlEntryPoint;
	}

	// Processing filter for WebSSO profile messages
	private SAMLProcessingFilter getSamlWebSSOProcessingFilter(SAMLContextProviderImpl contextProvider, SAMLProcessorImpl processor) throws Exception {
		SAMLProcessingFilter samlWebSSOProcessingFilter = new SAMLProcessingFilter();
		samlWebSSOProcessingFilter.setSAMLProcessor(processor);
		samlWebSSOProcessingFilter.setContextProvider(contextProvider);
		samlWebSSOProcessingFilter.setAuthenticationManager(authManager);
		samlWebSSOProcessingFilter.setAuthenticationSuccessHandler(getSuccessRedirectHandler());
		samlWebSSOProcessingFilter.setAuthenticationFailureHandler(getAuthenticationFailureHandler());

		samlWebSSOProcessingFilter.afterPropertiesSet();
		return samlWebSSOProcessingFilter;
	}

	private MetadataDisplayFilter getMetadataDisplayFilter(SAMLContextProviderImpl contextProvider, CachingMetadataManager metadata, KeyManager keyManager) throws Exception {
		MetadataDisplayFilter metadataDisplayFilter = new MetadataDisplayFilter();
		metadataDisplayFilter.setManager(metadata);
		metadataDisplayFilter.setKeyManager(keyManager);
		metadataDisplayFilter.setContextProvider(contextProvider);

		metadataDisplayFilter.afterPropertiesSet();
		return metadataDisplayFilter;
	}

	/*
	 * SAML 2.0 ECP profile
	 */
	private WebSSOProfileECPImpl getEcpprofile(SAMLProcessorImpl processor, CachingMetadataManager metadata) throws Exception {
		WebSSOProfileECPImpl ecpprofile = new WebSSOProfileECPImpl();
		ecpprofile.setProcessor(processor);
		ecpprofile.setMetadata(metadata);
		return ecpprofile;
	}

	/*
	 * SAML 2.0 Web SSO profile
	 */
	private WebSSOProfile getWebSSOprofile(SAMLProcessorImpl processor, CachingMetadataManager metadata) throws Exception {
		return new WebSSOProfileImpl(processor, metadata);
	}

	private SAMLProcessorImpl getProcessor(ParserPool parserPool) {
		VelocityEngine velocityEngine = VelocityFactory.getEngine();
		HTTPSOAP11Binding soapBinding = getSoapBinding(parserPool);

		Collection<SAMLBinding> bindings = new ArrayList<SAMLBinding>();
		bindings.add(getPostBinding(parserPool, velocityEngine));
		bindings.add(getRedirectDeflateBinding(parserPool));
		bindings.add(soapBinding);
		bindings.add(getArtifactBinding(soapBinding, parserPool, velocityEngine));
		bindings.add(getPAOS11Binding(parserPool));

		return new SAMLProcessorImpl(bindings);
	}

	private SAMLContextProviderImpl getContextProvider(CachingMetadataManager metadata, KeyManager keyManager)
	throws Exception {
		URL url = new URL(getAppUrl());

		SAMLContextProviderLB ctxProvider = new SAMLContextProviderLB();
		ctxProvider.setKeyManager(keyManager);
		ctxProvider.setMetadata(metadata);
		ctxProvider.setScheme(url.getProtocol());
		ctxProvider.setServerName(url.getHost());
		ctxProvider.setServerPort(url.getPort());
		ctxProvider.setContextPath(url.getPath());

		String includeServerPortInReqUrl = samlProps.get("includeServerPortInReqUrl");
		if (StringUtils.isNotBlank(includeServerPortInReqUrl)) {
			ctxProvider.setIncludeServerPortInRequestURL(Boolean.parseBoolean(includeServerPortInReqUrl));
		}

		String validateInResponseTo = samlProps.get("validateInResponseTo");
		if ("false".equalsIgnoreCase(validateInResponseTo)) {
			ctxProvider.setStorageFactory(new EmptyStorageFactory());
		}

		ctxProvider.afterPropertiesSet();
		return ctxProvider;
	}

	/*
	 * IDP Metadata configuration - paths to metadata of IDPs in circle of trust is here
	 */
	private CachingMetadataManager getMetadata(ParserPool parserPool, KeyManager keyManager) throws Exception {
		List<MetadataProvider> providers = new ArrayList<>();
		MetadataProvider provider = getIdpExtendedMetadataProvider(parserPool);
		if (provider != null) {
			providers.add(provider);
		}

		ExtendedMetadataDelegate spMetadataProvider = getSpExtendedMetadataProvider(parserPool);
		if (spMetadataProvider != null) {
			providers.add(spMetadataProvider);
		}

		CachingMetadataManager metadata = new CachingMetadataManager(providers);
		metadata.setDefaultIDP(samlProps.get("defaultIdp"));
		metadata.setKeyManager(keyManager);
		metadata.setTLSConfigurer(getTlsProtocolConfigurer(keyManager));

		metadata.afterPropertiesSet();
		return metadata;
	}

	private TLSProtocolConfigurer getTlsProtocolConfigurer(KeyManager keyManager) throws Exception {
		TLSProtocolConfigurer tlsProtocolConfigurer =  new TLSProtocolConfigurer();
		tlsProtocolConfigurer.setKeyManager(keyManager);
		tlsProtocolConfigurer.afterPropertiesSet();
		return tlsProtocolConfigurer;
	}

	/*
	 * Logger for SAML messages and events
	 */
	private SAMLDefaultLogger getSamlLogger() {
		if (samlLogger == null) {
			samlLogger = new SAMLDefaultLogger();
		}
		return samlLogger;
	}

	/*
	 * SAML 2.0 Holder-of-Key WebSSO Assertion Consumer
	 */
	private WebSSOProfileConsumerHoKImpl getHokWebSSOprofileConsumer() {
		return new WebSSOProfileConsumerHoKImpl();
	}

	/*
	 * SAML 2.0 WebSSO Assertion Consumer
	 */
	private WebSSOProfileConsumer getWebSSOprofileConsumer() {
		long maxAuthAge = 24;
		if (samlProps != null) {
			String maxAuthAgeStr = samlProps.getOrDefault("maxAuthAge", "24");

			try {
				maxAuthAge = Long.parseLong(maxAuthAgeStr);
			} catch (Exception e) {
				logger.error("Invalid max. authentication age: " + maxAuthAgeStr + ". Defaulting to 24 hours", e);
			}
		}

		WebSSOProfileConsumerImpl consumer = new WebSSOProfileConsumerImpl();
		consumer.setMaxAuthenticationAge(maxAuthAge * 60 * 60);
		return consumer;
	}

	private HTTPArtifactBinding getArtifactBinding(HTTPSOAP11Binding soapBinding, ParserPool parserPool, VelocityEngine velocityEngine) {
		final ArtifactResolutionProfileImpl artifactResolutionProfile = new ArtifactResolutionProfileImpl(getHttpClient());
		artifactResolutionProfile.setProcessor(new SAMLProcessorImpl(soapBinding));

		return new HTTPArtifactBinding(parserPool, velocityEngine, artifactResolutionProfile);
	}

	private HTTPSOAP11Binding getSoapBinding(ParserPool parserPool) {
		return new HTTPSOAP11Binding(parserPool);
	}

	private HTTPPAOS11Binding getPAOS11Binding(ParserPool parserPool) {
		return new HTTPPAOS11Binding(parserPool);
	}

	private HTTPRedirectDeflateBinding getRedirectDeflateBinding(ParserPool parserPool) {
		return new HTTPRedirectDeflateBinding(parserPool);
	}

	private HTTPPostBinding getPostBinding(ParserPool parserPool, VelocityEngine velocityEngine) {
		return new HTTPPostBinding(parserPool, velocityEngine);
	}

	/*
	 * Central storage of cryptographic keys
	 */
	private KeyManager getKeyManager() {
		DefaultResourceLoader loader = new FileSystemResourceLoader();
		Resource storeFile = loader.getResource("file:" + samlProps.get("keyStoreFilePath"));
		Map<String, String> keyPasswords = new HashMap<String, String>();
		keyPasswords.put(samlProps.get("keyAlias"), samlProps.get("keyPasswd"));

		return new JKSKeyManager(storeFile, samlProps.get("keyStorePasswd"), keyPasswords, samlProps.get("keyAlias"));
	}

	private HttpClient getHttpClient() {
		return new HttpClient(new MultiThreadedHttpConnectionManager());
	}

	/*
	 * XML parser pool needed for OpenSAML parsing
	 */
	@SuppressWarnings("unchecked")
	private StaticBasicParserPool getParserPool() {
		try {
			StaticBasicParserPool parserPool = new StaticBasicParserPool();
			parserPool.setBuilderFeatures(Collections.singletonMap("http://apache.org/xml/features/dom/defer-node-expansion", false));
			parserPool.initialize();
			return parserPool;
		} catch (XMLParserException e) {
			throw new RuntimeException("Error while initilizing parse pool", e);
		}
	}

	private WebSSOProfileOptions getDefaultWebSSOProfileOptions() {
		WebSSOProfileOptions webSSOProfileOptions = new WebSSOProfileOptions();
		webSSOProfileOptions.setIncludeScoping(false);
		return webSSOProfileOptions;
	}

	private SAMLDiscovery getSamlIDPDiscovery() {
		SAMLDiscovery idpDiscovery = new SAMLDiscovery();
		idpDiscovery.setIdpSelectionPath("/saml/idpSelection");
		return idpDiscovery;
	}

	/*
	 * Handler deciding where to redirect user after successful login
	 */
	private SimpleUrlAuthenticationSuccessHandler getSuccessRedirectHandler() {
		successRedirectHandler.setDefaultTargetUrl(getAppUrl() + DEFAULT_TARGET_URL);
		return successRedirectHandler;
	}

	/*
	 * Handler deciding where to redirect user after failed login
	 */
	private SimpleUrlAuthenticationFailureHandler getAuthenticationFailureHandler() {
		SimpleUrlAuthenticationFailureHandler failureHandler = new SimpleUrlAuthenticationFailureHandler();
		failureHandler.setDefaultFailureUrl(getAppUrl());
		return failureHandler;
	}

	@SuppressWarnings("deprecation")
	private MetadataProvider getIdpExtendedMetadataProvider(ParserPool parserPool)
	throws Exception {
		String metadataPath = samlProps.get("idpMetadataPath");
		String metadataUrl = samlProps.get("idpMetadataURL");
		if (StringUtils.isBlank(metadataPath) && StringUtils.isBlank(metadataUrl)) {
			return null;
		}

		AbstractMetadataProvider metadataProvider;
		if (StringUtils.isNotBlank(metadataPath)) {
			metadataProvider = new ResourceBackedMetadataProvider(new Timer(), new FilesystemResource(metadataPath));
		} else if (StringUtils.isNotBlank(metadataUrl)) {
			metadataProvider = new HTTPMetadataProvider(metadataUrl, 15000);
		} else {
			throw new RuntimeException("Either IdP metadata URL or file system path should be configured");
		}

		metadataProvider.setParserPool(parserPool);
		return metadataProvider;
	}

	private ExtendedMetadataDelegate getSpExtendedMetadataProvider(ParserPool parserPool) throws MetadataProviderException, ResourceException {
		String spMetadataPath = samlProps.get("spMetadataPath");
		if (StringUtils.isBlank(spMetadataPath)) {
			return null;
		}

		ResourceBackedMetadataProvider provider = new ResourceBackedMetadataProvider(new Timer(), new FilesystemResource(spMetadataPath));
		provider.setParserPool(parserPool);

		return new ExtendedMetadataDelegate(provider, getExtendedMetadata());
	}

	private SAMLLogoutFilter getLogoutFilter(SAMLContextProvider ctxtProvider, SAMLProcessorImpl processor, CachingMetadataManager metadata) {
		SimpleUrlLogoutSuccessHandler successHandler = new SimpleUrlLogoutSuccessHandler();
		successHandler.setDefaultTargetUrl(ConfigUtil.getInstance().getAppUrl());
		successHandler.setAlwaysUseDefaultTargetUrl(true);

		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.setInvalidateHttpSession(true);
		logoutHandler.setClearAuthentication(true);

		LogoutHandler[] handlers = new LogoutHandler[] { logoutHandler };
		SAMLLogoutFilter filter = new SAMLLogoutFilter(successHandler, handlers, handlers);
		filter.setContextProvider(ctxtProvider);

		SingleLogoutProfileImpl profile = new SingleLogoutProfileImpl();
		profile.setProcessor(processor);
		profile.setMetadata(metadata);
		filter.setProfile(profile);
		filter.setSamlLogger(getSamlLogger());
		return filter;
	}

	private SAMLLogoutProcessingFilter getLogoutProcFilter(SAMLContextProvider ctxtProvider, SAMLProcessorImpl processor, CachingMetadataManager metadata) {
		SimpleUrlLogoutSuccessHandler successHandler = new SimpleUrlLogoutSuccessHandler();
		successHandler.setDefaultTargetUrl(ConfigUtil.getInstance().getAppUrl());
		successHandler.setAlwaysUseDefaultTargetUrl(true);

		SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();
		logoutHandler.setInvalidateHttpSession(true);
		logoutHandler.setClearAuthentication(true);

		SingleLogoutProfileImpl profile = new SingleLogoutProfileImpl();
		profile.setProcessor(processor);
		profile.setMetadata(metadata);

		SAMLLogoutProcessingFilter filter = new SAMLLogoutProcessingFilter(successHandler, logoutHandler);
		filter.setLogoutProfile(profile);
		filter.setSamlLogger(getSamlLogger());
		filter.setSAMLProcessor(processor);
		filter.setContextProvider(ctxtProvider);
		filter.setSamlLogger(getSamlLogger());
		return filter;
	}

	private ExtendedMetadata getExtendedMetadata() {
		ExtendedMetadata extendedMetadata = new ExtendedMetadata();
		extendedMetadata.setLocal(true);
		extendedMetadata.setSecurityProfile("metaiop");
		extendedMetadata.setSslSecurityProfile("pkix");
		extendedMetadata.setSslHostnameVerification("default");
		extendedMetadata.setSigningKey(samlProps.get("keyAlias"));
		extendedMetadata.setEncryptionKey(samlProps.get("keyAlias"));
		extendedMetadata.setRequireArtifactResolveSigned(false);
		extendedMetadata.setRequireLogoutRequestSigned(false);
		extendedMetadata.setRequireLogoutResponseSigned(false);
		extendedMetadata.setIdpDiscoveryEnabled(false);
		extendedMetadata.setSignMetadata(false);

		return extendedMetadata;
	}

	private String getAppUrl() {
		String appUrl = ConfigUtil.getInstance().getAppUrl();
		if (StringUtils.isBlank(appUrl)) {
			return StringUtils.EMPTY;
		}

		while (appUrl.endsWith("/")) {
			appUrl = appUrl.substring(0, appUrl.length() - 1);
		}

		return appUrl;
	}

	private static final Map<String, Pair<String, String>> SIGNATURE_ALGOS = new HashMap<String, Pair<String, String>>() {
		{
			put("dsa-sha1",     Pair.make("DSA", SignatureConstants.ALGO_ID_SIGNATURE_DSA_SHA1));
			put("rsa-sha1",     Pair.make("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA1));
			put("rsa-sha256",   Pair.make("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA256));
			put("rsa-sha384",   Pair.make("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA384));
			put("rsa-sha512",   Pair.make("RSA", SignatureConstants.ALGO_ID_SIGNATURE_RSA_SHA512));
			put("ecdsa-sha1",   Pair.make("EC", SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA1));
			put("ecdsa-sha256", Pair.make("EC", SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA256));
			put("ecdsa-sha384", Pair.make("EC", SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA384));
			put("ecdsa-sha512", Pair.make("EC", SignatureConstants.ALGO_ID_SIGNATURE_ECDSA_SHA512));
		}
	};

	private static final Map<String, String> DIGEST_ALGOS = new HashMap<String, String>() {
		{
			put("sha1", SignatureConstants.ALGO_ID_DIGEST_SHA1);
			put("sha256", SignatureConstants.ALGO_ID_DIGEST_SHA256);
			put("sha384", SignatureConstants.ALGO_ID_DIGEST_SHA384);
			put("sha512", SignatureConstants.ALGO_ID_DIGEST_SHA512);
		}
	};
}