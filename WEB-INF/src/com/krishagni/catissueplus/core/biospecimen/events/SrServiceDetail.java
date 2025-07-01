package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirementService;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SrServiceDetail {
	private Long id;

	private Long reqId;

	private String cpShortTitle;

	private String reqCode;

	private String serviceCode;

	private int units;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getReqId() {
		return reqId;
	}

	public void setReqId(Long reqId) {
		this.reqId = reqId;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getReqCode() {
		return reqCode;
	}

	public void setReqCode(String reqCode) {
		this.reqCode = reqCode;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
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
		result.setCpShortTitle(srSvc.getRequirement().getCollectionProtocol().getShortTitle());
		result.setReqId(srSvc.getRequirement().getId());
		result.setReqCode(srSvc.getRequirement().getCode());
		result.setServiceCode(srSvc.getService().getCode());
		result.setUnits(srSvc.getUnits());
		return result;
	}

	public static List<SrServiceDetail> from(Collection<SpecimenRequirementService> srServices) {
		return Utility.nullSafeStream(srServices).map(SrServiceDetail::from).collect(Collectors.toList());
	}
}
