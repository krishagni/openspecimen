package com.krishagni.catissueplus.core.common.repository;

public class Order {
	private jakarta.persistence.criteria.Order order;

	public static Order of(jakarta.persistence.criteria.Order order) {
		Order result = new Order();
		result.setOrder(order);
		return result;
	}

	public jakarta.persistence.criteria.Order getOrder() {
		return order;
	}

	public void setOrder(jakarta.persistence.criteria.Order order) {
		this.order = order;
	}
}
