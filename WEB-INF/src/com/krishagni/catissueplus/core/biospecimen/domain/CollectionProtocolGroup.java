package com.krishagni.catissueplus.core.biospecimen.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.audit.services.impl.DeleteLogUtil;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.events.FormSummary;

@Configurable
public class CollectionProtocolGroup extends BaseEntity {
	private String name;

	private String activityStatus;

	private Set<CollectionProtocol> cps = new HashSet<>();

	private Set<CpGroupForm> forms = new HashSet<>();

	private Map<String, CpWorkflowConfig.Workflow> workflows = new HashMap<>();

	@Autowired
	private DaoFactory daoFactory;

	private transient Map<String, Set<Form>> formsMap;

	private transient Integer cpsCount;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Set<CollectionProtocol> getCps() {
		return cps;
	}

	public void setCps(Set<CollectionProtocol> cps) {
		this.cps = cps;
	}

	public Set<CpGroupForm> getForms() {
		return forms;
	}

	public void setForms(Set<CpGroupForm> forms) {
		this.forms = forms;
	}

	public Map<String, CpWorkflowConfig.Workflow> getWorkflows() {
		return workflows;
	}

	public void setWorkflows(Map<String, CpWorkflowConfig.Workflow> workflows) {
		this.workflows = workflows;
	}

	public String getWorkflowsJson() {
		try {
			return getWriteMapper().writeValueAsString(workflows.values());
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public void setWorkflowsJson(String json) {
		try {
			if (StringUtils.isBlank(json)) {
				return;
			}

			CpWorkflowConfig.Workflow[] workflows = getReadMapper().readValue(json, CpWorkflowConfig.Workflow[].class);
			this.workflows.clear();
			for (CpWorkflowConfig.Workflow workflow : workflows) {
				this.workflows.put(workflow.getName(), workflow);
			}
		} catch (Exception e) {
			throw OpenSpecimenException.serverError(e);
		}
	}

	public Map<String, Set<CpGroupForm>> getFormsByLevel() {
		Map<String, Set<CpGroupForm>> result = new HashMap<>();
		for (CpGroupForm form : getForms()) {
			Set<CpGroupForm> levelForms = result.computeIfAbsent(form.getLevel(), (k) -> new HashSet<>());
			levelForms.add(form);
		}

		return result;
	}

	public Set<CpGroupForm> getForms(String level) {
		return getForms().stream().filter(f -> f.getLevel().equals(level)).collect(Collectors.toSet());
	}

	public Integer getCpsCount() {
		return cpsCount;
	}

	public void setCpsCount(int cpsCount) {
		this.cpsCount = cpsCount;
	}

	public void update(CollectionProtocolGroup other) {
		if (Status.isDisabledStatus(other.getActivityStatus())) {
			delete(null);
			return;
		}

		setName(other.getName());
		setCps(other.getCps());
	}

	public void delete(String reason) {
		setName(Utility.getDisabledValue(getName(), 64));
		getCps().clear();
		getForms().clear();

		if (StringUtils.isNotBlank(reason)) {
			setOpComments(reason);
			DeleteLogUtil.getInstance().log(this);
		}

		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public void addForms(String level, List<Pair<Form, FormSummary>> formsToAdd) {
		for (Pair<Form, FormSummary> form : formsToAdd) {
			if (containsForm(level, form.first())) {
				continue;
			}

			FormSummary input = form.second();
			CpGroupForm groupForm = new CpGroupForm();
			groupForm.setGroup(this);
			groupForm.setLevel(level);
			groupForm.setForm(form.first());
			groupForm.setMultipleRecords(input.isMultipleRecords());
			groupForm.setNotifEnabled(input.isNotifEnabled());
			if (groupForm.isNotifEnabled()) {
				groupForm.setDataInNotif(input.isDataInNotif());
				Utility.nullSafeStream(input.getNotifUserGroups()).forEach(
					(iug) -> {
						Object key = null;
						UserGroup ug = null;
						if (iug.getId() != null) {
							ug = daoFactory.getUserGroupDao().getById(iug.getId());
							key = iug.getId();
						} else if (StringUtils.isNotBlank(iug.getName())) {
							ug = daoFactory.getUserGroupDao().getByName(iug.getName());
							key = iug.getName();
						}

						if (key != null && ug == null) {
							throw OpenSpecimenException.userError(UserGroupErrorCode.NOT_FOUND, key);
						}

						if (ug != null) {
							groupForm.getNotifUserGroups().add(ug);
						}
					}
				);
			}

			forms.add(groupForm);
		}
	}

	public void removeForms(String level, List<Form> formsToRemove) {
		forms.removeIf(form -> form.getLevel().equals(level) && formsToRemove.contains(form.getForm()));
	}

	public boolean containsForm(CpGroupForm form) {
		return containsForm(form.getLevel(), form.getForm());
	}

	public boolean containsForm(String level, Form form) {
		initFormsMap();
		return formsMap.get(level) != null && formsMap.get(level).contains(form);
	}

	public List<Long> getCpIds() {
		return getCps().stream().map(CollectionProtocol::getId).collect(Collectors.toList());
	}

	private void initFormsMap() {
		if (formsMap != null) {
			return;
		}

		formsMap = new HashMap<>();
		for (CpGroupForm form : forms) {
			Set<Form> levelForms = formsMap.computeIfAbsent(form.getLevel(), (k) -> new HashSet<>());
			levelForms.add(form.getForm());
		}
	}

	private ObjectMapper getReadMapper() {
		return new ObjectMapper();
	}

	private ObjectMapper getWriteMapper() {
		ObjectMapper mapper = new ObjectMapper();
		mapper.setVisibility(
			mapper.getSerializationConfig().getDefaultVisibilityChecker()
				.withFieldVisibility(JsonAutoDetect.Visibility.ANY)
				.withGetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withSetterVisibility(JsonAutoDetect.Visibility.NONE)
				.withCreatorVisibility(JsonAutoDetect.Visibility.NONE));
		return mapper;
	}
}
