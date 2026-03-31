package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonFilter;

import com.krishagni.catissueplus.core.biospecimen.domain.CpSpecimenLabelPrintSetting;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;

@JsonFilter("withoutId")
public class CpSpecimenLabelPrintSettingDetail implements Comparable<CpSpecimenLabelPrintSettingDetail> {
	private Long id;
	
	private String lineage;
	
	private String printMode;
	
	private Integer copies;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getLineage() {
		return lineage;
	}

	public void setLineage(String lineage) {
		this.lineage = lineage;
	}

	public String getPrintMode() {
		return printMode;
	}

	public void setPrintMode(String printMode) {
		this.printMode = printMode;
	}

	public Integer getCopies() {
		return copies;
	}

	public void setCopies(Integer copies) {
		this.copies = copies;
	}
	
	public static CpSpecimenLabelPrintSettingDetail from(CpSpecimenLabelPrintSetting setting) {
		CpSpecimenLabelPrintSettingDetail detail = new CpSpecimenLabelPrintSettingDetail();
		detail.setId(setting.getId());
		detail.setLineage(setting.getLineage());
		detail.setPrintMode(setting.getPrintMode() != null ? setting.getPrintMode().name() : null);
		detail.setCopies(setting.getCopies());
		return detail;
	}
	
	public static List<CpSpecimenLabelPrintSettingDetail> from(Set<CpSpecimenLabelPrintSetting> settings) {
		return settings.stream()
			.map(CpSpecimenLabelPrintSettingDetail::from)
			.sorted()
			.collect(Collectors.toList());
	}

	@Override
	public int compareTo(CpSpecimenLabelPrintSettingDetail other) {
		return LINEAGE_ORDER.indexOf(getLineage()) - LINEAGE_ORDER.indexOf(other.getLineage());
	}

	private static List<String> LINEAGE_ORDER = Arrays.asList(new String[] {
		Specimen.NEW,
		Specimen.DERIVED,
		Specimen.ALIQUOT
	});
}
