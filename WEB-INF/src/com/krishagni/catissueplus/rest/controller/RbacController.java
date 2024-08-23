package com.krishagni.catissueplus.rest.controller;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

import com.krishagni.catissueplus.core.common.events.RequestEvent;
import com.krishagni.catissueplus.core.common.events.ResponseEvent;
import com.krishagni.rbac.events.GroupDetail;
import com.krishagni.rbac.events.GroupRoleDetail;
import com.krishagni.rbac.events.OperationDetail;
import com.krishagni.rbac.events.PermissionDetail;
import com.krishagni.rbac.events.ResourceDetail;
import com.krishagni.rbac.events.RoleDetail;
import com.krishagni.rbac.events.SubjectRoleDetail;
import com.krishagni.rbac.events.SubjectRoleOp;
import com.krishagni.rbac.events.SubjectRoleOp.OP;
import com.krishagni.rbac.repository.OperationListCriteria;
import com.krishagni.rbac.repository.PermissionListCriteria;
import com.krishagni.rbac.repository.ResourceListCriteria;
import com.krishagni.rbac.repository.RoleListCriteria;
import com.krishagni.rbac.service.RbacService;

@Controller
@RequestMapping("/rbac")
public class RbacController {

	@Autowired
	private RbacService rbacSvc;
	
	private <T> RequestEvent<T> getRequest(T payload) {
		return new RequestEvent<T>(payload);
	}
	
	//
	// - Resource APIs
	//
	
	@RequestMapping(method = RequestMethod.GET, value="/resources")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<ResourceDetail> getAllResources(
			@RequestParam(value = "name", required = false) 
			String name,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {
		ResourceListCriteria criteria = new ResourceListCriteria()
			.query(name)
			.startAt(startAt)
			.maxResults(maxResults);
		
		ResponseEvent<List<ResourceDetail>> resp = rbacSvc.getResources(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/resources")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ResourceDetail createResource(@RequestBody ResourceDetail resourceDetail) {
		ResponseEvent<ResourceDetail> resp = rbacSvc.saveResource(getRequest(resourceDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/resources/{name}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public ResourceDetail deleteResource(@PathVariable("name") String resourceName) {
		ResponseEvent<ResourceDetail> resp = rbacSvc.deleteResource(getRequest(resourceName));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
		
	//
	// - Operation APIs
	//

	@RequestMapping(method = RequestMethod.GET, value="/operations")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<OperationDetail> getAllOperations(
			@RequestParam(value = "name", required = false) 
			String name,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) { 
		OperationListCriteria criteria = new OperationListCriteria()
			.query(name)
			.startAt(startAt)
			.maxResults(maxResults);
		
		ResponseEvent<List<OperationDetail>> resp = rbacSvc.getOperations(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/operations")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public OperationDetail addOperation(@RequestBody OperationDetail operation) {
		ResponseEvent<OperationDetail> resp = rbacSvc.saveOperation(getRequest(operation));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/operations/{name}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public OperationDetail deleteOperation(@PathVariable("name") String operationName) {
		ResponseEvent<OperationDetail> resp = rbacSvc.deleteOperation(getRequest(operationName));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	
	//
	// - Permission APIs
	//

	@RequestMapping(method = RequestMethod.GET, value="/permissions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<PermissionDetail> getAllPermissions(
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
	
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {
		PermissionListCriteria criteria = new PermissionListCriteria()
			.startAt(startAt)
			.maxResults(maxResults);
		
		ResponseEvent<List<PermissionDetail>> resp = rbacSvc.getPermissions(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/permissions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PermissionDetail addPermission(@RequestBody PermissionDetail permissionDetail) {
		ResponseEvent<PermissionDetail> resp = rbacSvc.addPermission(getRequest(permissionDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/permissions")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public PermissionDetail deletePermission(
			@RequestParam(value = "resource", required = true) 
			String resource,
			
			@RequestParam(value = "operation", required = true) 
			String operation) {
		PermissionDetail detail = new PermissionDetail();
		detail.setResourceName(resource);
		detail.setOperationName(operation);
		ResponseEvent<PermissionDetail> resp = rbacSvc.deletePermission(getRequest(detail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	
	//
	// - Role APIs
	//

	@RequestMapping(method = RequestMethod.GET, value="/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<RoleDetail> getRoles(
			@RequestParam(value = "name", required = false) 
			String name,
			
			@RequestParam(value = "startAt", required = false, defaultValue = "0")
			int startAt,
			
			@RequestParam(value = "maxResults", required = false, defaultValue = "100")
			int maxResults) {
		RoleListCriteria criteria = new RoleListCriteria()
			.query(name)
			.startAt(startAt)
			.maxResults(maxResults);

		ResponseEvent<List<RoleDetail>> resp = rbacSvc.getRoles(getRequest(criteria));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	@RequestMapping(method = RequestMethod.GET, value="/roles/count")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public Map<String, Long> getRolesCount(@RequestParam(value = "name", required = false) String name) {
		RoleListCriteria criteria = new RoleListCriteria().query(name);
		return Collections.singletonMap("count", ResponseEvent.unwrap(rbacSvc.getRolesCount(RequestEvent.wrap(criteria))));
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/roles/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public RoleDetail getRole(@PathVariable Long id) {
		ResponseEvent<RoleDetail> resp = rbacSvc.getRole(getRequest(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public RoleDetail createRole(@RequestBody RoleDetail roleDetail) {
		ResponseEvent<RoleDetail> resp = rbacSvc.saveRole(getRequest(roleDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/roles/{roleId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public RoleDetail updateRole(@PathVariable Long roleId, @RequestBody RoleDetail roleDetail) {
		roleDetail.setId(roleId);
		ResponseEvent<RoleDetail> resp = rbacSvc.updateRole(getRequest(roleDetail));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
		
	@RequestMapping(method = RequestMethod.DELETE, value="/roles/{id}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody 
	public RoleDetail deleteRole(@PathVariable("id") Long id) {
		ResponseEvent<RoleDetail> resp = rbacSvc.deleteRole(getRequest(id));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	

	//
	// - Subject APIs
	//	
	@RequestMapping(method = RequestMethod.GET, value="/subjects/{subjectId}/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<SubjectRoleDetail> getSubjectRoles(@PathVariable Long subjectId) {
		ResponseEvent<List<SubjectRoleDetail>> resp = rbacSvc.getSubjectRoles(getRequest(subjectId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.POST, value="/subjects/{id}/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SubjectRoleDetail addSubjectRole(
			@PathVariable("id") Long subjectId, 
			@RequestBody SubjectRoleDetail subjectRole) {
		return performSubjectRoleOp(SubjectRoleOp.OP.ADD, subjectId, subjectRole);
	}
	
	@RequestMapping(method = RequestMethod.PUT, value="/subjects/{id}/roles/{roleId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SubjectRoleDetail updateSubjectRole(
			@PathVariable("id") Long subjectId,
			@PathVariable("roleId") Long roleId,
			@RequestBody SubjectRoleDetail subjectRole) {
		subjectRole.setId(roleId);
		return performSubjectRoleOp(SubjectRoleOp.OP.UPDATE, subjectId, subjectRole);
	}
	
	@RequestMapping(method = RequestMethod.DELETE, value="/subjects/{id}/roles/{roleId}")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public SubjectRoleDetail removeSubjectRole(
			@PathVariable("id") Long subjectId,
			@PathVariable("roleId") Long roleId) {
		SubjectRoleDetail subjectRole= new SubjectRoleDetail();
		subjectRole.setId(roleId);
		return performSubjectRoleOp(SubjectRoleOp.OP.REMOVE, subjectId, subjectRole);
	}
		
	private SubjectRoleDetail performSubjectRoleOp(OP op, Long subjectId, SubjectRoleDetail subjectRole) {
		SubjectRoleOp subjectRoleOp = new SubjectRoleOp();
		subjectRoleOp.setSubjectId(subjectId);
		subjectRoleOp.setOp(op);
		subjectRoleOp.setSubjectRole(subjectRole);
		
		ResponseEvent<SubjectRoleDetail> resp = rbacSvc.updateSubjectRole(getRequest(subjectRoleOp));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}

	//
	// - Group APIs
	//
	@RequestMapping(method = RequestMethod.PUT, value="/groups/{groupId}/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public GroupDetail updateGroupRole(
			@PathVariable("groupId") Long groupId,
			@RequestBody GroupDetail group) {
		group.setId(groupId);
		ResponseEvent<GroupDetail> resp = rbacSvc.updateGroupRoles(getRequest(group));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
	
	@RequestMapping(method = RequestMethod.GET, value="/groups/{groupId}/roles")
	@ResponseStatus(HttpStatus.OK)
	@ResponseBody
	public List<GroupRoleDetail> getGroupRoles(@PathVariable Long groupId) {
		ResponseEvent<List<GroupRoleDetail>> resp = rbacSvc.getGroupRoles(getRequest(groupId));
		resp.throwErrorIfUnsuccessful();
		return resp.getPayload();
	}
}