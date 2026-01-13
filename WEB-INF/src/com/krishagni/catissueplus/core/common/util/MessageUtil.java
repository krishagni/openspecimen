package com.krishagni.catissueplus.core.common.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.MessageSource;

@Configurable
public class MessageUtil {

	private static MessageUtil instance = null;

	@Autowired
	private MessageSource messageSource;

	public static MessageUtil getInstance() {
		if (instance == null || instance.messageSource == null) {
			instance = new MessageUtil();
		}

		return instance;
	}

	public String getBooleanMsg(Boolean value) {
		String bool = "common_no";
		if (value != null && value) {
			bool = "common_yes";
		}

		return messageSource.getMessage(bool, null, Locale.getDefault());
	}
	
	public String getMessage(String code) {
		return getMessage(code, null);
	}

	public String getMessage(String code, Object[] params) {
		return getMessage(code, code, params);
	}

	public String getMessage(String code, String defaultMsg, Object[] params) {
		if (messageSource == null) {
			return errorCodeParams(code, defaultMsg, params);
		}

		return messageSource.getMessage(code, params, defaultMsg, Locale.getDefault());
	}

	private String errorCodeParams(String code, String defaultMsg, Object[] params) {
		StringBuilder msg = new StringBuilder(code);
		if (StringUtils.isNotBlank(defaultMsg)) {
			msg.append(":").append(defaultMsg);
		}

		if (params != null && params.length > 0) {
			msg.append(":").append(Arrays.stream(params).map(Object::toString).collect(Collectors.joining(", ")));
		}

		return msg.toString();
	}
}
