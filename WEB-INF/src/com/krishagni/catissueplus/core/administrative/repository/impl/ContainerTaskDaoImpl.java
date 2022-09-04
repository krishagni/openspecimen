package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerTask;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerTaskListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ContainerTaskDaoImpl extends AbstractDao<ContainerTask> implements ContainerTaskDao {

	@Override
	public Class<?> getType() {
		return ContainerTask.class;
	}

	@Override
	public List<ContainerTask> getTasks(ContainerTaskListCriteria crit) {
		Criteria<ContainerTask> query = getTasksListQuery(crit);
		return query.orderBy(query.asc("task.name")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Integer getTasksCount(ContainerTaskListCriteria crit) {
		Criteria<ContainerTask> query = getTasksListQuery(crit);
		return query.getCount("task.id").intValue();
	}

	@Override
	public ContainerTask getByName(String name) {
		return createNamedQuery(GET_BY_NAME, ContainerTask.class)
			.setParameter("name", name)
			.uniqueResult();
	}

	private Criteria<ContainerTask> getTasksListQuery(ContainerTaskListCriteria crit) {
		Criteria<ContainerTask> query = createCriteria(ContainerTask.class, "task");

		if (StringUtils.isNotBlank(crit.query())) {
			query.add(query.ilike("task.name", crit.query()));
		}

		if (StringUtils.isNotBlank(crit.activityStatus())) {
			query.add(query.eq("task.activityStatus", crit.activityStatus()));
		}

		return query;
	}

	private static final String FAQ = ContainerTask.class.getName();

	private static final String GET_BY_NAME = FAQ + ".getByName";
}
