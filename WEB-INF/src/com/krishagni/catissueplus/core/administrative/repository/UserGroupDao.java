package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface UserGroupDao extends Dao<UserGroup> {
	List<UserGroup> getGroups(UserGroupListCriteria crit);

	Long getGroupsCount(UserGroupListCriteria crit);

	Map<Long, Integer> getGroupUsersCount(Collection<Long> groupIds);

	UserGroup getByName(String name);

	List<UserGroup> getByNames(Collection<String> names);
}
