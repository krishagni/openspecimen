package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.WorkflowConfigUtil;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolSavedEvent;
import com.krishagni.catissueplus.core.biospecimen.domain.CpGroupForm;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig.Workflow;
import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.services.FormService;

public class CpGroupSettingsApplier {
	private DaoFactory daoFactory;

	private FormService formSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void apply(CollectionProtocolGroup group, Set<CollectionProtocol> cps) {
		if (CollectionUtils.isEmpty(cps)) {
			return;
		}

		addFormContexts(group, cps);
		boolean rmgApplied = applyRequestManagerGroup(group.getRequestManagerGroup(), cps, false);
		boolean workflowsApplied = applyWorkflowsOnJoin(group.getWorkflows(), cps);
		if (rmgApplied || workflowsApplied) {
			publishCpSavedEvents(cps);
		}
	}

	public boolean applyRequestManagerGroup(RequestManagerGroup oldRmg, RequestManagerGroup newRmg, Set<CollectionProtocol> cps) {
		if (newRmg != null) {
			return applyRequestManagerGroup(newRmg, cps, true);
		}

		return removeRequestManagerGroup(oldRmg, cps, true);
	}

	public boolean applyWorkflows(Collection<Workflow> workflows, Set<CollectionProtocol> cps, boolean force) {
		return applyWorkflows(workflows, cps, force, true);
	}

	public void removeWorkflowsInheritance(Collection<CollectionProtocol> cps) {
		if (CollectionUtils.isEmpty(cps)) {
			return;
		}

		for (CollectionProtocol cp : cps) {
			CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cp.getId());
			if (cfg != null && cfg.getGroupWorkflowsInherited() != null) {
				cfg.setGroupWorkflowsInherited(null);
				daoFactory.getCollectionProtocolDao().saveCpWorkflows(cfg);
			}
		}
	}

	//
	// creates form contexts for all the group CPs using the input level and forms
	//
	public void addFormContexts(CollectionProtocolGroup group, String level, List<FormSummary> inputForms) {
		Map<Long, Set<Long>> cpForms = getCpForms(group.getCps(), level);
		for (CollectionProtocol cp : group.getCps()) {
			for (FormSummary form : inputForms) {
				addFormContext(cp, level, form, getCpFormIds(cpForms, cp.getId()));
			}
		}
	}

	//
	// creates form contexts for the input CPs using the existing group forms at all levels;
	//
	private void addFormContexts(CollectionProtocolGroup group, Set<CollectionProtocol> cps) {
		for (Map.Entry<String, Set<CpGroupForm>> levelForms : group.getFormsByLevel().entrySet()) {
			Map<Long, Set<Long>> cpForms = getCpForms(cps, levelForms.getKey());
			for (CollectionProtocol cp : cps) {
				for (CpGroupForm form : levelForms.getValue()) {
					addFormContext(cp, levelForms.getKey(), toFormSummary(form), getCpFormIds(cpForms, cp.getId()));
				}
			}
		}
	}

	private void addFormContext(CollectionProtocol cp, String level, FormSummary input, Set<Long> formIds) {
		if (CollectionUtils.isNotEmpty(formIds) && formIds.contains(input.getFormId())) {
			return;
		}

		CollectionProtocolSummary cpSummary = new CollectionProtocolSummary();
		cpSummary.setId(cp.getId());

		boolean multipleForms = isMultipleFormsLevel(level);
		FormContextDetail formCtxt = new FormContextDetail();
		formCtxt.setCollectionProtocol(cpSummary);
		formCtxt.setFormId(input.getFormId());
		formCtxt.setLevel(level);
		formCtxt.setMultiRecord(multipleForms && input.isMultipleRecords());
		formCtxt.setNotifEnabled(multipleForms && input.isNotifEnabled());
		if (formCtxt.isNotifEnabled()) {
			formCtxt.setDataInNotif(input.isDataInNotif());
			formCtxt.setNotifUserGroups(input.getNotifUserGroups());
		}

		ResponseEvent.unwrap(formSvc.addFormContexts(RequestEvent.wrap(Collections.singletonList(formCtxt))));
	}

	private Map<Long, Set<Long>> getCpForms(Set<CollectionProtocol> cps, String level) {
		List<Long> cpIds = cps.stream().map(CollectionProtocol::getId).collect(Collectors.toList());
		return daoFactory.getCpGroupDao().getCpForms(cpIds, level);
	}

	private Set<Long> getCpFormIds(Map<Long, Set<Long>> cpForms, Long cpId) {
		Set<Long> result = new HashSet<>();
		if (cpForms.get(-1L) != null) {
			result.addAll(cpForms.get(-1L));
		}

		if (cpForms.get(cpId) != null) {
			result.addAll(cpForms.get(cpId));
		}

		return result;
	}

	private boolean applyRequestManagerGroup(RequestManagerGroup rmg, Set<CollectionProtocol> cps, boolean publish) {
		// TODO: should we allow null rmg to be applied?
		if (rmg == null || CollectionUtils.isEmpty(cps)) {
			return false;
		}

		for (CollectionProtocol cp : cps) {
			cp.setRequestManagerGroup(rmg);
		}

		if (publish) {
			publishCpSavedEvents(cps);
		}

		return true;
	}

	private boolean removeRequestManagerGroup(RequestManagerGroup oldRmg, Set<CollectionProtocol> cps, boolean publish) {
		if (oldRmg == null || CollectionUtils.isEmpty(cps)) {
			return false;
		}

		Set<CollectionProtocol> updatedCps = new HashSet<>();
		for (CollectionProtocol cp : cps) {
			if (!Objects.equals(cp.getRequestManagerGroup(), oldRmg)) {
				continue;
			}

			cp.setRequestManagerGroup(null);
			updatedCps.add(cp);
		}

		if (CollectionUtils.isEmpty(updatedCps)) {
			return false;
		}

		if (publish) {
			publishCpSavedEvents(updatedCps);
		}

		return true;
	}

	private FormSummary toFormSummary(CpGroupForm form) {
		FormSummary input = new FormSummary();
		input.setFormId(form.getForm().getId());
		input.setDbForm(form.getForm());
		input.setMultipleRecords(form.isMultipleRecords());
		input.setNotifEnabled(form.isNotifEnabled());
		input.setDataInNotif(form.isDataInNotif());
		if (form.isNotifEnabled()) {
			input.setNotifUserGroups(form.getNotifUserGroups().stream()
				.map(ug -> {
					UserGroupSummary result = new UserGroupSummary();
					result.setId(ug.getId());
					return result;
				})
				.collect(Collectors.toList())
			);
		}
		return input;
	}

	private boolean isMultipleFormsLevel(String level) {
		return !level.equals("ParticipantExtension") &&
			!level.equals("VisitExtension") &&
			!level.equals("SpecimenExtension");
	}

	private boolean applyWorkflows(Collection<Workflow> workflows, Set<CollectionProtocol> cps, boolean force, boolean publish) {
		Collection<Workflow> workflowsToApply = workflows != null ? workflows : Collections.emptyList();
		return updateWorkflows(cps, publish, cp -> applyWorkflows(workflowsToApply, cp, force));
	}

	private boolean applyWorkflows(Collection<Workflow> workflows, CollectionProtocol cp, boolean force) {
		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cp.getId());
		if (!force && cfg != null && !Boolean.TRUE.equals(cfg.getGroupWorkflowsInherited())) {
			return false;
		}

		if (cfg == null) {
			cfg = newWorkflowConfig(cp);
		}

		saveWorkflows(workflows, cfg);
		return true;
	}

	private boolean applyWorkflowsOnJoin(Map<String, Workflow> groupWorkflows, Set<CollectionProtocol> cps) {
		Map<String, Workflow> workflowsToApply = groupWorkflows != null ? groupWorkflows : Collections.emptyMap();
		return updateWorkflows(cps, false, cp -> applyWorkflowsOnJoin(workflowsToApply, cp));
	}

	private boolean applyWorkflowsOnJoin(Map<String, Workflow> groupWorkflows, CollectionProtocol cp) {
		CpWorkflowConfig cfg = daoFactory.getCollectionProtocolDao().getCpWorkflows(cp.getId());
		if (cfg == null) {
			cfg = newWorkflowConfig(cp);
			saveWorkflows(groupWorkflows.values(), cfg);
			return true;
		}

		boolean inherited = WorkflowConfigUtil.areSame(cfg.getWorkflows(), groupWorkflows);
		if (Objects.equals(cfg.getGroupWorkflowsInherited(), inherited)) {
			return false;
		}

		cfg.setGroupWorkflowsInherited(inherited);
		daoFactory.getCollectionProtocolDao().saveCpWorkflows(cfg);
		return true;
	}

	private boolean updateWorkflows(Set<CollectionProtocol> cps, boolean publish, Function<CollectionProtocol, Boolean> updater) {
		if (CollectionUtils.isEmpty(cps)) {
			return false;
		}

		Set<CollectionProtocol> updatedCps = cps.stream()
			.filter(cp -> Boolean.TRUE.equals(updater.apply(cp)))
			.collect(Collectors.toSet());
		if (publish && CollectionUtils.isNotEmpty(updatedCps)) {
			publishCpSavedEvents(updatedCps);
		}

		return CollectionUtils.isNotEmpty(updatedCps);
	}

	private CpWorkflowConfig newWorkflowConfig(CollectionProtocol cp) {
		CpWorkflowConfig cfg = new CpWorkflowConfig();
		cfg.setCp(cp);
		return cfg;
	}

	private CpWorkflowConfig saveWorkflows(Collection<Workflow> workflows, CpWorkflowConfig cfg) {
		cfg.getWorkflows().clear();
		if (workflows != null) {
			for (Workflow workflow : workflows) {
				cfg.getWorkflows().put(workflow.getName(), copyWorkflow(workflow));
			}
		}

		cfg.setGroupWorkflowsInherited(true);
		daoFactory.getCollectionProtocolDao().saveCpWorkflows(cfg);
		return cfg;
	}

	private Workflow copyWorkflow(Workflow input) {
		Workflow result = new Workflow();
		BeanUtils.copyProperties(input, result, "data");
		if (input.getData() != null) {
			result.setData(Utility.objectToMap(input.getData()));
		}

		return result;
	}

	private void publishCpSavedEvents(Set<CollectionProtocol> cps) {
		for (CollectionProtocol cp : cps) {
			EventPublisher.getInstance().publish(new CollectionProtocolSavedEvent(cp));
		}
	}
}
