package com.krishagni.catissueplus.core.administrative.events;

import java.util.List;
import java.util.Collection;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.common.util.Utility;

import com.krishagni.catissueplus.core.administrative.domain.PvAttribute;

public class PvAttributeDetail {
	private String attribute;

	private String sourceAttribute;

	private String name;

	private String definition;

	private Long pvCount;

	private Long formId;

	private List<PermissibleValueDetails> pvs;

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public String getSourceAttribute() {
		return sourceAttribute;
	}

	public void setSourceAttribute(String sourceAttribute) {
		this.sourceAttribute = sourceAttribute;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDefinition() {
		return definition;
	}

	public void setDefinition(String definition) {
		this.definition = definition;
	}

	public Long getPvCount() {
		return pvCount;
	}

	public void setPvCount(Long pvCount) {
		this.pvCount = pvCount;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}

	public List<PermissibleValueDetails> getPvs() {
		return pvs;
	}

	public void setPvs(List<PermissibleValueDetails> pvs) {
		this.pvs = pvs;
	}

	public static PvAttributeDetail from(PvAttribute attr) {
		PvAttributeDetail result = new PvAttributeDetail();
		result.setAttribute(attr.getPublicId());
		result.setName(attr.getLongName());
		result.setDefinition(attr.getDefinition());
		result.setFormId(attr.getFormId());
		return result;
	}

	public static List<PvAttributeDetail> from(Collection<PvAttribute> attributes) {
		return Utility.nullSafeStream(attributes).map(PvAttributeDetail::from).collect(Collectors.toList());
	}
}
