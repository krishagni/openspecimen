package com.krishagni.catissueplus.core.biospecimen.events;

public class MatchedRegistrationsList {
	private CollectionProtocolRegistrationDetail input;

	private CollectionProtocolRegistrationDetail cpr;

	private MatchedParticipantsList participants;

	public CollectionProtocolRegistrationDetail getInput() {
		return input;
	}

	public void setInput(CollectionProtocolRegistrationDetail input) {
		this.input = input;
	}

	public CollectionProtocolRegistrationDetail getCpr() {
		return cpr;
	}

	public void setCpr(CollectionProtocolRegistrationDetail cpr) {
		this.cpr = cpr;
	}

	public MatchedParticipantsList getParticipants() {
		return participants;
	}

	public void setParticipants(MatchedParticipantsList participants) {
		this.participants = participants;
	}
}