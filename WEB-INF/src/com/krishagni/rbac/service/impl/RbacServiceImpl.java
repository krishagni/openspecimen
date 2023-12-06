package com.krishagni.rbac.service.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.springframework.beans.BeanUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.administrative.domain.factory.SiteErrorCode;
import com.krishagni.catissueplus.core.administrative.repository.UserDao;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.factory.CpErrorCode;
import com.krishagni.catissueplus.core.common.PlusTransactional;
import com.krishagni.catissueplus.core.common.access.AccessCtrlMgr;
import com.krishagni.catissueplus.core.common.domain.Notification;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.EmailUtil;
import com.krishagni.catissueplus.core.common.util.MessageUtil;
import com.krishagni.catissueplus.core.common.util.NotifUtil;
import com.krishagni.rbac.common.errors.RbacErrorCode;
import com.krishagni.rbac.domain.Group;
import com.krishagni.rbac.domain.GroupRole;
import com.krishagni.rbac.domain.Operation;
import com.krishagni.rbac.domain.Permission;
import com.krishagni.rbac.domain.Resource;
import com.krishagni.rbac.domain.ResourceInstanceOp;
import com.krishagni.rbac.domain.Role;
import com.krishagni.rbac.domain.RoleAccessControl;
import com.krishagni.rbac.domain.Subject;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;
import com.krishagni.rbac.events.GroupDetail;
import com.krishagni.rbac.events.GroupRoleDetail;
import com.krishagni.rbac.events.OperationDetail;
import com.krishagni.rbac.events.PermissionDetail;
import com.krishagni.rbac.events.ResourceDetail;
import com.krishagni.rbac.events.ResourceInstanceOpDetails;
import com.krishagni.rbac.events.RoleAccessControlDetails;
import com.krishagni.rbac.events.RoleDetail;
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOp;
import com.krishagni.rbac.events.SubjectRoleOp.OP;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.events.SubjectRolesList;
import com.krishagni.rbac.repository.DaoFactory;
import com.krishagni.rbac.repository.OperationListCriteria;
import com.krishagni.rbac.repository.PermissionListCriteria;
import com.krishagni.rbac.repository.ResourceListCriteria;
import com.krishagni.rbac.repository.RoleListCriteria;
import com.krishagni.rbac.service.RbacService;

public class RbacServiceImpl implements RbacService {
	private DaoFactory daoFactory;
	
	private UserDao userDao;

	public DaoFactory getDaoFactory() {
		return daoFactory;
	}

	public void setDaoFactory(DaoFactory daoFactory) {
		this.daoFactory = daoFactory;
	}
	
	public void setUserDao(UserDao userDao) {
		this.userDao = userDao;
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<ResourceDetail>> getResources(RequestEvent<ResourceListCriteria> req) {
		try {
			return ResponseEvent.response(ResourceDetail.from(
					daoFactory.getResourceDao().getResources(req.getPayload())));
		} catch(Exception e) { 
			return ResponseEvent.serverError(e);
		} 
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<ResourceDetail> saveResource(RequestEvent<ResourceDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			ResourceDetail detail = req.getPayload();
			if (detail == null || StringUtils.isEmpty(detail.getName())) { 
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NAME_REQUIRED);
			}
			
			String resourceName = detail.getName();
			Resource resource = daoFactory.getResourceDao().getResourceByName(resourceName);			
			if (resource == null) {
				resource = new Resource();
				resource.setName(resourceName);
				
				daoFactory.getResourceDao().saveOrUpdate(resource);				
			}
			
			return ResponseEvent.response(ResourceDetail.from(resource));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<ResourceDetail> deleteResource(RequestEvent<String> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			String resourceName = req.getPayload();
			if (StringUtils.isEmpty(resourceName)) {
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NAME_REQUIRED);
			}
			
			Resource resource = daoFactory.getResourceDao().getResourceByName(resourceName);
			if (resource == null) {
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NOT_FOUND);
			}
			
			daoFactory.getResourceDao().delete(resource);
			return ResponseEvent.response(ResourceDetail.from(resource));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<OperationDetail>> getOperations(RequestEvent<OperationListCriteria> req) {
		try {
			return ResponseEvent.response(OperationDetail.from(
					daoFactory.getOperationDao().getOperations(req.getPayload())));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<OperationDetail> saveOperation(RequestEvent<OperationDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			OperationDetail detail = req.getPayload();
			String operationName = detail.getName();
			if (detail == null || StringUtils.isEmpty(operationName)) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NAME_REQUIRED);
			}
		
			Operation operation = daoFactory.getOperationDao().getOperationByName(operationName);			
			if (operation == null) {
				operation = new Operation();
				operation.setName(operationName);
				daoFactory.getOperationDao().saveOrUpdate(operation);				
			}
			
			return ResponseEvent.response(OperationDetail.from(operation));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<OperationDetail> deleteOperation(RequestEvent<String> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			String operationName = req.getPayload();
			if (StringUtils.isEmpty(operationName)) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NAME_REQUIRED);
			}
			
			Operation operation = daoFactory.getOperationDao().getOperationByName(operationName);
			if (operation == null) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NOT_FOUND);
			}

			daoFactory.getOperationDao().delete(operation);
			return ResponseEvent.response(OperationDetail.from(operation));
		} catch(Exception e) { 
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<PermissionDetail>> getPermissions(RequestEvent<PermissionListCriteria> req) {
		try {
			return ResponseEvent.response(PermissionDetail.from(
					daoFactory.getPermissionDao().getPermissions(req.getPayload())));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<PermissionDetail> addPermission(RequestEvent<PermissionDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			PermissionDetail details = req.getPayload();
			if (details == null) {
				details = new PermissionDetail();
			}
			
			String resourceName = details.getResourceName();
			if (StringUtils.isEmpty(resourceName)) { 
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NAME_REQUIRED);
			}
			
			String operationName = details.getOperationName();						
			if (StringUtils.isEmpty(operationName)) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NAME_REQUIRED);
			}
			
			Resource resource = daoFactory.getResourceDao().getResourceByName(resourceName);
			if (resource == null) {
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NOT_FOUND);
			}
			
			Operation operation = daoFactory.getOperationDao().getOperationByName(operationName);
			if (operation == null) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NOT_FOUND);
			}
			
			Permission permission = daoFactory.getPermissionDao().getPermission(resourceName, operationName);
			if (permission != null) {
				return ResponseEvent.userError(RbacErrorCode.DUPLICATE_PERMISSION);
			}
			
			permission = new Permission();
			permission.setResource(resource);
			permission.setOperation(operation);
			daoFactory.getPermissionDao().saveOrUpdate(permission);
			return ResponseEvent.response(PermissionDetail.from(permission));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<PermissionDetail> deletePermission(RequestEvent<PermissionDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			PermissionDetail detail = req.getPayload();
			if (detail == null) {
				detail = new PermissionDetail();
			}
			
			String resourceName = detail.getResourceName();
			if (StringUtils.isEmpty(resourceName)) { 
				return ResponseEvent.userError(RbacErrorCode.RESOURCE_NAME_REQUIRED);
			}
			
			String operationName = detail.getOperationName();						
			if (StringUtils.isEmpty(operationName)) {
				return ResponseEvent.userError(RbacErrorCode.OPERATION_NAME_REQUIRED);
			}
			
			Permission permission =  daoFactory.getPermissionDao().getPermission(resourceName, operationName);
			if (permission == null) {
				return ResponseEvent.userError(RbacErrorCode.PERMISSION_NOT_FOUND);
			}
			
			daoFactory.getPermissionDao().delete(permission);
			return ResponseEvent.response(PermissionDetail.from(permission));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<RoleDetail>> getRoles(RequestEvent<RoleListCriteria> req) {
		try {
			return ResponseEvent.response(RoleDetail.from(daoFactory.getRoleDao().getRoles(req.getPayload())));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<RoleDetail> getRole(RequestEvent<Long> req) {
		try {
			Role role = daoFactory.getRoleDao().getById(req.getPayload());
			if (role == null) {
				return ResponseEvent.userError(RbacErrorCode.ROLE_NOT_FOUND);
			}
			
			return ResponseEvent.response(RoleDetail.from(role));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
		
	@Override
	@PlusTransactional
	public ResponseEvent<RoleDetail> saveRole(RequestEvent<RoleDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			Role role = createRole(req.getPayload());
			ensureUniqueName(role, null);
			daoFactory.getRoleDao().saveOrUpdate(role, true);
			return ResponseEvent.response(RoleDetail.from(role));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<RoleDetail> updateRole(RequestEvent<RoleDetail> request) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			RoleDetail detail = request.getPayload();
			Role existing = daoFactory.getRoleDao().getById(detail.getId());
			if (existing == null) {
				return ResponseEvent.userError(RbacErrorCode.ROLE_NOT_FOUND);
			}
		
			Role newRole = createRole(detail);
			ensureUniqueName(newRole, existing);
			existing.updateRole(newRole);
			daoFactory.getRoleDao().saveOrUpdate(existing, true);
			return ResponseEvent.response(RoleDetail.from(existing));
		} catch(OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch(Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<RoleDetail> deleteRole(RequestEvent<Long> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			Long roleId = req.getPayload();
			Role role = daoFactory.getRoleDao().getById(roleId);
			if (role == null) {
				return ResponseEvent.userError(RbacErrorCode.ROLE_NOT_FOUND);
			}

			daoFactory.getRoleDao().delete(role);
			return ResponseEvent.response(RoleDetail.from(role));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}

	@Override
	@PlusTransactional
	public ResponseEvent<List<SubjectRoleDetail>> getSubjectRoles(RequestEvent<Long> req) {
		try {
			Long subjectId = req.getPayload();
			if (subjectId == null) {
				return ResponseEvent.userError(RbacErrorCode.SUBJECT_ID_REQUIRED);
			}
			
			Subject subject = daoFactory.getSubjectDao().getById(subjectId);
			if (subject == null) {
				return ResponseEvent.userError(RbacErrorCode.SUBJECT_NOT_FOUND);
			}
			
			return ResponseEvent.response(SubjectRoleDetail.from(subject.getRoles()));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SubjectRoleDetail> updateSubjectRole(RequestEvent<SubjectRoleOp> req) {
		try {
			SubjectRoleOp subjectRoleOp = req.getPayload();
			Subject subject = daoFactory.getSubjectDao().getById(subjectRoleOp.getSubjectId());
			if (subject == null) {
				return ResponseEvent.userError(RbacErrorCode.SUBJECT_NOT_FOUND);
			}
			
			User user = userDao.getById(subject.getId());
			SubjectRole resp = null;
			Map<String,Object> oldSrDetails = new HashMap<>();
			SubjectRole sr = null;
			switch (subjectRoleOp.getOp()) {
				case ADD -> {
					sr = createSubjectRole(subject, subjectRoleOp.getSubjectRole());
					AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, sr.getSite(), sr.getCollectionProtocol());
					resp = subject.addRole(sr);
				}
				case UPDATE -> {
					SubjectRole oldSr = subject.getRole(subjectRoleOp.getSubjectRole().getId());
					AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, oldSr.getSite(), oldSr.getCollectionProtocol());
					oldSrDetails.put("site", oldSr.getSite());
					oldSrDetails.put("collectionProtocol", oldSr.getCollectionProtocol());
					oldSrDetails.put("role", oldSr.getRole());
					sr = createSubjectRole(subject, subjectRoleOp.getSubjectRole());
					AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, sr.getSite(), sr.getCollectionProtocol());
					resp = subject.updateRole(sr);
				}
				case REMOVE -> {
					SubjectRole role = subject.getRole(subjectRoleOp.getSubjectRole().getId());
					AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, role.getSite(), role.getCollectionProtocol());
					resp = subject.removeSubjectRole(role);
				}
			}
			
			if (resp != null) {
				daoFactory.getSubjectDao().saveOrUpdate(subject, true);
				notifyUsers(user, resp, oldSrDetails, subjectRoleOp.getOp());
			}
			
			return ResponseEvent.response(SubjectRoleDetail.from(resp));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override 
	@PlusTransactional
	public List<SubjectRole> addSubjectRole(Site site, CollectionProtocol cp, User user, String[] roleNames, SubjectRoleOpNotif notifReq) {
		List<SubjectRole> roles = addOrRemoveSubjectRole(site, cp, user, roleNames, SubjectRoleOp.OP.ADD, true);
		notifReq.setRoleOp(OP.ADD.name());
		notifyUsers(roles, notifReq);
		return roles;
	}
	
	@Override 
	@PlusTransactional
	public List<SubjectRole> removeSubjectRole(Site site, CollectionProtocol cp, User user, String[] roleNames, SubjectRoleOpNotif notifReq) {
		List<SubjectRole> roles = addOrRemoveSubjectRole(site, cp, user, roleNames, SubjectRoleOp.OP.REMOVE, true);
		notifReq.setRoleOp(OP.REMOVE.name());
		notifyUsers(roles, notifReq);
		return roles;
	}

	@Override
	public void removeInvalidRoles(User user) {
		Subject subject = daoFactory.getSubjectDao().getById(user.getId());
		if (subject == null) {
			return;
		}

		Iterator<SubjectRole> roles = subject.getRoles().iterator();
		while (roles.hasNext()) {
			SubjectRole role = roles.next();
			if (role.getSite() != null && !role.getSite().getInstitute().equals(user.getInstitute())) {
				roles.remove();
			} else if (role.getCollectionProtocol() != null) {
				boolean validInstituteCp = role.getCollectionProtocol().getRepositories().stream()
					.anyMatch(s -> s.getInstitute().equals(user.getInstitute()));
				if (!validInstituteCp) {
					roles.remove();
				}
			}
		}
	}

	private ArrayList<SubjectRole> addOrRemoveSubjectRole(
		Site site, CollectionProtocol cp, User user, String[] roleNames, SubjectRoleOp.OP op, boolean systemRole) {

		Subject subject = daoFactory.getSubjectDao().getById(user.getId());
		if (subject == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.SUBJECT_NOT_FOUND);
		}
		
		ArrayList<SubjectRole> subjectRoles = new ArrayList<>();
		for (String role : roleNames) {
			SubjectRole sr = createSubjectRole(site, cp, role, systemRole);
			AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, sr.getSite(), sr.getCollectionProtocol());
			SubjectRole resp = switch (op) {
				case ADD -> subject.addRole(sr);
				case REMOVE -> subject.removeSubjectRole(sr);
				default -> null;
			};

			if (resp != null) {
				subjectRoles.add(resp);
			}
		}
		
		if (CollectionUtils.isEmpty(subjectRoles)) {
			return subjectRoles;
		}
		
		daoFactory.getSubjectDao().saveOrUpdate(subject, true);
		return subjectRoles;
	}

	@Override
	@PlusTransactional
	public void removeCpRoles(Long cpId) {
		daoFactory.getSubjectDao().removeRolesByCp(cpId);
	}

	private SubjectRole createSubjectRole(Site site, CollectionProtocol cp, String roleName, boolean systemRole) {
		Role role = daoFactory.getRoleDao().getRoleByName(roleName);
		if (role == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NOT_FOUND);
		}
		
		SubjectRole subjectRole = new SubjectRole();
		subjectRole.setSite(site);
		subjectRole.setCollectionProtocol(cp);
		subjectRole.setRole(role);
		subjectRole.setSystemRole(systemRole);
		
		return subjectRole;
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<List<GroupRoleDetail>> getGroupRoles(RequestEvent<Long> req) {
		try {
			Long groupId = req.getPayload();
			if (groupId == null) {
				return ResponseEvent.userError(RbacErrorCode.GROUP_ID_REQUIRED);
			}
			
			Group group = daoFactory.getGroupDao().getGroup(groupId);
			return ResponseEvent.response(GroupRoleDetail.from(group.getGroupRoles()));
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public  ResponseEvent<GroupDetail> updateGroupRoles(RequestEvent<GroupDetail> req) {
		try {
			AccessCtrlMgr.getInstance().ensureUserIsAdmin();
			
			GroupDetail detail = req.getPayload();
			if (detail == null || detail.getId() == null) {
				return ResponseEvent.userError(RbacErrorCode.GROUP_ID_REQUIRED);
			}
			
			List<GroupRoleDetail> roles = detail.getRoles();
			if (roles == null) {
				roles = new ArrayList<GroupRoleDetail>();
			}
			
			Group group = daoFactory.getGroupDao().getGroup(detail.getId());
			if (group == null) {
				ResponseEvent.response(RbacErrorCode.GROUP_NOT_FOUND);
			}
			
			List<GroupRole> groupRoles = new ArrayList<GroupRole>();
			for (GroupRoleDetail gd : roles) {
				GroupRole gr = createGroupRole(gd);
				gr.setGroup(group);
				
				groupRoles.add(gr);
			}
			
			//TODO - find a solution about this hack
			AbstractDao<?> dao = (AbstractDao<?>)daoFactory.getGroupDao();
			Session session = dao.getSessionFactory().getCurrentSession();
			
			group.updateRoles(groupRoles, session);
			daoFactory.getGroupDao().saveOrUpdate(group);
			return ResponseEvent.response(GroupDetail.from(group));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}
	}
	
	@Override
	@PlusTransactional
	public ResponseEvent<SubjectRolesList> assignRoles(RequestEvent<SubjectRolesList> req) {
		try {
			SubjectRolesList rolesList = req.getPayload();
			Long subjectId = rolesList.getSubjectId();
			String emailAddress = rolesList.getEmailAddress();

			User user = null;
			if (subjectId != null) {
				user = userDao.getById(subjectId);
			} else if (StringUtils.isNotBlank(emailAddress)) {
				user = userDao.getUserByEmailAddress(emailAddress);
			}
			
			if (user == null) {
				return ResponseEvent.userError(RbacErrorCode.SUBJECT_NOT_FOUND);
			}
			
			Subject subject = daoFactory.getSubjectDao().getById(user.getId());
			subject.removeAllSubjectRoles();

			if (CollectionUtils.isNotEmpty(rolesList.getRoles())) {
				for (SubjectRolesList.Role srd : rolesList.getRoles()) {
					SubjectRole sr = createSubjectRole(srd);
					AccessCtrlMgr.getInstance().ensureCreateUpdateUserRolesRights(user, sr.getSite(), sr.getCollectionProtocol());
					subject.addRole(sr);
				}
			}

			daoFactory.getSubjectDao().saveOrUpdate(subject);
			return ResponseEvent.response(
					SubjectRolesList.from(
							user.getId(), user.getEmailAddress(), subject.getRoles()));
		} catch (OpenSpecimenException ose) {
			return ResponseEvent.error(ose);
		} catch (Exception e) {
			return ResponseEvent.serverError(e);
		}		
	}

	//
	// Internal Api's can change without notice
	//
	
	@Override
	@PlusTransactional
	public boolean canUserPerformOp(Long userId, String resource, String[] operations) {
		return daoFactory.getSubjectDao().canUserPerformOps(userId, resource, operations);
	}

	@Override
	@PlusTransactional	
	public List<SubjectAccess> getAccessList(Long userId, String resource, String[] operations) {
		return daoFactory.getSubjectDao().getAccessList(userId, resource, operations);
	}
		
	//
	// HELPER METHODS
	// 
	
	private Role createRole(RoleDetail detail) {
		Role role = new Role();
		setName(detail, role);
		role.setDescription(detail.getDescription());
		role.setAcl(getAcl(detail, role));
		return role;
	}
	
	private void setName(RoleDetail detail, Role role) {
		if (StringUtils.isBlank(detail.getName())) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NAME_REQUIRED);
		}
		role.setName(detail.getName());
	}

	private Role getRole(String roleName) {
		if (roleName == null) {
			return null;
		}
		
		Role role = daoFactory.getRoleDao().getRoleByName(roleName);
		
		if (role == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NOT_FOUND);
		}
		
		return role;
	}
	
	private Set<RoleAccessControl> getAcl(RoleDetail detail, Role role) {
		Map<Resource, RoleAccessControl> aclMap = new LinkedHashMap<>();

		for (RoleAccessControlDetails inputAcl : detail.getAcl()) {
			RoleAccessControl acl = new RoleAccessControl();
			acl.setRole(role);

			Resource resource = daoFactory.getResourceDao().getResourceByName(inputAcl.getResourceName());
			acl.setResource(resource);
			if (resource == null) {
				throw OpenSpecimenException.userError(RbacErrorCode.RESOURCE_NOT_FOUND, inputAcl.getResourceName());
			}

			Map<Operation, ResourceInstanceOp> opsMap = new LinkedHashMap<>();
			for (ResourceInstanceOpDetails inputOp  : inputAcl.getOperations()) {
				Operation operation = daoFactory.getOperationDao().getOperationByName(inputOp.getOperationName());
				if (operation == null) {
					throw OpenSpecimenException.userError(RbacErrorCode.OPERATION_NOT_FOUND, inputOp.getOperationName());
				}
				
				ResourceInstanceOp op = new ResourceInstanceOp();
				op.setOperation(operation);
				op.setAccessControl(acl);
				opsMap.put(op.getOperation(), op);
			}

			acl.setOperations(new LinkedHashSet<>(opsMap.values()));
			aclMap.put(acl.getResource(), acl);
		}
		
		return new LinkedHashSet<>(aclMap.values());
	}
	
	private SubjectRole createSubjectRole(Subject subject, SubjectRoleDetail srd) {
		RoleDetail detail = srd.getRole();
		if (detail == null || StringUtils.isEmpty(detail.getName())) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NAME_REQUIRED);
		}
		
		Role role = daoFactory.getRoleDao().getRoleByName(detail.getName());
		if (role == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NOT_FOUND);
		}

		CollectionProtocol cp = getCollectionProtocol(srd);
		Site site = getSite(srd);

		SubjectRole sr = new SubjectRole();
		sr.setId(srd.getId());
		sr.setCollectionProtocol(cp);
		sr.setSite(site);
		sr.setRole(role);
		sr.setSystemRole(srd.getSystemRole());
		sr.setSubject(subject);

		if (site == null) {
			if (cp != null) {
				boolean valid = daoFactory.getCollectionProtocolDao().isCpAffiliatedToUserInstitute(cp.getId(), subject.getId());
				if (!valid) {
					throw OpenSpecimenException.userError(RbacErrorCode.INV_CP_USER, cp.getShortTitle());
				}
			}
		} else {
			if (!daoFactory.getSiteDao().isAffiliatedToUserInstitute(site.getId(), subject.getId())) {
				throw OpenSpecimenException.userError(RbacErrorCode.INV_SITE_USER, site.getName());
			}

			if (cp != null) {
				boolean valid = cp.getRepositories().stream().anyMatch(cpSite -> cpSite.equals(site));
				if (!valid) {
					throw OpenSpecimenException.userError(RbacErrorCode.INV_CP_SITE, site.getName(), cp.getShortTitle());
				}
			}
		}

		return sr;
	}

	private CollectionProtocol getCollectionProtocol(SubjectRoleDetail sd) {
		if (sd.getCollectionProtocol() == null) {
			/*
			 *  null cp means all cp's
			 */
			return null;
		}
		
		CollectionProtocol cp = null;
		Long cpId = sd.getCollectionProtocol().getId();
		String title = sd.getCollectionProtocol().getTitle();
		String shortTitle = sd.getCollectionProtocol().getShortTitle();
		
		if (cpId != null) {
			cp = daoFactory.getCollectionProtocolDao().getById(cpId);
		} else if (StringUtils.isNotBlank(title)) {
			cp = daoFactory.getCollectionProtocolDao().getCollectionProtocol(title);
		} else if (StringUtils.isNotBlank(shortTitle)) {
			cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(shortTitle);
		}
		
		if (cp == null) {
			throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND);
		}
		
		return cp;
	}
	
	private Site getSite(SubjectRoleDetail sd) {
		if (sd.getSite() == null) {
			/*
			 * null site means all sites'
			 */
			return null;
		}
		
		Site site = null;
		Long siteId = sd.getSite().getId();
		String name = sd.getSite().getName();
		
		if (siteId != null) {
			site = daoFactory.getSiteDao().getById(siteId);
		} else if (StringUtils.isNotBlank(name)) {
			site = daoFactory.getSiteDao().getSiteByName(name);
		}
		
		if (site == null) {
			throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND);
		}
		
		return site;
	}
	
	private SubjectRole createSubjectRole(SubjectRolesList.Role srd) {
		SubjectRole sr = new SubjectRole();
		
		String roleName = srd.getRoleName();
		if (StringUtils.isBlank(roleName)) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NAME_REQUIRED);
		}
		
		Role role = daoFactory.getRoleDao().getRoleByName(roleName);
		if (role == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NOT_FOUND);
		}
		sr.setRole(role);
		
		
		String cpShortTitle = srd.getCpShortTitle();
		if (SubjectRolesList.ALL_CURRENT_AND_FUTURE.equalsIgnoreCase(cpShortTitle)) {
			cpShortTitle = null;
		}

		if (StringUtils.isNotBlank(cpShortTitle)) {
			CollectionProtocol cp = daoFactory.getCollectionProtocolDao().getCpByShortTitle(cpShortTitle);
			if (cp == null) {
				throw OpenSpecimenException.userError(CpErrorCode.NOT_FOUND);
			}
			sr.setCollectionProtocol(cp);
		}
		
		String siteName = srd.getSiteName();
		if (SubjectRolesList.ALL_CURRENT_AND_FUTURE.equalsIgnoreCase(siteName)) {
			siteName = null;
		}

		if (StringUtils.isNotBlank(siteName)) {
			Site site = daoFactory.getSiteDao().getSiteByName(siteName);
			if (site == null) {
				throw OpenSpecimenException.userError(SiteErrorCode.NOT_FOUND);
			}
			sr.setSite(site);
		}
		
		return sr;
	}
	
	private GroupRole createGroupRole(GroupRoleDetail gd) {
		RoleDetail detail = gd.getRoleDetails();
		if (detail == null || StringUtils.isEmpty(detail.getName())) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NAME_REQUIRED);
		}
		
		Role role = daoFactory.getRoleDao().getRoleByName(detail.getName());
		if (role == null) {
			throw OpenSpecimenException.userError(RbacErrorCode.ROLE_NOT_FOUND);
		}
		
		GroupRole sr = new GroupRole();
		sr.setDsoId(gd.getDsoId() == null ? -1L : gd.getDsoId());
		sr.setRole(role);
		return sr;
	}
	
	private void ensureUniqueName(Role newRole, Role existingRole) {
		if (existingRole != null && existingRole.getName().equals(newRole.getName())) {
			return;
		}
		
		Role role = daoFactory.getRoleDao().getRoleByName(newRole.getName());
		if (role != null) {
			throw OpenSpecimenException.userError(RbacErrorCode.DUP_ROLE_NAME);
		}
	}

	private void notifyUsers(User subject, SubjectRole sr, Map<String, Object> oldSrDetails, OP op) {
		List<User> notifUsers = AccessCtrlMgr.getInstance().getSiteAdmins(sr.getSite(), sr.getCollectionProtocol());
		if (!notifUsers.contains(AuthUtil.getCurrentUser())) {
			//
			// if the current user is not site or super admin
			//
			notifUsers.add(AuthUtil.getCurrentUser());
		}

		Site site = sr.getSite();
		String siteOrInstName = site != null ? site.getName() : subject.getInstitute().getName();
		int siteOrInstChoice = site != null ? 2 : 1;

		CollectionProtocol cp = sr.getCollectionProtocol();
		String cpTitle = cp != null ? cp.getShortTitle() : StringUtils.EMPTY;
		int cpChoice = cp != null ? 2 : 1;

		SubjectRoleOpNotif notifReq = new SubjectRoleOpNotif();
		notifReq.setUser(subject);
		notifReq.setRole(sr);
		notifReq.setOldSrDetails(oldSrDetails);
		notifReq.setEndUserOp("UPDATE");
		notifReq.setRoleOp(op.name());
		notifReq.setAdmins(notifUsers);
		notifReq.setAdminNotifMsg("rbac_admin_notif_role_" + op.name().toLowerCase());
		notifReq.setAdminNotifParams(new Object[] {
			subject.getFirstName(), subject.getLastName(), sr.getRole().getName(),
			cpTitle, cpChoice, siteOrInstName, siteOrInstChoice
		});
		notifReq.setSubjectNotifMsg("rbac_user_notif_role_" + op.name().toLowerCase());
		notifReq.setSubjectNotifParams(new Object[] {sr.getRole().getName(), cpTitle, cpChoice, siteOrInstName, siteOrInstChoice});

		notifyUsers(notifReq);
	}

	private void notifyUsers(List<SubjectRole> roles, SubjectRoleOpNotif notifReq) {
		for (SubjectRole role : roles) {
			notifReq.setRole(role);
			notifyUsers(notifReq);
		}
	}

	private void notifyUsers(SubjectRoleOpNotif notifReq) {
		sendRoleUpdateEmail(notifReq.getAdmins(), notifReq.getUser(), notifReq.getRole(), notifReq.getOldSrDetails(), notifReq.getRoleOp());
		addRoleUpdateNotifs(notifReq);
	}

	private void sendRoleUpdateEmail(List<User> notifUsers, User subject, SubjectRole sr, Map<String, Object> oldSrDetails, String roleOp) {
		String [] subjParams = new String[] {subject.getFirstName(), subject.getLastName()};
		Map<String, Object> props = new HashMap<>();
		props.put("operation", roleOp);
		props.put("user", subject);
		props.put("sr", sr);
		props.put("oldSr", oldSrDetails);
		props.put("$subject", subjParams);
		props.put("ccAdmin", false);

		if (!notifUsers.contains(subject)) {
			sendRoleUpdateEmail(subject, props);
		}

		for (User rcpt : notifUsers) {
			sendRoleUpdateEmail(rcpt, props);
		}
	}

	private void sendRoleUpdateEmail(User rcpt, Map<String, Object> props) {
		props.put("rcpt", rcpt);
		EmailUtil.getInstance().sendEmail(ROLE_UPDATED_EMAIL_TMPL, new String[] { rcpt.getEmailAddress() }, null, props);
	}

	private void addRoleUpdateNotifs(SubjectRoleOpNotif notifReq) {
		List<User> admins = notifReq.getAdmins().stream()
			.filter(nu -> !nu.equals(notifReq.getUser())).collect(Collectors.toList());

		Notification adminNotif = new Notification();
		adminNotif.setEntityType(User.getEntityName());
		adminNotif.setEntityId(notifReq.getUser().getId());
		adminNotif.setOperation("UPDATE");
		adminNotif.setCreatedBy(AuthUtil.getCurrentUser());
		adminNotif.setCreationTime(Calendar.getInstance().getTime());
		if (!notifReq.getEndUserOp().equals("DELETE")) {
			adminNotif.setMessage(getRoleUpdateNotifMsg(notifReq, false));
			NotifUtil.getInstance().notify(adminNotif, Collections.singletonMap("user-roles", admins));
		}

		Notification userNotif = new Notification();
		BeanUtils.copyProperties(adminNotif, userNotif, "id", "message", "notifiedUsers");
		userNotif.setMessage(getRoleUpdateNotifMsg(notifReq, true));
		NotifUtil.getInstance().notify(userNotif, Collections.singletonMap("user-roles", Collections.singletonList(notifReq.getUser())));
	}

	private String getRoleUpdateNotifMsg(SubjectRoleOpNotif notifReq, boolean userNotif) {
		String msgKey;
		Object[] params;
		if (userNotif) {
			msgKey = notifReq.getSubjectNotifMsg();
			params = notifReq.getSubjectNotifParams();
		} else {
			msgKey = notifReq.getAdminNotifMsg();
			params = notifReq.getAdminNotifParams();
		}

		return MessageUtil.getInstance().getMessage(msgKey, params);
	}

	private static final String ROLE_UPDATED_EMAIL_TMPL = "users_role_updated";
}
