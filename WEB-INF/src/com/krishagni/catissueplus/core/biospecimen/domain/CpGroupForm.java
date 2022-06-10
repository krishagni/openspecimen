package com.krishagni.catissueplus.core.biospecimen.domain;


import java.util.HashSet;
import java.util.Set;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.de.domain.Form;

public class CpGroupForm extends BaseEntity {
	private CollectionProtocolGroup group;

	private String level;

	private Form form;

	private boolean multipleRecords;

	private boolean notifEnabled;

	private boolean dataInNotif;

	private Set<UserGroup> notifUserGroups = new HashSet<>();

	public CollectionProtocolGroup getGroup() {
		return group;
	}

	public void setGroup(CollectionProtocolGroup group) {
		this.group = group;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Form getForm() {
		return form;
	}

	public void setForm(Form form) {
		this.form = form;
	}

	public boolean isMultipleRecords() {
		return multipleRecords;
	}

	public void setMultipleRecords(boolean multipleRecords) {
		this.multipleRecords = multipleRecords;
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

	public Set<UserGroup> getNotifUserGroups() {
		return notifUserGroups;
	}

	public void setNotifUserGroups(Set<UserGroup> notifUserGroups) {
		this.notifUserGroups = notifUserGroups;
	}
}
