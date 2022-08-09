package com.krishagni.catissueplus.core.audit.events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.krishagni.catissueplus.core.common.events.UserSummary;

public class RevisionDetail {
	private Long revisionId;

	private UserSummary changedBy;

	private Date changedOn;

	private String ipAddress;

	private List<RevisionEntityRecordDetail> records;

	public Long getRevisionId() {
		return revisionId;
	}

	public void setRevisionId(Long revisionId) {
		this.revisionId = revisionId;
	}

	public UserSummary getChangedBy() {
		return changedBy;
	}

	public void setChangedBy(UserSummary changedBy) {
		this.changedBy = changedBy;
	}

	public Date getChangedOn() {
		return changedOn;
	}

	public void setChangedOn(Date changedOn) {
		this.changedOn = changedOn;
	}

	public String getIpAddress() {
		return ipAddress;
	}

	public void setIpAddress(String ipAddress) {
		this.ipAddress = ipAddress;
	}

	public List<RevisionEntityRecordDetail> getRecords() {
		return records;
	}

	public void setRecords(List<RevisionEntityRecordDetail> records) {
		this.records = records;
	}

	public void addRecord(RevisionEntityRecordDetail record) {
		if (records == null) {
			records = new ArrayList<>();
		}

		RevisionEntityRecordDetail existing = records.stream()
			.filter(er -> er.getEntityName().equals(record.getEntityName()) && er.getEntityId().equals(record.getEntityId()))
			.findFirst().orElse(null);
		if (existing == null) {
			records.add(record);
		} else {
			if (record.getType() == 0) {
				existing.setType(0);
			}

			if (record.getId() < existing.getId()) {
				existing.setId(record.getId());
			}
		}
	}

	public Long getLastRecordId() {
		return (records != null && !records.isEmpty()) ? records.get(records.size() - 1).getId() : 0L;
	}
}
