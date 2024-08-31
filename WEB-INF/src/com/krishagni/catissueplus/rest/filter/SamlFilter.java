package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;


import com.krishagni.catissueplus.core.auth.domain.AuthCredential;
import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.auth.domain.AuthToken;
import com.krishagni.catissueplus.core.auth.events.TokenDetail;
import com.krishagni.catissueplus.core.auth.services.impl.UserAuthenticationServiceImpl;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;
import com.krishagni.catissueplus.core.common.util.LogUtil;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SamlFilter extends FilterChainProxy {
	private static final LogUtil logger = LogUtil.getLogger(SamlFilter.class);

	private static final String SHOW_ERROR = "/#/alert";

	private DaoFactory daoFactory;
	
	private UserAuthenticationServiceImpl authService;
	
	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setAuthService(UserAuthenticationServiceImpl authService) {
		this.authService = authService;
	}

	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) 
	throws IOException {
		String appUrl = ConfigUtil.getInstance().getAppUrl();
		HttpServletRequest httpReq = (HttpServletRequest) request;
		HttpServletResponse httpResp = (HttpServletResponse) response;

		try {
			boolean samlEnabled = enableSaml();
			boolean metadataReq = isMetadataReq(httpReq);
			boolean logoutReq = isLogoutReq(httpReq);
			if (samlEnabled && (metadataReq || logoutReq || !isAuthenticated(httpReq))) {
				String token = AuthUtil.getTokenFromCookie(httpReq);
				if (logoutReq) {
					setupAuthContext(token);
					cleanupCredentials(token);
				}

				if (!metadataReq) {
					AuthUtil.clearTokenCookie(httpReq, httpResp);
				}

				super.doFilter(request, response, chain);
			} else {
				httpResp.sendRedirect(appUrl + "/#/home");
			}
		} catch (UsernameNotFoundException use) {
			httpResp.sendRedirect(appUrl + SHOW_ERROR + "?redirectTo=login&type=danger&msg=" + use.getMessage());
		} catch (Exception e) {
			logger.error("Error doing SAML based authentication", e);
			httpResp.sendRedirect(appUrl + SHOW_ERROR + "?redirectTo=login&type=danger&msg=" + e.getMessage());
		}
	}
	
	@SuppressWarnings({"deprecation" })
	public void setFilterChain(Filter generatorFilter, Map<String, Filter> filters) {
		List<SecurityFilterChain> filterChains = new ArrayList<>();
		for (Map.Entry<String, Filter> entry : filters.entrySet()) {
			RequestMatcher matcher = new AntPathRequestMatcher(entry.getKey());
			List<Filter> chainFilters = Arrays.asList(generatorFilter, entry.getValue());
			filterChains.add(new DefaultSecurityFilterChain(matcher, chainFilters));
		}

		try {
			Field field = FilterChainProxy.class.getDeclaredField("filterChains");
			field.setAccessible(true);
			field.set(this, filterChains);
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	@PlusTransactional
	private boolean enableSaml() {
		boolean samlEnabled = ConfigUtil.getInstance().getBoolSetting("auth", "saml_enable", false);
		if (!samlEnabled) {
			return false;
		}

		//
		// TODO: This is assuming there will be only one SAML domain
		//
		AuthDomain domain = daoFactory.getAuthDao().getAuthDomainByType("saml");
		if (domain != null) {
			//
			// This intialises SAML auth provider
			//
			domain.getAuthProviderInstance();
		}

		return samlEnabled;
	}

	@PlusTransactional
	private void setupAuthContext(String token) {
		if (StringUtils.isBlank(token)) {
			return;
		}

		AuthCredential credential = daoFactory.getAuthDao().getCredentials(AuthUtil.decodeToken(token));
		if (credential == null) {
			return;
		}

		SecurityContextHolder.getContext().setAuthentication(new Authentication() {
			private static final long serialVersionUID = -6025550670210593212L;

			@Override
			public Collection<? extends GrantedAuthority> getAuthorities() {
				return null;
			}

			@Override
			public Object getCredentials() {
				return credential.getCredential();
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
			public void setAuthenticated(boolean b) throws IllegalArgumentException {

			}

			@Override
			public String getName() {
				return null;
			}
		});
	}

	@PlusTransactional
	private void cleanupCredentials(String token) {
		if (StringUtils.isBlank(token)) {
			return;
		}

		ResponseEvent.unwrap(authService.removeToken(RequestEvent.wrap(token)));
	}

	private boolean isAuthenticated(HttpServletRequest httpReq) {
		String authToken = AuthUtil.getTokenFromCookie(httpReq);
		if (authToken == null) {
			return false;
		}

		TokenDetail tokenDetail = new TokenDetail();
		tokenDetail.setToken(authToken);
		tokenDetail.setIpAddress(Utility.getRemoteAddress(httpReq));

		RequestEvent<TokenDetail> atReq = new RequestEvent<>(tokenDetail);
		ResponseEvent<AuthToken> atResp = authService.validateToken(atReq);
		return atResp.isSuccessful();
	}

	private boolean isMetadataReq(HttpServletRequest httpReq) {
		return httpReq != null && httpReq.getRequestURI().endsWith("/saml/metadata");
	}

	private boolean isLogoutReq(HttpServletRequest httpReq) {
		return httpReq != null && httpReq.getRequestURI().endsWith("/saml/logout");
	}
}
