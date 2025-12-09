package com.krishagni.catissueplus.core.auth.services.impl;

import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.security.saml2.core.Saml2X509Credential;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrations;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthDomainSavedEvent;
import com.krishagni.catissueplus.core.auth.domain.AuthProvider;
import com.krishagni.catissueplus.core.auth.domain.factory.AuthProviderErrorCode;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

public class SamlIdpRegistrationRepository implements RelyingPartyRegistrationRepository, ApplicationListener<AuthDomainSavedEvent> {
	private Map<String, RelyingPartyRegistration> idpRegCache = new ConcurrentHashMap<>();

	private Map<Long, String> idpNames = new ConcurrentHashMap<>();

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public RelyingPartyRegistration findByRegistrationId(String idpName) {
		RelyingPartyRegistration repo = idpRegCache.get(idpName);
		if (repo == null) {
			repo = loadIdpMetadata(idpName);
		}

		return repo;
	}

	@Override
	public synchronized void onApplicationEvent(AuthDomainSavedEvent event) {
		AuthDomain domain = event.getEventData();
		String name = idpNames.remove(domain.getId());
		if (StringUtils.isNotBlank(name)) {
			idpRegCache.remove(name);
		}
	}

	@PlusTransactional
	private synchronized RelyingPartyRegistration loadIdpMetadata(String idpName) {
		AuthDomain domain = daoFactory.getAuthDao().getAuthDomainByName(idpName);
		if (domain == null) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.DOMAIN_NOT_FOUND, idpName);
		}

		AuthProvider provider = domain.getAuthProvider();
		if (!"saml".equals(provider.getAuthType())) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.NOT_SAML, idpName);
		}

		Map<String, String> props = provider.getProps();
		String metadataPath = props.get("idpMetadataPath");
		String metadataUrl = props.get("idpMetadataURL");
		if (StringUtils.isBlank(metadataPath) && StringUtils.isBlank(metadataUrl)) {
			throw OpenSpecimenException.userError(AuthProviderErrorCode.IDP_METADATA_NOT_SPECIFIED, idpName);
		}

		try {
			Pair<Saml2X509Credential, Saml2X509Credential> spCredentials = getSigningCredential(props);
			RelyingPartyRegistration.Builder builder = RelyingPartyRegistrations
				.fromMetadataLocation(StringUtils.isNotBlank(metadataPath) ? "file://" + metadataPath : metadataUrl)
				.registrationId(idpName)
				.signingX509Credentials(credentials -> credentials.add(spCredentials.first()))
				.decryptionX509Credentials(credentials -> credentials.add(spCredentials.second()))
				.entityId(props.get("entityId"));

			if (domain.isLegacySaml()) {
				builder.assertionConsumerServiceLocation(url("saml/SSO"));
			}

			RelyingPartyRegistration registration = builder.build();
			idpRegCache.put(idpName, registration);
			idpNames.put(domain.getId(), idpName);
			return registration;
		} catch (Exception e) {
			throw new RuntimeException("Error registering the IdP: " + idpName, e);
		}
	}

	private Pair<Saml2X509Credential, Saml2X509Credential> getSigningCredential(Map<String, String> props)
	throws Exception {
		KeyStore keyStore = getKeyStore(props.get("keyStoreFilePath"), props.get("keyStorePasswd"));
		X509Certificate spCertificate = getCertificate(keyStore, props.get("keyAlias"));
		PrivateKey spPrivateKey = getPrivateKey(keyStore, props.get("keyAlias"), props.get("keyPasswd"));

		Saml2X509Credential signingCredential = new Saml2X509Credential(spPrivateKey, spCertificate, Saml2X509Credential.Saml2X509CredentialType.SIGNING);
		Saml2X509Credential decryptionCredential = new Saml2X509Credential(spPrivateKey, spCertificate, Saml2X509Credential.Saml2X509CredentialType.DECRYPTION);
		return Pair.make(signingCredential, decryptionCredential);
	}

	private KeyStore getKeyStore(String storeFilePath, String storePasswd) throws Exception {
		DefaultResourceLoader loader = new FileSystemResourceLoader();
		Resource storeFile = loader.getResource("file:" + storeFilePath);

		KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
		keyStore.load(storeFile.getInputStream(), storePasswd.toCharArray());
		return keyStore;
	}

	public PrivateKey getPrivateKey(KeyStore keyStore, String alias, String password) throws Exception {
		// Retrieve the key entry using the alias and password
		KeyStore.PrivateKeyEntry pvtKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, new KeyStore.PasswordProtection(password.toCharArray()));
		if (pvtKeyEntry == null) {
			throw new RuntimeException("Private key entry not found for alias: " + alias);
		}

		return pvtKeyEntry.getPrivateKey();
	}

	public static X509Certificate getCertificate(KeyStore keyStore, String alias) throws Exception {
		// Retrieve the certificate from the keystore entry
		Certificate cert = keyStore.getCertificate(alias);

		if (cert == null) {
			throw new RuntimeException("Certificate not found for alias: " + alias);
		}

		return (X509Certificate) cert;
	}

	private String url(String path) {
		String appUrl = ConfigUtil.getInstance().getAppUrl();
		if (!appUrl.endsWith("/")) {
			appUrl = appUrl + "/";
		}

		appUrl += path;
		return appUrl;
	}
}
