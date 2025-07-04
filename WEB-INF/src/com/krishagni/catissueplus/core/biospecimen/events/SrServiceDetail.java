package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirementService;
import com.krishagni.catissueplus.core.common.util.Utility;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SrServiceDetail {
	private Long id;

	private String serviceCode;

	private String serviceDescription;

	private int units;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public static SrServiceDetail from(SpecimenRequirementService srSvc) {
		SrServiceDetail result = new SrServiceDetail();
		result.setId(srSvc.getId());
		result.setServiceCode(srSvc.getService().getCode());
		result.setServiceDescription(srSvc.getService().getDescription());
		result.setUnits(srSvc.getUnits());
		return result;
	}

	public static List<SrServiceDetail> from(Collection<SpecimenRequirementService> srServices) {
		return Utility.nullSafeStream(srServices).map(SrServiceDetail::from).collect(Collectors.toList());
	}
}
