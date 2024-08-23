package com.krishagni.rbac.repository.impl;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.rbac.domain.Role;
import com.krishagni.rbac.repository.RoleDao;
import com.krishagni.rbac.repository.RoleListCriteria;

public class RoleDaoImpl extends AbstractDao<Role> implements RoleDao {
	
	@Override
	public List<Role> getRoles(RoleListCriteria listCriteria) {
		Criteria<Role> query = createCriteria(Role.class, "r");
		if (StringUtils.isNotBlank(listCriteria.query())) {
			query.add(query.ilike("r.name", listCriteria.query()));
		}
		
		return query.orderBy(query.asc("r.name")).list(listCriteria.startAt(), listCriteria.maxResults());
	}

	@Override
	public Long getRolesCount(RoleListCriteria listCriteria) {
		Criteria<Role> query = createCriteria(Role.class, "r");
		if (StringUtils.isNotBlank(listCriteria.query())) {
			query.add(query.ilike("r.name", listCriteria.query()));
		}

		return query.getCount("r.id");
	}

	@Override
	public List<Role> getRolesByNames(List<String> roleNames) {
		return createNamedQuery(GET_ROLES_BY_NAMES, Role.class)
			.setParameterList("roleNames", roleNames)
			.list();
	}
	
	@Override
	public Role getRoleByName(String roleName) {
		List<Role> roles = getRolesByNames(Collections.singletonList(roleName));
		return roles.isEmpty() ? null : roles.get(0);
	}
	
	@Override
	public Class<?> getType() {
		return Role.class;
	}
	
	private static final String FQN = Role.class.getName();
	
	private static final String GET_ROLES_BY_NAMES = FQN + ".getRolesByNames";
}
