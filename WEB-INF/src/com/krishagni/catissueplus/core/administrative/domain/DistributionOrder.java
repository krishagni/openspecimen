package com.krishagni.catissueplus.core.administrative.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import com.krishagni.catissueplus.core.administrative.domain.factory.DistributionOrderErrorCode;
import com.krishagni.catissueplus.core.administrative.domain.factory.SpecimenRequestErrorCode;
import com.krishagni.catissueplus.core.administrative.events.DistributionOrderItemListCriteria;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.util.Utility;

@Audited
@Configurable
public class DistributionOrder extends BaseExtensionEntity {
	public enum Status { 
		PENDING,
		EXECUTED
	}

	public enum ClearListMode {
		ALL,
		DISTRIBUTED,
		NONE
	};
	
	private static final String ENTITY_NAME = "distribution_order";

	private static final String EXTN = "OrderExtension";
	
	private String name;
	
	private DistributionProtocol distributionProtocol;
	
	private Site site;
	
	private User requester;
	
	private Date creationDate;
	
	private User distributor;
	
	private Date executionDate;

	private Set<DistributionOrderItem> orderItems = new HashSet<DistributionOrderItem>();
	
	private Status status;
	
	private String activityStatus;
	
	private String trackingUrl;
	
	private String comments;

	private SpecimenRequest request;

	private SpecimenList specimenList;

	private Boolean allReservedSpecimens;

	private Long clearListId;

	private ClearListMode clearListMode;

	private Boolean checkoutSpecimens;

	@Autowired
	private DaoFactory daoFactory;

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	public static String getExtnEntityType() { return EXTN; }

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DistributionProtocol getDistributionProtocol() {
		return distributionProtocol;
	}

	public void setDistributionProtocol(DistributionProtocol distributionProtocol) {
		this.distributionProtocol = distributionProtocol;
	}

	public Site getSite() {
		return site;
	}

	public void setSite(Site distributionSite) {
		this.site = distributionSite;
	}

	public User getRequester() {
		return requester;
	}

	public void setRequester(User requester) {
		this.requester = requester;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	public User getDistributor() {
		return distributor;
	}

	public void setDistributor(User distributor) {
		this.distributor = distributor;
	}

	public Date getExecutionDate() {
		return executionDate;
	}

	public void setExecutionDate(Date executionDate) {
		this.executionDate = executionDate;
	}

	public Set<DistributionOrderItem> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(Set<DistributionOrderItem> orderItems) {
		this.orderItems = orderItems;
	}

	public Map<Long, DistributionOrderItem> getOrderItemsMap() {
		return getOrderItems().stream().collect(Collectors.toMap(DistributionOrderItem::getId, item -> item));
	}

	public Map<Long, DistributionOrderItem> getOrderItemsMapBySpecimenId() {
		return getOrderItems().stream().collect(Collectors.toMap(item -> item.getSpecimen().getId(), item -> item));
	}

	public Map<String, DistributionOrderItem> getOrderItemsMapBySpecimenCpAndLabel() {
		return getOrderItems().stream().collect(Collectors.toMap(
			item -> item.getSpecimen().getCollectionProtocol().getShortTitle() + "_" + item.getSpecimen().getLabel(),
			item -> item));
	}

	public Status getStatus() {
		return status;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}
	
	public String getTrackingUrl() {
		return trackingUrl;
	}

	public void setTrackingUrl(String trackingUrl) {
		this.trackingUrl = trackingUrl;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	@NotAudited
	public SpecimenRequest getRequest() {
		return request;
	}

	public void setRequest(SpecimenRequest request) {
		this.request = request;
	}

	public SpecimenList getSpecimenList() {
		return specimenList;
	}

	public void setSpecimenList(SpecimenList specimenList) {
		this.specimenList = specimenList;
	}

	public Boolean getAllReservedSpecimens() {
		return allReservedSpecimens;
	}

	public void setAllReservedSpecimens(Boolean allReservedSpecimens) {
		this.allReservedSpecimens = allReservedSpecimens;
	}

	public boolean isForAllReservedSpecimens() {
		return Boolean.TRUE.equals(allReservedSpecimens);
	}

	public Long getClearListId() {
		return clearListId;
	}

	public void setClearListId(Long clearListId) {
		this.clearListId = clearListId;
	}

	public ClearListMode getClearListMode() {
		return clearListMode;
	}

	public void setClearListMode(ClearListMode clearListMode) {
		this.clearListMode = clearListMode;
	}

	public Boolean getCheckoutSpecimens() {
		return checkoutSpecimens;
	}

	public void setCheckoutSpecimens(Boolean checkoutSpecimens) {
		this.checkoutSpecimens = checkoutSpecimens;
	}

	public Institute getInstitute() {
		return requester.getInstitute();
	}

	public void update(DistributionOrder newOrder) { // TODO: can't update executed order
		setName(newOrder.getName());
		setRequester(newOrder.getRequester());
		setDistributor(newOrder.getDistributor());
		setDistributionProtocol(newOrder.getDistributionProtocol());
		setCreationDate(newOrder.getCreationDate());
		setSite(newOrder.getSite());
		setExecutionDate(newOrder.getExecutionDate());
		setTrackingUrl(newOrder.getTrackingUrl());
		setComments(newOrder.getComments());
		setExtension(newOrder.getExtension());
		setSpecimenList(newOrder.getSpecimenList());
		setClearListId(newOrder.getClearListId());
		setClearListMode(newOrder.getClearListMode());
		setAllReservedSpecimens(newOrder.getAllReservedSpecimens());
		if (!isOrderExecuted()) {
			setCheckoutSpecimens(newOrder.getCheckoutSpecimens());
		}

		updateRequest(newOrder);
		updateOrderItems(newOrder);
	}

	public Set<DistributionOrderItem> getOrderItemsWithReqDetail() {
		Map<Long, SpecimenRequestItem> reqItemsMap = Collections.emptyMap();
		if (getRequest() != null) {
			reqItemsMap = getRequest().getSpecimenIdRequestItemMap();
		}

		for (DistributionOrderItem item : getOrderItems()) {
			item.setRequestItem(reqItemsMap.get(item.getSpecimen().getId()));
		}

		return getOrderItems();
	}

	public void distribute() {
		if (isOrderExecuted()) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.ALREADY_EXECUTED);
		}
		
		if (getSpecimenList() == null && !isForAllReservedSpecimens() && CollectionUtils.isEmpty(getOrderItems())) {
			throw OpenSpecimenException.userError(DistributionOrderErrorCode.NO_SPECIMENS_TO_DIST);
		}

		getOrderItemsWithReqDetail().forEach(DistributionOrderItem::distribute);
		setStatus(Status.EXECUTED);

		if (getRequest() != null) {
			getRequest().closeIfFulfilled();
		}
	}

	public void delete() {
		setName(Utility.getDisabledValue(getName(), 255));
		setActivityStatus(com.krishagni.catissueplus.core.common.util.Status.ACTIVITY_STATUS_DISABLED.getStatus());

		Map<Long, SpecimenRequestItem> reqItemsMap = Collections.emptyMap();
		if (getRequest() != null) {
			reqItemsMap = getRequest().getSpecimenIdRequestItemMap();
		}

		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
		DistributionOrderItemListCriteria crit = new DistributionOrderItemListCriteria().orderId(getId()).maxResults(50);
		int startAt = 0;
		boolean endOfItems = false;
		while (!endOfItems) {
			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getOrderItems(crit.startAt(startAt));
			for (DistributionOrderItem item : items) {
				item.setRequestItem(reqItemsMap.get(item.getSpecimen().getId()));
				item.undoDistribution();
			}

			endOfItems = items.size() < 50;
			startAt += items.size();
		}

		if (getRequest() != null) {
			getRequest().reopenIfNotFulfilled();
		}
	}

	public List<DistributionOrderItem> deleteItems(List<Long> itemIds) {
		Map<Long, SpecimenRequestItem> reqItemsMap = Collections.emptyMap();
		if (getRequest() != null) {
			reqItemsMap = getRequest().getSpecimenIdRequestItemMap();
		}

		DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
		DistributionOrderItemListCriteria crit = new DistributionOrderItemListCriteria().orderId(getId());
		int itemIdx = 0;
		List<DistributionOrderItem> deletedItems = new ArrayList<>();
		while (itemIdx < itemIds.size()) {
			crit.ids(itemIds.subList(itemIdx, Math.min(itemIdx + 50, itemIds.size())));
			itemIdx += 50;

			List<DistributionOrderItem> items = daoFactory.getDistributionOrderDao().getOrderItems(crit);
			for (DistributionOrderItem item : items) {
				if (isOrderExecuted()) {
					item.setRequestItem(reqItemsMap.get(item.getSpecimen().getId()));
					item.undoDistribution();
				}

				daoFactory.getDistributionOrderDao().deleteOrderItem(item);
				if (deletedItems.size() < 100) {
					deletedItems.add(item);
				}
			}
		}

		if (getRequest() != null) {
			getRequest().reopenIfNotFulfilled();
		}

		return deletedItems;
	}

	public boolean isOrderExecuted() {
		return Status.EXECUTED == status;
	}

	@Override
	public String getEntityType() {
		return getExtnEntityType();
	}

	@Override
	public boolean isCpBased() {
		return false;
	}

	@Override
	public Long getEntityId() {
		return getDistributionProtocol().getId();
	}

	@Override
	public Long getRecordId(Long formId, Long formCtxtId) {
		return daoFactory.getDistributionOrderDao().getCustomFieldRecordId(getId(), formId, formCtxtId);
	}

	@Override
	public boolean saveOrUpdateRecordEntry(boolean insert, Long formId, Long formCtxtId, Long recordId) {
		if (insert) {
			daoFactory.getDistributionOrderDao().insertCustomFieldRecordId(getId(), formId, formCtxtId, recordId);
		}

		return true;
	}

	@Override
	public Map<Long, Long> getRecordIds(Collection<Long> orderIds, Long formId, Long formCtxtId) {
		return daoFactory.getDistributionOrderDao().getCustomFieldRecordIds(orderIds, formId, formCtxtId);
	}

	private void updateRequest(DistributionOrder other) {
		if (isOrderExecuted()) {
			return;
		}

		SpecimenRequest request = other.getRequest();
		if (request != null && request.isClosed()) {
			throw OpenSpecimenException.userError(SpecimenRequestErrorCode.CLOSED, request.getId());
		}

		setRequest(request);
	}

	private void updateOrderItems(DistributionOrder other) {
		if (isOrderExecuted()) {
			/*
			 * Order items can't be modified once the order is distributed.
			 */
			return;
		}

		Map<Long, DistributionOrderItem> existingItems = getOrderItemsMapBySpecimenId();
		for (DistributionOrderItem item : other.getOrderItems()) {
			DistributionOrderItem existing = existingItems.remove(item.getSpecimen().getId());
			if (existing != null) {
				existing.setQuantity(item.getQuantity());
				existing.setCost(item.getCost());
				existing.setStatus(item.getStatus());
			} else {
				item.setOrder(this);
				getOrderItems().add(item);
			}
		}

		getOrderItems().removeAll(existingItems.values());
	}
	
	public DistributionOrderItem getItemBySpecimen(String label) {
		return getOrderItems().stream()
			.filter(item -> item.getSpecimen().getLabel().equals(label))
			.findFirst()
			.orElse(null);
	}
}
