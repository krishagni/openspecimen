package com.krishagni.catissueplus.core.biospecimen.services.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.domain.factory.UserGroupErrorCode;
import com.krishagni.catissueplus.core.administrative.events.UserGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocolGroup;
import com.krishagni.catissueplus.core.biospecimen.domain.CpGroupForm;
import com.krishagni.catissueplus.core.biospecimen.domain.CpWorkflowConfig;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpGroupErrorCode;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolGroupSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CollectionProtocolSummary;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupFormsDetail;
import com.krishagni.catissueplus.core.biospecimen.events.CpGroupWorkflowCfgDetail;
import com.krishagni.catissueplus.core.biospecimen.events.WorkflowDetail;
import com.krishagni.catissueplus.core.biospecimen.repository.CpGroupListCriteria;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.biospecimen.services.CollectionProtocolGroupService;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.ErrorType;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityOp;
import com.krishagni.catissueplus.core.common.events.BulkDeleteEntityResp;
import com.krishagni.catissueplus.core.common.events.EntityQueryCriteria;
import com.krishagni.catissueplus.core.common.events.Operation;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.Resource;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.Form;
import com.krishagni.catissueplus.core.de.domain.FormErrorCode;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.RemoveFormContextOp;
import com.krishagni.catissueplus.core.de.repository.FormDao;
import com.krishagni.catissueplus.core.de.services.FormService;
import com.krishagni.catissueplus.core.exporter.services.ExportService;
import com.krishagni.catissueplus.core.importer.services.ImportService;
import com.krishagni.rbac.common.errors.RbacErrorCode;

public class CollectionProtocolGroupServiceImpl implements CollectionProtocolGroupService {
	private DaoFactory daoFactory;

	private FormDao formDao;

	private FormService formSvc;

	private CpGroupSettingsApplier cpGroupSettingsApplier;

	private ExportService exportSvc;

	private ImportService importSvc;

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}

	public void setFormDao(FormDao formDao) {
		this.formDao = formDao;
	}

	public void setFormSvc(FormService formSvc) {
		this.formSvc = formSvc;
	}

	public void setCpGroupSettingsApplier(CpGroupSettingsApplier cpGroupSettingsApplier) {
		this.cpGroupSettingsApplier = cpGroupSettingsApplier;
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
			if (!AuthUtil.isAdmin() && siteCps != null && siteCps.isEmpty()) {
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
	public ResponseEvent<CollectionProtocolGroupSummary> getGroup(RequestEvent<EntityQueryCriteria> req) {
		try {
			EntityQueryCriteria crit = req.getPayload();
			CollectionProtocolGroup group = getGroup(crit.getId(), crit.getName());
			ensureReadAccess(group);

			group.setCpsCount(getCpsCount(group.getId()));
			return ResponseEvent.response(CollectionProtocolGroupSummary.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolGroupSummary> createGroup(RequestEvent<CollectionProtocolGroupSummary> req) {
		try {
			CollectionProtocolGroupSummary input = req.getPayload();
			if (CollectionUtils.isEmpty(input.getCps())) {
				throw OpenSpecimenException.userError(CpGroupErrorCode.CP_REQ);
			}

			CollectionProtocolGroup group = createGroup(input);
			ensureUniqueName(null, group);
			daoFactory.getCpGroupDao().saveOrUpdate(group, true);

			addCps(group, input.getCps());
			return ResponseEvent.response(CollectionProtocolGroupSummary.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<CollectionProtocolGroupSummary> updateGroup(RequestEvent<CollectionProtocolGroupSummary> req) {
		try {
			CollectionProtocolGroup existingGroup = getGroup(req.getPayload().getId(), null);
			ensureUpdateAccess(existingGroup);

			CollectionProtocolGroup group = createGroup(req.getPayload());
			ensureUniqueName(existingGroup, group);
			existingGroup.setName(group.getName());
			if (req.getPayload().isAttrModified("reqManagers")) {
				UserGroup existingReqManagers = existingGroup.getReqManagers();
				existingGroup.setReqManagers(group.getReqManagers());
				cpGroupSettingsApplier.applyReqManagers(existingReqManagers, group.getReqManagers(), existingGroup.getCps());
			}

			return ResponseEvent.response(CollectionProtocolGroupSummary.from(existingGroup));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> addGroupCps(RequestEvent<CollectionProtocolGroupSummary> req) {
		try {
			CollectionProtocolGroupSummary input = req.getPayload();
			if (CollectionUtils.isEmpty(input.getCps())) {
				return ResponseEvent.response(0);
			}

			CollectionProtocolGroup group = getGroup(input.getId(), input.getName());
			return ResponseEvent.response(addCps(group, input.getCps()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Integer> removeGroupCps(RequestEvent<CollectionProtocolGroupSummary> req) {
		try {
			CollectionProtocolGroupSummary input = req.getPayload();
			if (CollectionUtils.isEmpty(input.getCps())) {
				return ResponseEvent.response(0);
			}

			CollectionProtocolGroup group = getGroup(input.getId(), input.getName());

			List<CollectionProtocol> inputCps = getCps(input.getCps());
			Set<Long> cpsToRemove = inputCps.stream().map(CollectionProtocol::getId).collect(Collectors.toSet());
			List<Long> existingCpIds = daoFactory.getCpGroupDao().getGroupCpIds(group.getId(), cpsToRemove);
			if (existingCpIds.isEmpty()) {
				return ResponseEvent.response(0);
			}

			ensureUpdateCpRights(inputCps, new HashSet<>(existingCpIds));
			daoFactory.getCpGroupDao().removeCpsFromGroup(group.getId(), existingCpIds);
			return ResponseEvent.response(existingCpIds.size());
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<Map<String, Boolean>> getPermissions(RequestEvent<EntityQueryCriteria> req) {
		Map<String, Boolean> result = new HashMap<>();
		result.put("updateAllowed", false);

		try {
			EntityQueryCriteria crit = req.getPayload();
			ensureUpdateAccess(crit.getId());
			result.put("updateAllowed", true);
			return ResponseEvent.response(result);
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.response(result);
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

			String inputLevel = crit.paramString("level");
			Map<String, Set<CpGroupForm>> groupFormsByLevel;
			if (StringUtils.isBlank(inputLevel)) {
				groupFormsByLevel = group.getFormsByLevel();
			} else {
				groupFormsByLevel = Collections.singletonMap(inputLevel, group.getForms(inputLevel));
			}

			List<CpGroupFormsDetail> result = new ArrayList<>();
			groupFormsByLevel.forEach(
				(level, forms) -> result.add(CpGroupFormsDetail.from(group, level, forms))
			);

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

			List<FormSummary> forms = new ArrayList<>();
			for (FormSummary inputForm : input.getForms()) {
				Form form = getForm(inputForm.getFormId(), inputForm.getName());
				if (group.containsForm(input.getLevel(), form)) {
					continue;
				}

				inputForm.setFormId(form.getId());
				inputForm.setDbForm(form);
				forms.add(inputForm);
			}

			if (!isMultipleFormsLevel(input.getLevel())) {
				forms = Collections.singletonList(forms.get(forms.size() - 1));
			}

			cpGroupSettingsApplier.addFormContexts(group, input.getLevel(), forms);
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

			cpGroupSettingsApplier.applyWorkflows(input.getWorkflows(), group.getCps());

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
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getReadableSiteCps();
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		if (daoFactory.getCpGroupDao().getCpsCount(group.getId(), siteCps) <= 0) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private void ensureUpdateAccess(CollectionProtocolGroup group) {
		ensureUpdateAccess(group.getId());
	}

	private void ensureUpdateAccess(Long groupId) {
		if (AuthUtil.isAdmin()) {
			//
			// admins can update any CPG
			//
			return;
		}

		if (groupId == null) {
			//
			// new group being created
			// user needs to have CP update rights to create a new group
			//
			if (!AccessCtrlMgr.getInstance().hasUpdateCpRights()) {
				throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
			}

			return;
		}

		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.CP, Operation.UPDATE, false);
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		int total = getCpsCount(groupId);
		if (total == 0) {
			//
			// existing group without any CPs is accessible only to admins
			//
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		int accessible = daoFactory.getCpGroupDao().getCpsCount(groupId, siteCps);
		if (accessible < total) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}
	}

	private int getCpsCount(Long groupId) {
		Map<Long, Integer> counts = daoFactory.getCpGroupDao().getCpsCount(Collections.singleton(groupId));
		Integer count = counts.get(groupId);
		return count != null ? count : 0;
	}

	private CollectionProtocolGroup createGroup(CollectionProtocolGroupSummary input) {
		OpenSpecimenException ose = new OpenSpecimenException(ErrorType.USER_ERROR);

		CollectionProtocolGroup group = new CollectionProtocolGroup();
		group.setId(input.getId());
		if (StringUtils.isBlank(input.getName())) {
			ose.addError(CpGroupErrorCode.NAME_REQ);
		}

		group.setName(input.getName());
		group.setReqManagers(getReqManagers(input.getReqManagers(), ose));
		group.setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		ose.checkAndThrow();
		return group;
	}

	private UserGroup getReqManagers(UserGroupSummary input, OpenSpecimenException ose) {
		if (input == null) {
			return null;
		}

		Object key = null;
		UserGroup reqManagers = null;
		if (input.getId() != null) {
			reqManagers = daoFactory.getUserGroupDao().getById(input.getId());
			key = input.getId();
		} else if (StringUtils.isNotBlank(input.getName())) {
			reqManagers = daoFactory.getUserGroupDao().getByName(input.getName());
			key = input.getName();
		}

		if (key != null && reqManagers == null) {
			ose.addError(UserGroupErrorCode.NOT_FOUND, key);
		}

		return reqManagers;
	}

	private List<CollectionProtocol> getCps(List<CollectionProtocolSummary> cps) {
		Set<Long> ids = new HashSet<>();
		Set<String> shortTitles = new HashSet<>();
		for (CollectionProtocolSummary cp : cps) {
			if (cp.getId() != null) {
				ids.add(cp.getId());
			} else if (StringUtils.isNotBlank(cp.getShortTitle())) {
				shortTitles.add(cp.getShortTitle());
			}
		}

		if (ids.isEmpty() && shortTitles.isEmpty()) {
			throw OpenSpecimenException.userError(CpGroupErrorCode.CP_REQ);
		}

		List<CollectionProtocol> result = new ArrayList<>();
		if (!ids.isEmpty()) {
			List<CollectionProtocol> found = daoFactory.getCollectionProtocolDao().getByIds(ids);
			if (found.size() != ids.size()) {
				ids.removeAll(found.stream().map(CollectionProtocol::getId).collect(Collectors.toSet()));
				throw OpenSpecimenException.userError(
					CpGroupErrorCode.CP_NOT_FOUND,
					ids.stream().map(Object::toString).collect(Collectors.joining(",")),
					ids.size());
			}

			result.addAll(found);
		}

		if (!shortTitles.isEmpty()) {
			List<CollectionProtocol> found = daoFactory.getCollectionProtocolDao().getCpsByShortTitle(shortTitles);
			if (found.size() != shortTitles.size()) {
				shortTitles.removeIf(st -> found.stream().anyMatch(cp -> cp.getShortTitle().equalsIgnoreCase(st)));
				if (!shortTitles.isEmpty()) {
					throw OpenSpecimenException.userError(CpGroupErrorCode.CP_NOT_FOUND, String.join(",", shortTitles), shortTitles.size());
				}
			}

			result.addAll(found);
		}

		return result;
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

	private void ensureCpsNotInOtherGroups(Long groupId, Collection<Long> cpIds) {
		List<String> cpsInOtherGroups = daoFactory.getCpGroupDao().getCpsAssignedToOtherGroup(groupId, cpIds);
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

	private int addCps(CollectionProtocolGroup group, List<CollectionProtocolSummary> cps) {
		List<CollectionProtocol> inputCps = getCps(cps);

		Set<Long> cpIdsToAdd = inputCps.stream().map(CollectionProtocol::getId).collect(Collectors.toSet());
		List<Long> existingCpIds = daoFactory.getCpGroupDao().getGroupCpIds(group.getId(), cpIdsToAdd);

		cpIdsToAdd.removeAll(existingCpIds);
		if (cpIdsToAdd.isEmpty()) {
			return 0;
		}

		ensureUpdateCpRights(inputCps, cpIdsToAdd);
		ensureCpsNotInOtherGroups(group.getId(), cpIdsToAdd);
		daoFactory.getCpGroupDao().addCpsToGroup(group.getId(), cpIdsToAdd);

		Set<CollectionProtocol> addedCps = inputCps.stream().filter(cp -> cpIdsToAdd.contains(cp.getId())).collect(Collectors.toSet());
		cpGroupSettingsApplier.apply(group, addedCps);
		return addedCps.size();
	}

	private void ensureUpdateCpRights(List<CollectionProtocol> inputCps, Set<Long> cpIds) {
		if (AuthUtil.isAdmin()) {
			return;
		}

		Set<SiteCpPair> siteCps = AccessCtrlMgr.getInstance().getSiteCps(Resource.CP, Operation.UPDATE, false);
		if (siteCps != null && siteCps.isEmpty()) {
			throw OpenSpecimenException.userError(RbacErrorCode.ACCESS_DENIED);
		}

		Set<Long> accessibleCpIds = daoFactory.getCpGroupDao().filterCps(cpIds, siteCps);
		if (accessibleCpIds.containsAll(cpIds)) {
			return;
		}

		Set<Long> inaccessibleCpIds = Utility.subtract(cpIds, accessibleCpIds);
		String titles = inputCps.stream().filter(cp -> inaccessibleCpIds.contains(cp.getId())).map(CollectionProtocol::getShortTitle).collect(Collectors.joining(", "));
		throw OpenSpecimenException.userError(CpGroupErrorCode.CP_UPDATE_REQ, titles);
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
