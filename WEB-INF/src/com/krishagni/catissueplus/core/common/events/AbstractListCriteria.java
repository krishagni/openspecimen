package com.krishagni.catissueplus.core.common.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

public abstract class AbstractListCriteria<T extends ListCriteria<T>> implements ListCriteria<T> {
	private Long lastId;

	private int startAt;
	
	private int maxResults;

	private boolean limitItems;
	
	private boolean includePhi;
	
	private String query;
	
	private boolean exactMatch;
	
	private boolean includeStat;

	private boolean includeExtensions;
	
	private List<Long> ids = new ArrayList<>();

	private List<Long> notInIds = new ArrayList<>();

	private String orderBy;

	private boolean asc = true;

	private boolean orderByStarred;

	private boolean enableIdRange;

	@Override
	public Long lastId() {
		return lastId;
	}

	@Override
	public T lastId(Long lastId) {
		this.lastId = lastId;
		return self();
	}

	@Override
	public int startAt() {
		return Math.max(startAt, 0);
	}

	@Override
	@JsonProperty("startAt")
	public T startAt(int startAt) {
		this.startAt = startAt;
		return self();
	}

	@Override
	public int maxResults() {
		return maxResults <= 0 ? 100 : maxResults;
	}

	@Override
	@JsonProperty("maxResults")
	public T maxResults(int maxResults) {
		this.maxResults = maxResults;
		return self();
	}

	public int rangeFactor() {
		return 100;
	}

	@Override
	public boolean limitItems() {
		return limitItems;
	}

	@Override
	public T limitItems(boolean limitItems) {
		this.limitItems = limitItems;
		return self();
	}

	@Override
	public boolean includePhi() {
		return includePhi;
	}

	@Override
	public T includePhi(boolean includePhi) {
		this.includePhi = includePhi;
		return self();
	}

	@Override
	public String query() {
		return query;
	}

	@Override
	@JsonProperty("searchStr")
	public T query(String query) {
		this.query = query;
		return self();
	}
	
	@Override
	public boolean exactMatch() {
		return exactMatch;
	}
	
	@Override
	@JsonProperty("exactMatch")
	public T exactMatch(boolean exactMatch) {
		this.exactMatch = exactMatch;
		return self();
	}

//	public MatchMode matchMode() {
//		return exactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE;
//	}

	@Override
	@JsonProperty("includeStats")
	public boolean includeStat() {
		return includeStat;
	}

	@Override
	public T includeStat(boolean includeStat) {
		this.includeStat = includeStat;
		return self();
	}

	@JsonProperty("includeExtensions")
	public boolean includeExtensions() {
		return includeExtensions;
	}

	public T includeExtensions(boolean includeExtensions) {
		this.includeExtensions = includeExtensions;
		return self();
	}

	@Override
	public List<Long> ids() {
		return ids;
	}
	
	@Override
	@JsonProperty("ids")
	public T ids(List<Long> ids) {
		this.ids = ids;
		return self();
	}

	@Override
	public List<Long> notInIds() {
		return notInIds;
	}

	@Override
	@JsonProperty("notInIds")
	public T notInIds(List<Long> notInIds) {
		this.notInIds = notInIds;
		return self();
	}


	public String orderBy() {
		return orderBy;
	}

	@JsonProperty("orderBy")
	public T orderBy(String orderBy) {
		this.orderBy = orderBy;
		return self();
	}

	public boolean asc() {
		return asc;
	}

	@JsonProperty("asc")
	public T asc(boolean asc) {
		this.asc = asc;
		return self();
	}

	public boolean orderByStarred() {
		return orderByStarred;
	}

	@JsonProperty("orderByStarred")
	public T orderByStarred(boolean orderByStarred) {
		this.orderByStarred = orderByStarred;
		return self();
	}

	public boolean enableIdRange() {
		return enableIdRange;
	}

	public T enableIdRange(boolean enableIdRange) {
		this.enableIdRange = enableIdRange;
		return self();
	}
	
	public abstract T self();

	public String toString() {
		return new StringBuilder()
			.append("last id = ").append(lastId()).append(", ")
			.append("start at = ").append(startAt()).append(", ")
			.append("max results = ").append(maxResults()).append(", ")
			.append("limit items = ").append(limitItems()).append(", ")
			.append("include phi = ").append(includePhi()).append(", ")
			.append("query = ").append(query()).append(", ")
			.append("exact match = ").append(exactMatch()).append(", ")
			.append("include stat = ").append(includeStat()).append(", ")
			.append("ids = ").append(StringUtils.join(ids(), ","))
			.toString();
	}
}
