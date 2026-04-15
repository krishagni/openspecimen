package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.SpecimenRequest;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class SpecimenRequestSummary implements Serializable {
	private Long id;

	private Long catalogId;

	private UserSummary requestor;

	private String requestorEmailId;

	private String irbId;

	private Long dpId;

	private String dpShortTitle;

	private Long cpId;

	private String cpShortTitle;

	private Date dateOfRequest;

	private String screeningStatus;

	private Date dateOfScreening;

	private UserSummary screenedBy;

	private String screeningComments;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public UserSummary getRequestor() {
		return requestor;
	}

	public void setRequestor(UserSummary requestor) {
		this.requestor = requestor;
	}

	public String getRequestorEmailId() {
		return requestorEmailId;
	}

	public void setRequestorEmailId(String requestorEmailId) {
		this.requestorEmailId = requestorEmailId;
	}

	public String getIrbId() {
		return irbId;
	}

	public void setIrbId(String irbId) {
		this.irbId = irbId;
	}

	public Long getCpId() {
		return cpId;
	}

	public void setCpId(Long cpId) {
		this.cpId = cpId;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
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

	public Date getDateOfRequest() {
		return dateOfRequest;
	}

	public void setDateOfRequest(Date dateOfRequest) {
		this.dateOfRequest = dateOfRequest;
	}

	public String getScreeningStatus() {
		return screeningStatus;
	}

	public void setScreeningStatus(String screeningStatus) {
		this.screeningStatus = screeningStatus;
	}

	public Date getDateOfScreening() {
		return dateOfScreening;
	}

	public void setDateOfScreening(Date dateOfScreening) {
		this.dateOfScreening = dateOfScreening;
	}

	public UserSummary getScreenedBy() {
		return screenedBy;
	}

	public void setScreenedBy(UserSummary screenedBy) {
		this.screenedBy = screenedBy;
	}

	public String getScreeningComments() {
		return screeningComments;
	}

	public void setScreeningComments(String screeningComments) {
		this.screeningComments = screeningComments;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static void copyTo(SpecimenRequest request, SpecimenRequestSummary summary) {
		summary.setId(request.getId());
		summary.setCatalogId(request.getCatalogId());

		if (request.getRequestor() != null) {
			summary.setRequestor(UserSummary.from(request.getRequestor()));
		}

		summary.setRequestorEmailId(request.getRequestorEmailId());
		summary.setIrbId(request.getIrbId());
		summary.setDateOfRequest(request.getDateOfRequest());
		summary.setScreeningStatus(request.getScreeningStatus().name());
		summary.setDateOfScreening(request.getDateOfScreening());
		summary.setScreeningComments(request.getScreeningComments());
		summary.setActivityStatus(request.getActivityStatus());

		if (request.getScreenedBy() != null) {
			summary.setScreenedBy(UserSummary.from(request.getScreenedBy()));
		}

		if (request.getCp() != null) {
			summary.setCpId(request.getCp().getId());
			summary.setCpShortTitle(request.getCp().getShortTitle());
		}

		if (request.getDp() != null) {
			summary.setDpId(request.getDp().getId());
			summary.setDpShortTitle(request.getDp().getShortTitle());
		}
	}

	public static SpecimenRequestSummary from(SpecimenRequest request) {
		SpecimenRequestSummary result = new SpecimenRequestSummary();
		copyTo(request, result);
		return result;
	}

	public static List<SpecimenRequestSummary> from(Collection<SpecimenRequest> requests) {
		return requests.stream().map(SpecimenRequestSummary::from).collect(Collectors.toList());
	}
}