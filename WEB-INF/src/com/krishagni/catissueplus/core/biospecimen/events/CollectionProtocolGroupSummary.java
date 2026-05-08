package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;

@JsonFilter("withoutId")
@ListenAttributeChanges
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionProtocolGroupSummary extends AttributeModifiedSupport {
	private Long id;

	private String name;

	private Integer cpsCount;

	private UserGroupSummary reqManagers;

	private List<CollectionProtocolSummary> cps;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Integer getCpsCount() {
		return cpsCount;
	}

	public void setCpsCount(Integer cpsCount) {
		this.cpsCount = cpsCount;
	}

	public UserGroupSummary getReqManagers() {
		return reqManagers;
	}

	public void setReqManagers(UserGroupSummary reqManagers) {
		this.reqManagers = reqManagers;
	}

	public List<CollectionProtocolSummary> getCps() {
		return cps;
	}

	public void setCps(List<CollectionProtocolSummary> cps) {
		this.cps = cps;
	}

	public static CollectionProtocolGroupSummary from(CollectionProtocolGroup group) {
		CollectionProtocolGroupSummary result = new CollectionProtocolGroupSummary();
		result.setId(group.getId());
		result.setName(group.getName());
		result.setCpsCount(group.getCpsCount());
		result.setReqManagers(UserGroupSummary.from(group.getReqManagers()));
		return result;
	}

	public static List<CollectionProtocolGroupSummary> from(Collection<CollectionProtocolGroup> groups) {
		return groups.stream().map(CollectionProtocolGroupSummary::from).collect(Collectors.toList());
	}
}
