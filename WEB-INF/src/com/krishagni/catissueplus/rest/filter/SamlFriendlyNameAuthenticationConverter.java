package com.krishagni.catissueplus.rest.filter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.opensaml.saml.saml2.core.Assertion;
import org.opensaml.saml.saml2.core.Attribute;
import org.opensaml.saml.saml2.core.AttributeStatement;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.saml2.provider.service.authentication.OpenSaml5AuthenticationProvider;
import org.springframework.security.saml2.provider.service.authentication.Saml2AssertionAuthentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2Authentication;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertion;
import org.springframework.security.saml2.provider.service.authentication.Saml2ResponseAssertionAccessor;

public class SamlFriendlyNameAuthenticationConverter implements Converter<OpenSaml5AuthenticationProvider.ResponseToken, AbstractAuthenticationToken> {

	private final Converter<OpenSaml5AuthenticationProvider.ResponseToken, Saml2Authentication> defaultConverter =
		new OpenSaml5AuthenticationProvider.ResponseAuthenticationConverter();

	@Override
	public AbstractAuthenticationToken convert(OpenSaml5AuthenticationProvider.ResponseToken token) {
		Saml2AssertionAuthentication authentication = (Saml2AssertionAuthentication) defaultConverter.convert(token);
		Saml2ResponseAssertionAccessor assertion = authentication.getCredentials();
		Map<String, List<Object>> attributes = new LinkedHashMap<>(assertion.getAttributes());


		addFriendlyNameAliases(token.getResponse().getAssertions(), attributes);
		Saml2ResponseAssertion expandedAssertion = Saml2ResponseAssertion
			.withResponseValue(assertion.getResponseValue())
			.nameId(assertion.getNameId())
			.sessionIndexes(assertion.getSessionIndexes())
			.attributes(attributes)
			.build();

		return new Saml2AssertionAuthentication(
			authentication.getPrincipal(),
			expandedAssertion,
			authentication.getAuthorities(),
			authentication.getRelyingPartyRegistrationId()
		);
	}

	private void addFriendlyNameAliases(List<Assertion> assertions, Map<String, List<Object>> attributes) {
		if (assertions == null || assertions.isEmpty()) {
			return;
		}

		for (AttributeStatement statement : assertions.get(0).getAttributeStatements()) {
			for (Attribute attribute : statement.getAttributes()) {
				String friendlyName = attribute.getFriendlyName();
				if (StringUtils.isBlank(friendlyName)) {
					continue;
				}

				List<Object> values = attributes.get(attribute.getName());
				if (values != null) {
					attributes.putIfAbsent(friendlyName, values);
				}
			}
		}
	}
}
