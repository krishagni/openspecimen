package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.Service;
import com.krishagni.catissueplus.core.common.util.Utility;

public class ServiceDetail {
	private Long id;

	private Long cpId;

	private String cpShortTitle;

	private String code;

	private String description;

	private String activityStatus;

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

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
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

	public static ServiceDetail from(Service service) {
		ServiceDetail result = new ServiceDetail();
		result.setId(service.getId());
		result.setCpId(service.getCp().getId());
		result.setCpShortTitle(service.getCp().getShortTitle());
		result.setCode(service.getCode());
		result.setDescription(service.getDescription());
		result.setActivityStatus(service.getActivityStatus());
		return result;
	}

	public static List<ServiceDetail> from(Collection<Service> services) {
		return Utility.nullSafeStream(services).map(ServiceDetail::from).collect(Collectors.toList());
	}
}
