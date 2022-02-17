package com.krishagni.catissueplus.core.biospecimen.events;

import java.util.HashMap;
import java.util.Map;

public class CpQueryCriteria {
	private Long id;
	
	private String title;
	
	private String shortTitle;
	
	private boolean fullObject;

	private Map<String, Object> params = new HashMap<>();

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getShortTitle() {
		return shortTitle;
	}

	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	public boolean isFullObject() {
		return fullObject;
	}

	public void setFullObject(boolean fullObject) {
		this.fullObject = fullObject;
	}

	public void param(String name, Object value) {
		params.put(name, value);
	}

	public <T> T param(String name) {
		return (T) params.get(name);
	}
}
