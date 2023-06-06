package com.krishagni.catissueplus.core.common.repository;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.Predicate;

public class Conjunction extends Junction {

	public Conjunction(CriteriaBuilder cb) {
		super(cb);
	}

	@Override
	public Restriction getRestriction() {
		return Restriction.of(cb.and(restrictions.stream().map(Restriction::getPredicate).toArray(Predicate[]::new)));
//		Predicate predicate = restrictions.stream()
//			.map(Restriction::getPredicate)
//			.reduce(cb.conjunction(), (result, p) -> cb.and(result, p));
//		return Restriction.of(predicate);
	}
}
