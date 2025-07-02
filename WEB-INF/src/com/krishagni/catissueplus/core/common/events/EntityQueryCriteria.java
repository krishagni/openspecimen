package com.krishagni.catissueplus.core.common.events;

import java.util.HashMap;
import java.util.Map;

public class EntityQueryCriteria {
	private Long id;
	
	private String name;

	private Map<String, Object> params = new HashMap<>();
	
	public EntityQueryCriteria(Long id) {
		this.id = id;		
	}
	
	public EntityQueryCriteria(String name) {
		this.name = name;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Map<String, Object> getParams() {
		return params;
	}

	public void setParams(Map<String, Object> params) {
		this.params = params;
	}

	public Number paramNumber(String name) {
		Object value = getParamValue(name);
		return value instanceof Number ? (Number) value : null;
	}

	public Long paramLong(String name) {
		Number number = paramNumber(name);
		return number != null ? number.longValue() : null;
	}

	public String paramString(String name) {
		Object value = getParamValue(name);
		return value instanceof String ? (String) value : null;
	}

	public Boolean paramBoolean(String name) {
		Object value = getParamValue(name);
		return value instanceof Boolean ? (Boolean) value : null;
	}

	private Object getParamValue(String name) {
		if (params == null) {
			return null;
		}

		return params.get(name);
	}
}
