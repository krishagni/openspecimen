
package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishagni.catissueplus.core.administrative.domain.DistributionProtocol;
import com.krishagni.catissueplus.core.administrative.domain.DpDistributionSite;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.common.util.Utility;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;
import com.krishagni.catissueplus.core.de.events.FormSummary;
import com.krishagni.catissueplus.core.de.events.SavedQuerySummary;

@ListenAttributeChanges
public class DistributionProtocolDetail extends DistributionProtocolSummary {

	private List<UserSummary> coordinators;

	private String irbId;

	private String activityStatus;

	private SavedQuerySummary report;

	private FormSummary orderExtnForm;

	private Boolean disableEmailNotifs;

	private String attachmentType;

	private String orderItemLabelFormat;

	private Map<String, List<String>> distributingSites = new HashMap<>();

	private ExtensionDetail extensionDetail;

	public List<UserSummary> getCoordinators() {
		return coordinators;
	}

	public void setCoordinators(List<UserSummary> coordinators) {
		this.coordinators = coordinators;
	}

	public String getIrbId() {
		return irbId;
	}

	public void setIrbId(String irbId) {
		this.irbId = irbId;
	}

	public String getActivityStatus() {
		return activityStatus;
	}

	public void setActivityStatus(String activityStatus) {
		this.activityStatus = activityStatus;
	}

	public SavedQuerySummary getReport() {
		return report;
	}

	public void setReport(SavedQuerySummary report) {
		this.report = report;
	}

	public FormSummary getOrderExtnForm() {
		return orderExtnForm;
	}

	public void setOrderExtnForm(FormSummary orderExtnForm) {
		this.orderExtnForm = orderExtnForm;
	}

	public Boolean getDisableEmailNotifs() {
		return disableEmailNotifs;
	}

	public void setDisableEmailNotifs(Boolean disableEmailNotifs) {
		this.disableEmailNotifs = disableEmailNotifs;
	}

	public String getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(String attachmentType) {
		this.attachmentType = attachmentType;
	}

	public String getOrderItemLabelFormat() {
		return orderItemLabelFormat;
	}

	public void setOrderItemLabelFormat(String orderItemLabelFormat) {
		this.orderItemLabelFormat = orderItemLabelFormat;
	}

	public Map<String, List<String>> getDistributingSites() {
		return distributingSites;
	}

	public void setDistributingSites(Map<String, List<String>> distributingSites) {
		this.distributingSites = distributingSites;
	}

	@JsonIgnore
	public List<Map<String, Object>> getDistributingSitesMapList() {
		List<Map<String, Object>> result = new ArrayList<>();

		for (Map.Entry<String, List<String>> instituteSites : distributingSites.entrySet()) {
			Map<String, Object> distSite = new HashMap<>();
			distSite.put("institute", instituteSites.getKey());
			distSite.put("site", instituteSites.getValue());
			result.add(distSite);
		}

		return result;
	}

	@JsonProperty("distributingSitesMapList")
	public void setDistributingSitesMapList(List<Map<String, Object>> input) {
		Map<String, List<String>> distributingSites = new HashMap<>();

		for (Map<String, Object> site : input) {
			String institute = (String)site.get("institute");
			List<String> sites = (List<String>)site.get("site");
			distributingSites.put(institute, sites);
		}

		setDistributingSites(distributingSites);
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public static DistributionProtocolDetail from(DistributionProtocol dp) {
		DistributionProtocolDetail detail = new DistributionProtocolDetail();
		
		copy(dp, detail);
		detail.setIrbId(dp.getIrbId());
		detail.setPrincipalInvestigator(UserSummary.from(dp.getPrincipalInvestigator()));
		detail.setCoordinators(UserSummary.from(dp.getCoordinators()));
		detail.setActivityStatus(dp.getActivityStatus());
		detail.setDisableEmailNotifs(dp.getDisableEmailNotifs());
		detail.setAttachmentType(dp.getAttachmentType() != null ? dp.getAttachmentType().name() : null);
		detail.setOrderItemLabelFormat(dp.getOrderItemLabelFormat());

		if (dp.getReport() != null) {
			detail.setReport(SavedQuerySummary.fromSavedQuery(dp.getReport()));
		}

		if (dp.getOrderExtnForm() != null) {
			detail.setOrderExtnForm(FormSummary.from(dp.getOrderExtnForm()));
		}
		
		Set<DpDistributionSite> distSites = dp.getDistributingSites();
		detail.setDistributingSites(DpDistributionSite.getInstituteSitesMap(distSites));
		detail.setExtensionDetail(ExtensionDetail.from(dp.getExtension()));

		return detail;
	}

	public static List<DistributionProtocolDetail> from(List<DistributionProtocol> dps) {
		return Utility.nullSafeStream(dps).map(DistributionProtocolDetail::from).collect(Collectors.toList());
	}
}
