package com.krishagni.rbac.service;

import java.util.List;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.User;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.rbac.domain.SubjectAccess;
import com.krishagni.rbac.domain.SubjectRole;
import com.krishagni.rbac.events.GroupDetail;
import com.krishagni.rbac.events.GroupRoleDetail;
import com.krishagni.rbac.events.OperationDetail;
import com.krishagni.rbac.events.PermissionDetail;
import com.krishagni.rbac.events.ResourceDetail;
import com.krishagni.rbac.events.RoleDetail;
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOp;
import com.krishagni.rbac.events.SubjectRoleOpNotif;
import com.krishagni.rbac.events.SubjectRolesList;
import com.krishagni.rbac.repository.OperationListCriteria;
import com.krishagni.rbac.repository.PermissionListCriteria;
import com.krishagni.rbac.repository.ResourceListCriteria;
import com.krishagni.rbac.repository.RoleListCriteria;

public interface RbacService {
	ResponseEvent<List<ResourceDetail>> getResources(RequestEvent<ResourceListCriteria> req);
	
	ResponseEvent<ResourceDetail> saveResource(RequestEvent<ResourceDetail> req);
	
	ResponseEvent<ResourceDetail> deleteResource(RequestEvent<String> req);
		
	//
	// Operation APIs
	//
	
	ResponseEvent<List<OperationDetail>> getOperations(RequestEvent<OperationListCriteria> req);
	
	ResponseEvent<OperationDetail> saveOperation(RequestEvent<OperationDetail> req);
	
	ResponseEvent<OperationDetail> deleteOperation(RequestEvent<String> req);
	
		
	//
	// Permission APIs
	//
	
	ResponseEvent<List<PermissionDetail>> getPermissions(RequestEvent<PermissionListCriteria> req);
	
	ResponseEvent<PermissionDetail> addPermission(RequestEvent<PermissionDetail> req);
	
	ResponseEvent<PermissionDetail> deletePermission(RequestEvent<PermissionDetail> req);
	
	//
	// Role APIs
	//
	
	ResponseEvent<List<RoleDetail>> getRoles(RequestEvent<RoleListCriteria> req);

	ResponseEvent<Long> getRolesCount(RequestEvent<RoleListCriteria> req);
	
	ResponseEvent<RoleDetail> getRole(RequestEvent<Long> req);

	ResponseEvent<RoleDetail> saveRole(RequestEvent<RoleDetail> req);
	
	ResponseEvent<RoleDetail> updateRole(RequestEvent<RoleDetail> req);
	
	ResponseEvent<RoleDetail> deleteRole(RequestEvent<Long> req);
	
	//
	// Subject - Group roles API's
	//
	
	ResponseEvent<SubjectRoleDetail> updateSubjectRole(RequestEvent<SubjectRoleOp> req);
	
	ResponseEvent<List<SubjectRoleDetail>> getSubjectRoles(RequestEvent<Long> req);
	
	ResponseEvent<GroupDetail> updateGroupRoles(RequestEvent<GroupDetail> req);
		
	ResponseEvent<List<GroupRoleDetail>> getGroupRoles(RequestEvent<Long> req);
	
	//
	// Bulk update subject roles
	//
	ResponseEvent<SubjectRolesList> assignRoles(RequestEvent<SubjectRolesList> req);
	
	//
	// Internal APIs - can change without notice.
	//
	boolean canUserPerformOp(Long userId, String resource, String[] operations);
	
	List<SubjectAccess> getAccessList(Long userId, String resource, String[] operations);

	List<SubjectRole> addSubjectRole(Site site, CollectionProtocol cp, User user, String[] roleNames, SubjectRoleOpNotif notif);

	List<SubjectRole> removeSubjectRole(Site site, CollectionProtocol cp, User user, String[] roleNames, SubjectRoleOpNotif notif);

	void removeInvalidRoles(User user);

	void removeCpRoles(Long cpId);
}
