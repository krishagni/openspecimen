package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.domain.ExtAppMessagesList;
import com.krishagni.catissueplus.core.common.service.MessageLogService;

@Controller
@RequestMapping("/external-app-messages")
public class ExtAppMessagesController {

	@Autowired
	private MessageLogService msgLogSvc;

	@RequestMapping(method = RequestMethod.POST)
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, String> processMessages(@RequestBody ExtAppMessagesList messages) {
		String txnId = msgLogSvc.processMessages(messages.getProtocol(), messages.getMessages());
		return Collections.singletonMap("transactionId", txnId);
	}
}
