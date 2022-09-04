package com.krishagni.rbac.repository.impl;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.rbac.domain.Permission;
import com.krishagni.rbac.repository.PermissionDao;
import com.krishagni.rbac.repository.PermissionListCriteria;

public class PermissionDaoImpl extends AbstractDao<Permission> implements PermissionDao  {
	private static final String FQN = Permission.class.getName();
	
	private static final String GET_PERM_BY_RESOURCE_OPERATION = FQN + ".getPermissionByResourceAndOperation";
	
	@Override
	public Permission getPermission(Long permissionId) {
		return getCurrentSession().get(Permission.class, permissionId);
	}

	@Override
	public Permission getPermission(String resourceName, String operationName) {
		List<Permission> perms = createNamedQuery(GET_PERM_BY_RESOURCE_OPERATION, Permission.class)
			.setParameter("resourceName", resourceName)
			.setParameter("operationName", operationName)
			.list();
		return perms.isEmpty() ? null : perms.get(0);
	}

	@Override
	public List<Permission> getPermissions(PermissionListCriteria listCriteria) {
		return createCriteria(Permission.class, "p")
			.list(listCriteria.startAt(), listCriteria.maxResults());
	}
}
