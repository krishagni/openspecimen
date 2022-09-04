package com.krishagni.catissueplus.core.common.repository;

public class PropertyBuilder {
	private AbstractCriteria<?, ?> query;

	public PropertyBuilder(AbstractCriteria<?, ?> query) {
		this.query = query;
	}

	public Property property(String attribute) {
		return query.getProperty(attribute);
	}
}
