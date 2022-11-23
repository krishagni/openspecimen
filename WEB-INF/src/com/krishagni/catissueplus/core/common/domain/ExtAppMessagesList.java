package com.krishagni.catissueplus.core.common.domain;

import java.util.List;

public class ExtAppMessagesList {
	private String protocol; // protocol or app - hl7 vs redcap vs openclinica

	private List<ExtAppMessage> messages;

	public String getProtocol() {
		return protocol;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public List<ExtAppMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<ExtAppMessage> messages) {
		this.messages = messages;
	}
}
