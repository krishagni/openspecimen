package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CpGroupForm;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CollectionProtocolGroupFactory;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupFormsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolGroupService;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolService;
import com.krishagni.catissueplus.core.common.Pair;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormContextDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class CollectionProtocolGroupServiceImpl implements CollectionProtocolGroupService {
	private CollectionProtocolGroupFactory groupFactory;

	private DaoFactory daoFactory;

	private FormDao formDao;

	private FormService formSvc;

	private CollectionProtocolService cpSvc;

	private ExportService exportSvc;

	private ImportService importSvc;

	public void setGroupFactory(CollectionProtocolGroupFactory groupFactory) {
		this.groupFactory = groupFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void setCpSvc(CollectionProtocolService cpSvc) {
		this.cpSvc = cpSvc;
	}

	public void setExportSvc(ExportService exportSvc) {
		this.exportSvc = exportSvc;
	}

	public void setImportSvc(ImportService importSvc) {
		this.importSvc = importSvc;
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CollectionProtocolGroupSummary>> getGroups(RequestEvent<CpGroupListCriteria> req) {
		try {
			CpGroupListCriteria crit = req.getPayload();
			Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
			if (siteCps != null && siteCps.isEmpty()) {
				return ResponseEvent.response(Collections.emptyList());
			}

			List<CollectionProtocolGroup> groups = daoFactory.getCpGroupDao().getGroups(crit.siteCps(siteCps));
			if (crit.includeStat() && !groups.isEmpty()) {
				Map<Long, CollectionProtocolGroup> groupsMap = groups.stream()
					.collect(Collectors.toMap(CollectionProtocolGroup::getId, g -> g));
				Map<Long, Integer> counts = daoFactory.getCpGroupDao().getCpsCount(groupsMap.keySet());
				for (Map.Entry<Long, Integer> count : counts.entrySet()) {
					groupsMap.get(count.getKey()).setCpsCount(count.getValue());
				}
			}

			return ResponseEvent.response(CollectionProtocolGroupSummary.from(groups));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolGroupDetail> getGroup(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			CollectionProtocolGroup group = getGroup(crit.getId(), crit.getName());
			ensureReadAccess(group);
			return ResponseEvent.response(CollectionProtocolGroupDetail.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolGroupDetail> createGroup(RequestEvent<CollectionProtocolGroupDetail> req) {
		try {
			CollectionProtocolGroup group = groupFactory.createGroup(req.getPayload());
			ensureUniqueName(null, group);
			ensureCpsNotInOtherGroups(group);
			ensureUpdateAccess(group);
			daoFactory.getCpGroupDao().saveOrUpdate(group);
			return ResponseEvent.response(CollectionProtocolGroupDetail.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolGroupDetail> updateGroup(RequestEvent<CollectionProtocolGroupDetail> req) {
		try {
			CollectionProtocolGroup existingGroup = getGroup(req.getPayload().getId(), null);
			ensureUpdateAccess(existingGroup);

			CollectionProtocolGroup group = groupFactory.createGroup(req.getPayload());
			ensureUniqueName(existingGroup, group);
			ensureCpsNotInOtherGroups(group);
			ensureUpdateAccess(group);

			Set<CollectionProtocol> addedCps = Utility.subtract(group.getCps(), existingGroup.getCps());
			existingGroup.update(group);
			addCps(existingGroup, addedCps);

			return ResponseEvent.response(CollectionProtocolGroupDetail.from(existingGroup));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<BulkDeleteEntityResp<CollectionProtocolGroupSummary>> deleteGroups(RequestEvent<BulkDeleteEntityOp> req) {
		Set<Long> groupIds = req.getPayload().getIds();

		List<CollectionProtocolGroup> groups = daoFactory.getCpGroupDao().getByIds(groupIds);
		if (groupIds.size() != groups.size()) {
			groups.forEach(group -> groupIds.remove(group.getId()));
			throw OpenSpecimenException.userError(CpGroupErrorCode.NOT_FOUND, groupIds);
		}

		groups.forEach(this::ensureUpdateAccess);
		groups.forEach(group -> group.delete(req.getPayload().getReason()));

		BulkDeleteEntityResp<CollectionProtocolGroupSummary> resp = new BulkDeleteEntityResp<>();
		resp.setCompleted(true);
		resp.setEntities(CollectionProtocolGroupSummary.from(groups));
		return ResponseEvent.response(resp);
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<CpGroupFormsDetail>> getForms(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			CollectionProtocolGroup group = getGroup(crit.getId(), crit.getName());
			ensureReadAccess(group);

			String level = crit.paramString("level");
			Map<String, Set<CpGroupForm>> groupFormsByLevel;
			if (StringUtils.isBlank(level)) {
				groupFormsByLevel = group.getFormsByLevel();
			} else {
				groupFormsByLevel = Collections.singletonMap(level, group.getForms(level));
			}

			List<CpGroupFormsDetail> result = new ArrayList<>();
			for (Map.Entry<String, Set<CpGroupForm>> gfEntry : groupFormsByLevel.entrySet()) {
				List<Form> forms = gfEntry.getValue().stream().map(CpGroupForm::getForm).collect(Collectors.toList());
				result.add(CpGroupFormsDetail.from(group, gfEntry.getKey(), gfEntry.getValue()));
			}

			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> addForms(RequestEvent<CpGroupFormsDetail> req) {
		try {
			CpGroupFormsDetail input = req.getPayload();
			if (CollectionUtils.isEmpty(input.getForms())) {
				return ResponseEvent.response(false);
			}

			CollectionProtocolGroup group = getGroup(input.getGroupId(), input.getGroupName());
			ensureUpdateAccess(group);

			List<Pair<Form, FormSummary>> forms = new ArrayList<>();
			for (FormSummary inputForm : input.getForms()) {
				Form form = getForm(inputForm.getFormId(), inputForm.getName());
				if (group.containsForm(input.getLevel(), form)) {
					continue;
				}

				forms.add(Pair.make(form, inputForm));
			}

			if (!isMultipleFormsLevel(input.getLevel())) {
				forms = Collections.singletonList(forms.get(forms.size() - 1));
			}

			addForms(group, input.getLevel(), forms);
			group.addForms(input.getLevel(), forms);
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Boolean> removeForms(RequestEvent<CpGroupFormsDetail> req) {
		try {
			CpGroupFormsDetail input = req.getPayload();
			if (CollectionUtils.isEmpty(input.getForms())) {
				return ResponseEvent.response(false);
			}

			CollectionProtocolGroup group = getGroup(input.getGroupId(), input.getGroupName());
			ensureUpdateAccess(group);

			List<Form> forms = new ArrayList<>();
			for (FormSummary inputForm : input.getForms()) {
				Form form = getForm(inputForm.getFormId(), inputForm.getName());
				if (!group.containsForm(input.getLevel(), form)) {
					continue;
				}

				forms.add(form);
			}

			removeForms(group, input.getLevel(), forms);
			group.removeForms(input.getLevel(), forms);
			return ResponseEvent.response(true);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpGroupWorkflowCfgDetail> getWorkflows(RequestEvent<EntityQueryCriteria> req) {
		try {
			Date startTime = Calendar.getInstance().getTime();
			EntityQueryCriteria crit = req.getPayload();
			CollectionProtocolGroup group = getGroup(crit.getId(), crit.getName());
			ensureReadAccess(group);

			if (crit.paramBoolean("export")) {
				exportSvc.saveJob("cpgWf", startTime, Collections.singletonMap("cpGroupId", group.getId().toString()));
			}

			return ResponseEvent.response(CpGroupWorkflowCfgDetail.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CpGroupWorkflowCfgDetail> saveWorkflows(RequestEvent<CpGroupWorkflowCfgDetail> req) {
		try {
			Date startTime = Calendar.getInstance().getTime();
			CpGroupWorkflowCfgDetail input = req.getPayload();
			CollectionProtocolGroup group = getGroup(input.getGroupId(), input.getGroupName());
			ensureUpdateAccess(group);

			CpWorkflowCfgDetail cpCfgDetail = new CpWorkflowCfgDetail();
			cpCfgDetail.setWorkflows(input.getWorkflows());
			for (CollectionProtocol cp : group.getCps()) {
				cpCfgDetail.setCpId(cp.getId());
				cpSvc.saveWorkflows(cp, cpCfgDetail);
			}

			if (input.getWorkflows() != null) {
				Map<String, CpWorkflowConfig.Workflow> wfMap = new HashMap<>();
				for (WorkflowDetail detail : input.getWorkflows().values()) {
					CpWorkflowConfig.Workflow wf = new CpWorkflowConfig.Workflow();
					BeanUtils.copyProperties(detail, wf);
					wfMap.put(wf.getName(), wf);
				}

				group.setWorkflows(wfMap);
			}

			daoFactory.getCpGroupDao().saveOrUpdate(group);
			if (Boolean.TRUE.equals(input.getImportWfs())) {
				importSvc.saveJob("cpgWf", "UPSERT", startTime, Collections.singletonMap("cpGroupId", group.getId().toString()));
			}

			return ResponseEvent.response(CpGroupWorkflowCfgDetail.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	private void ensureReadAccess(CollectionProtocolGroup group) {
		for (CollectionProtocol cp : group.getCps()) {
			try {
				AccessCtrlMgr.getInstance().ensureReadCpRights(cp);
				return;
			} catch (OpenSpecimenException ose) {
				// continue testing with the next CP
			}
		}

		if (!group.getCps().isEmpty() || !AuthUtil.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUpdateAccess(CollectionProtocolGroup group) {
		for (CollectionProtocol cp : group.getCps()) {
			AccessCtrlMgr.getInstance().ensureUpdateCpRights(cp);
		}

		if (group.getCps().isEmpty() && !AuthUtil.isAdmin()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUniqueName(CollectionProtocolGroup existingGrp, CollectionProtocolGroup newGrp) {
		if (existingGrp != null && existingGrp.getName().equals(newGrp.getName())) {
			return;
		}

		CollectionProtocolGroup dbGroup = daoFactory.getCpGroupDao().getByName(newGrp.getName());
		if (dbGroup != null) {
			throw OpenSpecimenException.userError(CpGroupErrorCode.DUP_NAME, newGrp.getName());
		}
	}

	private void ensureCpsNotInOtherGroups(CollectionProtocolGroup group) {
		List<String> cpsInOtherGroups = daoFactory.getCpGroupDao().getCpsUsedInOtherGroups(group);
		if (CollectionUtils.isNotEmpty(cpsInOtherGroups)) {
			throw OpenSpecimenException.userError(
				CpGroupErrorCode.CP_IN_OTH_GRPS,
				String.join(",", cpsInOtherGroups),
				cpsInOtherGroups.size());
		}
	}

	private CollectionProtocolGroup getGroup(Long groupId, String groupName) {
		CollectionProtocolGroup group = null;
		Object key = null;

		if (groupId != null) {
			group = daoFactory.getCpGroupDao().getById(groupId);
			key = groupId;
		} else if (StringUtils.isNotBlank(groupName)) {
			group = daoFactory.getCpGroupDao().getByName(groupName);
			key = groupName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(CpGroupErrorCode.NAME_REQ);
		} else if (group == null) {
			throw OpenSpecimenException.userError(CpGroupErrorCode.NOT_FOUND, key);
		}

		return group;
	}

	private void addCps(CollectionProtocolGroup group, Set<CollectionProtocol> cps) {
		if (CollectionUtils.isEmpty(cps)) {
			return;
		}

		//
		// add forms
		//
		List<Long> cpIds = cps.stream().map(CollectionProtocol::getId).collect(Collectors.toList());
		for (Map.Entry<String, Set<CpGroupForm>> levelForms : group.getFormsByLevel().entrySet()) {
			Map<Long, Set<Long>> cpForms = daoFactory.getCpGroupDao().getCpForms(cpIds, levelForms.getKey());
			for (CollectionProtocol cp : cps) {
				Set<Long> formIds = getCpFormIds(cpForms, cp.getId());
				for (CpGroupForm form : levelForms.getValue()) {
					FormSummary input = new FormSummary();
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

					addForm(cp, levelForms.getKey(), form.getForm(), input, formIds);
				}
			}
		}

		//
		// add workflows
		//
		Map<String, CpWorkflowConfig.Workflow> groupWfs = group.getWorkflows();
		if (!groupWfs.isEmpty()) {
			CpWorkflowCfgDetail cpWfs = new CpWorkflowCfgDetail();
			cpWfs.setWorkflows(WorkflowDetail.from(groupWfs.values()));
			for (CollectionProtocol cp : cps) {
				cpWfs.setCpId(cp.getId());
				cpSvc.saveWorkflows(cp, cpWfs);
			}
		}
	}

	private void addForms(CollectionProtocolGroup group, String level, List<Pair<Form, FormSummary>> inputForms) {
		Map<Long, Set<Long>> cpForms = daoFactory.getCpGroupDao().getCpForms(group.getCpIds(), level);
		for (CollectionProtocol cp : group.getCps()) {
			Set<Long> formIds = getCpFormIds(cpForms, cp.getId());
			for (Pair<Form, FormSummary> form : inputForms) {
				addForm(cp, level, form.first(), form.second(), formIds);
			}
		}
	}

	private void addForm(CollectionProtocol cp, String level, Form form, FormSummary input, Set<Long> formIds) {
		boolean multipleForms = isMultipleFormsLevel(level);
		if (CollectionUtils.isNotEmpty(formIds)) {
			if (formIds.contains(form.getId())) {
				return;
			}
		}

		CollectionProtocolSummary cpSummary = new CollectionProtocolSummary();
		cpSummary.setId(cp.getId());

		FormContextDetail formCtxt = new FormContextDetail();
		formCtxt.setCollectionProtocol(cpSummary);
		formCtxt.setFormId(form.getId());
		formCtxt.setLevel(level);
		formCtxt.setMultiRecord(multipleForms && input.isMultipleRecords());
		formCtxt.setNotifEnabled(multipleForms && input.isNotifEnabled());
		if (formCtxt.isNotifEnabled()) {
			formCtxt.setDataInNotif(input.isDataInNotif());
			formCtxt.setNotifUserGroups(input.getNotifUserGroups());
		}

		ResponseEvent.unwrap(formSvc.addFormContexts( RequestEvent.wrap(Collections.singletonList(formCtxt))));
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

	private boolean removeForms(CollectionProtocolGroup group, String level, List<Form> inputForms) {
		Map<Long, Set<Long>> cpForms = daoFactory.getCpGroupDao().getCpForms(group.getCpIds(), level);

		boolean removed = false;
		for (Form form : inputForms) {
			for (CollectionProtocol cp : group.getCps()) {
				Set<Long> formIds = cpForms.get(cp.getId());
				if (CollectionUtils.isEmpty(formIds) || !formIds.contains(form.getId())) {
					continue;
				}

				removeForm(cp.getId(), form, level);
				removed = true;
			}
		}

		return removed;
	}

	private void removeForm(Long cpId, Form form, String level) {
		RemoveFormContextOp op = new RemoveFormContextOp();
		op.setCpId(cpId);
		op.setFormId(form.getId());
		op.setEntityType(level);
		op.setRemoveType(RemoveFormContextOp.RemoveType.SOFT_REMOVE);
		ResponseEvent<Boolean> resp = formSvc.removeFormContext(new RequestEvent<>(op));
		resp.throwErrorIfUnsuccessful();
	}

	private Form getForm(Long formId, String formName) {
		Form form = null;
		Object key = null;

		if (formId != null) {
			form = formDao.getFormById(formId);
			key = formId;
		} else if (StringUtils.isNotBlank(formName)) {
			form = formDao.getFormByName(formName);
			key = formName;
		}

		if (key == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NAME_REQUIRED);
		} else if (form == null) {
			throw OpenSpecimenException.userError(FormErrorCode.NOT_FOUND, key, 1);
		}

		return form;
	}

	private boolean isMultipleFormsLevel(String level) {
		return !level.equals("ParticipantExtension") &&
			!level.equals("VisitExtension") &&
			!level.equals("SpecimenExtension");
	}
}
