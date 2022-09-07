package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerActivityLog;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogDao;
import com.krishagni.catissueplus.core.administrative.repository.ContainerActivityLogListCriteria;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ContainerActivityLogDaoImpl extends AbstractDao<ContainerActivityLog> implements ContainerActivityLogDao {
	@Override
	public Class<ContainerActivityLog> getType() {
		return ContainerActivityLog.class;
	}

	@Override
	public List<ContainerActivityLog> getActivityLogs(ContainerActivityLogListCriteria crit) {
		Criteria<ContainerActivityLog> query = createCriteria(ContainerActivityLog.class, "log")
			.join("log.container", "container")
			.join("log.task", "task")
			.join("log.performedBy", "performedBy")
			.leftJoin("log.activity", "activity");

		query.add(query.eq("log.activityStatus", "Active"));
		if (crit.fromDate() != null) {
			query.add(query.ge("log.activityDate", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(query.lt("log.activityDate", crit.toDate()));
		}

		if (crit.performedBy() != null) {
			query.add(query.eq("performedBy.id", crit.performedBy()));
		}

		if (crit.containerId() != null) {
			query.add(query.eq("container.id", crit.containerId()));
		}

		if (crit.taskId() != null) {
			query.add(query.eq("task.id", crit.taskId()));
		}

		if (crit.scheduledActivityId() != null) {
			query.add(query.eq("activity.id", crit.scheduledActivityId()));
		}

		return query.orderBy(query.desc("log.id")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public Map<Long, Date> getLatestScheduledActivityDate(Collection<Long> schedActivityIds) {
		if (CollectionUtils.isEmpty(schedActivityIds)) {
			return Collections.emptyMap();
		}

		List<Object[]> rows = createNamedQuery(GET_LATEST_SCHED_ACTIVITY_DATE, Object[].class)
			.setParameterList("activityIds", schedActivityIds)
			.list();

		Map<Long, Date> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Date) row[1]);
		}

		return result;
	}

	private static final String FQN = ContainerActivityLog.class.getName();

	private static final String GET_LATEST_SCHED_ACTIVITY_DATE = FQN + ".getLatestScheduledActivityDate";
}
