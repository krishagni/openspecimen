package com.krishagni.catissueplus.core.common.repository;

import javax.persistence.criteria.AbstractQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Subquery;

public class SubQuery<R> extends AbstractCriteria<SubQuery<R>, R> {
	protected boolean distinct;

	protected Expression<R> selection;

	protected SubQuery(CriteriaBuilder builder, AbstractQuery<?> mainQuery, Class<?> fromClass, Class<R> returnClass, String alias) {
		this.fromClass = fromClass;
		this.builder = builder;
		query = mainQuery.subquery(returnClass);
		root = query.from(fromClass);
		rootAlias = alias;
	}

	public static <R> SubQuery<R> create(CriteriaBuilder builder, AbstractQuery<?> mainQuery, Class<R> fromClass, String alias) {
		return new SubQuery<>(builder, mainQuery, fromClass, fromClass, alias);
	}

	public static <R> SubQuery<R> create(CriteriaBuilder builder, AbstractQuery<?> mainQuery, Class<?> fromClass, Class<R> returnClass, String alias) {
		return new SubQuery<>(builder, mainQuery, fromClass, returnClass, alias);
	}

	public Subquery<R> getQuery() {
		Subquery<R> sq = (Subquery<R>) query;
		return sq.select(selection).distinct(distinct);
	}

	@Override
	public SubQuery<R> self() {
		return this;
	}

	public SubQuery<R> select(String attribute) {
		selection = getExpression(attribute);
		return this;
	}

	public SubQuery<R> distinct() {
		return distinct(true);
	}

	public SubQuery<R> distinct(boolean distinct) {
		this.distinct = distinct;
		return this;
	}
}
