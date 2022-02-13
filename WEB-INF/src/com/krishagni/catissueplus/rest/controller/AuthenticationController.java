
package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.auth.events.LoginDetail;
import com.krishagni.catissueplus.core.auth.services.UserAuthenticationService;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import ua_parser.Client;
import ua_parser.Device;
import ua_parser.OS;
import ua_parser.Parser;
import ua_parser.UserAgent;

@Controller
@RequestMapping("/sessions")
public class AuthenticationController {

	@Autowired
	private UserAuthenticationService userAuthService;

	@Autowired
	private HttpServletRequest httpReq;

	@RequestMapping(method=RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> authenticate(@RequestBody LoginDetail loginDetail, HttpServletResponse httpResp) {
		String ua = httpReq.getHeader("user-agent");
		if (StringUtils.isNotBlank(ua)) {
			Parser parser = new Parser();
			Client client = parser.parse(ua);
			if (client.device != Device.OTHER || client.os != OS.OTHER || client.userAgent != UserAgent.OTHER) {
				loginDetail.setDeviceDetails(parser.parse(ua).toString());
			}
		}

		loginDetail.setIpAddress(Utility.getRemoteAddress(httpReq));
		loginDetail.setApiUrl(httpReq.getRequestURI());
		loginDetail.setRequestMethod(RequestMethod.POST.name());

		ResponseEvent<Map<String, Object>> resp = userAuthService.authenticateUser(new RequestEvent<>(loginDetail));
		if (!resp.isSuccessful()) {
			AuthUtil.clearTokenCookie(httpReq, httpResp);
			throw resp.getError();
		}

		String authToken = (String)resp.getPayload().get("token");
		AuthUtil.setTokenCookie(httpReq, httpResp, authToken);

		User user = (User) resp.getPayload().get("user");
		Map<String, Object> detail = new HashMap<>();
		if (user == null) {
			detail.putAll(resp.getPayload());
		} else {
			detail.put("id", user.getId());
			detail.put("firstName", user.getFirstName());
			detail.put("lastName", user.getLastName());
			detail.put("loginName", user.getLoginName());
			detail.put("token", authToken);
			detail.put("admin", user.isAdmin());
			detail.put("instituteAdmin", user.isInstituteAdmin());
		}

		return detail;
	}

	@RequestMapping(method=RequestMethod.DELETE)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> delete(HttpServletResponse httpResp) {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		String token = (String) auth.getCredentials();
		String status = ResponseEvent.unwrap(userAuthService.removeToken(RequestEvent.wrap(token)));
		AuthUtil.clearTokenCookie(httpReq, httpResp);
		return Collections.singletonMap("Status", status);
	}

	@RequestMapping(method=RequestMethod.POST, value="/refresh-cookie")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> resetCookie(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		AuthUtil.resetTokenCookie(httpReq, httpResp);
		return Collections.singletonMap("Status", "Success");
	}

	@RequestMapping(method=RequestMethod.POST, value="/impersonate")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> impersonate(HttpServletRequest httpReq, HttpServletResponse httpResp, @RequestBody Map<String, Object> input) {
		Map<String, Object> token = ResponseEvent.unwrap(userAuthService.impersonate(RequestEvent.wrap(input)));
		AuthUtil.setImpersonateTokenCookie(httpReq, httpResp, (String) token.get("impersonateUserToken"));
		return token;
	}

	@RequestMapping(method=RequestMethod.DELETE, value="/impersonate")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Object> impersonate(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		AuthUtil.setImpersonateTokenCookie(httpReq, httpResp, null);
		return Collections.singletonMap("impersonateUserToken", null);
	}
}
