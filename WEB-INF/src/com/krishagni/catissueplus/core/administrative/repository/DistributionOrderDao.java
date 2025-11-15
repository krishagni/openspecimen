package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.administrative.domain.DistributionOrderItem;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderListCriteria;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderSummary;
import com.krishagni.catissueplus.core.common.repository.Dao;

public interface DistributionOrderDao extends Dao<DistributionOrder> {
	List<DistributionOrderSummary> getOrders(DistributionOrderListCriteria criteria);

	Long getOrdersCount(DistributionOrderListCriteria criteria);

	DistributionOrder getOrder(String name);

	List<DistributionOrder> getOrders(List<String> names);

	List<DistributionOrder> getUnpickedOrders(Date distSince, int startAt, int maxOrders);

	List<DistributionOrderItem> getDistributedOrderItems(List<Long> specimenIds);

	Map<String, Object> getOrderIds(String key, Object value);

	List<DistributionOrderItem> getOrderItems(DistributionOrderItemListCriteria crit);

	DistributionOrderItem getOrderItem(Long orderId, String spmnLabel);

	void saveOrUpdateOrderItem(DistributionOrderItem item);

	void deleteOrderItem(DistributionOrderItem item);

	Long getCustomFieldRecordId(Long orderId, Long formId, Long formCtxtId);

	Map<Long, Long> getCustomFieldRecordIds(Collection<Long> orderIds, Long formId, Long formCtxtId);

	int insertCustomFieldRecordId(Long orderId, Long formId, Long formCtxtId, Long recordId);
}
