package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.ScheduledContainerActivity;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityDao;
import com.krishagni.catissueplus.core.administrative.repository.ScheduledContainerActivityListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ScheduledContainerActivityDaoImpl extends AbstractDao<ScheduledContainerActivity> implements ScheduledContainerActivityDao {
	@Override
	public Class<ScheduledContainerActivity> getType() {
		return ScheduledContainerActivity.class;
	}

	@Override
	public List<ScheduledContainerActivity> getActivities(ScheduledContainerActivityListCriteria crit) {
		Criteria<ScheduledContainerActivity> query = createCriteria(ScheduledContainerActivity.class, "activity")
			.join("activity.task", "task")
			.join("activity.container", "container");

		if (crit.containerId() != null) {
			query.add(query.eq("container.id", crit.containerId()));
		}

		if (crit.taskId() != null) {
			query.add(query.eq("task.id", crit.taskId()));
		} else if (StringUtils.isNotBlank(crit.taskName())) {
			query.add(query.eq("task.name", crit.taskName()));
		}

		if (StringUtils.isNotBlank(crit.activityStatus())) {
			query.add(query.eq("activity.activityStatus", crit.activityStatus()));
		}

		return query.orderBy(query.asc("activity.id")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public ScheduledContainerActivity getActivity(Long containerId, String name) {
		Criteria<ScheduledContainerActivity> query = createCriteria(ScheduledContainerActivity.class, "activity")
			.join("activity.container", "container");
		return query.add(query.eq("container.id", containerId))
			.add(query.eq("activity.name", name))
			.uniqueResult();
	}
}