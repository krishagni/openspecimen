package com.krishagni.catissueplus.core.biospecimen.events;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class SpecimenListDigestItem {
	private UserSummary user;

	private String cpShortTitle;

	private Long addedSpecimens;

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public Long getAddedSpecimens() {
		return addedSpecimens;
	}

	public void setAddedSpecimens(Long addedSpecimens) {
		this.addedSpecimens = addedSpecimens;
	}
}
