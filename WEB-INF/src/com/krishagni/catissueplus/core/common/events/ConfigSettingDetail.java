package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.domain.ConfigProperty;
import com.krishagni.catissueplus.core.common.domain.ConfigProperty.DataType;
import com.krishagni.catissueplus.core.common.domain.ConfigSetting;
import com.krishagni.catissueplus.core.common.domain.Module;

@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class ConfigSettingDetail implements Comparable<ConfigSettingDetail> {
	private String module;
	
	private String name;
	
	private String value;
	
	private DataType type;
	
	private Set<String> allowedValues;
	
	private String displayNameCode;
	
	private String descCode;
	
	private Date activationDate;

	private UserSummary activatedBy;
	
	private boolean secured;

	public String getModule() {
		return module;
	}

	public void setModule(String module) {
		this.module = module;
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public DataType getType() {
		return type;
	}

	public void setType(DataType type) {
		this.type = type;
	}

	public Set<String> getAllowedValues() {
		return allowedValues;
	}

	public void setAllowedValues(Set<String> allowedValues) {
		this.allowedValues = allowedValues;
	}

	public String getDisplayNameCode() {
		return displayNameCode;
	}

	public void setDisplayNameCode(String displayNameCode) {
		this.displayNameCode = displayNameCode;
	}

	public String getDescCode() {
		return descCode;
	}

	public void setDescCode(String descCode) {
		this.descCode = descCode;
	}
	
	public Date getActivationDate() {
		return activationDate;
	}

	public void setActivationDate(Date activationDate) {
		this.activationDate = activationDate;
	}

	public UserSummary getActivatedBy() {
		return activatedBy;
	}

	public void setActivatedBy(UserSummary activatedBy) {
		this.activatedBy = activatedBy;
	}

	public boolean isSecured() {
		return secured;
	}

	public void setSecured(boolean secured) {
		this.secured = secured;
	}

	@Override
	public int compareTo(ConfigSettingDetail o) {
		int cmp = module.compareTo(o.module);
		if (cmp == 0) {
			cmp = name.compareTo(o.name);
		}

		return cmp;
	}

	public static ConfigSettingDetail from(ConfigSetting setting) {
		return from(setting, false);
	}

	public static ConfigSettingDetail from(ConfigSetting setting, boolean includeActivatedBy) {
		ConfigSettingDetail result = new ConfigSettingDetail();
		
		ConfigProperty property = setting.getProperty();
		Module module = property.getModule();
		
		result.setModule(module.getName());
		result.setName(property.getName());
		result.setType(property.getDataType());
		result.setAllowedValues(new HashSet<>(property.getAllowedValues()));
		result.setDescCode(property.getDescCode());
		result.setDisplayNameCode(property.getDisplayNameCode());
		result.setSecured(property.isSecured());
		result.setValue(property.isSecured() ? "********" : setting.getValue());
		result.setActivationDate(setting.getActivationDate());
		if (includeActivatedBy) {
			result.setActivatedBy(UserSummary.from(setting.getActivatedBy()));
		}
		return result;
	}
	
	public static List<ConfigSettingDetail> from(Collection<ConfigSetting> settings) {
		return settings.stream().map(ConfigSettingDetail::from).sorted().collect(Collectors.toList());
	}

	public static List<ConfigSettingDetail> from(Collection<ConfigSetting> settings, boolean includeActivatedBy) {
		return settings.stream().map(s -> from(s, includeActivatedBy)).sorted().collect(Collectors.toList());
	}
}
