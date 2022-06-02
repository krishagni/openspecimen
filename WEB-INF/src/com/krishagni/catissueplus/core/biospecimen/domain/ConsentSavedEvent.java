package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Map;

import com.krishagni.catissueplus.core.common.events.OpenSpecimenEvent;

public class ConsentSavedEvent extends OpenSpecimenEvent<Map<String, String>> {

	private static final long serialVersionUID = 6923029037727440272L;

	private CollectionProtocolRegistration cpr;

	private Visit visit;

	public ConsentSavedEvent(CollectionProtocolRegistration cpr, Map<String, String> responses) {
		super(null, responses);
		this.cpr = cpr;
	}

	public ConsentSavedEvent(CollectionProtocolRegistration cpr, Visit visit, Map<String, String> responses) {
		this(cpr, responses);
		this.visit = visit;
	}

	public CollectionProtocolRegistration getCpr() {
		return cpr;
	}

	public Visit getVisit() {
		return visit;
	}
}
