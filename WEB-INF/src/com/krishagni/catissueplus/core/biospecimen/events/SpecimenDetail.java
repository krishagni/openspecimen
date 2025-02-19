package com.krishagni.catissueplus.core.biospecimen.events;

import java.io.Serial;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import com.krishagni.catissueplus.core.administrative.domain.PermissibleValue;
import com.krishagni.catissueplus.core.administrative.events.StorageLocationSummary;
import com.krishagni.catissueplus.core.biospecimen.domain.Specimen;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenRequirement;
import com.krishagni.catissueplus.core.biospecimen.domain.Visit;
import com.krishagni.catissueplus.core.biospecimen.repository.DaoFactory;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.OpenSpecimenAppCtxProvider;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.domain.DeObject;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@ListenAttributeChanges
public class SpecimenDetail extends SpecimenInfo {

	@Serial
	private static final long serialVersionUID = -752005520158376620L;

	private CollectionEventDetail collectionEvent;
	
	private ReceivedEventDetail receivedEvent;
	
	private String labelFmt;
	
	private Set<String> biohazards;
	
	private String comments;
	
	private Boolean closeAfterChildrenCreation;  
	
	private List<SpecimenDetail> children;

	private List<SpecimenInfo> specimensPool;

	private List<SpecimenInfo> pooledSpecimens;

	//
	// Properties required for auto-creation of containers
	//
	private StorageLocationSummary containerLocation;

	private Long containerTypeId;

	private String containerTypeName;

	// This is needed for creation of derivatives from BO for closing parent specimen.
	private Boolean closeParent;
	
	private Boolean poolSpecimen;
	
	private ExtensionDetail extensionDetail;

	private boolean reserved;

	//
	// transient variables specifying action to be performed
	//
	private boolean forceDelete;

	private Integer incrParentFreezeThaw;

	private UserSummary transferUser;

	private Date transferTime;

	private String transferComments;

	private Boolean checkout;

	private boolean autoCollectParents;

	private String uid;

	private String parentUid;

	private Long dpId;

	private StorageLocationSummary holdingLocation;

	private boolean checkedOut;

	private StorageLocationSummary checkoutPosition;

	private String kitLabel;

	private String kitBarcode;

	private boolean update;

	private Long ancestorId;


	public CollectionEventDetail getCollectionEvent() {
		return collectionEvent;
	}

	public void setCollectionEvent(CollectionEventDetail collectionEvent) {
		this.collectionEvent = collectionEvent;
	}

	public ReceivedEventDetail getReceivedEvent() {
		return receivedEvent;
	}

	public void setReceivedEvent(ReceivedEventDetail receivedEvent) {
		this.receivedEvent = receivedEvent;
	}

	public String getLabelFmt() {
		return labelFmt;
	}

	public void setLabelFmt(String labelFmt) {
		this.labelFmt = labelFmt;
	}

	public List<SpecimenDetail> getChildren() {
		return children;
	}

	public void setChildren(List<SpecimenDetail> children) {
		this.children = children;
	}

	public List<SpecimenInfo> getSpecimensPool() {
		return specimensPool;
	}

	public void setSpecimensPool(List<SpecimenInfo> specimensPool) {
		this.specimensPool = specimensPool;
	}

	public List<SpecimenInfo> getPooledSpecimens() {
		return pooledSpecimens;
	}

	public void setPooledSpecimens(List<SpecimenInfo> pooledSpecimens) {
		this.pooledSpecimens = pooledSpecimens;
	}

	@JsonIgnore
	public StorageLocationSummary getContainerLocation() {
		return containerLocation;
	}

	@JsonProperty
	public void setContainerLocation(StorageLocationSummary containerLocation) {
		this.containerLocation = containerLocation;
	}

	@JsonIgnore
	public Long getContainerTypeId() {
		return containerTypeId;
	}

	@JsonProperty
	public void setContainerTypeId(Long containerTypeId) {
		this.containerTypeId = containerTypeId;
	}

	@JsonIgnore
	public String getContainerTypeName() {
		return containerTypeName;
	}

	@JsonProperty
	public void setContainerTypeName(String containerTypeName) {
		this.containerTypeName = containerTypeName;
	}

	public Set<String> getBiohazards() {
		return biohazards;
	}

	public void setBiohazards(Set<String> biohazards) {
		this.biohazards = biohazards;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	@JsonIgnore
	public Boolean getCloseAfterChildrenCreation() {
		return closeAfterChildrenCreation;
	}

	@JsonProperty
	public void setCloseAfterChildrenCreation(Boolean closeAfterChildrenCreation) {
		this.closeAfterChildrenCreation = closeAfterChildrenCreation;
	}

	@JsonIgnore
	public Boolean getCloseParent() {
		return closeParent;
	}

	@JsonProperty
	public void setCloseParent(Boolean closeParent) {
		this.closeParent = closeParent;
	}

	public Boolean getPoolSpecimen() {
		return poolSpecimen;
	}

	public void setPoolSpecimen(Boolean poolSpecimen) {
		this.poolSpecimen = poolSpecimen;
	}

	@JsonIgnore
	public boolean closeParent() {
		return closeParent == null ? false : closeParent;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public boolean isReserved() {
		return reserved;
	}

	public void setReserved(boolean reserved) {
		this.reserved = reserved;
	}

	@JsonIgnore
	public boolean isForceDelete() {
		return forceDelete;
	}

	public void setForceDelete(boolean forceDelete) {
		this.forceDelete = forceDelete;
	}

	@JsonIgnore
	public Integer getIncrParentFreezeThaw() {
		return incrParentFreezeThaw;
	}

	@JsonProperty
	public void setIncrParentFreezeThaw(Integer incrParentFreezeThaw) {
		this.incrParentFreezeThaw = incrParentFreezeThaw;
	}

	@JsonIgnore
	public UserSummary getTransferUser() {
		return transferUser;
	}

	@JsonProperty
	public void setTransferUser(UserSummary transferUser) {
		this.transferUser = transferUser;
	}

	@JsonIgnore
	public Date getTransferTime() {
		return transferTime;
	}

	@JsonProperty
	public void setTransferTime(Date transferTime) {
		this.transferTime = transferTime;
	}

	@JsonIgnore
	public String getTransferComments() {
		return transferComments;
	}

	@JsonProperty
	public void setTransferComments(String transferComments) {
		this.transferComments = transferComments;
	}

	@JsonIgnore
	public Boolean getCheckout() {
		return checkout;
	}

	@JsonProperty
	public void setCheckout(Boolean checkout) {
		this.checkout = checkout;
	}

	@JsonIgnore
	public boolean isAutoCollectParents() {
		return autoCollectParents;
	}

	@JsonProperty
	public void setAutoCollectParents(boolean autoCollectParents) {
		this.autoCollectParents = autoCollectParents;
	}

	public String getUid() {
		return uid;
	}

	public void setUid(String uid) {
		this.uid = uid;
	}

	public String getParentUid() {
		return parentUid;
	}

	public void setParentUid(String parentUid) {
		this.parentUid = parentUid;
	}

	public Long getDpId() {
		return dpId;
	}

	public void setDpId(Long dpId) {
		this.dpId = dpId;
	}

	@JsonIgnore
	public StorageLocationSummary getHoldingLocation() {
		return holdingLocation;
	}

	@JsonProperty
	public void setHoldingLocation(StorageLocationSummary holdingLocation) {
		this.holdingLocation = holdingLocation;
	}

	public boolean isCheckedOut() {
		return checkedOut;
	}

	public void setCheckedOut(boolean checkedOut) {
		this.checkedOut = checkedOut;
	}

	public StorageLocationSummary getCheckoutPosition() {
		return checkoutPosition;
	}

	public void setCheckoutPosition(StorageLocationSummary checkoutPosition) {
		this.checkoutPosition = checkoutPosition;
	}

	public String getKitLabel() {
		return kitLabel;
	}

	public void setKitLabel(String kitLabel) {
		this.kitLabel = kitLabel;
	}

	public String getKitBarcode() {
		return kitBarcode;
	}

	public void setKitBarcode(String kitBarcode) {
		this.kitBarcode = kitBarcode;
	}

	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public Long getAncestorId() {
		return ancestorId;
	}

	public void setAncestorId(Long ancestorId) {
		this.ancestorId = ancestorId;
	}

	@JsonIgnore
	public String getLogKey() {
		String result = getId().toString();
		if (StringUtils.isNotBlank(getLabel())) {
			result += " (" + getLabel() + ")";
		}

		return result;
	}

	public static SpecimenDetail from(Specimen specimen) {
		return from(specimen, true, true);
	}

	public static SpecimenDetail from(Specimen specimen, boolean partial, boolean excludePhi) {
		return from(specimen, partial, excludePhi, false);
	}

	public static SpecimenDetail from(Specimen specimen, boolean partial, boolean excludePhi, boolean excludeChildren) {
		SpecimenDetail result = new SpecimenDetail();
		SpecimenInfo.fromTo(specimen, result);

		if (specimen.getPooledEvent() != null) {
			result.setSpecimensPool(
				specimen.getPooledEvent().getPoolItems().stream()
					.filter(s -> !s.isDeleted())
					.map(SpecimenInfo::from)
					.sorted()
					.collect(Collectors.toList())
			);
		}

		if (specimen.isPoolItem() && CollectionUtils.isNotEmpty(specimen.getPoolItemEvents())) {
			result.setPooledSpecimens(
				specimen.getPoolItemEvents().stream()
					.filter(e -> !e.getPooledSpecimen().isDeleted())
					.map(e -> SpecimenInfo.from(e.getPooledSpecimen()))
					.sorted()
					.collect(Collectors.toList())
			);
		}
		
		SpecimenRequirement sr = specimen.getSpecimenRequirement();
		if (!excludeChildren) {
			if (sr == null) {
				List<SpecimenDetail> children = Utility.nullSafeStream(specimen.getChildCollection())
					.map(child -> from(child, partial, excludePhi, excludeChildren))
					.collect(Collectors.toList());
				sort(children);
				result.setChildren(children);
			} else {
				List<SpecimenDetail> children = getSpecimens(
					specimen.getVisit(), sr.getChildSpecimenRequirements(),
					specimen.getChildCollection(),
					partial, excludePhi, excludeChildren);
				for (SpecimenDetail child : children) {
					child.setParentId(result.getId());
					child.setParentLabel(result.getLabel());
				}

				result.setChildren(children);
			}
		}

		//
		// false to ensure we don't end up in infinite recurssion
		//
		result.setLabelFmt(specimen.getLabelTmpl(false));
		result.setReqCode(sr != null ? sr.getCode() : null);
		result.setBiohazards(PermissibleValue.toValueSet(specimen.getBiohazards()));
		result.setComments(specimen.getComment());
		result.setReserved(specimen.isReserved());

		if (!partial) {
			result.setExtensionDetail(ExtensionDetail.from(specimen.getExtension(), excludePhi));
			if (specimen.isPrimary()) {
				result.setCollectionEvent(CollectionEventDetail.from(specimen.getCollectionEvent()));
				result.setReceivedEvent(ReceivedEventDetail.from(specimen.getReceivedEvent()));
			} else {
				result.setCollectionEvent(CollectionEventDetail.from(specimen.getCollRecvDetails()));
				result.setReceivedEvent(ReceivedEventDetail.from(specimen.getCollRecvDetails()));
			}
		}

		result.setUid(specimen.getUid());
		result.setParentUid(specimen.getParentUid());
		result.setCheckedOut(specimen.getCheckoutPosition() != null);
		result.setCheckoutPosition(StorageLocationSummary.from(specimen.getCheckoutPosition()));
		result.setAncestorId(specimen.getAncestorId());
		return result;
	}
	
	public static List<SpecimenDetail> from(Collection<Specimen> specimens) {
		return Utility.nullSafeStream(specimens).map(SpecimenDetail::from).collect(Collectors.toList());
	}
	
	public static SpecimenDetail from(SpecimenRequirement anticipated) {
		return SpecimenDetail.from(anticipated, false);
	}

	public static SpecimenDetail from(SpecimenRequirement anticipated, boolean excludeChildren) {
		SpecimenDetail result = new SpecimenDetail();		
		SpecimenInfo.fromTo(anticipated, result);
		if (!excludeChildren) {
			result.setChildren(fromAnticipated(anticipated.getChildSpecimenRequirements(), excludeChildren));
		}

		result.setLabelFmt(anticipated.getLabelTmpl());
		if (anticipated.getLabelAutoPrintModeToUse() != null) {
			result.setLabelAutoPrintMode(anticipated.getLabelAutoPrintModeToUse().name());
		}
		result.setReqCode(anticipated.getCode());

		if (anticipated.getDefaultCustomFieldValues() != null && !anticipated.getDefaultCustomFieldValues().isEmpty()) {
			Long cpId = anticipated.getCollectionProtocol().getId();
			DeObject extn = DeObject.fromValueMap(cpId, Specimen.EXTN, anticipated.getDefaultCustomFieldValues());
			result.setExtensionDetail(ExtensionDetail.from(extn, false, true));
		}

		return result;		
	}

	public static void sort(List<SpecimenDetail> specimens) {
		Collections.sort(specimens);
		
		for (SpecimenDetail specimen : specimens) {
			if (specimen.getChildren() != null) {
				sort(specimen.getChildren());
			}
		}
	}
	
	public static List<SpecimenDetail> getSpecimens(
			Visit visit,
			Collection<SpecimenRequirement> anticipated,
			Collection<Specimen> specimens,
			boolean partial,
			boolean excludePhi,
			boolean excludeChildren) {

		if (visit != null && visit.getCpEvent() != null && visit.getCollectionProtocol().isKitLabelsEnabled()) {
			if (visit.getKitLabels() == null) {
				DaoFactory daoFactory = OpenSpecimenAppCtxProvider.getBean("biospecimenDaoFactory");
				visit.setKitLabels(daoFactory.getSpecimenDao().getKitLabels(visit));
			}
		}

		List<SpecimenDetail> result = Utility.stream(specimens)
			.map(s -> SpecimenDetail.from(s, partial, excludePhi, excludeChildren))
			.collect(Collectors.toList());

		merge(visit, anticipated, result, null, getReqSpecimenMap(result), excludeChildren);
		SpecimenDetail.sort(result);

		if (visit != null && visit.getKitLabels() != null) {
			List<SpecimenDetail> workingList = new ArrayList<>(result);
			while (!workingList.isEmpty()) {
				SpecimenDetail spmn = workingList.remove(0);
				if (StringUtils.isNotBlank(spmn.getKitLabel()) || spmn.getReqId() == null) {
					continue;
				}

				if (visit.getKitLabels() != null) {
					Map<String, String> kit = visit.getKitLabels().get(spmn.getReqId());
					if (kit != null) {
						spmn.setKitLabel(kit.get("label"));
						spmn.setKitBarcode(kit.get("barcode"));
					}
				}

				if (spmn.getChildren() != null) {
					workingList.addAll(0, spmn.getChildren());
				}
			}
		}

		return result;
	}

	private static Map<Long, SpecimenDetail> getReqSpecimenMap(List<SpecimenDetail> specimens) {
		Map<Long, SpecimenDetail> reqSpecimenMap = new HashMap<>();

		List<SpecimenDetail> remaining = new ArrayList<>(specimens);
		while (!remaining.isEmpty()) {
			SpecimenDetail specimen = remaining.remove(0);
			Long srId = (specimen.getReqId() == null) ? -1 : specimen.getReqId();
			reqSpecimenMap.put(srId, specimen);

			if (specimen.getChildren() != null) {
				remaining.addAll(specimen.getChildren());
			}
		}
		
		return reqSpecimenMap;
	}
	
	private static void merge(
			Visit visit,
			Collection<SpecimenRequirement> anticipatedSpecimens, 
			List<SpecimenDetail> result, 
			SpecimenDetail currentParent,
			Map<Long, SpecimenDetail> reqSpecimenMap,
			boolean excludeChildren) {
		
		for (SpecimenRequirement anticipated : anticipatedSpecimens) {
			SpecimenDetail specimen = reqSpecimenMap.get(anticipated.getId());
			if (specimen != null && excludeChildren) {
				continue;
			}

			if (specimen != null) {
				merge(visit, anticipated.getChildSpecimenRequirements(), result, specimen, reqSpecimenMap, excludeChildren);
			} else if (!anticipated.isClosed()) {
				specimen = SpecimenDetail.from(anticipated, excludeChildren);
				setVisitDetails(visit, specimen);

				if (currentParent == null) {
					result.add(specimen);
				} else {
					specimen.setParentId(currentParent.getId());
					currentParent.getChildren().add(specimen);
				}				
			}						
		}
	}

	private static void setVisitDetails(Visit visit, SpecimenDetail specimen) {
		if (visit == null) {
			return;
		}

		specimen.setCprId(visit.getRegistration().getId());
		specimen.setVisitId(visit.getId());
		specimen.setVisitName(visit.getName());
		specimen.setVisitStatus(visit.getStatus());
		specimen.setSprNo(visit.getSurgicalPathologyNumber());
		specimen.setVisitDate(visit.getVisitDate());
		Utility.nullSafeStream(specimen.getChildren()).forEach(child -> setVisitDetails(visit, child));
	}

	private static List<SpecimenDetail> fromAnticipated(Collection<SpecimenRequirement> anticipatedSpecimens, boolean excludeChildren) {
		return Utility.nullSafeStream(anticipatedSpecimens)
			.filter(anticipated -> !anticipated.isClosed())
			.map(s -> SpecimenDetail.from(s, excludeChildren))
			.collect(Collectors.toList());
	}
}
