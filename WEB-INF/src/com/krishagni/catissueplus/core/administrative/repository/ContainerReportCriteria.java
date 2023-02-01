package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ContainerReportCriteria extends AbstractListCriteria<ContainerReportCriteria> {
	private boolean topLevelContainers;

	private List<String> names;

	private List<String> cps;

	private List<String> types;

	private Date fromDate;

	private Date toDate;

	private Set<SiteCpPair> siteCps;

	@Override
	public ContainerReportCriteria self() {
		return this;
	}

	public boolean topLevelContainers() {
		return topLevelContainers;
	}

	public ContainerReportCriteria topLevelContainers(boolean topLevelContainers) {
		this.topLevelContainers = topLevelContainers;
		return self();
	}

	public List<String> names() {
		return names;
	}

	@JsonProperty("name")
	public ContainerReportCriteria names(List<String> names) {
		this.names = names;
		return self();
	}

	public List<String> cps() {
		return cps;
	}

	@JsonProperty("cp")
	public ContainerReportCriteria cps(List<String> cps) {
		this.cps = cps;
		return self();
	}

	public List<String> types() {
		return types;
	}

	@JsonProperty("type")
	public ContainerReportCriteria types(List<String> types) {
		this.types = types;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	@JsonProperty("fromDate")
	public ContainerReportCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Date toDate() {
		return toDate;
	}

	@JsonProperty("toDate")
	public ContainerReportCriteria toDate(Date toDate) {
		this.toDate = toDate;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public ContainerReportCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public Map<String, String> toStrMap() {
		Map<String, String> map = new HashMap<>();
		if (names != null && !names.isEmpty()) {
			map.put("names", String.join(", ", names));
		}

		if (cps != null && !cps.isEmpty()) {
			map.put("cps", String.join(", ", cps));
		}

		if (fromDate != null) {
			map.put("fromDate", Utility.getDateTimeString(fromDate));
		}

		if (toDate != null) {
			map.put("toDate", Utility.getDateTimeString(toDate));
		}

		return map;
	}

	public StorageContainerListCriteria toListCriteria() {
		return new StorageContainerListCriteria()
			.names(names)
			.cpShortTitles(cps != null ? new HashSet<String>(cps) : null)
			.ids(ids())
			.topLevelContainers(topLevelContainers)
			.siteCps(siteCps);
	}
}
