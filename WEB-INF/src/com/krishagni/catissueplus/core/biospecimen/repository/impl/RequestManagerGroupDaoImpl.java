package com.krishagni.catissueplus.core.biospecimen.repository.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.biospecimen.domain.RequestManagerGroup;
import com.krishagni.catissueplus.core.biospecimen.repository.RequestManagerGroupDao;
import com.krishagni.catissueplus.core.biospecimen.repository.RmgListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class RequestManagerGroupDaoImpl extends AbstractDao<RequestManagerGroup> implements RequestManagerGroupDao {
	@Override
	public Class<RequestManagerGroup> getType() {
		return RequestManagerGroup.class;
	}

	@Override
	public List<RequestManagerGroup> getGroups(RmgListCriteria crit) {
		Criteria<RequestManagerGroup> query = getGroupsQuery(crit);
		return query.orderBy(query.asc("rmg.name"))
			.list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Long getGroupsCount(RmgListCriteria crit) {
		return getGroupsQuery(crit).getCount("rmg.id");
	}

	@Override
	public Map<Long, Long> getActiveCpCounts(List<Long> groupIds) {
		if (CollectionUtils.isEmpty(groupIds)) {
			return new HashMap<>();
		}

		List<Object[]> rows = createNamedQuery(GET_ACTIVE_CP_COUNTS, Object[].class)
			.setParameter("groupIds", groupIds)
			.list();

		Map<Long, Long> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Long) row[1]);
		}

		return result;
	}

	@Override
	public RequestManagerGroup getByName(String name) {
		return createNamedQuery(GET_BY_NAME, RequestManagerGroup.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	private Criteria<RequestManagerGroup> getGroupsQuery(RmgListCriteria crit) {
		Criteria<RequestManagerGroup> query = createCriteria(RequestManagerGroup.class, "rmg");
		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("rmg.name", crit.query()));
		}

		return query;
	}

	private static final String FQN = RequestManagerGroup.class.getName();

	private static final String GET_BY_NAME = FQN + ".getByName";

	private static final String GET_ACTIVE_CP_COUNTS = FQN + ".getActiveCpCounts";
}
