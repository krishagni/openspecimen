package com.krishagni.catissueplus.core.common.repository;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.From;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.MapJoin;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;

public abstract class AbstractCriteria<T extends AbstractCriteria<T, R>, R> {
	public enum JoinType {
		RIGHT_JOIN,
		LEFT_JOIN,
		INNER_JOIN,

		LEFT_JOIN_MAP,

		INNER_JOIN_MAP
	}

	protected Session session;

	protected CriteriaBuilder builder;

	protected AbstractQuery<R> query;

	protected Class<?> fromClass;

	protected Root<?> root;

	protected String rootAlias;

	protected Map<String, Join> joins = new HashMap<>();

	public abstract T self();

	public Class<?> fromClass() {
		return fromClass;
	}

	public T join(String attribute, String alias) {
		return createAlias(attribute, alias);
	}

	public T joinMap(String attribute, String alias) {
		return createAlias(attribute, alias, JoinType.INNER_JOIN_MAP);
	}

	public T leftJoin(String attribute, String alias) {
		return createAlias(attribute, alias, JoinType.LEFT_JOIN);
	}

	public T leftJoin(String attribute, String alias, Supplier<Restriction> restriction) {
		return createAlias(attribute, alias, JoinType.LEFT_JOIN, restriction);
	}

	public T leftJoinMap(String attribute, String alias) {
		return createAlias(attribute, alias, JoinType.LEFT_JOIN_MAP);
	}

	public T leftJoinMap(String attribute, String alias, Supplier<Restriction> restriction) {
		return createAlias(attribute, alias, JoinType.LEFT_JOIN_MAP, restriction);
	}

	public T createAlias(String attribute, String alias) {
		return createAlias(attribute, alias, Criteria.JoinType.INNER_JOIN);
	}

	public T createAlias(String attribute, String alias, Criteria.JoinType joinType) {
		return createAlias(attribute, alias, joinType, null);
	}

	public T createAlias(String attribute, String alias, Criteria.JoinType joinType, Supplier<Restriction> restriction) {
		if (StringUtils.isBlank(attribute)) {
			throw new CriteriaException("Attribute is null or empty");
		}

		String[] parts = attribute.split("\\.");
		if (parts.length > 2) {
			throw new CriteriaException("Attribute path length cannot be more than 2 : " + attribute);
		}

		String joinWith = parts.length == 1 ? parts[0] : parts[1];
		From joinFrom = parts.length == 1 ? root : StringUtils.equals(parts[0], rootAlias) ? root :  joins.get(parts[0]);
		if (joinFrom == null) {
			throw new CriteriaException("Unknown join path: " + attribute);
		}

		switch (joinType) {
			case LEFT_JOIN -> joins.put(alias, joinFrom.join(joinWith, javax.persistence.criteria.JoinType.LEFT));
			case RIGHT_JOIN -> joins.put(alias, joinFrom.join(joinWith, javax.persistence.criteria.JoinType.RIGHT));
			case INNER_JOIN_MAP -> joins.put(alias, joinFrom.joinMap(joinWith));
			case LEFT_JOIN_MAP -> joins.put(alias, joinFrom.joinMap(joinWith, javax.persistence.criteria.JoinType.LEFT));
			default -> joins.put(alias, joinFrom.join(joinWith));
		}

		if (restriction != null) {
			joins.get(alias).on(restriction.get().getPredicate());
		}

		return self();
	}

	public boolean hasJoin(String alias) {
		return joins.containsKey(alias);
	}

	public boolean hasAlias(String alias) {
		return joins.containsKey(alias) || alias.equals(rootAlias);
	}

	public Expression<String> upper(String attribute) {
		return builder.upper(getExpression(attribute));
	}

	public Expression<String> lower(String attribute) {
		return builder.lower(getExpression(attribute));
	}

	public <T> Expression<T> key(String alias) {
		return (Expression<T>) getMapJoin(alias).key();
	}

	public <T> Expression<T> value(String alias) {
		return (Expression<T>) getMapJoin(alias).value();
	}

	public T add(Restriction restriction) {
		Predicate predicate = query.getRestriction();
		if (predicate == null) {
			query.where(restriction.getPredicate());
		} else {
			query.where(builder.and(predicate, restriction.getPredicate()));
		}

		return self();
	}

	public T add(Junction junction) {
		return add(junction.getRestriction());
	}

	public Restriction or(Restriction... restrictions) {
		Junction junction = disjunction();
		for (Restriction restriction : restrictions) {
			junction.add(restriction);
		}

		return junction.getRestriction();
	}

	public Restriction and(Restriction... restrictions) {
		Junction junction = conjunction();
		for (Restriction restriction : restrictions) {
			junction.add(restriction);
		}

		return junction.getRestriction();
	}

	public Restriction isNull(String attribute) {
		return Restriction.of(builder.isNull(getExpression(attribute)));
	}

	public Restriction isNotNull(String attribute) {
		return Restriction.of(builder.isNotNull(getExpression(attribute)));
	}

	public Restriction isEmpty(String attribute) {
		return Restriction.of(builder.isEmpty(getExpression(attribute)));
	}

	public Restriction isNotEmpty(String attribute) {
		return Restriction.of(builder.isNotEmpty(getExpression(attribute)));
	}

	public Restriction exists(SubQuery<?> subQuery) {
		return Restriction.of(builder.exists(subQuery.getQuery()));
	}

	public Restriction in(String attribute, Collection<?> values) {
		return Restriction.of(getExpression(attribute).in(values));
	}

	public Restriction in(String attribute, Object... values) {
		return Restriction.of(getExpression(attribute).in(values));
	}

	public Restriction in(String attribute, SubQuery<?> subQuery) {
		return Restriction.of(getExpression(attribute).in(subQuery.getQuery()));
	}

	public Restriction in(Expression<?> expr, Collection<?> values) {
		return Restriction.of(expr.in(values));
	}

	public Restriction notIn(String attribute, Collection<?> values) {
		return not(in(attribute, values));
	}

	public Restriction notIn(String attribute, Object... values) {
		return not(in(attribute, values));
	}

	public Restriction notIn(String attribute, SubQuery<?> subQuery) {
		return not(in(attribute, subQuery));
	}

	public <V> Restriction valueIn(V value, SubQuery<V> subQuery) {
		return Restriction.of(subQuery.getQuery().in(value));
	}

	public Restriction not(Restriction restriction) {
		return Restriction.of(builder.not(restriction.getPredicate()));
	}

	public <Y extends Comparable<? super Y>> Restriction gt(String attribute, Y value) {
		return Restriction.of(builder.greaterThan(getExpression(attribute), value));
	}

	public <Y extends Comparable<? super Y>> Restriction ge(String attribute, Y value) {
		return Restriction.of(builder.greaterThanOrEqualTo(getExpression(attribute), value));
	}

	public <Y extends Comparable<? super Y>> Restriction lt(String attribute, Y value) {
		return Restriction.of(builder.lessThan(getExpression(attribute), value));
	}

	public Restriction ltExpr(String attribute1, String attribute2) {
		return Restriction.of(builder.lessThan(getExpression(attribute1), getExpression(attribute2)));
	}

	public <Y extends Comparable<? super Y>> Restriction le(String attribute, Y value) {
		return Restriction.of(builder.lessThanOrEqualTo(getExpression(attribute), value));
	}

	public <Y extends Comparable<? super Y>> Restriction between(String attribute, Y start, Y end) {
		return Restriction.of(builder.between(getExpression(attribute), start, end));
	}

	public Restriction eq(String attribute, Object value) {
		return Restriction.of(builder.equal(getExpression(attribute), value));
	}

	public Restriction eq(Expression<?> expr, Object value) {
		return Restriction.of(builder.equal(expr, value));
	}

	public Restriction eq(Property prop1, Property prop2) {
		return Restriction.of(builder.equal(prop1.getExpr(), prop2.getExpr()));
	}

	public Restriction eqExpr(String attribute1, String attribute2) {
		return Restriction.of(builder.equal(getExpression(attribute1), getExpression(attribute2)));
	}

	public Restriction ne(String attribute, Object value) {
		return Restriction.of(builder.notEqual(getExpression(attribute), value));
	}

	public Restriction neExpr(String attribute1, String attribute2) {
		return Restriction.of(builder.notEqual(getExpression(attribute1), getExpression(attribute2)));
	}

	public Restriction like(String attribute, String pattern) {
		return like(attribute, pattern, true);
	}

	public Restriction like(String attribute, String pattern, boolean addWildCards) {
		if (addWildCards) {
			pattern = "%" + pattern + "%";
		}

		return Restriction.of(builder.like(getExpression(attribute), pattern));
	}

	public Restriction ilike(String attribute, String pattern) {
		return ilike(getExpression(attribute), pattern);
	}

	public Restriction ilike(Expression<String> expression, String pattern) {
		return ilike(expression, pattern, true);
	}

	public Restriction ilike(Expression<String> expression, String pattern, boolean addWildCards) {
		if (addWildCards) {
			pattern = "%" + pattern + "%";
		}

		return Restriction.of(builder.like(builder.lower(expression), pattern.toLowerCase()));
	}

	public Disjunction disjunction() {
		return new Disjunction(builder);
	}

	public Conjunction conjunction() {
		return new Conjunction(builder);
	}

	public <Z> SubQuery<Z> createSubQuery(Class<?> fromClass, Class<Z> returnClass, String alias) {
		return SubQuery.create(builder, query, fromClass, returnClass, alias);
	}

	public SubQuery<Long> createSubQuery(Class<?> fromClass, String alias) {
		return createSubQuery(fromClass, Long.class, alias);
	}

	public Expression<String> concat(Expression<String> expr, String value) {
		return builder.concat(expr, value);
	}

	public Expression<String> concat(String value, Expression<String> expr) {
		return builder.concat(value, expr);
	}

	public Expression<String> concat(Expression<String> expr1, Expression<String> expr2) {
		return builder.concat(expr1, expr2);
	}

	public Property locate(String attribute, String value) {
		return Property.of(builder.locate(getExpression(attribute), value));
	}

	public Property ilocate(String attribute, String value) {
		return Property.of(builder.locate(builder.lower(getExpression(attribute)), value.toLowerCase()));
	}

	public Property function(String name, Class<?> returnType, Property... args) {
		Expression<?>[] exprArgs = Arrays.stream(args).map(Property::getExpr).toArray(Expression<?>[]::new);
		return Property.of(builder.function(name, returnType, exprArgs));
	}

	public Property max(String attribute) {
		return Property.of(builder.max(getExpression(attribute)));
	}

	public Order asc(String attribute) {
		return Order.of(builder.asc(getExpression(attribute)));
	}

	public Order asc(Restriction cond, String whenTrue, String otherwise) {
		return Order.of(
			builder.asc(
				builder.selectCase(cond.getPredicate())
					.when(true, getExpression(whenTrue))
					.otherwise(getExpression(otherwise))
			)
		);
	}

	public Order asc(Property property) {
		return Order.of(builder.asc(property.getExpr()));
	}

	public Order desc(String attribute) {
		return Order.of(builder.desc(getExpression(attribute)));
	}

	public Order desc(Restriction cond, String whenTrue, String otherwise) {
		return Order.of(
			builder.desc(
				builder.selectCase()
					.when(cond.getPredicate(), getExpression(whenTrue))
					.otherwise(getExpression(otherwise))
			)
//				builder.selectCase(cond.getPredicate())
//					.when(true, getExpression(whenTrue))
//					.otherwise(getExpression(otherwise))
//			)
		);
	}

	public Order desc(Property property) {
		return Order.of(builder.desc(property.getExpr()));
	}

	public T groupBy(String attribute) {
		query.groupBy(Collections.singletonList(getExpression(attribute)));
		return self();
	}

	public T groupBy(Property prop) {
		query.groupBy(Collections.singletonList(prop.getExpr()));
		return self();
	}

	public T groupBy(List<Property> grouping) {
		query.groupBy(grouping.stream().map(Property::getExpr).collect(Collectors.toList()));
		return self();
	}

	public PropertyBuilder propertyBuilder() {
		return new PropertyBuilder(this);
	}

	public Property getProperty(String name) {
		return Property.of(getExpression(name));
	}

	public Property literal(String literal) {
		return Property.of(builder.literal(literal));
	}

	public <X> Expression<X> getExpression(String attribute) {
		if (StringUtils.isBlank(attribute)) {
			throw new CriteriaException("Attribute is null or empty");
		}

		String[] parts = attribute.split("\\.");
		if (parts.length > 2) {
			throw new CriteriaException("Attribute path length cannot be more than 2 : " + attribute);
		}

		if (parts.length == 1) {
			return root.get(parts[0]);
		} else {
			From<?, ?> from;
			if (parts[0].equals(rootAlias)) {
				from = root;
			} else {
				from = joins.get(parts[0]);
			}

			if (from == null) {
				throw new CriteriaException("Unknown attribute: " + attribute);
			}

			if (from instanceof MapJoin<?,?,?>) {
				MapJoin<?, ?, ?> mapJoin = (MapJoin<?, ?, ?>) from;
				if (parts[1].equals("indices")) {
					return (Expression<X>) mapJoin.key();
				} else if (parts[1].equals("elements")) {
					return (Expression<X>) mapJoin.value();
				}

				// eventually will result in an error
			}

			return from.get(parts[1]);
		}
	}

	private MapJoin<?, ?, ?> getMapJoin(String alias) {
		Join<?, ?> join = joins.get(alias);
		if (join == null) {
			throw new CriteriaException("Unknown alias: " + alias);
		}

		if (!(join instanceof MapJoin<?,?,?>)) {
			throw new CriteriaException("Alias " + alias + " is not of map join type.");
		}

		return (MapJoin<?,?,?>) join;
	}
}
