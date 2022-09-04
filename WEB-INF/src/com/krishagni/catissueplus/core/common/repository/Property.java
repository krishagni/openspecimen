package com.krishagni.catissueplus.core.common.repository;

import javax.persistence.criteria.Expression;

public class Property {
	private Expression<?> expr;

	public static Property of(Expression<?> selection) {
		Property element = new Property();
		element.setExpr(selection);
		return element;
	}

	public Expression<?> getExpr() {
		return expr;
	}

	public void setExpr(Expression<?> expr) {
		this.expr = expr;
	}
}
