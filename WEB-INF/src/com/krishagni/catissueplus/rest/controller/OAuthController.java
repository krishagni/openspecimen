package com.krishagni.catissueplus.rest.controller;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.auth.services.OAuthService;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.ConfigUtil;

@Controller
@RequestMapping("/oauth")
public class OAuthController {

	@Autowired
	private OAuthService oAuthService;

	@RequestMapping(method = RequestMethod.GET, value="/login/{domainId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public void redirectToIdp(@PathVariable("domainId") Long id, HttpServletResponse httpResp)
	throws IOException {
		httpResp.sendRedirect(oAuthService.getAuthorizeUrl(id));
	}

	@RequestMapping(method = RequestMethod.GET, value="/callback")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ResponseEntity<?> handleCallback(
		@RequestParam(value = "code")
		String code,

		@RequestParam(value = "state")
		String state,

		HttpServletRequest httpReq,

		HttpServletResponse httpResp)
	throws IOException {
		String redirectUrl = null;

		try {
			String jwt = oAuthService.exchangeCodeForToken(httpReq, httpResp, state, code);
			oAuthService.generateAppToken(httpReq, httpResp, jwt);
			redirectUrl = homeUrl();
		} catch (OpenSpecimenException ose) {
			redirectUrl = errorUrl(ose.getMessage());
		}

		return ResponseEntity.status(HttpStatus.FOUND)
			.location(URI.create(redirectUrl))
			.build();
	}

	@RequestMapping(method = RequestMethod.POST, value="/validate-token")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> authenticate(HttpServletRequest httpReq, HttpServletResponse httpResp, @RequestBody Map<String, Object> tokenDetails) {
		try {
			String jwt = (String) tokenDetails.get("jwt");
			String appToken = oAuthService.generateAppToken(httpReq, httpResp, jwt);
			return Collections.singletonMap("token", appToken);
		} catch (Exception e) {
			AuthUtil.clearTokenCookie(httpReq, httpResp);
			throw e;
		}
	}

	private String errorUrl(String message) {
		message = URLEncoder.encode(message, StandardCharsets.UTF_8);
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
