package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class DistributionOrderStat {
	private Long id;
	
	private String name;
	
	private Long dpId;

	private String dpShortTitle;

	private Date executionDate;

	private Map<String, Object> groupByAttrVals = new HashMap<>();
	
	private Long distributedSpecimenCount;
	
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

	public Long getDpId() {
		return dpId;
	}

	public void setDpId(Long dpId) {
		this.dpId = dpId;
	}

	public String getDpShortTitle() {
		return dpShortTitle;
	}

	public void setDpShortTitle(String dpShortTitle) {
		this.dpShortTitle = dpShortTitle;
	}

	public Date getExecutionDate() {
		return executionDate;
	}

	public void setExecutionDate(Date executionDate) {
		this.executionDate = executionDate;
	}

	public Map<String, Object> getGroupByAttrVals() {
		return groupByAttrVals;
	}
	
	public void setGroupByAttrVals(Map<String, Object> groupByAttrVals) {
		this.groupByAttrVals = groupByAttrVals;
	}
	
	public Long getDistributedSpecimenCount() {
		return distributedSpecimenCount;
	}

	public void setDistributedSpecimenCount(Long distributedSpecimenCount) {
		this.distributedSpecimenCount = distributedSpecimenCount;
	}

}
