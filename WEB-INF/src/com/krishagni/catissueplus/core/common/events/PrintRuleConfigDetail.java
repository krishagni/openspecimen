package com.krishagni.catissueplus.core.common.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.krishagni.catissueplus.core.common.domain.PrintRuleConfig;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PrintRuleConfigDetail {
	private Long id;

	private String objectType;

	private Long instituteId;

	private String instituteName;

	private UserSummary updatedBy;

	private Date updatedOn;

	private String activityStatus;

	private String description;

	private Map<String, Object> rule;

	private Integer sortOrder;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getObjectType() {
		return objectType;
	}

	public void setObjectType(String objectType) {
		this.objectType = objectType;
	}

	public Long getInstituteId() {
		return instituteId;
	}

	public void setInstituteId(Long instituteId) {
		this.instituteId = instituteId;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public UserSummary getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(UserSummary updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdatedOn() {
		return updatedOn;
	}

	public void setUpdatedOn(Date updatedOn) {
		this.updatedOn = updatedOn;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, Object> getRule() {
		return rule;
	}

	public void setRule(Map<String, Object> rule) {
		this.rule = rule;
	}

	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}
	public static PrintRuleConfigDetail from(PrintRuleConfig rule) {
		PrintRuleConfigDetail detail = new PrintRuleConfigDetail();

		detail.setId(rule.getId());
		detail.setObjectType(rule.getObjectType());
		detail.setInstituteId(rule.getInstitute() != null ? rule.getInstitute().getId() : null);
		detail.setInstituteName(rule.getInstitute() != null ? rule.getInstitute().getName() : null);
		detail.setUpdatedBy(UserSummary.from(rule.getUpdatedBy()));
		detail.setUpdatedOn(rule.getUpdatedOn());
		detail.setActivityStatus(rule.getActivityStatus());
		detail.setDescription(rule.getDescription());
		detail.setRule(rule.getRuleDef(true));
		detail.setSortOrder(rule.getSortOrder());
		return detail;
	}

	public static List<PrintRuleConfigDetail> from(Collection<PrintRuleConfig> rules) {
		return rules.stream().map(PrintRuleConfigDetail::from).collect(Collectors.toList());
	}
}