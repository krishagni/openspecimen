package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.domain.StorageContainer;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@ListenAttributeChanges
public class StorageContainerDetail extends StorageContainerSummary {
	private Double temperature;

	private String cellDisplayProp;

	private String comments;

	private ExtensionDetail extensionDetail;

	private Set<String> allowedDistributionProtocols = new HashSet<>();

	private Set<String> calcAllowedDistributionProtocols = new HashSet<>();

	private Set<Integer> occupiedPositions = new HashSet<>();

	private Map<String, Integer> specimensByType;

	private Long rootContainerId;

	private boolean printLabels;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private UserSummary transferredBy;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Date transferDate;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String transferComments;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Boolean checkOut;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Boolean createIfAbsent;

	public Double getTemperature() {
		return temperature;
	}

	public void setTemperature(Double temperature) {
		this.temperature = temperature;
	}

	public String getCellDisplayProp() {
		return cellDisplayProp;
	}

	public void setCellDisplayProp(String cellDisplayProp) {
		this.cellDisplayProp = cellDisplayProp;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public Set<String> getAllowedDistributionProtocols() {
		return allowedDistributionProtocols;
	}

	public void setAllowedDistributionProtocols(Set<String> allowedDistributionProtocols) {
		this.allowedDistributionProtocols = allowedDistributionProtocols;
	}

	public Set<String> getCalcAllowedDistributionProtocols() {
		return calcAllowedDistributionProtocols;
	}

	public void setCalcAllowedDistributionProtocols(Set<String> calcAllowedDistributionProtocols) {
		this.calcAllowedDistributionProtocols = calcAllowedDistributionProtocols;
	}

	public Set<Integer> getOccupiedPositions() {
		return occupiedPositions;
	}

	public void setOccupiedPositions(Set<Integer> occupiedPositions) {
		this.occupiedPositions = occupiedPositions;
	}

	public Map<String, Integer> getSpecimensByType() {
		return specimensByType;
	}

	public void setSpecimensByType(Map<String, Integer> specimensByType) {
		this.specimensByType = specimensByType;
	}

	public Long getRootContainerId() {
		return rootContainerId;
	}

	public void setRootContainerId(Long rootContainerId) {
		this.rootContainerId = rootContainerId;
	}

	public boolean isPrintLabels() {
		return printLabels;
	}

	public void setPrintLabels(boolean printLabels) {
		this.printLabels = printLabels;
	}

	public UserSummary getTransferredBy() {
		return transferredBy;
	}

	public void setTransferredBy(UserSummary transferredBy) {
		this.transferredBy = transferredBy;
	}

	public Date getTransferDate() {
		return transferDate;
	}

	public void setTransferDate(Date transferDate) {
		this.transferDate = transferDate;
	}

	public String getTransferComments() {
		return transferComments;
	}

	public void setTransferComments(String transferComments) {
		this.transferComments = transferComments;
	}

	public Boolean getCheckOut() {
		return checkOut;
	}

	public void setCheckOut(Boolean checkOut) {
		this.checkOut = checkOut;
	}

	public Boolean getCreateIfAbsent() {
		return createIfAbsent;
	}

	public void setCreateIfAbsent(Boolean createIfAbsent) {
		this.createIfAbsent = createIfAbsent;
	}

	public static StorageContainerDetail from(StorageContainer container) {
		return from(container, true);
	}

	public static StorageContainerDetail from(StorageContainer container, boolean hydrated) {
		StorageContainerDetail result = new StorageContainerDetail();
		StorageContainerDetail.transform(container, result, hydrated);

		result.setTemperature(container.getTemperature());
		result.setComments(container.getComments());
		result.setExtensionDetail(ExtensionDetail.from(container.getExtension()));
		if (container.getCellDisplayProp() != null) {
			result.setCellDisplayProp(container.getCellDisplayProp().name());
		} else {
			result.setCellDisplayProp(StorageContainer.CellDisplayProp.SPECIMEN_LABEL.name());
		}

		result.setAllowedSpecimenClasses(PermissibleValue.toValueSet(container.getAllowedSpecimenClasses()));
		result.setAllowedSpecimenTypes(PermissibleValue.toValueSet(container.getAllowedSpecimenTypes()));
		result.setAllowedCollectionProtocols(getCpNames(container.getAllowedCps()));
		result.setAllowedDistributionProtocols(getDpNames(container.getAllowedDps()));

		if (hydrated) {
			result.setCalcAllowedSpecimenClasses(PermissibleValue.toValueSet(container.getCompAllowedSpecimenClasses()));
			result.setCalcAllowedSpecimenTypes(PermissibleValue.toValueSet(container.getCompAllowedSpecimenTypes()));
			result.setCalcAllowedCollectionProtocols(getCpNames(container.getCompAllowedCps()));
			result.setCalcAllowedDistributionProtocols(getDpNames(container.getCompAllowedDps()));
			result.setOccupiedPositions(container.occupiedPositionsOrdinals());
		}
		return result;
	}
	
	private static Set<String> getCpNames(Collection<CollectionProtocol> cps) {
		return cps.stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet());
	}

	private static Set<String> getDpNames(Collection<DistributionProtocol> dps) {
		return dps.stream().map(DistributionProtocol::getShortTitle).collect(Collectors.toSet());
	}
}
