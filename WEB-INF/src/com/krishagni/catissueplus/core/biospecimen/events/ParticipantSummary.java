
package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;

import com.krishagni.catissueplus.core.biospecimen.domain.Participant;

public class ParticipantSummary {
	private Long id;

	private String source;

	private String firstName = "";

	private String lastName = "";
	
	private String empi;

	private String uid;

	private String emailAddress;

	private Boolean emailOptIn;

	private String phoneNumber;

	private Boolean textOptIn;

	private Boolean textOptInConsent;

	private Date birthDate;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmpi() {
		return empi;
	}

	public void setEmpi(String empi) {
		this.empi = empi;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public Boolean getEmailOptIn() {
		return emailOptIn;
	}

	public void setEmailOptIn(Boolean emailOptIn) {
		this.emailOptIn = emailOptIn;
	}

	public String getPhoneNumber() {
		return phoneNumber;
	}

	public void setPhoneNumber(String phoneNumber) {
		this.phoneNumber = phoneNumber;
	}

	public Boolean getTextOptIn() {
		return textOptIn;
	}

	public void setTextOptIn(Boolean textOptIn) {
		this.textOptIn = textOptIn;
	}

	public Boolean getTextOptInConsent() {
		return textOptInConsent;
	}

	public void setTextOptInConsent(Boolean textOptInConsent) {
		this.textOptInConsent = textOptInConsent;
	}

	public Date getBirthDate() {
		return birthDate;
	}

	public void setBirthDate(Date birthDate) {
		this.birthDate = birthDate;
	}

	public static ParticipantSummary from(Participant p, boolean excludePhi) {
		ParticipantSummary result = new ParticipantSummary();
		result.setId(p.getId());
		result.setSource(p.getSource());
		if (excludePhi) {
			return result;
		}

		result.setFirstName(p.getFirstName());
		result.setLastName(p.getLastName());
		result.setEmpi(p.getEmpi());
		result.setUid(p.getUid());
		result.setEmailAddress(p.getEmailAddress());
		result.setEmailOptIn(p.isEmailOptIn());
		result.setPhoneNumber(p.getPhoneNumber());
		result.setTextOptIn(p.isTextOptIn());
		result.setTextOptInConsent(p.getTextOptInConsent());
		result.setBirthDate(p.getBirthDate());
		return result;
	}
}
