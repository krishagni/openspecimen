package com.krishagni.catissueplus.core.administrative.events;

public class ContainerQueryCriteria {	
	private Long id;
	
	private String name;

	private String barcode;

	private boolean includeStats;

	private boolean errorIfNotFound = true;
	
	public ContainerQueryCriteria(Long id) {
		this.id = id;
	}
	
	public ContainerQueryCriteria(String name) {
		this.name = name;
	}

	public ContainerQueryCriteria(String name, String barcode) {
		this.name = name;
		this.barcode = barcode;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getBarcode() {
		return barcode;
	}

	public boolean includeStats() {
		return includeStats;
	}

	public ContainerQueryCriteria includeStats(boolean includeStats) {
		this.includeStats = includeStats;
		return this;
	}

	public boolean errorIfNotFound() {
		return errorIfNotFound;
	}

	public ContainerQueryCriteria errorIfNotFound(boolean errorIfNotFound) {
		this.errorIfNotFound = errorIfNotFound;
		return this;
	}
}
