package com.krishagni.rbac.events;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.MessageUtil;

public class SubjectRoleExportDetail {
	private Long subjectId;

	private String firstName;

	private String lastName;

	private String emailAddress;

	private String instituteName;

	private String primarySite;

	private String domainName;

	private boolean apiUser;

	private boolean manageWfs;

	private boolean manageForms;

	private String type;

	private String activityStatus;

	private String roleName;

	private String siteName;

	private String cpShortTitle;

	public Long getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(Long subjectId) {
		this.subjectId = subjectId;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmailAddress() {
		return emailAddress;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public String getPrimarySite() {
		return primarySite;
	}

	public void setPrimarySite(String primarySite) {
		this.primarySite = primarySite;
	}

	public String getDomainName() {
		return domainName;
	}

	public void setDomainName(String domainName) {
		this.domainName = domainName;
	}

	public boolean isApiUser() {
		return apiUser;
	}

	public void setApiUser(boolean apiUser) {
		this.apiUser = apiUser;
	}

	public boolean isManageWfs() {
		return manageWfs;
	}

	public void setManageWfs(boolean manageWfs) {
		this.manageWfs = manageWfs;
	}

	public boolean isManageForms() {
		return manageForms;
	}

	public void setManageForms(boolean manageForms) {
		this.manageForms = manageForms;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getRoleName() {
		return roleName;
	}

	public void setRoleName(String roleName) {
		this.roleName = roleName;
	}

	public String getSiteName() {
		return siteName;
	}

	public void setSiteName(String siteName) {
		this.siteName = siteName;
	}

	public String getCpShortTitle() {
		return cpShortTitle;
	}

	public void setCpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
	}

	public static SubjectRoleExportDetail from(UserSummary user, String roleName, String cpShortTitle, String siteName) {
		SubjectRoleExportDetail result = new SubjectRoleExportDetail();
		result.setSubjectId(user.getId());
		result.setFirstName(user.getFirstName());
		result.setLastName(user.getLastName());
		result.setEmailAddress(user.getEmailAddress());
		result.setInstituteName(user.getInstituteName());
		result.setPrimarySite(user.getPrimarySite());
		result.setDomainName(user.getDomain());
		result.setApiUser(isTrue(user.getApiUser()));
		result.setManageWfs(isTrue(user.getManageWfs()));
		result.setManageForms(isTrue(user.getManageForms()));
		result.setType(getUserType(user.getType()));
		result.setActivityStatus(user.getActivityStatus());
		result.setRoleName(roleName);
		result.setCpShortTitle(cpShortTitle);
		result.setSiteName(siteName);
		return result;
	}

	private static boolean isTrue(Boolean truth) {
		return Boolean.TRUE.equals(truth);
	}

	private static String getUserType(String type) {
		if (StringUtils.isBlank(type)) {
			return null;
		}

		return switch (type) {
			case "SUPER" -> MessageUtil.getInstance().getMessage("user_type_super_admin");
			case "INSTITUTE" -> MessageUtil.getInstance().getMessage("user_type_institute_admin");
			case "CONTACT" -> MessageUtil.getInstance().getMessage("user_type_contact");
			case "NONE" -> MessageUtil.getInstance().getMessage("user_type_regular");
			default -> "Unexpected value: " + type;
		};
	}
}
