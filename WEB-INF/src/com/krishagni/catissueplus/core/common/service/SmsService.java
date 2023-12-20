package com.krishagni.catissueplus.core.common.service;

import java.util.Map;

public interface SmsService {
	String sendMessage(String providerName, String to, String message);

	String sendMessage(String providerName, String to, String message, Map<String, Object> props);

	String onMessageReceive(String providerName, String from, String message);
}
