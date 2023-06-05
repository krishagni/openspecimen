package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.UserGroup;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupDao;
import com.krishagni.catissueplus.core.administrative.repository.UserGroupListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.util.Status;

public class UserGroupDaoImpl extends AbstractDao<UserGroup> implements UserGroupDao {

	public Class<UserGroup> getType() {
		return UserGroup.class;
	}

	@Override
	public List<UserGroup> getGroups(UserGroupListCriteria crit) {
		Criteria<UserGroup> query = createCriteria(UserGroup.class, "g")
			.join("g.institute", "institute");

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("g.name", crit.query()));
		}

		if (StringUtils.isNotBlank(crit.institute())) {
			query.add(query.eq("institute.name", crit.institute()));
		}

		return query.orderBy(query.asc("g.name")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getGroupsCount(UserGroupListCriteria crit) {
		Criteria<UserGroup> query = createCriteria(UserGroup.class, "g")
			.createAlias("g.institute", "institute");

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("g.name", crit.query()));
		}

		if (StringUtils.isNotBlank(crit.institute())) {
			query.add(query.eq("institute.name", crit.institute()));
		}

		return query.getCount("g.id");
	}

	@Override
	public Map<Long, Integer> getGroupUsersCount(Collection<Long> groupIds) {
		if (CollectionUtils.isEmpty(groupIds)) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = createNamedQuery(GET_USERS_COUNT, Object[].class)
			.setParameter("groupIds", groupIds)
			.list();

		Map<Long, Integer> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Integer) row[1]);
		}

		return result;
	}

	@Override
	public UserGroup getByName(String name) {
		List<UserGroup> groups = getByNames(Collections.singleton(name));
		return groups.isEmpty() ? null : groups.iterator().next();
	}

	@Override
	public List<UserGroup> getByNames(Collection<String> names) {
		return createNamedQuery(GET_BY_NAMES, UserGroup.class)
			.setParameterList("names", names)
			.list();
	}

	@Override
	public boolean isMemberOf(String groupName, Long userId) {
		Criteria<Long> query = createCriteria(UserGroup.class, Long.class, "ug")
			.join("ug.users", "user");

		Long count = query.add(query.eq("ug.name", groupName))
			.add(query.eq("user.id", userId))
			.add(query.ne("user.activityStatus", Status.ACTIVITY_STATUS_DISABLED.getStatus()))
			.select(query.count("user.id"))
			.uniqueResult();
		return count != null && count > 0;
	}

	private static final String FQN = UserGroup.class.getName();

	private static final String GET_BY_NAMES = FQN + ".getByNames";

	private static final String GET_USERS_COUNT = FQN + ".getUsersCount";
}
