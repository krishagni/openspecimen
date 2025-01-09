package com.krishagni.catissueplus.core.administrative.repository;

import java.util.List;

import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;


public class SiteListCriteria extends AbstractListCriteria<SiteListCriteria> {

	private List<String> names;

	private String resource;
	
	private String operation;

	private String institute;

	private boolean listAll;

	private List<String> includeTypes;

	private List<String> excludeTypes;
	
	@Override
	public SiteListCriteria self() {
		return this;
	}

	public List<String> names() {
		return names;
	}

	public SiteListCriteria names(List<String> names) {
		this.names = names;
		return self();
	}
	
	public String resource() {
		return resource;
	}
	
	public SiteListCriteria resource(String resource) {
		this.resource = resource;
		return self();
	}
	
	public String operation() {
		return operation;
	}
	
	public SiteListCriteria operation(String operation) {
		this.operation = operation;
		return self();
	}

	public String institute() {
		return institute;
	}

	public SiteListCriteria institute(String institute) {
		this.institute = institute;
		return self();
	}

	public boolean listAll() {
		return listAll;
	}
	
	public SiteListCriteria listAll(boolean listAll) {
		this.listAll = listAll;
		return self();
	}

	public List<String> includeTypes() {
		return includeTypes;
	}

	public SiteListCriteria includeTypes(List<String> includeTypes) {
		this.includeTypes = includeTypes;
		return self();
	}

	public List<String> excludeTypes() {
		return excludeTypes;
	}

	public SiteListCriteria excludeTypes(List<String> excludeTypes) {
		this.excludeTypes = excludeTypes;
		return self();
	}
}
