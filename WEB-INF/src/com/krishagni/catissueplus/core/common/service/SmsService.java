package com.krishagni.catissueplus.core.common.service;

import java.util.Map;

public interface SmsService {
	boolean sendMessage(String providerName, String to, String message);

	boolean sendMessage(String providerName, String to, String message, Map<String, Object> props);
}
