
package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang3.ObjectUtils;

import com.krishagni.catissueplus.core.common.AttributeModifiedSupport;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.domain.IntervalUnit;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

@ListenAttributeChanges
public class VisitSummary extends AttributeModifiedSupport implements Comparable<VisitSummary> {
	private Long id;

	private Long cpId;

	private Long eventId;

	private Long cprId;

	private String ppid;

	private String name;

	private String eventCode;

	private String eventLabel;
	
	private Integer eventPoint;

	private IntervalUnit eventPointUnit;
	
	private String status;
	
	private Date visitDate;
	
	private Date anticipatedVisitDate;

	private int totalPendingSpmns;

	private int pendingPrimarySpmns;
	
	private int plannedPrimarySpmnsColl;
	
	private int uncollectedPrimarySpmns;

	private int unplannedPrimarySpmnsColl;

	private int storedSpecimens;

	private int notStoredSpecimens;

	private int distributedSpecimens;

	private int closedSpecimens;

	private String missedReason;

	private UserSummary missedBy;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public Long getEventId() {
		return eventId;
	}

	public void setEventId(Long eventId) {
		this.eventId = eventId;
	}

	public Long getCprId() {
		return cprId;
	}

	public void setCprId(Long cprId) {
		this.cprId = cprId;
	}

	public String getPpid() {
		return ppid;
	}

	public void setPpid(String ppid) {
		this.ppid = ppid;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEventCode() {
		return eventCode;
	}

	public void setEventCode(String eventCode) {
		this.eventCode = eventCode;
	}

	public String getEventLabel() {
		return eventLabel;
	}

	public void setEventLabel(String eventLabel) {
		this.eventLabel = eventLabel;
	}

	public Integer getEventPoint() {
		return eventPoint;
	}

	public void setEventPoint(Integer eventPoint) {
		this.eventPoint = eventPoint;
	}

	public IntervalUnit getEventPointUnit() {
		return eventPointUnit;
	}

	public void setEventPointUnit(IntervalUnit eventPointUnit) {
		this.eventPointUnit = eventPointUnit;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getVisitDate() {
		return visitDate;
	}

	public void setVisitDate(Date visitDate) {
		this.visitDate = visitDate;
	}

	public Date getAnticipatedVisitDate() {
		return anticipatedVisitDate;
	}

	public void setAnticipatedVisitDate(Date anticipatedVisitDate) {
		this.anticipatedVisitDate = anticipatedVisitDate;
	}

	public int getTotalPendingSpmns() {
		return totalPendingSpmns;
	}

	public void setTotalPendingSpmns(int totalPendingSpmns) {
		this.totalPendingSpmns = totalPendingSpmns;
	}

	public int getPendingPrimarySpmns() {
		return pendingPrimarySpmns;
	}

	public void setPendingPrimarySpmns(int pendingPrimarySpmns) {
		this.pendingPrimarySpmns = pendingPrimarySpmns;
	}

	public int getPlannedPrimarySpmnsColl() {
		return plannedPrimarySpmnsColl;
	}

	public void setPlannedPrimarySpmnsColl(int plannedPrimarySpmnsColl) {
		this.plannedPrimarySpmnsColl = plannedPrimarySpmnsColl;
	}

	public int getUncollectedPrimarySpmns() {
		return uncollectedPrimarySpmns;
	}

	public void setUncollectedPrimarySpmns(int uncollectedPrimarySpmns) {
		this.uncollectedPrimarySpmns = uncollectedPrimarySpmns;
	}

	public int getUnplannedPrimarySpmnsColl() {
		return unplannedPrimarySpmnsColl;
	}

	public void setUnplannedPrimarySpmnsColl(int unplannedPrimarySpmnsColl) {
		this.unplannedPrimarySpmnsColl = unplannedPrimarySpmnsColl;
	}

	public int getStoredSpecimens() {
		return storedSpecimens;
	}

	public void setStoredSpecimens(int storedSpecimens) {
		this.storedSpecimens = storedSpecimens;
	}

	public int getNotStoredSpecimens() {
		return notStoredSpecimens;
	}

	public void setNotStoredSpecimens(int notStoredSpecimens) {
		this.notStoredSpecimens = notStoredSpecimens;
	}

	public int getDistributedSpecimens() {
		return distributedSpecimens;
	}

	public void setDistributedSpecimens(int distributedSpecimens) {
		this.distributedSpecimens = distributedSpecimens;
	}

	public int getClosedSpecimens() {
		return closedSpecimens;
	}

	public void setClosedSpecimens(int closedSpecimens) {
		this.closedSpecimens = closedSpecimens;
	}

	public String getMissedReason() {
		return missedReason;
	}

	public void setMissedReason(String missedReason) {
		this.missedReason = missedReason;
	}

	public UserSummary getMissedBy() {
		return missedBy;
	}

	public void setMissedBy(UserSummary missedBy) {
		this.missedBy = missedBy;
	}

	public void setAnticipatedVisitDate(Date baseline, Integer interval, IntervalUnit unit) {
		if (eventPoint == null || eventPointUnit == null) {
			return;
		}

		Calendar cal = Calendar.getInstance();
		cal.setTime(baseline);
		addInterval(cal, eventPoint, eventPointUnit);
		addInterval(cal, -interval, unit);
		setAnticipatedVisitDate(cal.getTime());
	}

	@Override
	public int compareTo(VisitSummary other) {
		Integer thisEventPoint = Utility.getNoOfDays(eventPoint, eventPointUnit);
		Integer otherEventPoint = Utility.getNoOfDays(other.eventPoint, other.eventPointUnit);
		int result = ObjectUtils.compare(thisEventPoint, otherEventPoint, true);
		if (result != 0) {
			return result;
		}

		result = ObjectUtils.compare(this.eventId, other.eventId, true);
		if (result != 0) {
			return result;
		}

		return compareDates(other);
	}

	public int compareDates(VisitSummary other) {
		return ObjectUtils.compare(
			getVisitDate() != null ? getVisitDate() : getAnticipatedVisitDate(),
			other.getVisitDate() != null ? other.getVisitDate() : other.getAnticipatedVisitDate(),
			true
		);
	}

	private void addInterval(Calendar cal, Integer interval, IntervalUnit intervalUnit) {
		switch (intervalUnit) {
			case DAYS -> cal.add(Calendar.DAY_OF_YEAR, interval);
			case WEEKS -> cal.add(Calendar.WEEK_OF_YEAR, interval);
			case MONTHS -> cal.add(Calendar.MONTH, interval);
			case YEARS -> cal.add(Calendar.YEAR, interval);
		}
	}

}
