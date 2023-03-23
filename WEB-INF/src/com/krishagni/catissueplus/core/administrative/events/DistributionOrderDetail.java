package com.krishagni.catissueplus.core.administrative.events;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.DistributionOrder;
import com.krishagni.catissueplus.core.biospecimen.events.SpecimenListSummary;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

public class DistributionOrderDetail extends DistributionOrderSummary implements Mergeable<String, DistributionOrderDetail>, Serializable {
	private UserSummary distributor;
	
	private String trackingUrl;
	
	private String comments;

	private SpecimenRequestSummary request;

	private SpecimenListSummary specimenList;

	private Boolean allReservedSpmns;

	private Long clearListId;

	private String clearListMode;

	private List<DistributionOrderItemDetail> orderItems = new ArrayList<>();
	
	private String activityStatus;

	private Map<String, Object> extraAttrs;

	private ExtensionDetail extensionDetail;

	//
	// For BO template
	//
	private DistributionOrderItemDetail orderItem;

	//
	// for async execution
	//
	private boolean async;

	private boolean completed = true;

	private boolean copyItemsFromExistingOrder;

	private boolean distributeAvailableQty;

	private boolean checkout;

	public UserSummary getDistributor() {
		return distributor;
	}

	public void setDistributor(UserSummary distributor) {
		this.distributor = distributor;
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

	public SpecimenRequestSummary getRequest() {
		return request;
	}

	public void setRequest(SpecimenRequestSummary request) {
		this.request = request;
	}

	public SpecimenListSummary getSpecimenList() {
		return specimenList;
	}

	public void setSpecimenList(SpecimenListSummary specimenList) {
		this.specimenList = specimenList;
	}

	public Boolean getAllReservedSpmns() {
		return allReservedSpmns;
	}

	public void setAllReservedSpmns(Boolean allReservedSpmns) {
		this.allReservedSpmns = allReservedSpmns;
	}

	public Long getClearListId() {
		return clearListId;
	}

	public void setClearListId(Long clearListId) {
		this.clearListId = clearListId;
	}

	public String getClearListMode() {
		return clearListMode;
	}

	public void setClearListMode(String clearListMode) {
		this.clearListMode = clearListMode;
	}

	public List<DistributionOrderItemDetail> getOrderItems() {
		return orderItems;
	}

	public void setOrderItems(List<DistributionOrderItemDetail> orderItems) {
		this.orderItems = orderItems;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public Map<String, Object> getExtraAttrs() {
		return extraAttrs;
	}

	public void setExtraAttrs(Map<String, Object> extraAttrs) {
		this.extraAttrs = extraAttrs;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public DistributionOrderItemDetail getOrderItem() {
		return orderItem;
	}

	public void setOrderItem(DistributionOrderItemDetail orderItem) {
		this.orderItem = orderItem;
	}

	public boolean isAsync() {
		return async;
	}

	public void setAsync(boolean async) {
		this.async = async;
	}

	public boolean isCompleted() {
		return completed;
	}

	public void setCompleted(boolean completed) {
		this.completed = completed;
	}

	public boolean isCopyItemsFromExistingOrder() {
		return copyItemsFromExistingOrder;
	}

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	public void setCopyItemsFromExistingOrder(boolean copyItemsFromExistingOrder) {
		this.copyItemsFromExistingOrder = copyItemsFromExistingOrder;
	}

	public boolean isDistributeAvailableQty() {
		return distributeAvailableQty;
	}

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	public void setDistributeAvailableQty(boolean distributeAvailableQty) {
		this.distributeAvailableQty = distributeAvailableQty;
	}

	public boolean isCheckout() {
		return checkout;
	}

	public void setCheckout(boolean checkout) {
		this.checkout = checkout;
	}

	public static DistributionOrderDetail from(DistributionOrder order) {
		return from(order, false);
	}

	public static DistributionOrderDetail from(DistributionOrder order, boolean includeOrderItems) {
		DistributionOrderDetail detail = new DistributionOrderDetail();
		fromTo(order, detail);

		if (order.getDistributor() != null ) {
			detail.setDistributor(UserSummary.from(order.getDistributor()));
		}

		if (order.getRequest() != null) {
			detail.setRequest(SpecimenRequestSummary.from(order.getRequest()));
		}

		if (order.getSpecimenList() != null) {
			detail.setSpecimenList(SpecimenListSummary.fromSpecimenList(order.getSpecimenList()));
		}

		detail.setClearListId(order.getClearListId());
		detail.setClearListMode(order.getClearListMode() != null ? order.getClearListMode().name() : null);
		detail.setAllReservedSpmns(order.getAllReservedSpecimens());
		detail.setTrackingUrl(order.getTrackingUrl());
		detail.setComments(order.getComments());
		detail.setExtensionDetail(ExtensionDetail.from(order.getExtension()));

		if (includeOrderItems) {
			detail.setOrderItems(DistributionOrderItemDetail.from(order.getOrderItems()));
		}

		detail.setCheckout(Boolean.TRUE.equals(order.getCheckoutSpecimens()));
		detail.setActivityStatus(order.getActivityStatus());
		return detail;
	}
	
	public static List<DistributionOrderDetail> from(List<DistributionOrder> orders) {
		return orders.stream().map(DistributionOrderDetail::from).collect(Collectors.toList());
	}

	@Override
	@JsonIgnore
	public String getMergeKey() {
		return getName();
	}

	@Override
	public void merge(DistributionOrderDetail other) {
		getOrderItems().add(other.getOrderItem());
	}
}
