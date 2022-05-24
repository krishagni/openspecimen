package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenListsFolder;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenListsFolderSummary {
	private Long id;

	private String name;

	private String description;

	private UserSummary owner;

	private Date creationTime;

	private Integer cartsCount;

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

	public UserSummary getOwner() {
		return owner;
	}

	public void setOwner(UserSummary owner) {
		this.owner = owner;
	}

	public Date getCreationTime() {
		return creationTime;
	}

	public void setCreationTime(Date creationTime) {
		this.creationTime = creationTime;
	}

	public Integer getCartsCount() {
		return cartsCount;
	}

	public void setCartsCount(Integer cartsCount) {
		this.cartsCount = cartsCount;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static <T extends SpecimenListsFolderSummary> T fromTo(SpecimenListsFolder folder, T result) {
		result.setId(folder.getId());
		result.setName(folder.getName());
		result.setDescription(folder.getDescription());
		result.setOwner(UserSummary.from(folder.getOwner()));
		result.setCreationTime(folder.getCreationTime());
		result.setActivityStatus(folder.getActivityStatus());
		return result;
	}

	public static SpecimenListsFolderSummary from(SpecimenListsFolder folder) {
		return fromTo(folder, new SpecimenListsFolderSummary());
	}

	public static List<SpecimenListsFolderSummary> from(List<SpecimenListsFolder> folders) {
		return Utility.nullSafeStream(folders).map(SpecimenListsFolderSummary::from).collect(Collectors.toList());
	}
}
