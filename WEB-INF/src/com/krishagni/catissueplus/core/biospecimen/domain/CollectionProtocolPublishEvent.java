package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.Date;
import java.util.Set;

import com.krishagni.catissueplus.core.administrative.domain.User;

public class CollectionProtocolPublishEvent extends BaseEntity {
	private CollectionProtocol cp;

	private User publishedBy;

	private Date publicationDate;

	private Set<User> reviewers;

	private String changes;

	private String reason;

	private CollectionProtocolPublishedVersion publishedVersion;

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public User getPublishedBy() {
		return publishedBy;
	}

	public void setPublishedBy(User publishedBy) {
		this.publishedBy = publishedBy;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public Set<User> getReviewers() {
		return reviewers;
	}

	public void setReviewers(Set<User> reviewers) {
		this.reviewers = reviewers;
	}

	public String getChanges() {
		return changes;
	}

	public void setChanges(String changes) {
		this.changes = changes;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public CollectionProtocolPublishedVersion getPublishedVersion() {
		return publishedVersion;
	}

	public void setPublishedVersion(CollectionProtocolPublishedVersion publishedVersion) {
		this.publishedVersion = publishedVersion;
	}
}
