package com.krishagni.catissueplus.core.common.repository;

import javax.persistence.criteria.Predicate;

public class Restriction {
	private Predicate predicate;

	private Restriction(Predicate predicate) {
		this.predicate = predicate;
	}

	public static Restriction of(Predicate predicate) {
		return new Restriction(predicate);
	}

	public Predicate getPredicate() {
		return predicate;
	}

	public void setPredicate(Predicate predicate) {
		this.predicate = predicate;
	}
}
