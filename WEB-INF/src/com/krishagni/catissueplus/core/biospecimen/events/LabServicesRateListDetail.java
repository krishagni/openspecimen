package com.krishagni.catissueplus.core.biospecimen.events;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.krishagni.catissueplus.core.biospecimen.domain.LabServicesRateList;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LabServicesRateListDetail {
	private Long id;

	private String name;

	private String description;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate startDate;

	@JsonFormat(pattern = "yyyy-MM-dd")
	private LocalDate endDate;

	private UserSummary creator;

	private Date creationTime;

	private UserSummary updater;

	private Date updateTime;

	private String activityStatus;

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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public LocalDate getStartDate() {
		return startDate;
	}

	public void setStartDate(LocalDate startDate) {
		this.startDate = startDate;
	}

	public LocalDate getEndDate() {
		return endDate;
	}

	public void setEndDate(LocalDate endDate) {
		this.endDate = endDate;
	}

	public UserSummary getCreator() {
		return creator;
	}

	public void setCreator(UserSummary creator) {
		this.creator = creator;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public UserSummary getUpdater() {
		return updater;
	}

	public void setUpdater(UserSummary updater) {
		this.updater = updater;
	}

	public Date getUpdateTime() {
		return updateTime;
	}

	public void setUpdateTime(Date updateTime) {
		this.updateTime = updateTime;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static LabServicesRateListDetail from(LabServicesRateList rateList) {
		LabServicesRateListDetail result = new LabServicesRateListDetail();
		result.setId(rateList.getId());
		result.setName(rateList.getName());
		result.setDescription(rateList.getDescription());
		result.setStartDate(rateList.getStartDate());
		result.setEndDate(rateList.getEndDate());
		result.setCreator(UserSummary.from(rateList.getCreator()));
		result.setCreationTime(rateList.getCreationTime());
		if (rateList.getUpdater() != null) {
			result.setUpdater(UserSummary.from(rateList.getUpdater()));
			result.setUpdateTime(rateList.getUpdateTime());
		}
		result.setActivityStatus(rateList.getActivityStatus());

		return result;
	}

	public static List<LabServicesRateListDetail> from(Collection<LabServicesRateList> rateLists) {
		return Utility.nullSafeStream(rateLists).map(LabServicesRateListDetail::from).collect(Collectors.toList());
	}
}
