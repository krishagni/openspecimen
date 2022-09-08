package com.krishagni.catissueplus.core.de.events;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.domain.SavedQuery;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SavedQuerySummary {

	private Long id;

	private String title;

	private UserSummary createdBy;

	private UserSummary lastModifiedBy;
	
	private Date lastModifiedOn;

	private Boolean starred;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public UserSummary getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(UserSummary createdBy) {
		this.createdBy = createdBy;
	}

	public UserSummary getLastModifiedBy() {
		return lastModifiedBy;
	}

	public void setLastModifiedBy(UserSummary lastModifiedBy) {
		this.lastModifiedBy = lastModifiedBy;
	}

	public Date getLastModifiedOn() {
		return lastModifiedOn;
	}

	public void setLastModifiedOn(Date lastModifiedOn) {
		this.lastModifiedOn = lastModifiedOn;
	}

	public Boolean getStarred() {
		return starred;
	}

	public void setStarred(Boolean starred) {
		this.starred = starred;
	}

	public static SavedQuerySummary fromSavedQuery(SavedQuery savedQuery) {
		if (savedQuery == null) {
			return null;
		}

		SavedQuerySummary querySummary = new SavedQuerySummary();
		querySummary.setId(savedQuery.getId());
		querySummary.setTitle(savedQuery.getTitle());
		querySummary.setCreatedBy(UserSummary.from(savedQuery.getCreatedBy()));
		querySummary.setLastModifiedBy(UserSummary.from(savedQuery.getLastUpdatedBy()));
		querySummary.setLastModifiedOn(savedQuery.getLastUpdated());		
		return querySummary;
	}
}
