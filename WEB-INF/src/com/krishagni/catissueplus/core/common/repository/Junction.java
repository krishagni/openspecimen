package com.krishagni.catissueplus.core.common.repository;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;

public abstract class Junction {
	protected CriteriaBuilder cb;

	protected List<Restriction> restrictions = new ArrayList<>();

	public Junction(CriteriaBuilder cb) {
		this.cb = cb;
	}

	public Junction add(Restriction restriction) {
		restrictions.add(restriction);
		return this;
	}

	public Junction add(Junction junction) {
		restrictions.add(junction.getRestriction());
		return this;
	}

	public boolean hasConditions() {
		return !restrictions.isEmpty();
	}

	public abstract Restriction getRestriction();
}
