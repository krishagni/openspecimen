package com.krishagni.catissueplus.core.common.service;

import java.util.List;

import com.krishagni.catissueplus.core.common.domain.ExtAppMessage;
import com.krishagni.catissueplus.core.common.domain.MessageLog;

public interface MessageLogService {

	MessageLog getMessageLog(Long id);

	void registerHandler(String extAppName, MessageHandler handler);

	void retryPendingMessages();

	String processMessages(String extAppName, List<ExtAppMessage> messages);
}
