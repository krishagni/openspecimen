package com.krishagni.catissueplus.core.administrative.events;

import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.events.SpecimenInfo;
import com.krishagni.catissueplus.core.common.events.UserSummary;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReserveSpecimensDetail {
	private Long dpId;

	private String dpShortTitle;

	private List<SpecimenInfo> specimens;

	private UserSummary user;

	private Date time;

	private String comments;

	private Boolean cancelOp;

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

	public List<SpecimenInfo> getSpecimens() {
		return specimens;
	}

	public void setSpecimens(List<SpecimenInfo> specimens) {
		this.specimens = specimens;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public Boolean getCancelOp() {
		return cancelOp;
	}

	public void setCancelOp(Boolean cancelOp) {
		this.cancelOp = cancelOp;
	}
}
