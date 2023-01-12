package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList.Status;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ContainerStoreListCriteria extends AbstractListCriteria<ContainerStoreListCriteria> {

	private Long containerId;

	private Integer maxRetries;

	private Date lastRetryTime;

	private Date fromDate;

	private Date toDate;

	private List<Status> statuses;

	@Override
	public ContainerStoreListCriteria self() {
		return this;
	}

	public Long containerId() {
		return containerId;
	}

	public ContainerStoreListCriteria containerId(Long containerId) {
		this.containerId = containerId;
		return self();
	}

	public Integer maxRetries() {
		return maxRetries;
	}

	public ContainerStoreListCriteria maxRetries(Integer maxRetries) {
		this.maxRetries = maxRetries;
		return self();
	}

	public Date lastRetryTime() {
		return lastRetryTime;
	}

	public ContainerStoreListCriteria lastRetryTime(Date lastRetryTime) {
		this.lastRetryTime = lastRetryTime;
		return self();
	}

	public Date fromDate() {
		return fromDate;
	}

	public ContainerStoreListCriteria fromDate(Date fromDate) {
		this.fromDate = fromDate;
		return self();
	}

	public Date toDate() {
		return toDate;
	}

	public ContainerStoreListCriteria toDate(Date toDate) {
		this.toDate = toDate;
		return self();
	}

	public List<Status> statuses() {
		return statuses;
	}

	public ContainerStoreListCriteria statuses(List<Status> statuses) {
		this.statuses = statuses;
		return self();
	}
}
