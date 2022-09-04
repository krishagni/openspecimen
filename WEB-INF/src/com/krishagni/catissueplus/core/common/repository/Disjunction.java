package com.krishagni.catissueplus.core.common.repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;

public class Disjunction extends Junction {

	public Disjunction(CriteriaBuilder cb) {
		super(cb);
	}

	@Override
	public Restriction getRestriction() {
		Predicate predicate = restrictions.stream()
			.map(Restriction::getPredicate)
			.reduce(cb.disjunction(), (result, p) -> cb.or(result, p));
		return Restriction.of(predicate);
	}
}
