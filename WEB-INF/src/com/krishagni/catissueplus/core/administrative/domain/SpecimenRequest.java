package com.krishagni.catissueplus.core.administrative.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;

import com.krishagni.catissueplus.core.administrative.domain.factory.SpecimenRequestErrorCode;
import com.krishagni.catissueplus.core.biospecimen.domain.BaseExtensionEntity;
import com.krishagni.catissueplus.core.biospecimen.domain.CollectionProtocol;
import com.krishagni.catissueplus.core.biospecimen.domain.SpecimenList;
import com.krishagni.catissueplus.core.common.errors.OpenSpecimenException;
import com.krishagni.catissueplus.core.common.service.impl.EventPublisher;
import com.krishagni.catissueplus.core.common.util.AuthUtil;
import com.krishagni.catissueplus.core.common.util.Status;

@Audited
public class SpecimenRequest extends BaseExtensionEntity {
	private static final String ENTITY_NAME = "specimen_request";

	public enum ScreeningStatus {
		PENDING,
		APPROVED,
		REJECTED
	}

	private Long catalogId;

	private String catalogQueryDef;

	private User requestor;

	private String requestorEmailId;

	private String irbId;

	private Date dateOfRequest;

	private ScreeningStatus screeningStatus = ScreeningStatus.PENDING;

	private User screenedBy;

	private Date dateOfScreening;

	private String screeningComments;

	private User processedBy;

	private Date dateOfProcessing;

	private CollectionProtocol cp;

	private DistributionProtocol dp;

	private Set<SpecimenRequestItem> items = new LinkedHashSet<>();

	private String itemsCriteriaJson;

	private String activityStatus;

	private String comments;

	private Set<DistributionOrder> orders = new HashSet<>();

	private SpecimenList cart;

	public Long getCatalogId() {
		return catalogId;
	}

	public void setCatalogId(Long catalogId) {
		this.catalogId = catalogId;
	}

	public String getCatalogQueryDef() {
		return catalogQueryDef;
	}

	public void setCatalogQueryDef(String catalogQueryDef) {
		this.catalogQueryDef = catalogQueryDef;
	}

	public User getRequestor() {
		return requestor;
	}

	public void setRequestor(User requestor) {
		this.requestor = requestor;
	}

	public String getRequestorEmailId() {
		return requestor != null ? requestor.getEmailAddress() : requestorEmailId;
	}

	public void setRequestorEmailId(String requestorEmailId) {
		this.requestorEmailId = requestorEmailId;
	}

	public String getIrbId() {
		return irbId;
	}

	public void setIrbId(String irbId) {
		this.irbId = irbId;
	}

	public Date getDateOfRequest() {
		return dateOfRequest;
	}

	public void setDateOfRequest(Date dateOfRequest) {
		this.dateOfRequest = dateOfRequest;
	}

	public ScreeningStatus getScreeningStatus() {
		return screeningStatus;
	}

	public void setScreeningStatus(ScreeningStatus screeningStatus) {
		this.screeningStatus = screeningStatus;
	}

	public User getScreenedBy() {
		return screenedBy;
	}

	public void setScreenedBy(User screenedBy) {
		this.screenedBy = screenedBy;
	}

	public Date getDateOfScreening() {
		return dateOfScreening;
	}

	public void setDateOfScreening(Date dateOfScreening) {
		this.dateOfScreening = dateOfScreening;
	}

	public String getScreeningComments() {
		return screeningComments;
	}

	public void setScreeningComments(String screeningComments) {
		this.screeningComments = screeningComments;
	}

	public User getProcessedBy() {
		return processedBy;
	}

	public void setProcessedBy(User processedBy) {
		this.processedBy = processedBy;
	}

	public Date getDateOfProcessing() {
		return dateOfProcessing;
	}

	public void setDateOfProcessing(Date dateOfProcessing) {
		this.dateOfProcessing = dateOfProcessing;
	}

	public CollectionProtocol getCp() {
		return cp;
	}

	public void setCp(CollectionProtocol cp) {
		this.cp = cp;
	}

	public DistributionProtocol getDp() {
		return dp;
	}

	public void setDp(DistributionProtocol dp) {
		this.dp = dp;
	}

	public Set<SpecimenRequestItem> getItems() {
		return items;
	}

	public void setItems(Set<SpecimenRequestItem> items) {
		this.items = items;
	}

	public String getItemsCriteriaJson() {
		return itemsCriteriaJson;
	}

	public void setItemsCriteriaJson(String itemsCriteriaJson) {
		this.itemsCriteriaJson = itemsCriteriaJson;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public String getComments() {
		return comments;
	}

	public void setComments(String comments) {
		this.comments = comments;
	}

	@NotAudited
	public Set<DistributionOrder> getOrders() {
		return orders;
	}

	public void setOrders(Set<DistributionOrder> orders) {
		this.orders = orders;
	}

	@NotAudited
	public SpecimenList getCart() {
		return cart;
	}

	public void setCart(SpecimenList cart) {
		this.cart = cart;
	}

	@Override
	public String getEntityType() {
		return "SpecimenRequest-" + catalogId;
	}

	public Map<Long, SpecimenRequestItem> getSpecimenIdRequestItemMap() {
		return getItems().stream()
			.collect(Collectors.toMap(item -> item.getSpecimen().getId(), item -> item));
	}

	public Set<Long> getSpecimenIds() {
		return getItems().stream().map(item -> item.getSpecimen().getId()).collect(Collectors.toSet());
	}

	public void closeIfFulfilled() {
		boolean anyPending = getItems().stream().anyMatch(SpecimenRequestItem::isPending);
		if (anyPending) {
			return;
		}

		close("Automatic closure on request fulfillment.");
	}

	public void close(String comments) {
		setProcessedBy(AuthUtil.getCurrentUser());
		setDateOfProcessing(Calendar.getInstance().getTime());
		setComments(comments);
		setActivityStatus(Status.ACTIVITY_STATUS_CLOSED.getStatus());
		EventPublisher.getInstance().publish(SpecimenRequestEvent.closed(this));
	}

	public void reopenIfNotFulfilled() {
		if (!isClosed()) {
			return;
		}

		boolean anyPending = getItems().stream().anyMatch(SpecimenRequestItem::isPending);
		if (anyPending) {
			reopen("Automatic reopening of request.");
		}
	}

	public void reopen(String comments) {
		if (!isClosed()) {
			return;
		}

		setProcessedBy(null);
		setDateOfProcessing(null);
		setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());

		if (StringUtils.isNotBlank(comments)) {
			comments = comments;
			if (StringUtils.isNotBlank(getComments())) {
				comments += "\n\n" + getComments();
			}

			setComments(comments);
		}
	}

	public void delete() {
		setActivityStatus(Status.ACTIVITY_STATUS_DISABLED.getStatus());
	}

	public boolean isClosed() {
		return Status.ACTIVITY_STATUS_CLOSED.getStatus().equals(getActivityStatus());
	}

	public boolean isApproved() {
		return ScreeningStatus.APPROVED == getScreeningStatus();
	}

	public boolean isRejected() { return ScreeningStatus.REJECTED == getScreeningStatus(); }

	public void approve(User user, Date time, String comments) {
		if (isApproved()) {
			// already approved, no change
			return;
		}

		updateScreeningStatus(ScreeningStatus.APPROVED, user, time, comments);
	}

	public void reject(User user, Date time, String comments) {
		if (isRejected()) {
			return;
		}

		if (isApproved()) {
			ensureReqIsNotUsedInOrder();
		}

		updateScreeningStatus(ScreeningStatus.REJECTED, user, time, comments);
		close(comments);
	}

	public void resetScreeningStatus(String comments) {
		if (isApproved()) {
			ensureReqIsNotUsedInOrder();
		}

		updateScreeningStatus(ScreeningStatus.PENDING, null, null, comments);
	}

	public static String getEntityName() {
		return ENTITY_NAME;
	}

	private void updateScreeningStatus(ScreeningStatus inputStatus, User user, Date time, String comments) {
		if (user == null && inputStatus != ScreeningStatus.PENDING) {
			user = AuthUtil.getCurrentUser();
		}

		if (time == null && inputStatus != ScreeningStatus.PENDING) {
			time = Calendar.getInstance().getTime();
		}

		if (time != null && time.after(getDateOfRequest())) {
			time = Calendar.getInstance().getTime();
		}

		setScreeningStatus(inputStatus);
		setScreenedBy(user);
		setDateOfScreening(time);
		setScreeningComments(comments);
		if (inputStatus == ScreeningStatus.PENDING) {
			setActivityStatus(Status.ACTIVITY_STATUS_ACTIVE.getStatus());
		}
	}

	private void ensureReqIsNotUsedInOrder() {
		Set<String> reqOrders = getOrders().stream()
			.filter(o -> !Status.isDisabledStatus(o.getActivityStatus()))
			.map(DistributionOrder::getName)
			.collect(Collectors.toSet());
		if (!reqOrders.isEmpty()) {
			String names = StringUtils.join(reqOrders, ", ");
			throw OpenSpecimenException.userError(SpecimenRequestErrorCode.USED_IN_ORDER, getId(), names);
		}
	}
}
