package com.krishagni.catissueplus.core.common.util;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Base64;
import java.util.Collection;
import java.util.TimeZone;

import javax.servlet.ServletContext;
import javax.servlet.SessionCookieConfig;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

import com.krishagni.catissueplus.core.administrative.domain.Institute;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.common.domain.ConfigErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;

public class AuthUtil {
	private static final LogUtil logger = LogUtil.getLogger(AuthUtil.class);

	private static final String OS_AUTH_TOKEN_HDR = "X-OS-API-TOKEN";

	private static final String OS_IMP_USER_HDR = "X-OS-IMPERSONATE-USER";

	private static final String DEF_SESSION_COOKIE_NAME = "JSESSIONID";

	public static Authentication getAuth() {
		return SecurityContextHolder.getContext().getAuthentication();
	}

	public static boolean isSignedIn() {
		return getCurrentUser() != null;
	}

	public static String getAuthToken() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}

		String encodedToken = (String) auth.getCredentials();
		if (encodedToken == null) {
			return null;
		}

		return decodeToken(encodedToken);
	}
	
	public static User getCurrentUser() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null) {
			return null;
		}
		
		return (User)auth.getPrincipal();
	}

	public static Institute getCurrentUserInstitute() {
		User user = AuthUtil.getCurrentUser();
		return (user != null) ? user.getInstitute() : null;
	}
	
	public static String getRemoteAddr() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof UserAuthToken token) {
			return token.getIpAddress();
		}

		return null;
	}

	public static TimeZone getUserTimeZone() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (!(auth instanceof UserAuthToken token)) {
			return null;
		}

		User currUser = getCurrentUser();
		String timeZone = currUser != null && currUser.getTimeZone() != null ? currUser.getTimeZone() : token.getTimeZone();
		return toTimeZone(timeZone);
	}

	public static boolean isImpersonated() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth instanceof UserAuthToken token) {
			return token.isImpersonated();
		}

		return false;
	}

	public static void setCurrentUser(User user) {
		setCurrentUser(user, null, null);
	}

	public static void setCurrentUser(User user, String ipAddress) {
		setCurrentUser(user, null, ipAddress, null, false);
	}

	public static void setCurrentUser(User user, String authToken, HttpServletRequest httpReq) {
		setCurrentUser(user, authToken, httpReq, false);
	}

	public static void setCurrentUser(User user, String authToken, HttpServletRequest httpReq, boolean impersonated) {
		setCurrentUser(user, authToken, null, httpReq, impersonated);
	}

	public static void setCurrentUser(User user, String authToken, String ipAddress, HttpServletRequest httpReq, boolean impersonated) {
		UserAuthToken token = new UserAuthToken(user, authToken, user.getAuthorities());
		if (httpReq != null) {
			token.setDetails(new WebAuthenticationDetailsSource().buildDetails(httpReq));
			token.setTimeZone(httpReq.getHeader("X-OS-CLIENT-TZ"));
			token.setIpAddress(Utility.getRemoteAddress(httpReq));
		} else if (ipAddress != null) {
			token.setIpAddress(ipAddress);
		}

		token.setImpersonated(impersonated);
		SecurityContextHolder.getContext().setAuthentication(token);
	}

	public static void clearCurrentUser() {
		SecurityContextHolder.clearContext();
	}
	
	public static boolean isAdmin() {
		return getCurrentUser() != null && getCurrentUser().isAdmin();
	}
	
	public static boolean isInstituteAdmin() {
		return getCurrentUser() != null && getCurrentUser().isInstituteAdmin();
	}
	
	public static String encodeToken(String token) {
		if (token == null) {
			return null;
		}

		return new String(Base64.getEncoder().encode(token.getBytes()));
	}
	
	public static String decodeToken(String token) {
		return new String(Base64.getDecoder().decode(token.getBytes()));
	}
	
	public static String getTokenFromCookie(HttpServletRequest httpReq) {
		return getCookieValue(httpReq, "osAuthToken");
	}

	public static void setTokenCookie(HttpServletRequest httpReq, HttpServletResponse httpResp, String authToken) {
		setCookie(httpReq, httpResp, "osAuthToken", authToken, -1);
	}

	public static void clearTokenCookie(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		setTokenCookie(httpReq, httpResp, null);
		setImpersonateTokenCookie(httpReq, httpResp, null);
		invalidateSession(httpReq, httpResp);
	}

	public static void setImpersonateTokenCookie(HttpServletRequest httpReq, HttpServletResponse httpResp, String impToken) {
		setCookie(httpReq, httpResp, "osImpersonateUser", impToken, 60 * 60);
	}

	public static void clearImpersonateTokenCookie(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		setImpersonateTokenCookie(httpReq, httpResp, null);
	}

	public static void setCookie(HttpServletRequest httpReq, HttpServletResponse httpResp, String name, String value, int maxAge) {
		String cookieValue = name + "=" + (value != null ? value : "") + ";";
		cookieValue += "Path=" + getContextPath(httpReq) + ";";
		cookieValue += "HttpOnly;";
		cookieValue += "SameSite=Strict;";
		if (httpReq.isSecure()) {
			cookieValue += "secure;";
		}

		if (value == null) {
			cookieValue += "Max-Age=0;";
		} else if (maxAge != -1) {
			cookieValue += "Max-Age=" + maxAge + ";";
		}

		httpResp.addHeader("Set-Cookie", cookieValue);
	}

	public static void resetTokenCookie(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		setTokenCookie(httpReq, httpResp, getAuthTokenFromHeader(httpReq));
	}

	public static String getAuthTokenFromHeader(HttpServletRequest httpReq) {
		return Utility.getHeader(httpReq, OS_AUTH_TOKEN_HDR);
	}

	public static String getImpersonateUser(HttpServletRequest httpReq) {
		String impUserString = Utility.getHeader(httpReq, OS_IMP_USER_HDR);
		if (StringUtils.isNotBlank(impUserString) && !impUserString.equalsIgnoreCase("undefined") && !impUserString.equalsIgnoreCase("null")) {
			return impUserString;
		}

		return getCookieValue(httpReq, "osImpersonateUser");
	}

	private static String getContextPath(HttpServletRequest httpReq) {
		String path = ConfigUtil.getInstance().getAppUrl();
		if (StringUtils.isBlank(path)) {
			path = httpReq.getContextPath();
		} else {
			try {
				path = new URL(path).getPath();
			} catch (MalformedURLException url) {
				throw OpenSpecimenException.userError(ConfigErrorCode.INVALID_SETTING_VALUE, path);
			}
		}

		if (StringUtils.isBlank(path)) {
			path = "/";
		}

		return path;
	}

	private static String getCookieValue(HttpServletRequest httpReq, String cookieName) {
		String cookieHdr = Utility.getHeader(httpReq, "Cookie");
		if (StringUtils.isBlank(cookieHdr)) {
			return null;
		}

		String value = null;
		String[] cookies = cookieHdr.split(";");
		for (String cookie : cookies) {
			if (!cookie.trim().startsWith(cookieName)) {
				continue;
			}

			String[] parts = cookie.trim().split("=", 2);
			if (parts.length == 2) {
				try {
					value = URLDecoder.decode(parts[1], "utf-8");
					if (value.startsWith("%") || Utility.isQuoted(value)) {
						value = value.substring(1, value.length() - 1);
					}
				} catch (Exception e) {
					logger.error("Error obtaining " + cookieName + " from cookie", e);
				}
				break;
			}
		}

		return value;
	}

	private static void invalidateSession(HttpServletRequest httpReq, HttpServletResponse httpResp) {
		HttpSession session = httpReq.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		String sessionCookieName = DEF_SESSION_COOKIE_NAME;
		ServletContext ctxt = httpReq.getServletContext();
		if (ctxt != null) {
			SessionCookieConfig cookieCfg = ctxt.getSessionCookieConfig();
			if (cookieCfg != null && StringUtils.isNotBlank(cookieCfg.getName())) {
				sessionCookieName = cookieCfg.getName();
			}
		}

		setCookie(httpReq, httpResp, sessionCookieName, null, 0);
	}

	private static TimeZone toTimeZone(String timeZone) {
		if (StringUtils.isBlank(timeZone)) {
			return null;
		}

		try {
			return TimeZone.getTimeZone(timeZone);
		} catch (Exception e) {
			logger.error("Error obtaining time zone information for: " + timeZone, e);
		}

		return null;
	}

	private static class UserAuthToken extends UsernamePasswordAuthenticationToken {
		private String timeZone;

		private String ipAddress;

		private boolean impersonated;

		public UserAuthToken(Object principal, Object credentials) {
			super(principal, credentials);
		}

		public UserAuthToken(Object principal, Object credentials, Collection<? extends GrantedAuthority> authorities) {
			super(principal, credentials, authorities);
		}

		public String getTimeZone() {
			return timeZone;
		}

		public void setTimeZone(String timeZone) {
			this.timeZone = timeZone;
		}

		public String getIpAddress() {
			return ipAddress;
		}

		public void setIpAddress(String ipAddress) {
			this.ipAddress = ipAddress;
		}

		public void setImpersonated(boolean impersonated) {
			this.impersonated = impersonated;
		}

		public boolean isImpersonated() {
			return impersonated;
		}
	}
}