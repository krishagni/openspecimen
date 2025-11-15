package com.krishagni.catissueplus.core.administrative.repository.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.administrative.events.DistributionProtocolDetail;
import com.krishagni.catissueplus.core.administrative.repository.DistributionOrderDao;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.repository.AbstractDao;
import com.krishagni.catissueplus.core.common.repository.Criteria;
import com.krishagni.catissueplus.core.common.repository.Disjunction;
import com.krishagni.catissueplus.core.common.repository.SubQuery;

public class DistributionOrderDaoImpl extends AbstractDao<DistributionOrder> implements DistributionOrderDao {

	@Override
	public List<DistributionOrderSummary> getOrders(DistributionOrderListCriteria listCrit) {
		Criteria<Object[]> query = getOrderListQuery(listCrit);
		query.orderBy(query.desc("creationDate"));

		addProjections(query, CollectionUtils.isNotEmpty(listCrit.sites()));
		List<Object[]> rows = query.list(listCrit.startAt(), listCrit.maxResults());
		
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
		return getOrderListQuery(listCrit).getCount("order.id");
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
			.setParameter("distEarlierThan", distSince)
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
		Criteria<DistributionOrderItem> query = Criteria.create(getCurrentSession(), DistributionOrderItem.class, "orderItem");
		query.createAlias("orderItem.order", "order")
			.add(query.eq("order.id", crit.orderId()));

		if (crit.storedInContainers()) {
			query.createAlias("orderItem.specimen", "specimen")
				.createAlias("specimen.position", "spmnPos")
				.add(query.isNotNull("spmnPos.id"))
				.add(query.eq("orderItem.status", DistributionOrderItem.Status.DISTRIBUTED_AND_CLOSED));
		}

		if (CollectionUtils.isNotEmpty(crit.ids())) {
			query.add(query.in("orderItem.id", crit.ids()));
		}

		return query.orderBy(query.asc("orderItem.id")).list(crit.startAt(), crit.maxResults());
	}

	@Override
	public DistributionOrderItem getOrderItem(Long orderId, String spmnLabel) {
		Criteria<DistributionOrderItem> query = Criteria.create(getCurrentSession(), DistributionOrderItem.class, "item")
			.createAlias("item.order", "order")
			.createAlias("item.specimen", "spmn");
		return query.add(query.eq("order.id", orderId))
			.add(query.eq("spmn.label", spmnLabel))
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

	@Override
	public Long getCustomFieldRecordId(Long orderId, Long formId, Long formCtxtId) {
		return (Long) getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_ID)
			.setParameter("orderId", orderId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.uniqueResult();
	}

	@Override
	public Map<Long, Long> getCustomFieldRecordIds(Collection<Long> orderIds, Long formId, Long formCtxtId) {
		List<Object[]> rows = getCurrentSession().getNamedQuery(GET_CUSTOM_FIELD_RECORD_IDS)
			.setParameterList("orderIds", orderIds)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.list();

		Map<Long, Long> result = new HashMap<>();
		for (Object[] row : rows) {
			result.put((Long) row[0], (Long) row[1]);
		}

		return result;
	}

	@Override
	public int insertCustomFieldRecordId(Long orderId, Long formId, Long formCtxtId, Long recordId) {
		int rows = getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_RECORD)
			.setParameter("orderId", orderId)
			.setParameter("formId", formId)
			.setParameter("formCtxtId", formCtxtId)
			.setParameter("recordId", recordId)
			.executeUpdate();

		getCurrentSession().getNamedQuery(INSERT_CUSTOM_FIELD_REC_STATUS)
			.setParameter("orderId", orderId)
			.setParameter("formId", formId)
			.setParameter("recordId", recordId)
			.executeUpdate();

		return rows;
	}

	@SuppressWarnings("unchecked")
	private Criteria<Object[]> getOrderListQuery(DistributionOrderListCriteria crit) {
		Criteria<Object[]> query = createCriteria(DistributionOrder.class, Object[].class, "order")
			.join("order.distributionProtocol", "dp")
			.join("order.requester", "user")
			.leftJoin("order.site", "site");
		
		//
		// Add search restrictions
		//
		applyIdsFilter(query, "order.id", crit.ids());
		addSitesRestriction(query, crit);
		addNameRestriction(query, crit);
		addDpRestriction(query, crit);
		addRequestorRestriction(query, crit);
		addRequestRestriction(query, crit);
		addExecutionDtRestriction(query, crit);
		addReceivingSiteRestriction(query, crit);
		addReceivingInstRestriction(query, crit);
		addStatusRestriction(query, crit);
		return query;
	}

	//
	// Restrict by accessible distributing sites
	//
	private void addSitesRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
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

		query.join("dp.distributingSites", "distSites")
			.join("distSites.institute", "distInst")
			.join("distInst.sites", "instSite")
			.leftJoin("distSites.site", "distSite");

		Disjunction instituteConds = query.disjunction();
		if (!siteIds.isEmpty()) {
			instituteConds.add(query.in("instSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			instituteConds.add(query.in("distInst.id", instituteIds));
		}

		Disjunction siteConds = query.disjunction();
		if (!siteIds.isEmpty()) {
			siteConds.add(query.in("distSite.id", siteIds));
		}

		if (!instituteIds.isEmpty()) {
			SubQuery<Long> instituteSites = query.createSubQuery(Site.class, "site")
				.join("site.institute", "institute")
				.select("site.id");
			instituteSites.add(instituteSites.in("institute.id", instituteIds));
			siteConds.add(query.in("distSite.id", instituteSites));
		}

		query.add(query.or(
			query.and(query.isNull("distSites.site"), instituteConds.getRestriction()),
			query.and(query.isNotNull("distSites.site"), siteConds.getRestriction())
		));
	}
	
	private void addNameRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (StringUtils.isBlank(crit.query())) {
			return;
		}
		
		query.add(query.ilike("order.name", crit.query()));
	}
	
	private void addDpRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (crit.dpId() != null) {
			query.add(query.eq("dp.id", crit.dpId()));
		} else if (StringUtils.isNotBlank(crit.dpShortTitle())) {
			query.add(query.ilike("dp.shortTitle", crit.dpShortTitle()));
		}		
	}
	
	private void addRequestorRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (crit.requestorId() != null) {
			query.add(query.eq("user.id", crit.requestorId()));
		} else if (StringUtils.isNotBlank(crit.requestor())) {
			query.add(
				query.disjunction()
					.add(query.ilike("user.firstName", crit.requestor()))
					.add(query.ilike("user.lastName", crit.requestor()))
			);
		}	
	}

	private void addRequestRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (crit.requestId() == null) {
			return;
		}

		query.join("order.request", "request")
			.add(query.eq("request.id", crit.requestId()));
	}

	private void addExecutionDtRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
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
						
		query.add(query.between("order.executionDate", from.getTime(), to.getTime()));
	}

	private void addReceivingSiteRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingSite())) {
			return;
		}
		
		query.add(query.ilike("site.name", crit.receivingSite()));
	}
	
	private void addReceivingInstRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (StringUtils.isBlank(crit.receivingInstitute())) {
			return;
		}
		
		query.join("site.institute", "institute")
			.add(query.ilike("institute.name", crit.receivingInstitute()));
	}

	private void addStatusRestriction(Criteria<?> query, DistributionOrderListCriteria crit) {
		if (StringUtils.isBlank(crit.status())) {
			return;
		}

		try {
			query.add(query.eq("order.status", DistributionOrder.Status.valueOf(crit.status())));
		} catch (Exception e) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.INVALID_STATUS, crit.status());
		}
	}
	
	private void addProjections(Criteria<?> query, boolean isDistinct) {
		query.select(
			"order.id", "order.name", "order.creationDate", "order.executionDate", "order.status",
			"dp.id", "dp.shortTitle", "site.id", "site.name",
			"user.id", "user.firstName", "user.lastName", "user.emailAddress"
		).distinct(isDistinct);
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

		List<Object[]> rows = createNamedQuery(query, Object[].class)
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

	private static final String GET_CUSTOM_FIELD_RECORD_ID = FQN + ".getCustomFieldRecordId";

	private static final String GET_CUSTOM_FIELD_RECORD_IDS = FQN + "..getCustomFieldRecordIds";

	private static final String INSERT_CUSTOM_FIELD_RECORD = FQN + ".insertCustomFieldRecord";

	private static final String INSERT_CUSTOM_FIELD_REC_STATUS = FQN + ".insertCustomFieldRecordStatus";
}
