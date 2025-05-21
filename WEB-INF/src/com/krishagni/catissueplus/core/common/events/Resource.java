package com.krishagni.catissueplus.core.common.events;

import org.apache.commons.lang3.StringUtils;

public enum Resource {
	CP("CollectionProtocol"),
	
	PARTICIPANT("ParticipantPhi"),
	
	PARTICIPANT_DEID("ParticipantDeid"),

	CONSENT("Consent"),

	VISIT("Visit"),
	
	SPECIMEN("Specimen"),

	PRIMARY_SPECIMEN("PrimarySpecimen"),
	
	SURGICAL_PATHOLOGY_REPORT("SurgicalPathologyReport"),
	
	STORAGE_CONTAINER("StorageContainer"),
	
	USER("User"),
	
	ORDER("Order"),
	
	DP("DistributionProtocol"),

	SCHEDULED_JOB("ScheduledJob"),
	
	SHIPPING_N_TRACKING("ShippingAndTracking"),

	QUERY("Query"),

	PRINT_RULE("PrintRule")

	;

	private final String name;
	
	private Resource(String name) {
		this.name = name;
	}
	
	public String getName() { 
		return name;
	}
	
	public static Resource fromName(String name) {
		if (StringUtils.isBlank(name)) {
			return null;
		}
		
		Resource result = null;
		for (Resource r : Resource.values()) {
			if (r.name.equalsIgnoreCase(name)) {
				result =  r;
				break;
			}
		}
		
		return result;
	}
	
}
