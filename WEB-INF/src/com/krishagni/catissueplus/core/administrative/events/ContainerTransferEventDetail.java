package com.krishagni.catissueplus.core.administrative.events;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.krishagni.catissueplus.core.administrative.domain.ContainerTransferEvent;
import com.krishagni.catissueplus.core.common.events.UserSummary;

public class ContainerTransferEventDetail {
	private Long id;

	private Long containerId;

	private String containerName;

	private String containerDisplayName;

	private String fromSite;

	private StorageLocationSummary fromLocation;

	private String toSite;

	private StorageLocationSummary toLocation;

	private UserSummary user;

	private Date time;

	private String reason;

	private String freezerName;

	private String freezerDisplayName;

	private List<String> cps;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Long getContainerId() {
		return containerId;
	}

	public void setContainerId(Long containerId) {
		this.containerId = containerId;
	}

	public String getContainerName() {
		return containerName;
	}

	public void setContainerName(String containerName) {
		this.containerName = containerName;
	}

	public String getContainerDisplayName() {
		return containerDisplayName;
	}

	public void setContainerDisplayName(String containerDisplayName) {
		this.containerDisplayName = containerDisplayName;
	}

	public String getFromSite() {
		return fromSite;
	}

	public void setFromSite(String fromSite) {
		this.fromSite = fromSite;
	}

	public StorageLocationSummary getFromLocation() {
		return fromLocation;
	}

	public void setFromLocation(StorageLocationSummary fromLocation) {
		this.fromLocation = fromLocation;
	}

	public String getToSite() {
		return toSite;
	}

	public void setToSite(String toSite) {
		this.toSite = toSite;
	}

	public StorageLocationSummary getToLocation() {
		return toLocation;
	}

	public void setToLocation(StorageLocationSummary toLocation) {
		this.toLocation = toLocation;
	}

	public UserSummary getUser() {
		return user;
	}

	public void setUser(UserSummary user) {
		this.user = user;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public String getReason() {
		return reason;
	}

	public void setReason(String reason) {
		this.reason = reason;
	}

	public String getFreezerName() {
		return freezerName;
	}

	public void setFreezerName(String freezerName) {
		this.freezerName = freezerName;
	}

	public String getFreezerDisplayName() {
		return freezerDisplayName;
	}

	public void setFreezerDisplayName(String freezerDisplayName) {
		this.freezerDisplayName = freezerDisplayName;
	}

	public List<String> getCps() {
		return cps;
	}

	public void setCps(List<String> cps) {
		this.cps = cps;
	}

	public static List<ContainerTransferEventDetail> from(Collection<ContainerTransferEvent> events) {
		return events.stream().map(ContainerTransferEventDetail::from).collect(Collectors.toList());
	}

	public static ContainerTransferEventDetail from(ContainerTransferEvent event) {
		ContainerTransferEventDetail result = new ContainerTransferEventDetail();
		result.setId(event.getId());
		result.setFromSite(event.getFromSite().getName());
		result.setToSite(event.getToSite().getName());
		result.setContainerId(event.getContainer().getId());
		result.setContainerName(event.getContainer().getName());
		result.setContainerDisplayName(event.getContainer().getDisplayName());
		result.setUser(UserSummary.from(event.getUser()));
		result.setTime(event.getTime());
		result.setReason(event.getReason());

		if (event.getFromContainer() != null ) {
			StorageLocationSummary fromLocation = new StorageLocationSummary();
			fromLocation.setId(event.getFromContainer().getId());
			fromLocation.setName(event.getFromContainer().getName());
			fromLocation.setDisplayName(event.getFromContainer().getDisplayName());
			fromLocation.setPositionY(event.getFromRow());
			fromLocation.setPositionX(event.getFromColumn());
			fromLocation.setPosition(event.getFromPosition());
			fromLocation.setMode(event.getFromContainer().getPositionLabelingMode().name());
			result.setFromLocation(fromLocation);
		}

		if (event.getToContainer() != null ) {
			StorageLocationSummary toLocation = new StorageLocationSummary();
			toLocation.setId(event.getToContainer().getId());
			toLocation.setName(event.getToContainer().getName());
			toLocation.setDisplayName(event.getToContainer().getDisplayName());
			toLocation.setPositionY(event.getToRow());
			toLocation.setPositionX(event.getToColumn());
			toLocation.setPosition(event.getToPosition());
			toLocation.setMode(event.getToContainer().getPositionLabelingMode().name());
			result.setToLocation(toLocation);
		}

		return result;
	}
}
