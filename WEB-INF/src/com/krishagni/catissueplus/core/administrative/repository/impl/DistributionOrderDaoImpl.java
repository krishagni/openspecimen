package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Disjunction;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.ProjectionList;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.criterion.Subqueries;
import org.hibernate.sql.JoinType;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.repository.DistributionOrderDao;
import com.krishagni.catissueplus.core.common.OrderByNotNullProperty;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;

public class DistributionOrderDaoImpl extends AbstractDao<DistributionOrder> implements DistributionOrderDao {

	@SuppressWarnings("unchecked")
	@Override
	public List<DistributionOrderSummary> getOrders(DistributionOrderListCriteria listCrit) {
		Criteria query = getOrderListQuery(listCrit)
			.setFirstResult(listCrit.startAt())
			.setMaxResults(listCrit.maxResults())
			.addOrder(OrderByNotNullProperty.desc("executionDate", "creationDate"));

		addProjections(query, CollectionUtils.isNotEmpty(listCrit.sites()));
		List<Object[]> rows = query.list();
		
		List<DistributionOrderSummary> result = new ArrayList<>();
		Map<Long, DistributionOrderSummary> doMap = new HashMap<>();
		
		for (Object[] row : rows) {
			DistributionOrderSummary order = getDoSummary(row);
			result.add(order);
			
			if (listCrit.includeStat()) {
				doMap.put(order.getId(), order);
			}
		}
		
		if (listCrit.includeStat()) {
			loadOrderItemsCount(doMap);
		}
		
		return result;
	}

	@Override
	public Long getOrdersCount(DistributionOrderListCriteria listCrit) {
		Number count = (Number) getOrderListQuery(listCrit)
			.setProjection(Projections.rowCount())
			.uniqueResult();
		return count.longValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public DistributionOrder getOrder(String name) {
		List<DistributionOrder> result = getOrders(Collections.singletonList(name));
		return result.isEmpty() ? null : result.iterator().next();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DistributionOrder> getOrders(List<String> names) {
		return getCurrentSession().getNamedQuery(GET_ORDERS_BY_NAME)
			.setParameterList("names", names)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DistributionOrder> getUnpickedOrders(Date distSince, int startAt, int maxOrders) {
		return getCurrentSession().getNamedQuery(GET_UNPICKED_ORDERS)
			.setDate("distEarlierThan", distSince)
			.setFirstResult(startAt)
			.setMaxResults(maxOrders)
			.list();
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DistributionOrderItem> getDistributedOrderItems(List<Long> specimenIds) {
		return getSessionFactory().getCurrentSession()
			.getNamedQuery(GET_DISTRIBUTED_ITEMS_BY_SPMN_IDS)
			.setParameterList("ids", specimenIds)
			.list();
	}

	@Override
	public Class<DistributionOrder> getType() {
		return DistributionOrder.class;
	}

	@Override
	public Map<String, Object> getOrderIds(String key, Object value) {
		return getObjectIds("orderId", key, value);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<DistributionOrderItem> getOrderItems(DistributionOrderItemListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(DistributionOrderItem.class, "orderItem")
			.createAlias("orderItem.order", "order")
			.add(Restrictions.eq("order.id", crit.orderId()));

		if (crit.storedInContainers()) {
			query.createAlias("orderItem.specimen", "specimen")
				.createAlias("specimen.position", "spmnPos")
				.add(Restrictions.isNotNull("spmnPos.id"))
				.add(Restrictions.eq("orderItem.status", DistributionOrderItem.Status.DISTRIBUTED_AND_CLOSED));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(Restrictions.in("orderItem.id", crit.ids()));
		}

		return query.setFirstResult(crit.startAt())
			.setMaxResults(crit.maxResults())
			.addOrder(Order.asc("orderItem.id"))
			.list();
	}

	@Override
	public DistributionOrderItem getOrderItem(Long orderId, String spmnLabel) {
		return (DistributionOrderItem) getCurrentSession().createCriteria(DistributionOrderItem.class, "item")
			.createAlias("item.order", "order")
			.createAlias("item.specimen", "spmn")
			.add(Restrictions.eq("order.id", orderId))
			.add(Restrictions.eq("spmn.label", spmnLabel).ignoreCase())
			.uniqueResult();
	}

	@Override
	public void saveOrUpdateOrderItem(DistributionOrderItem item) {
		getCurrentSession().saveOrUpdate(item);
	}

	@Override
	public void deleteOrderItem(DistributionOrderItem item) {
		getCurrentSession().delete(item);
	}

	@SuppressWarnings("unchecked")
	private Criteria getOrderListQuery(DistributionOrderListCriteria crit) {
		Criteria query = getCurrentSession().createCriteria(DistributionOrder.class)
			.createAlias("distributionProtocol", "dp")
			.createAlias("requester", "user")
			.createAlias("site", "site", JoinType.LEFT_OUTER_JOIN);
		
		//
		// Add search restrictions
		//
		MatchMode matchMode = crit.exactMatch() ? MatchMode.EXACT : MatchMode.ANYWHERE;
		applyIdsFilter(query, "id", crit.ids());
		addSitesRestriction(query, crit);
		addNameRestriction(query, crit, matchMode);
		addDpRestriction(query, crit, matchMode);
		addRequestorRestriction(query, crit, matchMode);
		addRequestRestriction(query, crit);
		addExecutionDtRestriction(query, crit);
		addReceivingSiteRestriction(query, crit, matchMode);
		addReceivingInstRestriction(query, crit, matchMode);
		addStatusRestriction(query, crit);
		return query;
	}

	//
	// Restrict by accessible distributing sites
	//
	private void addSitesRestriction(Criteria query, DistributionOrderListCriteria crit) {
		if (CollectionUtils.isEmpty(crit.sites())) {
			return;
		}

		Set<Long> instituteIds = new HashSet<>();
		Set<Long> siteIds      = new HashSet<>();
		for (SiteCpPair site : crit.sites()) {
			if (site.getSiteId() != null) {
				siteIds.add(site.getSiteId());
			} else if (site.getInstituteId() != null) {
				instituteIds.add(site.getInstituteId());
			}
		}

		query.createAlias("dp.distributingSites", "distSites")
			.createAlias("distSites.site", "distSite", JoinType.LEFT_OUTER_JOIN)
			.createAlias("distSites.institute", "distInst")
			.createAlias("distInst.sites", "instSite");

		Disjunction instituteConds = Restrictions.disjunction();
		if (!siteIds.isEmpty()) {
			instituteConds.add(Restrictions.in("instSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			instituteConds.add(Restrictions.in("distInst.id", instituteIds));
		}

		Disjunction siteConds = Restrictions.disjunction();
		if (!siteIds.isEmpty()) {
			siteConds.add(Restrictions.in("distSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			DetachedCriteria instituteSites = DetachedCriteria.forClass(Site.class)
				.add(Restrictions.in("institute.id", instituteIds))
				.setProjection(Projections.property("id"));
			siteConds.add(Subqueries.propertyIn("distSite.id", instituteSites));
		}

		query.add(Restrictions.or(
			Restrictions.and(Restrictions.isNull("distSites.site"), instituteConds),
			Restrictions.and(Restrictions.isNotNull("distSites.site"), siteConds)
		));
	}
	
	private void addNameRestriction(Criteria query, DistributionOrderListCriteria crit, MatchMode mode) {
		if (StringUtils.isBlank(crit.query())) {
			return;
		}
		
		query.add(Restrictions.ilike("name", crit.query(), mode));
	}
	
	private void addDpRestriction(Criteria query, DistributionOrderListCriteria crit, MatchMode mode) {
		if (crit.dpId() != null) {
			query.add(Restrictions.eq("dp.id", crit.dpId()));
		} else if (StringUtils.isNotBlank(crit.dpShortTitle())) {
			query.add(Restrictions.ilike("dp.shortTitle", crit.dpShortTitle(), mode));
		}		
	}
	
	private void addRequestorRestriction(Criteria query, DistributionOrderListCriteria crit, MatchMode mode) {
		if (crit.requestorId() != null) {
			query.add(Restrictions.eq("user.id", crit.requestorId()));
		} else if (StringUtils.isNotBlank(crit.requestor())) {
			query.add(
				Restrictions.disjunction()
					.add(Restrictions.ilike("user.firstName", crit.requestor(), mode))
					.add(Restrictions.ilike("user.lastName", crit.requestor(), mode))
			);
		}	
	}

	private void addRequestRestriction(Criteria query, DistributionOrderListCriteria crit) {
		if (crit.requestId() == null) {
			return;
		}

		query.createAlias("request", "request")
			.add(Restrictions.eq("request.id", crit.requestId()));
	}

	private void addExecutionDtRestriction(Criteria query, DistributionOrderListCriteria crit) {
		if (crit.executionDate() == null) {
			return;
		}
		
		Calendar from = Calendar.getInstance();
		from.setTime(crit.executionDate());
		from.set(Calendar.HOUR_OF_DAY, 0);
		from.set(Calendar.MINUTE, 0);
		from.set(Calendar.SECOND, 0);
		from.set(Calendar.MILLISECOND, 0);
			
		Calendar to = Calendar.getInstance();
		to.setTime(crit.executionDate());
		to.set(Calendar.HOUR_OF_DAY, 23);
		to.set(Calendar.MINUTE, 59);
		to.set(Calendar.SECOND, 59);
		to.set(Calendar.MILLISECOND, 999);
						
		query.add(Restrictions.between("executionDate", from.getTime(), to.getTime()));
	}

	private void addReceivingSiteRestriction(Criteria query, DistributionOrderListCriteria crit, MatchMode mode) {
		if (StringUtils.isBlank(crit.receivingSite())) {
			return;
		}
		
		query.add(Restrictions.ilike("site.name", crit.receivingSite(), mode));
	}
	
	private void addReceivingInstRestriction(Criteria query, DistributionOrderListCriteria crit, MatchMode mode) {
		if (StringUtils.isBlank(crit.receivingInstitute())) {
			return;
		}
		
		query.createAlias("site.institute", "institute")
			.add(Restrictions.ilike("institute.name", crit.receivingInstitute(), mode));
	}

	private void addStatusRestriction(Criteria query, DistributionOrderListCriteria crit) {
		if (StringUtils.isBlank(crit.status())) {
			return;
		}

		try {
			query.add(Restrictions.eq("status", DistributionOrder.Status.valueOf(crit.status())));
		} catch (Exception e) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.INVALID_STATUS, crit.status());
		}
	}
	
	private void addProjections(Criteria query, boolean isDistinct) {
		ProjectionList projs = Projections.projectionList();
		if (isDistinct) {
			//added as duplicate entries come due to distributing site lists
			query.setProjection(Projections.distinct(projs));
		} else {
			query.setProjection(projs);
		}
		
		projs.add(Projections.property("id"));
		projs.add(Projections.property("name"));
		projs.add(Projections.property("creationDate"));
		projs.add(Projections.property("executionDate"));
		projs.add(Projections.property("status"));
		projs.add(Projections.property("dp.id"));
		projs.add(Projections.property("dp.shortTitle"));
		projs.add(Projections.property("site.id"));
		projs.add(Projections.property("site.name"));
		projs.add(Projections.property("user.id"));
		projs.add(Projections.property("user.firstName"));
		projs.add(Projections.property("user.lastName"));
		projs.add(Projections.property("user.emailAddress"));
	}
	
	private DistributionOrderSummary getDoSummary(Object[] row) {
		DistributionOrderSummary result = new DistributionOrderSummary();
		result.setId((Long)row[0]);
		result.setName((String)row[1]);
		result.setCreationDate((Date)row[2]);
		result.setExecutionDate((Date)row[3]);
		result.setStatus(((DistributionOrder.Status)row[4]).name());
		
		DistributionProtocolDetail dp = new DistributionProtocolDetail();
		dp.setId((Long)row[5]);
		dp.setShortTitle((String)row[6]);
		result.setDistributionProtocol(dp);
		
		result.setSiteId((Long)row[7]);
		result.setSiteName((String)row[8]);
		
		UserSummary requester = new UserSummary();
		requester.setId((Long)row[9]);
		requester.setFirstName((String)row[10]);
		requester.setLastName((String)row[11]);
		requester.setEmailAddress((String)row[12]);
		result.setRequester(requester);
		
		return result;
	}

	private void loadOrderItemsCount(Map<Long, DistributionOrderSummary> doMap) {
		loadOrderItemsCount(doMap, GET_ORDER_ITEMS_COUNT);
		loadOrderItemsCount(doMap, GET_ORDER_LIST_ITEMS_COUNT);
	}

	private void loadOrderItemsCount(Map<Long, DistributionOrderSummary> doMap, String query) {
		if (doMap.isEmpty()) {
			return;
		}

		List<Object[]> rows = getCurrentSession().getNamedQuery(query)
			.setParameterList("orderIds", doMap.keySet())
			.list();

		for (Object[] row : rows) {
			DistributionOrderSummary order = doMap.remove((Long)row[0]);
			order.setSpecimenCnt((Long)row[1]);
		}
	}
	
	public static final String FQN  = DistributionOrder.class.getName();
	
	private static final String GET_ORDERS_BY_NAME = FQN + ".getOrdersByName";

	private static final String GET_UNPICKED_ORDERS = FQN + ".getUnpickedSpecimenOrders";

	private static final String GET_DISTRIBUTED_ITEMS_BY_SPMN_IDS = FQN + ".getDistributedItemsBySpmnIds";
	
	private static final String GET_ORDER_ITEMS_COUNT = FQN + ".getOrderItemsCount";

	private static final String GET_ORDER_LIST_ITEMS_COUNT = FQN + ".getListItemsCount";
}
