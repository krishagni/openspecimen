package com.krishagni.catissueplus.core.auth.domain;

import java.time.Instant;

import com.krishagni.catissueplus.core.biospecimen.domain.BaseEntity;

public class OAuthState extends BaseEntity {
	private String state;

	private AuthDomain domain;

	private String verifier;

	private String challenge;

	private Instant time;

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public AuthDomain getDomain() {
		return domain;
	}

	public void setDomain(AuthDomain domain) {
		this.domain = domain;
	}

	public String getVerifier() {
		return verifier;
	}

	public void setVerifier(String verifier) {
		this.verifier = verifier;
	}

	public String getChallenge() {
		return challenge;
	}

	public void setChallenge(String challenge) {
		this.challenge = challenge;
	}

	public Instant getTime() {
		return time;
	}

	public void setTime(Instant time) {
		this.time = time;
	}
}
