package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.biospecimen.domain.LabSpecimenService;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;

public class SpecimenServiceDetail {
	private Long id;

	private String cpShortTitle;

	private String label;

	private String barcode;

	private Long specimenId;

	private String serviceCode;

	private Long serviceId;

	private int units;

	private Date serviceDate;

	private UserSummary servicedBy;

	private String comments;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getBarcode() {
		return barcode;
	}

	public void setBarcode(String barcode) {
		this.barcode = barcode;
	}

	public Long getSpecimenId() {
		return specimenId;
	}

	public void setSpecimenId(Long specimenId) {
		this.specimenId = specimenId;
	}

	public String getServiceCode() {
		return serviceCode;
	}

	public void setServiceCode(String serviceCode) {
		this.serviceCode = serviceCode;
	}

	public Long getServiceId() {
		return serviceId;
	}

	public void setServiceId(Long serviceId) {
		this.serviceId = serviceId;
	}

	public int getUnits() {
		return units;
	}

	public void setUnits(int units) {
		this.units = units;
	}

	public Date getServiceDate() {
		return serviceDate;
	}

	public void setServiceDate(Date serviceDate) {
		this.serviceDate = serviceDate;
	}

	public UserSummary getServicedBy() {
		return servicedBy;
	}

	public void setServicedBy(UserSummary servicedBy) {
		this.servicedBy = servicedBy;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public static SpecimenServiceDetail from(LabSpecimenService spmnSvc) {
		return SpecimenServiceDetail.from(spmnSvc.getSpecimen(), spmnSvc);
	}

	public static SpecimenServiceDetail from(Specimen specimen, LabSpecimenService spmnSvc) {
		SpecimenServiceDetail result = new SpecimenServiceDetail();
		result.setId(spmnSvc.getId());
		result.setCpShortTitle(specimen.getCpShortTitle());
		result.setLabel(specimen.getLabel());
		result.setBarcode(specimen.getBarcode());
		result.setSpecimenId(specimen.getId());
		result.setServiceCode(spmnSvc.getService().getCode());
		result.setServiceId(spmnSvc.getService().getId());
		result.setUnits(spmnSvc.getUnits());
		result.setServiceDate(spmnSvc.getServiceDate());
		result.setServicedBy(UserSummary.from(spmnSvc.getServicedBy()));
		result.setComments(spmnSvc.getComments());
		return result;
	}

	public static List<SpecimenServiceDetail> from(Collection<LabSpecimenService> services) {
		return Utility.nullSafeStream(services).map(SpecimenServiceDetail::from).collect(Collectors.toList());
	}

	public static List<SpecimenServiceDetail> from(Specimen specimen, Collection<LabSpecimenService> services) {
		return Utility.nullSafeStream(services).map(svc -> from(specimen, svc)).collect(Collectors.toList());
	}
}
