package com.krishagni.catissueplus.core.administrative.repository;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.common.access.SiteCpPair;
import com.krishagni.catissueplus.core.common.errors.CommonErrorCode;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.events.AbstractListCriteria;

public class StorageContainerListCriteria extends AbstractListCriteria<StorageContainerListCriteria> {
	public enum Status {
		AVAILABLE,

		CHECKED_OUT,

		ARCHIVED
	}

	private List<String> names;

	private List<String> barcodes;

	private boolean onlyFreeContainers;
	
	private Long parentContainerId;
	
	private String siteName;

	private String canHold;
	
	private boolean includeChildren;
	
	private boolean topLevelContainers;
	
	private Boolean storeSpecimensEnabled;
	
	private String specimenClass;
	
	private String specimenType;
	
	private boolean hierarchical;
	
	private Set<Long> cpIds;
	
	private Set<String> cpShortTitles;

	private Set<String> dpShortTitles;

	private Set<SiteCpPair> siteCps;

	private StorageContainer.UsageMode usageMode;

	private List<Status> statuses;

	private Set<String> types;


	@Override
	public StorageContainerListCriteria self() {
		return this;
	}

	public List<String> names() {
		return names;
	}

	public StorageContainerListCriteria names(List<String> names) {
		this.names = names;
		return self();
	}

	public List<String> barcodes() {
		return barcodes;
	}

	public StorageContainerListCriteria barcodes(List<String> barcodes) {
		this.barcodes = barcodes;
		return self();
	}

	public boolean onlyFreeContainers() {
		return onlyFreeContainers;
	}
	
	public StorageContainerListCriteria onlyFreeContainers(boolean onlyFreeContainers) {
		this.onlyFreeContainers = onlyFreeContainers;
		return self();
	}
	
	public Long parentContainerId() {
		return parentContainerId;
	}
	
	public StorageContainerListCriteria parentContainerId(Long parentContainerId) {
		this.parentContainerId = parentContainerId;
		return self();
	}
	
	public String siteName() {
		return siteName;
	}
	
	public StorageContainerListCriteria siteName(String siteName) {
		this.siteName = siteName;
		return self();
	}

	public String canHold() {
		return canHold;
	}

	public StorageContainerListCriteria canHold(String canHold) {
		this.canHold = canHold;
		return self();
	}

	public boolean includeChildren() {
		return includeChildren;
	}
	
	public StorageContainerListCriteria includeChildren(boolean includeChildren) {
		this.includeChildren = includeChildren;
		return self();
	}
	
	public boolean topLevelContainers() {
		return topLevelContainers;
	}
	
	public StorageContainerListCriteria topLevelContainers(boolean topLevelContainers) {
		this.topLevelContainers = topLevelContainers;
		return self();
	}
	
	public Boolean storeSpecimensEnabled() {
		return storeSpecimensEnabled;
	}
	
	public StorageContainerListCriteria storeSpecimensEnabled(Boolean storeSpecimensEnabled) {
		this.storeSpecimensEnabled = storeSpecimensEnabled;
		return self();
	}
	
	public String specimenClass() {
		return specimenClass;
	}
	
	public StorageContainerListCriteria specimenClass(String specimenClass) {
		this.specimenClass = specimenClass;
		return self();
	}
	
	public String specimenType() {
		return specimenType;
	}
	
	public StorageContainerListCriteria specimenType(String specimenType) {
		this.specimenType = specimenType;
		return self();
	}
	
	public boolean hierarchical() {
		return hierarchical;
	}
	
	public StorageContainerListCriteria hierarchical(boolean hierarchical) {
		this.hierarchical = hierarchical;
		return self();
	}
	
	public Set<Long> cpIds() {
		return cpIds;
	}
	
	public StorageContainerListCriteria cpIds(Set<Long> cpIds) {
		this.cpIds = cpIds;
		return self();
	}
	
	public StorageContainerListCriteria cpIds(Long[] cpIds) {
		if (cpIds != null && cpIds.length > 0) {
			this.cpIds = new HashSet<>(Arrays.asList(cpIds));
		} else {
			this.cpIds = null;
		}
		
		return self();
	}	
	
	public Set<String> cpShortTitles() {
		return cpShortTitles;
	}

	public StorageContainerListCriteria cpShortTitles(Set<String> cpShortTitles) {
		this.cpShortTitles = cpShortTitles;
		return self();
	}


	public StorageContainerListCriteria cpShortTitles(String[] cpShortTitles) {
		if (cpShortTitles != null && cpShortTitles.length > 0) {
			this.cpShortTitles = new HashSet<>(Arrays.asList(cpShortTitles));
		} else {
			this.cpShortTitles = null;
		}

		return self();
	}

	public Set<String> dpShortTitles() {
		return dpShortTitles;
	}

	public StorageContainerListCriteria dpShortTitles(Set<String> dpShortTitles) {
		this.dpShortTitles = dpShortTitles;
		return self();
	}

	public StorageContainerListCriteria dpShortTitles(String[] dpShortTitles) {
		if (dpShortTitles != null && dpShortTitles.length > 0) {
			this.dpShortTitles = new HashSet<>(Arrays.asList(dpShortTitles));
		} else {
			this.dpShortTitles = null;
		}

		return self();
	}

	public Set<SiteCpPair> siteCps() {
		return siteCps;
	}

	public StorageContainerListCriteria siteCps(Set<SiteCpPair> siteCps) {
		this.siteCps = siteCps;
		return self();
	}

	public StorageContainer.UsageMode usageMode() {
		if (usageMode != null) {
			return usageMode;
		}

		//
		// this is all done for backward compatibility to avoid making changes to clients who want
		// to query containers that can store specimens in the repo.
		//
		boolean dpSpecified = CollectionUtils.isNotEmpty(dpShortTitles);
		boolean cpSpecified = CollectionUtils.isNotEmpty(cpShortTitles) || CollectionUtils.isNotEmpty(cpIds);
		boolean spmnTypeSpecified = StringUtils.isNotBlank(specimenClass) || StringUtils.isNotBlank(specimenType);

		if (dpSpecified && !cpSpecified && !spmnTypeSpecified) {
			return StorageContainer.UsageMode.DISTRIBUTION;
		} else if (!dpSpecified && (cpSpecified || spmnTypeSpecified)) {
			return StorageContainer.UsageMode.STORAGE;
		}

		return null;
	}

	public StorageContainerListCriteria usageMode(StorageContainer.UsageMode usageMode) {
		this.usageMode = usageMode;
		return self();
	}

	public StorageContainerListCriteria usageMode(String usageMode) {
		try {
			if (StringUtils.isNotBlank(usageMode)) {
				this.usageMode = StorageContainer.UsageMode.valueOf(usageMode);
			}

			return self();
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	public List<Status> statuses() {
		return statuses;
	}

	public StorageContainerListCriteria statuses(List<String> input) {
		try {
			if (CollectionUtils.isEmpty(input)) {
				return self();
			}

			this.statuses = input.stream().map(Status::valueOf).distinct().collect(Collectors.toList());
			return self();
		} catch (Exception e) {
			throw OpenSpecimenException.userError(CommonErrorCode.INVALID_INPUT, e.getMessage());
		}
	}

	public Set<String> types() {
		return types;
	}

	public StorageContainerListCriteria types(Set<String> types) {
		this.types = types;
		return self();
	}
}
