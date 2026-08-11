package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Date;
import java.util.Objects;

public class PvAttribute {
	private String publicId;

	private String longName;

	private String definition;

	private String version;

	private Date lastUpdated;

	private Long formId;

	private Date deletedOn;

	public String getPublicId() {
		return publicId;
	}

	public void setPublicId(String publicId) {
		this.publicId = publicId;
	}

	public String getLongName() {
		return longName;
	}

	public void setLongName(String longName) {
		this.longName = longName;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public Date getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Date lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public Date getDeletedOn() {
		return deletedOn;
	}

	public void setDeletedOn(Date deletedOn) {
		this.deletedOn = deletedOn;
	}

	public boolean isFormScoped() {
		return formId != null;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (!(obj instanceof PvAttribute)) {
			return false;
		}

		PvAttribute other = (PvAttribute) obj;
		return Objects.equals(publicId, other.publicId);
	}

	@Override
	public int hashCode() {
		return publicId != null ? publicId.hashCode() : 0;
	}
}
