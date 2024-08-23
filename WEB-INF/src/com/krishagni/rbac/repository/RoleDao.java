package com.krishagni.rbac.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.repository.Dao;
import com.krishagni.rbac.domain.Role;

public interface RoleDao extends Dao<Role> {
	List<Role> getRoles(RoleListCriteria listCriteria);

	Long getRolesCount(RoleListCriteria listCriteria);
	
	List<Role> getRolesByNames(List<String> roleNames);

	Role getRoleByName(String roleName);
}
