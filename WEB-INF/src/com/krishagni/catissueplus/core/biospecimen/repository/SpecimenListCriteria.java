package com.krishagni.catissueplus.core.biospecimen.repository;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class SpecimenListCriteria extends AbstractListCriteria<SpecimenListCriteria> {
	private Long cpId;

	private String cpShortTitle;

	private String[] lineages;

	private String[] collectionStatuses;

	private List<SiteCpPair> siteCps;

	private List<String> labels;

	private List<String> barcodes;
	
	private Long specimenListId;
	
	private boolean useMrnSites;

	private String storageLocationSite;

	private Long cprId;

	private String ppid;

	private Long visitId;

	private List<String> visitNames;

	private Long ancestorId;

	private String anatomicSite;

	private String type;

	private String container;

	private Long containerId;

	private Long ancestorContainerId;

	private boolean available;

	private boolean noQty;

	private Long reservedForDp;

	private boolean minimalInfo;

	private boolean includeOnlyTbr;

	@Override
	public SpecimenListCriteria self() {		
		return this;
	} 

	public Long cpId() {
		return cpId;
	}

	@JsonProperty("cpId")
	public SpecimenListCriteria cpId(Long cpId) {
		this.cpId = cpId;
		return self();
	}

	public String cpShortTitle() {
		return cpShortTitle;
	}

	@JsonProperty("cpShortTitle")
	public SpecimenListCriteria cpShortTitle(String cpShortTitle) {
		this.cpShortTitle = cpShortTitle;
		return self();
	}

	public String[] lineages() {
		return lineages;
	}

	@JsonProperty("lineages")
	@JsonAlias({"lineage"})
	public SpecimenListCriteria lineages(String[] lineages) {
		this.lineages = lineages;
		return self();
	}

	public String[] collectionStatuses() {
		return collectionStatuses;
	}

	@JsonProperty("collectionStatuses")
	@JsonAlias({"collectionStatus"})
	public SpecimenListCriteria collectionStatuses(String[] collectionStatuses) {
		this.collectionStatuses = collectionStatuses;
		return self();
	}

	public List<SiteCpPair> siteCps() {
		return siteCps;
	}
	
	public SpecimenListCriteria siteCps(List<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public List<String> labels() {
		return labels;
	}

	@JsonProperty("labels")
	@JsonAlias({"label"})
	public SpecimenListCriteria labels(List<String> labels) {
		this.labels = labels;
		return self();
	}

	public List<String> barcodes() {
		return barcodes;
	}

	@JsonProperty("barcodes")
	@JsonAlias({"barcode"})
	public SpecimenListCriteria barcodes(List<String> barcodes) {
		this.barcodes = barcodes;
		return self();
	}

	public Long specimenListId() {
		return specimenListId;
	}

	@JsonProperty("listId")
	@JsonAlias({"specimenListId"})
	public SpecimenListCriteria specimenListId(Long specimenListId) {
		this.specimenListId = specimenListId;
		return self();
	}
	
	public boolean useMrnSites() {
		return this.useMrnSites;
	}
	
	public SpecimenListCriteria useMrnSites(boolean useMrnSites) {
		this.useMrnSites = useMrnSites;
		return self();
	}

	public String storageLocationSite() {
		return storageLocationSite;
	}

	@JsonProperty("storageLocationSite")
	@JsonAlias({"locationSite"})
	public SpecimenListCriteria storageLocationSite(String storageLocationSite) {
		this.storageLocationSite = storageLocationSite;
		return self();
	}

	public Long cprId() {
		return cprId;
	}

	@JsonProperty("cprId")
	public SpecimenListCriteria cprId(Long cprId) {
		this.cprId = cprId;
		return self();
	}

	public String ppid() {
		return ppid;
	}

	@JsonProperty("ppid")
	public SpecimenListCriteria ppid(String ppid) {
		this.ppid = ppid;
		return self();
	}

	public Long visitId() {
		return visitId;
	}

	@JsonProperty("visitId")
	public SpecimenListCriteria visitId(Long visitId) {
		this.visitId = visitId;
		return self();
	}

	public List<String> visitNames() {
		return visitNames;
	}

	@JsonProperty("visitNames")
	public SpecimenListCriteria visitNames(List<String> visitNames) {
		this.visitNames = visitNames;
		return self();
	}

	public Long ancestorId() {
		return ancestorId;
	}

	@JsonProperty("ancestorId")
	public SpecimenListCriteria ancestorId(Long ancestorId) {
		this.ancestorId = ancestorId;
		return self();
	}

	public String anatomicSite() {
		return anatomicSite;
	}

	@JsonProperty("anatomicSite")
	public SpecimenListCriteria anatomicSite(String anatomicSite) {
		this.anatomicSite = anatomicSite;
		return self();
	}

	public String type() {
		return type;
	}

	@JsonProperty("type")
	public SpecimenListCriteria type(String type) {
		this.type = type;
		return self();
	}

	public String container() {
		return container;
	}

	@JsonProperty("container")
	public SpecimenListCriteria container(String container) {
		this.container = container;
		return self();
	}

	public Long containerId() {
		return containerId;
	}

	@JsonProperty("containerId")
	public SpecimenListCriteria containerId(Long containerId) {
		this.containerId = containerId;
		return self();
	}

	public Long ancestorContainerId() {
		return ancestorContainerId;
	}

	@JsonProperty("ancestorContainerId")
	public SpecimenListCriteria ancestorContainerId(Long ancestorContainerId) {
		this.ancestorContainerId = ancestorContainerId;
		return self();
	}

	public boolean available() {
		return this.available;
	}

	@JsonProperty("available")
	public SpecimenListCriteria available(boolean available) {
		this.available = available;
		return self();
	}

	public boolean noQty() {
		return this.noQty;
	}

	@JsonProperty("noQty")
	public SpecimenListCriteria noQty(boolean noQty) {
		this.noQty = noQty;
		return self();
	}

	public Long reservedForDp() {
		return reservedForDp;
	}

	@JsonProperty("reservedForDp")
	public SpecimenListCriteria reservedForDp(Long reservedForDp) {
		this.reservedForDp = reservedForDp;
		return self();
	}

	public boolean minimalInfo() {
		return minimalInfo;
	}

	@JsonProperty("minimalInfo")
	public SpecimenListCriteria minimalInfo(boolean minimalInfo) {
		this.minimalInfo = minimalInfo;
		return self();
	}

	public boolean includeOnlyTbr() {
		return includeOnlyTbr;
	}

	@JsonProperty("includeOnlyTbr")
	public SpecimenListCriteria includeOnlyTbr(boolean includeOnlyTbr) {
		this.includeOnlyTbr = includeOnlyTbr;
		return self();
	}
}