package com.krishagni.catissueplus.core.de.events;

import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public class FormFieldSummary {
	private String name;
	
	private String caption;
	
	private String type;
	
	private List<String> pvs;
	
	private List<FormFieldSummary> subFields;
	
	private Map<String, Object> lookupProps;

	private Boolean flatten;

	private Boolean multiple;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<String> getPvs() {
		return pvs;
	}

	public void setPvs(List<String> pvs) {
		this.pvs = pvs;
	}

	public List<FormFieldSummary> getSubFields() {
		return subFields;
	}

	public void setSubFields(List<FormFieldSummary> subFields) {
		this.subFields = subFields;
	}

	public Map<String, Object> getLookupProps() {
		return lookupProps;
	}

	public void setLookupProps(Map<String, Object> lookupProps) {
		this.lookupProps = lookupProps;
	}

	public void setLookupProps(Properties lookupProps) {
		this.lookupProps = lookupProps.entrySet().stream()
			.collect(Collectors.toMap(e -> e.getKey().toString(), Map.Entry::getValue));
	}

	public Boolean getFlatten() {
		return flatten;
	}

	public void setFlatten(boolean flatten) {
		this.flatten = flatten;
	}

	public Boolean getMultiple() {
		return multiple;
	}

	public void setMultiple(boolean multiple) {
		this.multiple = multiple;
	}
}
