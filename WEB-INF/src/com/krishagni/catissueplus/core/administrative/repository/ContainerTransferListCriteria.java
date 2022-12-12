package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ContainerTransferListCriteria extends AbstractListCriteria<ContainerTransferListCriteria> {
	private List<String> freezerNames;

	private List<String> cps;

	private Date fromDate;

	private Date toDate;

	private Set<SiteCpPair> siteCps;

	@Override
	public ContainerTransferListCriteria self() {
		return this;
	}

	public List<String> freezerNames() {
		return freezerNames;
	}

	public ContainerTransferListCriteria freezerNames(List<String> freezerNames) {
		this.freezerNames = freezerNames;
		return self();
	}

	public List<String> cps() {
		return cps;
	}

	public ContainerTransferListCriteria cps(List<String> cps) {
		this.cps = cps;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	public ContainerTransferListCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Date toDate() {
		return toDate;
	}

	public ContainerTransferListCriteria toDate(Date toDate) {
		this.toDate = toDate;
		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public ContainerTransferListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public Map<String, String> toStrMap() {
		Map<String, String> map = new HashMap<>();
		if (freezerNames != null && !freezerNames.isEmpty()) {
			map.put("freezers", String.join(", ", freezerNames));
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
}
