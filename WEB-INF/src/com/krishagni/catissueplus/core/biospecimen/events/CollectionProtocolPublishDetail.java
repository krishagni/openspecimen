package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;


import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolPublishEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class CollectionProtocolPublishDetail {
	private Long id;

	private Long cpId;

	private String cpTitle;

	private String cpShortTitle;

	private String changes;

	private String reason;

	private UserSummary publishedBy;

	private Date publicationDate;

	private List<UserSummary> reviewers;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getCpTitle() {
		return cpTitle;
	}

	public void setCpTitle(String cpTitle) {
		this.cpTitle = cpTitle;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
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

	public UserSummary getPublishedBy() {
		return publishedBy;
	}

	public void setPublishedBy(UserSummary publishedBy) {
		this.publishedBy = publishedBy;
	}

	public Date getPublicationDate() {
		return publicationDate;
	}

	public void setPublicationDate(Date publicationDate) {
		this.publicationDate = publicationDate;
	}

	public List<UserSummary> getReviewers() {
		return reviewers;
	}

	public void setReviewers(List<UserSummary> reviewers) {
		this.reviewers = reviewers;
	}

	public static CollectionProtocolPublishDetail from(CollectionProtocolPublishEvent event) {
		CollectionProtocolPublishDetail result = new CollectionProtocolPublishDetail();
		result.setId(event.getId());
		result.setCpId(event.getCp().getId());
		result.setCpTitle(event.getCp().getTitle());
		result.setCpShortTitle(event.getCp().getShortTitle());
		result.setChanges(event.getChanges());
		result.setReason(event.getReason());
		result.setPublishedBy(UserSummary.from(event.getPublishedBy()));
		result.setPublicationDate(event.getPublicationDate());
		result.setReviewers(UserSummary.from(event.getReviewers()));
		return result;
	}

	public static List<CollectionProtocolPublishDetail> from(Collection<CollectionProtocolPublishEvent> events) {
		return Utility.nullSafeStream(events).map(CollectionProtocolPublishDetail::from).collect(Collectors.toList());
	}
}
