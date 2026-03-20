package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CollectionProtocolGroupSummary {
	private Long id;

	private String name;

	private Integer cpsCount;

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
		return result;
	}

	public static List<CollectionProtocolGroupSummary> from(Collection<CollectionProtocolGroup> groups) {
		return groups.stream().map(CollectionProtocolGroupSummary::from).collect(Collectors.toList());
	}
}
