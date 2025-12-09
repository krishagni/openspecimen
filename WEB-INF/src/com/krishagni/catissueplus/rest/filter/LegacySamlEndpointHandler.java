package com.krishagni.catissueplus.rest.filter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.filter.GenericFilterBean;

import com.krishagni.catissueplus.core.auth.domain.AuthDomain;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.PlusTransactional;


public class LegacySamlEndpointHandler extends GenericFilterBean {
	//
	// Mapping of Purpose -> Legacy endpoint
	//
	private static final Map<String, AntPathRequestMatcher> REQ_MATCHERS = new HashMap<>() {
		{
			put("login",  new AntPathRequestMatcher("/saml/login"));
			put("acs",    new AntPathRequestMatcher("/saml/SSO/**"));
			put("logout", new AntPathRequestMatcher("/saml/SingleLogout/**"));
		}
	};

	private DaoFactory daoFactory;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
		HttpServletRequest httpReq = (HttpServletRequest) request;
		for (Map.Entry<String, AntPathRequestMatcher> matcherEntry : REQ_MATCHERS.entrySet()) {
			if (matcherEntry.getValue().matches(httpReq)) {
				String targetUrl = getTargetUrl(matcherEntry.getKey());
				if (StringUtils.isBlank(targetUrl)) {
					continue;
				}

				RequestDispatcher dispatcher = httpReq.getRequestDispatcher(targetUrl);
				if (dispatcher != null) {
					dispatcher.forward(request, response);
					return;
				}
			}
		}

		chain.doFilter(request, response);
	}

	private String getTargetUrl(final String urlType) {
		if (StringUtils.isBlank(urlType)) {
			return null;
		}

		String idpId = getLegacySamlDomain();
		if (StringUtils.isBlank(idpId) ) {
			return null;
		}

		return switch (urlType) {
			case "login" -> "/saml2/authenticate/" + idpId;
			case "acs" -> "/login/saml2/sso/" + idpId;
			case "logout" -> "/logout/saml2/slo/" + idpId;
			default -> null;
		};
	}

	@PlusTransactional
	private String getLegacySamlDomain() {
		AuthDomain domain = daoFactory.getAuthDao().getLegacySamlAuthDomain();
		return domain != null ? domain.getName() : null;
	}
}
