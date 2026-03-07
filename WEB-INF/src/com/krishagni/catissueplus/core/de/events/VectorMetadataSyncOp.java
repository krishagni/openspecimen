package com.krishagni.catissueplus.core.de.events;

public class VectorMetadataSyncOp {
	private Long cpId;

	private Long cpGroupId;

	private Integer pvBatchSize;

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getCpGroupId() {
		return cpGroupId;
	}

	public void setCpGroupId(Long cpGroupId) {
		this.cpGroupId = cpGroupId;
	}

	public Integer getPvBatchSize() {
		return pvBatchSize;
	}

	public void setPvBatchSize(Integer pvBatchSize) {
		this.pvBatchSize = pvBatchSize;
	}
}
