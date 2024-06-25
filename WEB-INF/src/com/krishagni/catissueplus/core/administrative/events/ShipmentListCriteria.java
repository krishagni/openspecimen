package com.krishagni.catissueplus.core.administrative.events;

import java.util.Set;

import com.krishagni.catissueplus.core.administrative.domain.Shipment;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class ShipmentListCriteria extends AbstractListCriteria<ShipmentListCriteria> {
	private String name;

	private String sendingSite;
	
	private String recvInstitute;
	
	private String recvSite;

	private String labelOrBarcode;

	private Shipment.Status status;
	
	private Set<SiteCpPair> sites;
	
	@Override
	public ShipmentListCriteria self() {
		return this;
	}
	
	public String name() {
		return name;
	}
	
	public ShipmentListCriteria name(String name) {
		this.name = name;
		return self();
	}

	public String sendingSite() {
		return sendingSite;
	}

	public ShipmentListCriteria sendingSite(String sendingSite) {
		this.sendingSite = sendingSite;
		return self();
	}
	
	public String recvInstitute() {
		return recvInstitute;
	}
	
	public ShipmentListCriteria recvInstitute(String recvInstitute) {
		this.recvInstitute = recvInstitute;
		return self();
	}
	
	public String recvSite() {
		return recvSite;
	}
	
	public ShipmentListCriteria recvSite(String recvSite) {
		this.recvSite = recvSite;
		return self();
	}

	public String labelOrBarcode() {
		return labelOrBarcode;
	}

	public ShipmentListCriteria labelOrBarcode(String labelOrBarcode) {
		this.labelOrBarcode = labelOrBarcode;
		return self();
	}

	public Shipment.Status status() {
		return status;
	}

	public ShipmentListCriteria status(Shipment.Status status) {
		this.status = status;
		return self();
	}
	
	public Set<SiteCpPair> sites() {
		return sites;
	}
	
	public ShipmentListCriteria sites(Set<SiteCpPair> sites) {
		this.sites = sites;
		return self();
	}
}
