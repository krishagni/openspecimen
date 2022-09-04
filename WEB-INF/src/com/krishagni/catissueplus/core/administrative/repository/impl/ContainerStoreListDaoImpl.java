package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList.Op;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreListItem;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;

public class ContainerStoreListDaoImpl extends AbstractDao<ContainerStoreList> implements ContainerStoreListDao {

	@Override
	public Class<?> getType() {
		return ContainerStoreList.class;
	}

	@Override
	public List<ContainerStoreList> getStoreLists(ContainerStoreListCriteria crit) {
		Criteria<ContainerStoreList> query = getQuery(crit);
		return query.orderBy(query.asc("sl.id"))
			.list(crit.startAt(), CollectionUtils.isNotEmpty(crit.ids()) ? crit.ids().size() : crit.maxResults());
	}

	@Override
	public Map<Op, Integer> getStoreListItemsCount(Date from, Date to) {
		List<Object[]> rows = createNamedQuery(GET_ITEMS_COUNT_BY_OP, Object[].class)
			.setParameter("fromDate", from)
			.setParameter("toDate", to)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Op)row[0], row -> ((Long)row[1]).intValue()));
	}

	@Override
	public void saveOrUpdateItem(ContainerStoreListItem item) {
		getCurrentSession().saveOrUpdate(item);
	}

	private Criteria<ContainerStoreList> getQuery(ContainerStoreListCriteria crit) {
		Criteria<ContainerStoreList> query = createCriteria(ContainerStoreList.class, "sl");
		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(query.in("sl.id", crit.ids()));
		}

		if (crit.fromDate() != null) {
			query.add(query.ge("sl.executionTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(query.le("sl.executionTime", crit.toDate()));
		}

		if (crit.lastRetryTime() != null) {
			query.add(
				query.or(
					query.isNull("sl.executionTime"),
					query.le("sl.executionTime", crit.lastRetryTime())
				)
			);
		}

		if (crit.maxRetries() != null) {
			query.add(
					query.or(
							query.isNull("sl.noOfRetries"),
							query.le("sl.noOfRetries", crit.maxRetries())
					)
			);
		}

		if (crit.statuses() != null) {
			query.add(query.in("sl.status", crit.statuses()));
		}

		return query;
	}

	private static final String FQN = ContainerStoreList.class.getName();

	private static final String GET_ITEMS_COUNT_BY_OP = FQN + ".getItemsCountByOp";
}
