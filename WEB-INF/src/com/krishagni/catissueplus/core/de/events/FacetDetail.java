package com.krishagni.catissueplus.core.de.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class FacetDetail {
	private String expr;

	private String caption;

	private Collection<Object> values = new ArrayList<>();

	private Integer dbRows;

	public String getExpr() {
		return expr;
	}

	public void setExpr(String expr) {
		this.expr = expr;
	}

	public String getCaption() {
		return caption;
	}

	public void setCaption(String caption) {
		this.caption = caption;
	}

	public Collection<Object> getValues() {
		return values;
	}

	public void setValues(Collection<Object> values) {
		this.values = values;
	}

	public Integer getDbRows() {
		return dbRows;
	}

	public void setDbRows(Integer dbRows) {
		this.dbRows = dbRows;
	}
}
