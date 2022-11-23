package com.krishagni.catissueplus.core.common.domain;

import java.util.Calendar;

public class ExtAppMessage {
	private String message;

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public MessageLog toMessageLog() {
		MessageLog log = new MessageLog();
		log.setMessage(message);
		log.setReceiveTime(Calendar.getInstance().getTime());
		return log;
	}
}
