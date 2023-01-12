package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;

import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreList.Op;
import com.krishagni.catissueplus.core.administrative.domain.ContainerStoreListItem;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListCriteria;
import com.krishagni.catissueplus.core.administrative.repository.ContainerStoreListDao;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class ContainerStoreListDaoImpl extends AbstractDao<ContainerStoreList> implements ContainerStoreListDao {

	@Override
	public Class<?> getType() {
		return ContainerStoreList.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<ContainerStoreList> getStoreLists(ContainerStoreListCriteria crit) {
		return getQuery(crit).addOrder(Order.asc("sl.id"))
			.setFirstResult(crit.startAt())
			.setMaxResults(CollectionUtils.isNotEmpty(crit.ids()) ? crit.ids().size() : crit.maxResults())
			.list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public Map<Op, Integer> getStoreListItemsCount(Date from, Date to) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_ITEMS_COUNT_BY_OP)
			.setTimestamp("fromDate", from)
			.setTimestamp("toDate", to)
			.list();
		return rows.stream().collect(Collectors.toMap(row -> (Op)row[0], row -> ((Long)row[1]).intValue()));
	}

	@Override
	public void saveOrUpdateItem(ContainerStoreListItem item) {
		getCurrentSession().saveOrUpdate(item);
	}

	@Override
	public List<StorageContainer> getAutomatedFreezers(List<Long> storeListIds) {
		DetachedCriteria subQuery = DetachedCriteria.forClass(ContainerStoreList.class, "sl")
			.createAlias("sl.container", "c")
			.add(Restrictions.in("sl.id", storeListIds))
			.setProjection(Projections.property("c.id"));

		return getCurrentSession().createCriteria(StorageContainer.class, "c")
			.createAlias("c.site", "site")
			.add(Subqueries.propertyIn("c.id", subQuery))
			.list();
	}

	private Criteria getQuery(ContainerStoreListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(ContainerStoreList.class, "sl")
			.createAlias("sl.container", "c");

		if (crit.containerId() != null) {
			query.add(Restrictions.eq("c.id", crit.containerId()));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(Restrictions.in("sl.id", crit.ids()));
		}

		if (crit.fromDate() != null) {
			query.add(Restrictions.ge("sl.executionTime", crit.fromDate()));
		}

		if (crit.toDate() != null) {
			query.add(Restrictions.le("sl.executionTime", crit.toDate()));
		}

		if (crit.lastRetryTime() != null) {
			query.add(
				Restrictions.or(
					Restrictions.isNull("sl.executionTime"),
					Restrictions.le("sl.executionTime", crit.lastRetryTime())
				)
			);
		}

		if (crit.maxRetries() != null) {
			query.add(
					Restrictions.or(
							Restrictions.isNull("sl.noOfRetries"),
							Restrictions.le("sl.noOfRetries", crit.maxRetries())
					)
			);
		}

		if (crit.statuses() != null) {
			query.add(Restrictions.in("sl.status", crit.statuses()));
		}

		return query;
	}

	private static final String FQN = ContainerStoreList.class.getName();

	private static final String GET_ITEMS_COUNT_BY_OP = FQN + ".getItemsCountByOp";
}
