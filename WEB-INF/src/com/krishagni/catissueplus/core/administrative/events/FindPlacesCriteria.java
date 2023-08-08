package com.krishagni.catissueplus.core.administrative.events;

import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;

public class FindPlacesCriteria {
	private Long freezerId;

	private Long cpId;

	private Long typeId;

	private int requiredPlaces;

	private Boolean allInOneContainer;

	private Boolean strictMatch;

	private Set<SiteCpPair> siteCps;

	private int startAt;

	private int maxResults;

	public Long getFreezerId() {
		return freezerId;
	}

	public void setFreezerId(Long freezerId) {
		this.freezerId = freezerId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getTypeId() {
		return typeId;
	}

	public void setTypeId(Long typeId) {
		this.typeId = typeId;
	}

	public int getRequiredPlaces() {
		return requiredPlaces;
	}

	public void setRequiredPlaces(int requiredPlaces) {
		this.requiredPlaces = requiredPlaces;
	}

	public Boolean getAllInOneContainer() {
		return allInOneContainer;
	}

	public void setAllInOneContainer(Boolean allInOneContainer) {
		this.allInOneContainer = allInOneContainer;
	}

	public Boolean getStrictMatch() {
		return strictMatch;
	}

	public void setStrictMatch(Boolean strictMatch) {
		this.strictMatch = strictMatch;
	}

	public Set<SiteCpPair> getSiteCps() {
		return siteCps;
	}

	public void setSiteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
	}

	public int getStartAt() {
		return Math.max(startAt, 0);
	}

	public void setStartAt(int startAt) {
		this.startAt = startAt;
	}

	public int getMaxResults() {
		return maxResults > 0 ? maxResults : 100;
	}

	public void setMaxResults(int maxResults) {
		this.maxResults = maxResults;
	}
}
