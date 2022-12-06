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

	private Set<String> allowedSpecimenClasses = new HashSet<>();
	
	private Set<String> calcAllowedSpecimenClasses = new HashSet<>();
	
	private Set<String> allowedSpecimenTypes = new HashSet<>();
	
	private Set<String> calcAllowedSpecimenTypes = new HashSet<>();

	private Set<String> allowedCollectionProtocols = new HashSet<>();
	
	private Set<String> calcAllowedCollectionProtocols = new HashSet<>();

	private Set<String> allowedDistributionProtocols = new HashSet<>();

	private Set<String> calcAllowedDistributionProtocols = new HashSet<>();

	private Set<Integer> occupiedPositions = new HashSet<>();

	private Map<String, Integer> specimensByType;

	private boolean printLabels;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private UserSummary transferredBy;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Date transferDate;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private String transferComments;

	@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
	private Boolean checkOut;

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

	public Set<String> getAllowedSpecimenClasses() {
		return allowedSpecimenClasses;
	}

	public void setAllowedSpecimenClasses(Set<String> allowedSpecimenClasses) {
		this.allowedSpecimenClasses = allowedSpecimenClasses;
	}

	public Set<String> getCalcAllowedSpecimenClasses() {
		return calcAllowedSpecimenClasses;
	}

	public void setCalcAllowedSpecimenClasses(Set<String> calcAllowedSpecimenClasses) {
		this.calcAllowedSpecimenClasses = calcAllowedSpecimenClasses;
	}

	public Set<String> getAllowedSpecimenTypes() {
		return allowedSpecimenTypes;
	}

	public void setAllowedSpecimenTypes(Set<String> allowedSpecimenTypes) {
		this.allowedSpecimenTypes = allowedSpecimenTypes;
	}

	public Set<String> getCalcAllowedSpecimenTypes() {
		return calcAllowedSpecimenTypes;
	}

	public void setCalcAllowedSpecimenTypes(Set<String> calcAllowedSpecimenTypes) {
		this.calcAllowedSpecimenTypes = calcAllowedSpecimenTypes;
	}

	public Set<String> getAllowedCollectionProtocols() {
		return allowedCollectionProtocols;
	}

	public void setAllowedCollectionProtocols(Set<String> allowedCollectionProtocols) {
		this.allowedCollectionProtocols = allowedCollectionProtocols;
	}

	public Set<String> getCalcAllowedCollectionProtocols() {
		return calcAllowedCollectionProtocols;
	}

	public void setCalcAllowedCollectionProtocols(Set<String> calcAllowedCollectionProtocols) {
		this.calcAllowedCollectionProtocols = calcAllowedCollectionProtocols;
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

	public static StorageContainerDetail from(StorageContainer container) {
		StorageContainerDetail result = new StorageContainerDetail();
		StorageContainerDetail.transform(container, result);

		result.setTemperature(container.getTemperature());
		result.setComments(container.getComments());
		result.setExtensionDetail(ExtensionDetail.from(container.getExtension()));
		if (container.getCellDisplayProp() != null) {
			result.setCellDisplayProp(container.getCellDisplayProp().name());
		} else {
			result.setCellDisplayProp(StorageContainer.CellDisplayProp.SPECIMEN_LABEL.name());
		}

		result.setAllowedSpecimenClasses(PermissibleValue.toValueSet(container.getAllowedSpecimenClasses()));
		result.setCalcAllowedSpecimenClasses(PermissibleValue.toValueSet(container.getCompAllowedSpecimenClasses()));

		result.setAllowedSpecimenTypes(PermissibleValue.toValueSet(container.getAllowedSpecimenTypes()));
		result.setCalcAllowedSpecimenTypes(PermissibleValue.toValueSet(container.getCompAllowedSpecimenTypes()));
		
		result.setAllowedCollectionProtocols(getCpNames(container.getAllowedCps()));		
		result.setCalcAllowedCollectionProtocols(getCpNames(container.getCompAllowedCps()));

		result.setAllowedDistributionProtocols(getDpNames(container.getAllowedDps()));
		result.setCalcAllowedDistributionProtocols(getDpNames(container.getCompAllowedDps()));
		
		result.setOccupiedPositions(container.occupiedPositionsOrdinals());
		return result;
	}
	
	private static Set<String> getCpNames(Collection<CollectionProtocol> cps) {
		return cps.stream().map(CollectionProtocol::getShortTitle).collect(Collectors.toSet());
	}

	private static Set<String> getDpNames(Collection<DistributionProtocol> dps) {
		return dps.stream().map(DistributionProtocol::getShortTitle).collect(Collectors.toSet());
	}
}
