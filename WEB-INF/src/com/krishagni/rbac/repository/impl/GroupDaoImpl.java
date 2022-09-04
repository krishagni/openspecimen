package com.krishagni.rbac.repository.impl;

import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.rbac.domain.Group;
import com.krishagni.rbac.repository.GroupDao;

public class GroupDaoImpl extends AbstractDao<Group> implements GroupDao {
	@Override
	public Group getGroup(Long groupId) {
		return getCurrentSession().get(Group.class, groupId);
	}
}
