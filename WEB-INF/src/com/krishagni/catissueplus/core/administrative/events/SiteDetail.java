package com.krishagni.catissueplus.core.administrative.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import com.krishagni.catissueplus.core.administrative.domain.Site;
import com.krishagni.catissueplus.core.common.ListenAttributeChanges;
import com.krishagni.catissueplus.core.common.events.UserSummary;
import com.krishagni.catissueplus.core.de.events.ExtensionDetail;

@ListenAttributeChanges
public class SiteDetail extends SiteSummary {
	private List<UserSummary> coordinators = new ArrayList<UserSummary>();

	private String address;
	
	private ExtensionDetail extensionDetail;
	
	public List<UserSummary> getCoordinators() {
		return coordinators;
	}

	public void setCoordinators(List<UserSummary> coordinatorCollection) {
		this.coordinators = coordinatorCollection;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public ExtensionDetail getExtensionDetail() {
		return extensionDetail;
	}

	public void setExtensionDetail(ExtensionDetail extensionDetail) {
		this.extensionDetail = extensionDetail;
	}

	public static SiteDetail from(Site site) {
		SiteDetail detail = new SiteDetail();
		copy(site, detail);
		detail.setAddress(site.getAddress());
		detail.setCoordinators(UserSummary.from(site.getCoordinators()));
		detail.setExtensionDetail(ExtensionDetail.from(site.getExtension()));
		return detail;
	}
	
	public static List<SiteDetail> from(Collection<Site> sites) {
		List<SiteDetail> result = new ArrayList<SiteDetail>();
		
		if (CollectionUtils.isEmpty(sites)) {
			return result;
		}
		
		for (Site site : sites) {
			result.add(from(site));
		}
		
		return result;
	}
}
