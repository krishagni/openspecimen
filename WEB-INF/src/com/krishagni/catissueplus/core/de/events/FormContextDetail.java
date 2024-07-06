package com.krishagni.catissueplus.core.de.events;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;

public class FormContextDetail {
	private Long formCtxtId;
	
	private CollectionProtocolSummary collectionProtocol;

	private String instituteName;
	
	private String level;

	private Long entityId;
	
	private Long formId;
	
	private Integer sortOrder;

	private boolean multiRecord;

	private boolean notifEnabled;

	private boolean dataInNotif;

	private List<UserGroupSummary> notifUserGroups;
	
	private boolean sysForm;

	public Long getFormCtxtId() {
		return formCtxtId;
	}

	public void setFormCtxtId(Long formCtxtId) {
		this.formCtxtId = formCtxtId;
	}

	public CollectionProtocolSummary getCollectionProtocol() {
		return collectionProtocol;
	}

	public void setCollectionProtocol(CollectionProtocolSummary collectionProtocol) {
		this.collectionProtocol = collectionProtocol;
	}

	public String getInstituteName() {
		return instituteName;
	}

	public void setInstituteName(String instituteName) {
		this.instituteName = instituteName;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityId(Long entityId) {
		this.entityId = entityId;
	}

	public Long getFormId() {
		return formId;
	}

	public void setFormId(Long formId) {
		this.formId = formId;
	}
	
	public Integer getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(Integer sortOrder) {
		this.sortOrder = sortOrder;
	}

	public boolean isMultiRecord() {
		return multiRecord;
	}

	public void setMultiRecord(boolean multiRecord) {
		this.multiRecord = multiRecord;
	}

	public boolean isNotifEnabled() {
		return notifEnabled;
	}

	public void setNotifEnabled(boolean notifEnabled) {
		this.notifEnabled = notifEnabled;
	}

	public boolean isDataInNotif() {
		return dataInNotif;
	}

	public void setDataInNotif(boolean dataInNotif) {
		this.dataInNotif = dataInNotif;
	}

	public List<UserGroupSummary> getNotifUserGroups() {
		return notifUserGroups;
	}

	public void setNotifUserGroups(List<UserGroupSummary> notifUserGroups) {
		this.notifUserGroups = notifUserGroups;
	}

	public boolean isSysForm() {
		return sysForm;
	}

	public void setSysForm(boolean sysForm) {
		this.sysForm = sysForm;
	}
}
