package com.krishagni.catissueplus.rest.filter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.saml2.provider.service.metadata.OpenSamlMetadataResolver;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistration;
import org.springframework.security.saml2.provider.service.registration.RelyingPartyRegistrationRepository;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationFilter;
import org.springframework.security.saml2.provider.service.servlet.filter.Saml2WebSsoAuthenticationRequestFilter;
import org.springframework.security.saml2.provider.service.web.DefaultRelyingPartyRegistrationResolver;
import org.springframework.security.saml2.provider.service.web.Saml2MetadataFilter;
import org.springframework.security.web.SecurityFilterChain;

import com.krishagni.catissueplus.core.auth.services.impl.SamlAuthenticationHandler;

@Configuration
@EnableWebSecurity
public class SamlSecurityConfig {

	private final RelyingPartyRegistrationRepository relyingPartyRegistrationRepository;

	public SamlSecurityConfig(RelyingPartyRegistrationRepository relyingPartyRegistrationRepository) {
		this.relyingPartyRegistrationRepository = relyingPartyRegistrationRepository;
	}

	@Bean
	public Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver() {
		// This bean is available now because it is in a dedicated @Configuration
		return new DefaultRelyingPartyRegistrationResolver(relyingPartyRegistrationRepository);
	}

	@Bean
	public Saml2MetadataFilter saml2MetadataFilter(Converter<HttpServletRequest, RelyingPartyRegistration> relyingPartyRegistrationResolver) {
		OpenSamlMetadataResolver metadataResolver = new OpenSamlMetadataResolver();
		return new Saml2MetadataFilter(relyingPartyRegistrationResolver, metadataResolver);
	}

	@Bean(name = "saml2Filter")
	public SecurityFilterChain filterChain(HttpSecurity http, Saml2MetadataFilter saml2MetadataFilter, LegacySamlEndpointHandler legacySamlEndpointHandler, SamlAuthenticationHandler authHandler)
	throws Exception {
		http.authorizeHttpRequests(
			authorize -> authorize
				.antMatchers("/login/saml2/**", "/saml2/**", "/logout/saml2/**").permitAll() // Allow access to authenticate / login
				.anyRequest().authenticated()
			)
			.csrf(csrf -> csrf.ignoringAntMatchers("/saml/**", "/login/saml2/sso/**", "/saml2/**", "/logout/saml2/slo/**"))
			.saml2Login(
				saml2 -> saml2
					.relyingPartyRegistrationRepository(this.relyingPartyRegistrationRepository)
					.successHandler(authHandler)
					.failureHandler(authHandler)
			)
			.saml2Logout(Customizer.withDefaults())
			.addFilterBefore(legacySamlEndpointHandler, Saml2WebSsoAuthenticationRequestFilter.class)
			.addFilterBefore(saml2MetadataFilter, Saml2WebSsoAuthenticationFilter.class);
		// ... potentially .csrf().disable() or .exceptionHandling()
		return http.build();
	}
}