package com.krishagni.catissueplus.core.common.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Selection;

import org.hibernate.Session;

public class Criteria<R> extends AbstractCriteria<Criteria<R>, R> {
	protected boolean distinct;

	protected List<Selection<?>> selections = new ArrayList<>();

	protected Criteria() {

	}

	protected Criteria(Session session, Class<?> fromClass, Class<R> returnClass, String alias) {
		this.session = session;
		this.fromClass = fromClass;

		builder = session.getCriteriaBuilder();
		query = builder.createQuery(returnClass);
		root = query.from(fromClass);
		rootAlias = alias;
	}

	public static <R> Criteria<R> create(Session session, Class<R> fromClass, String alias) {
		return new Criteria<>(session, fromClass, fromClass, alias);
	}

	public static <R> Criteria<R> create(Session session, Class<?> fromClass, Class<R> returnClass, String alias) {
		return new Criteria<>(session, fromClass, returnClass, alias);
	}

	@Override
	public Criteria<R> self() {
		return this;
	}

	public Criteria<R> select(String... attributes) {
		selections = Arrays.stream(attributes).map(this::getExpression).collect(Collectors.toList());
		return this;
	}

	public Criteria<R> select(Property... properties) {
		selections = Arrays.stream(properties).map(Property::getExpr).collect(Collectors.toList());
		return this;
	}

	public Criteria<R> select(List<Property> selections) {
		this.selections = selections.stream().map(Property::getExpr).collect(Collectors.toList());
		return this;
	}

	public Property column(String attribute) {
		return Property.of(getExpression(attribute));
	}

	public Property count(String attribute) {
		return Property.of(builder.count(getExpression(attribute)));
	}

	public Property distinctCount(String attribute) {
		return Property.of(builder.countDistinct(getExpression(attribute)));
	}

	public Criteria<R> distinct() {
		return distinct(true);
	}

	public Criteria<R> distinct(boolean distinct) {
		this.distinct = distinct;
		return this;
	}

	public List<R> list() {
		return list(-1, -1);
	}

	public List<R> list(int startAt, int maxResults) {
		CriteriaQuery<R> cq = (CriteriaQuery<R>) query;
		cq.distinct(distinct);
		if (selections == null || selections.isEmpty()) {
			cq.select((Selection<? extends R>) root);
		} else {
			cq.multiselect(selections);
		}

		if (startAt >= 0 && maxResults >= 0) {
			return session.createQuery(cq).setFirstResult(startAt).setMaxResults(maxResults).list();
		} else {
			return session.createQuery(cq).list();
		}
	}

	public R uniqueResult() {
		CriteriaQuery<R> cq = (CriteriaQuery<R>) query;
		cq.distinct(distinct);
		if (selections == null || selections.isEmpty()) {
			cq.select((Selection<? extends R>) root);
		} else {
			cq.multiselect(selections);
		}

		return session.createQuery(cq).uniqueResult();
	}

	public Long getCount(String attribute) {
		CriteriaQuery<R> cq = (CriteriaQuery<R>) query;
		cq.select((Expression<R>) builder.count(getExpression(attribute)));
		return (Long) session.createQuery(cq).getSingleResult();
	}

	public Criteria<R> orderBy(Order... orderList) {
		CriteriaQuery<R> cq = (CriteriaQuery<R>) query;
		cq.orderBy(Arrays.stream(orderList).map(Order::getOrder).collect(Collectors.toList()));
		return this;
	}

	public Criteria<R> addOrder(Order order) {
		CriteriaQuery<R> cq = (CriteriaQuery<R>) query;
		List<javax.persistence.criteria.Order> orderList = cq.getOrderList();
		if (orderList == null || orderList.isEmpty()) {
			orderList = new ArrayList<>();
			cq.orderBy(orderList);
		}

		orderList.add(order.getOrder());
		return this;
	}
}
