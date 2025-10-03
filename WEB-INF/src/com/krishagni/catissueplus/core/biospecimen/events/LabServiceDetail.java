package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.LabService;
import com.krishagni.catissueplus.core.common.util.Utility;

public class LabServiceDetail {
	private Long id;

	private String code;

	private String description;

	private String activityStatus;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public static LabServiceDetail from(LabService service) {
		LabServiceDetail result = new LabServiceDetail();
		result.setId(service.getId());
		result.setCode(service.getCode());
		result.setDescription(service.getDescription());
		result.setActivityStatus(service.getActivityStatus());
		return result;
	}

	public static List<LabServiceDetail> from(Collection<LabService> services) {
		return Utility.nullSafeStream(services).map(LabServiceDetail::from).collect(Collectors.toList());
	}
}
