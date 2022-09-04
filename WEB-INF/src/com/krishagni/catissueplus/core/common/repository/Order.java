package com.krishagni.catissueplus.core.common.repository;

public class Order {
	private javax.persistence.criteria.Order order;

	public static Order of(javax.persistence.criteria.Order order) {
		Order result = new Order();
		result.setOrder(order);
		return result;
	}

	public javax.persistence.criteria.Order getOrder() {
		return order;
	}

	public void setOrder(javax.persistence.criteria.Order order) {
		this.order = order;
	}
}
