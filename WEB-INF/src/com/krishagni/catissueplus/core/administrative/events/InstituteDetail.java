
package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.Institute;

public class InstituteDetail {

	private Long id;

	private String name;

	private String activityStatus;

	private int usersCount;

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

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public int getUsersCount() {
		return usersCount;
	}

	public void setUsersCount(int usersCount) {
		this.usersCount = usersCount;
	}

	public static InstituteDetail from(Institute institute) {
		InstituteDetail detail = new InstituteDetail();
		detail.setId(institute.getId());
		detail.setName(institute.getName());
		detail.setActivityStatus(institute.getActivityStatus());
		return detail;
	}

	public static List<InstituteDetail> from(List<Institute> institutes) {
		List<InstituteDetail> result = new ArrayList<InstituteDetail>();

		if (CollectionUtils.isEmpty(institutes)) {
			return result;
		}

		for (Institute institute : institutes) {
			result.add(from(institute));
		}

		return result;
	}

}